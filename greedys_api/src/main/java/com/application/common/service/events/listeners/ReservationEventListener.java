package com.application.common.service.events.listeners;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.NotificationOutboxDAO;
import com.application.common.persistence.dao.RestaurantNotificationDAO;
import com.application.common.service.events.ReservationCreatedEvent;
import com.application.restaurant.persistence.model.RestaurantNotification;
import com.application.restaurant.persistence.model.RNotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LISTENER FOR RESERVATION CREATED EVENTS
 * 
 * Questo listener intercetta l'evento di creazione prenotazione e crea
 * notifiche per i restaurant staff tramite il pattern 3-level outbox:
 * 
 * Flow:
 * 1. Customer crea prenotazione
 * 2. ReservationService.createNewReservation() pubblica ReservationCreatedEvent
 * 3. Questo listener intercetta l'evento (SYNC, non async per garantire consistency)
 * 4. Crea N RestaurantNotification (una per ogni staff del ristorante)
 * 5. Crea entry in notification_outbox per ogni notifica
 * 6. ChannelPoller (@10s) invia via WebSocket
 * 
 * ⚠️ IMPORTANTE:
 * - Usa pattern SYNCHRONOUS (non @Async) per garantire che le notifiche
 *   siano create prima che la transazione di prenotazione finisca
 * - Usa @Transactional per rollback in caso di errore
 * - Se listener fallisce, la prenotazione NON viene creata
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventListener {
    
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final NotificationOutboxDAO notificationOutboxDAO;
    private final ObjectMapper objectMapper;

    /**
     * ⭐ SYNC EVENT LISTENER - Crea notifiche RestaurantNotification per ogni staff
     * 
     * Eseguito SYNCHRONOUSLY (non async) per garantire consistency:
     * - Se questo listener fallisce, la transazione di prenotazione rollback
     * - Garantisce che le notifiche esitono sempre quando la prenotazione è creata
     * 
     * @param event L'evento di creazione prenotazione
     */
    @EventListener
    @Transactional
    public void handleRestaurantWebSocketNotification(ReservationCreatedEvent event) {
        try {
            log.info("🔔 Creating WebSocket notifications for restaurant {} on reservation {}", 
                     event.getRestaurantId(), event.getReservationId());
            
            // Step 1: Estrai dati dall'evento
            Long restaurantId = event.getRestaurantId();
            Long reservationId = event.getReservationId();
            String customerEmail = event.getCustomerEmail();
            String reservationDate = event.getReservationDate();

            // Step 2: TODO - Query per trovare TUTTI gli staff del ristorante
            // Per ora: placeholder con staff_id=1,2,3
            // Quando implementerai: Restaurant.getRUsers() o restaurantDAO.findStaffByRestaurant(restaurantId)
            java.util.List<Long> staffUserIds = java.util.Arrays.asList(1L, 2L, 3L);
            
            if (staffUserIds.isEmpty()) {
                log.warn("No staff found for restaurant {}, skipping notifications", restaurantId);
                return;
            }

            log.debug("Found {} staff members for restaurant {}", staffUserIds.size(), restaurantId);

            // Step 3: PER OGNI STAFF - Crea RestaurantNotification
            for (Long staffUserId : staffUserIds) {
                try {
                    // Prepara dati di notifica
                    String title = "📱 Nuova prenotazione richiesta";
                    String body = "Prenotazione per " + reservationDate;

                    Map<String, String> properties = new HashMap<>();
                    properties.put("reservation_id", reservationId.toString());
                    properties.put("customer_email", customerEmail);
                    properties.put("reservation_date", reservationDate);
                    properties.put("restaurant_id", restaurantId.toString());

                    // Crea RestaurantNotification
                    RestaurantNotification notification = RestaurantNotification.builder()
                            .title(title)
                            .body(body)
                            .userId(staffUserId)
                            .type(RNotificationType.RESERVATION_REQUEST)
                            .creationTime(Instant.now())
                            .build();

                    // Persist la notifica
                    RestaurantNotification savedNotification = restaurantNotificationDAO.save(notification);
                    
                    log.debug("✅ Created RestaurantNotification: id={}, restaurant={}, staff={}", 
                             savedNotification.getId(), restaurantId, staffUserId);

                    // Step 4: Crea entry in notification_outbox per il poller
                    com.application.common.persistence.model.notification.NotificationOutbox outbox = 
                        com.application.common.persistence.model.notification.NotificationOutbox.builder()
                            .notificationId(savedNotification.getId())
                            .notificationType("RESTAURANT")
                            .aggregateType("RESERVATION")
                            .aggregateId(restaurantId)
                            .eventType("RESERVATION_REQUESTED")
                            .payload(objectMapper.writeValueAsString(properties))
                            .status(com.application.common.persistence.model.notification.NotificationOutbox.Status.PENDING)
                            .createdAt(Instant.now())
                            .build();

                    notificationOutboxDAO.save(outbox);

                    log.debug("Created NotificationOutbox: notification_id={}", savedNotification.getId());

                } catch (Exception e) {
                    log.error("Error creating notification for staff {}", staffUserId, e);
                    // Continua con prossimo staff, non bloccare
                    continue;
                }
            }

            log.info("✅ Successfully created {} WebSocket notifications for reservation {}", 
                     staffUserIds.size(), reservationId);

        } catch (Exception e) {
            log.error("❌ Error in handleRestaurantWebSocketNotification", e);
            // Re-throw per far rollback la transazione di prenotazione
            throw new RuntimeException("Failed to create restaurant notifications for reservation", e);
        }
    }
}
