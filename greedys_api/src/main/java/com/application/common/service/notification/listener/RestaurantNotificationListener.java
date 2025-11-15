package com.application.common.service.notification.listener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.NotificationOutboxDAO;
import com.application.common.persistence.dao.RestaurantNotificationDAO;
import com.application.common.persistence.model.notification.NotificationOutbox;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.persistence.model.RestaurantNotification;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LISTENER FOR RESTAURANT NOTIFICATIONS
 * 
 * Responsabilità:
 * 1. Ascolta eventi da RabbitMQ
 * 2. Fa idempotency check per processed_by='RESTAURANT_LISTENER'
 * 3. Crea N RestaurantNotification (una per ogni staff di ristorante)
 * 4. Persiste in notification_outbox
 * 5. Marca evento come processato
 * 
 * ⭐ DIFFERENZA DA AdminListener:
 * - AdminListener: notifiche PER ADMIN (tutti gli admin del sistema)
 * - RestaurantListener: notifiche PER STAFF RISTORANTE (solo staff del ristorante interessato)
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Slf4j
@Service
public class RestaurantNotificationListener {

    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final NotificationOutboxDAO notificationOutboxDAO;
    private final RUserDAO rUserDAO;
    private final ObjectMapper objectMapper;

    public RestaurantNotificationListener(RestaurantNotificationDAO restaurantNotificationDAO,
                                        EventOutboxDAO eventOutboxDAO,
                                        NotificationOutboxDAO notificationOutboxDAO,
                                        RUserDAO rUserDAO,
                                        ObjectMapper objectMapper) {
        this.restaurantNotificationDAO = restaurantNotificationDAO;
        this.eventOutboxDAO = eventOutboxDAO;
        this.notificationOutboxDAO = notificationOutboxDAO;
        this.rUserDAO = rUserDAO;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa un evento per creare RestaurantNotification.
     * 
     * @param eventPayload Il payload JSON dell'evento
     */
    @Transactional
    public void onEventReceived(String eventPayload) {
        try {
            log.debug("Restaurant listener received event: {}", eventPayload);

            // Step 1: Parse il payload
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = objectMapper.readValue(eventPayload, Map.class);
            String eventId = (String) eventData.get("eventId");
            String eventType = (String) eventData.get("eventType");

            if (eventId == null) {
                log.warn("Event payload missing eventId: {}", eventPayload);
                return;
            }

            // Step 2: ⭐ IDEMPOTENCY CHECK
            boolean alreadyProcessed = eventOutboxDAO.existsByEventIdAndProcessedBy(eventId, "RESTAURANT_LISTENER");

            if (alreadyProcessed) {
                log.warn("Event {} already processed by RESTAURANT_LISTENER, skipping", eventId);
                return;
            }

            log.info("Processing event {} for restaurant notifications", eventId);

            // Step 3: Crea RestaurantNotification per ogni staff
            createRestaurantNotifications(eventId, eventType, eventData);

            // Step 4: Marca come processato
            eventOutboxDAO.updateProcessedBy(eventId, "RESTAURANT_LISTENER", Instant.now());

            log.info("Event {} successfully processed for restaurant notifications", eventId);

        } catch (Exception e) {
            log.error("Error processing event in RestaurantNotificationListener", e);
        }
    }

    /**
     * Crea RestaurantNotification per i relativi staff.
     * 
     * @param eventId L'ID univoco dell'evento
     * @param eventType Il tipo di evento
     * @param eventData I dati dell'evento
     */
    private void createRestaurantNotifications(String eventId, String eventType, Map<String, Object> eventData) {
        try {
            // Step 1: Estrai il restaurantId dall'evento
            Object restaurantIdObj = eventData.get("restaurantId");
            Long restaurantId = restaurantIdObj != null ? ((Number) restaurantIdObj).longValue() : null;

            if (restaurantId == null) {
                log.warn("Event {} missing restaurantId, skipping restaurant notifications", eventId);
                return;
            }

            // Step 2: ⭐ Query per trovare tutti gli staff di questo ristorante
            java.util.Collection<RUser> staffList = rUserDAO.findByRestaurantId(restaurantId);

            if (staffList == null || staffList.isEmpty()) {
                log.warn("No staff found for restaurant {}, skipping notifications", restaurantId);
                return;
            }

            log.debug("Found {} staff members for restaurant {}", staffList.size(), restaurantId);

            // Step 3: ⭐ Per ogni staff, crea una RestaurantNotification
            for (RUser staff : staffList) {
                try {
                    RestaurantNotification notification = createNotificationFromEvent(eventType, eventData, restaurantId, staff.getId());

                    if (notification == null) {
                        log.warn("Could not create notification for event type: {}", eventType);
                        continue;
                    }

                    // Step 4: Persist la notifica
                    RestaurantNotification savedNotification = restaurantNotificationDAO.save(notification);

                    log.debug("Created RestaurantNotification: id={}, restaurant={}, staff={}", 
                             savedNotification.getId(), restaurantId, staff.getId());

                    // Step 5: Crea entry in notification_outbox
                    NotificationOutbox outbox = NotificationOutbox.builder()
                            .notificationId(savedNotification.getId())
                            .notificationType("RESTAURANT")
                            .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                            .aggregateId(restaurantId)
                            .eventType(eventType)
                            .payload(objectMapper.writeValueAsString(eventData))
                            .status(NotificationOutbox.Status.PENDING)
                            .retryCount(0)
                            .build();

                    notificationOutboxDAO.save(outbox);

                    log.debug("Created NotificationOutbox entry: notification_id={}, staff_id={}", 
                             savedNotification.getId(), staff.getId());

                } catch (Exception e) {
                    log.error("Error creating notification for staff {}", staff.getId(), e);
                    // Continue with next staff instead of failing entire event
                    continue;
                }
            }

            log.info("Successfully created {} notifications for event {} in restaurant {}", 
                    staffList.size(), eventId, restaurantId);

        } catch (Exception e) {
            log.error("Error creating restaurant notifications", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Crea una RestaurantNotification basata sul tipo di evento.
     * 
     * @param eventType Il tipo di evento
     * @param eventData I dati dell'evento
     * @param restaurantId L'ID del ristorante
     * @param staffUserId L'ID dello staff che riceve la notifica
     * @return Una RestaurantNotification pronta per il persist
     */
    private RestaurantNotification createNotificationFromEvent(String eventType, Map<String, Object> eventData, 
                                                               Long restaurantId, Long staffUserId) {
        String title;
        String body;
        Map<String, String> properties = new HashMap<>();

        switch (eventType) {
            case "RESERVATION_REQUESTED":
                title = "Nuova prenotazione";
                body = "Tavolo per " + eventData.getOrDefault("numberOfPeople", "?") + 
                       " persone alle " + eventData.getOrDefault("requestedTime", "?");
                properties.put("reservation_id", eventData.getOrDefault("reservationId", "").toString());
                properties.put("customer_name", eventData.getOrDefault("customerName", "").toString());
                break;

            case "RESERVATION_CONFIRMED":
                title = "Prenotazione confermata";
                body = "La prenotazione di " + eventData.getOrDefault("customerName", "Cliente") + " è confermata";
                properties.put("reservation_id", eventData.getOrDefault("reservationId", "").toString());
                break;

            case "RESERVATION_CANCELLED":
                title = "Prenotazione cancellata";
                body = "La prenotazione di " + eventData.getOrDefault("customerName", "Cliente") + " è stata cancellata";
                properties.put("reservation_id", eventData.getOrDefault("reservationId", "").toString());
                break;

            default:
                log.warn("Unknown event type for restaurant: {}", eventType);
                return null;
        }

        return RestaurantNotification.builder()
                .title(title)
                .body(body)
                .properties(properties)
                .userId(staffUserId)
                .userType("RESTAURANT_USER")
                .read(false)
                .sharedRead(true)  // Primo staff che agisce, tutti vedono
                .creationTime(Instant.now())
                .build();
    }
}
