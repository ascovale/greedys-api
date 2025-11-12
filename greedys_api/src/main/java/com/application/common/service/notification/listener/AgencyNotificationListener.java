package com.application.common.service.notification.listener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.AgencyNotificationDAO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.NotificationOutboxDAO;
import com.application.common.persistence.model.notification.AgencyNotification;
import com.application.common.persistence.model.notification.NotificationOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LISTENER FOR AGENCY NOTIFICATIONS
 * 
 * Responsabilità:
 * 1. Ascolta eventi da RabbitMQ
 * 2. Fa idempotency check per processed_by='AGENCY_LISTENER'
 * 3. Crea AgencyNotification per l'agency user specifico
 * 4. Persiste in notification_outbox
 * 5. Marca evento come processato
 * 
 * ⭐ CASO D'USO:
 * - Notifiche per agency managers/admins
 * - Report su prenotazioni, finanza, performance
 * - Una AgencyNotification per evento (non multipli)
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Slf4j
@Service
public class AgencyNotificationListener {

    private final AgencyNotificationDAO agencyNotificationDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final NotificationOutboxDAO notificationOutboxDAO;
    private final ObjectMapper objectMapper;

    public AgencyNotificationListener(AgencyNotificationDAO agencyNotificationDAO,
                                     EventOutboxDAO eventOutboxDAO,
                                     NotificationOutboxDAO notificationOutboxDAO,
                                     ObjectMapper objectMapper) {
        this.agencyNotificationDAO = agencyNotificationDAO;
        this.eventOutboxDAO = eventOutboxDAO;
        this.notificationOutboxDAO = notificationOutboxDAO;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa un evento per creare AgencyNotification.
     * 
     * @param eventPayload Il payload JSON dell'evento
     */
    @Transactional
    public void onEventReceived(String eventPayload) {
        try {
            log.debug("Agency listener received event: {}", eventPayload);

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
            boolean alreadyProcessed = eventOutboxDAO.existsByEventIdAndProcessedBy(eventId, "AGENCY_LISTENER");

            if (alreadyProcessed) {
                log.warn("Event {} already processed by AGENCY_LISTENER, skipping", eventId);
                return;
            }

            log.info("Processing event {} for agency notifications", eventId);

            // Step 3: Crea AgencyNotification
            createAgencyNotification(eventId, eventType, eventData);

            // Step 4: Marca come processato
            eventOutboxDAO.updateProcessedBy(eventId, "AGENCY_LISTENER", Instant.now());

            log.info("Event {} successfully processed for agency notifications", eventId);

        } catch (Exception e) {
            log.error("Error processing event in AgencyNotificationListener", e);
        }
    }

    /**
     * Crea AgencyNotification per l'agency user specifico.
     * 
     * ⭐ NOTA: Se agencyId/managerId non presenti, non creiamo notifica
     * (a differenza di restaurant/admin che hanno singoli user)
     * 
     * @param eventId L'ID univoco dell'evento
     * @param eventType Il tipo di evento
     * @param eventData I dati dell'evento
     */
    private void createAgencyNotification(String eventId, String eventType, Map<String, Object> eventData) {
        try {
            // Step 1: Estrai agencyUserId/managerId dall'evento
            Object agencyUserIdObj = eventData.get("agencyUserId");
            Long agencyUserId = agencyUserIdObj != null ? ((Number) agencyUserIdObj).longValue() : null;

            if (agencyUserId == null) {
                log.warn("Event {} missing agencyUserId, skipping agency notification", eventId);
                return;
            }

            // Step 2: Crea la notifica per l'agency user
            AgencyNotification notification = createNotificationFromEvent(eventType, eventData, agencyUserId);

            if (notification == null) {
                log.warn("Could not create notification for event type: {}", eventType);
                return;
            }

            // Step 3: Persist la notifica
            AgencyNotification savedNotification = agencyNotificationDAO.save(notification);

            log.debug("Created AgencyNotification: id={}, agency_user_id={}", 
                     savedNotification.getId(), agencyUserId);

            // Step 4: Crea entry in notification_outbox
            NotificationOutbox outbox = NotificationOutbox.builder()
                    .notificationId(savedNotification.getId())
                    .notificationType("AGENCY")
                    .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                    .aggregateId(agencyUserId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(eventData))
                    .status(NotificationOutbox.Status.PENDING)
                    .retryCount(0)
                    .build();

            notificationOutboxDAO.save(outbox);

            log.debug("Created NotificationOutbox entry: notification_id={}", savedNotification.getId());

        } catch (Exception e) {
            log.error("Error creating agency notification", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Crea una AgencyNotification basata sul tipo di evento.
     * 
     * ⭐ EVENT TYPES per agency:
     * - RESERVATION_BULK_IMPORTED: Bulk import di prenotazioni
     * - HIGH_VOLUME_DAY: Alert per giorno con molte prenotazioni
     * - REVENUE_REPORT: Report settimanale di ricavi
     * - CUSTOMER_CHURN_ALERT: Alert per clienti che non prenotano
     * - RESTAURANT_PERFORMANCE: Report performance ristorante
     * 
     * @param eventType Il tipo di evento
     * @param eventData I dati dell'evento
     * @param agencyUserId L'ID dell'agency user che riceve la notifica
     * @return Una AgencyNotification pronta per il persist
     */
    private AgencyNotification createNotificationFromEvent(String eventType, Map<String, Object> eventData, 
                                                           Long agencyUserId) {
        String title;
        String body;
        Map<String, String> properties = new HashMap<>();

        switch (eventType) {
            case "RESERVATION_BULK_IMPORTED":
                title = "Prenotazioni importate con successo";
                body = "Sono state importate " + eventData.getOrDefault("importCount", "0") + 
                       " prenotazioni dal file";
                properties.put("import_count", eventData.getOrDefault("importCount", "").toString());
                properties.put("file_name", eventData.getOrDefault("fileName", "").toString());
                break;

            case "HIGH_VOLUME_DAY":
                title = "Alert: Alto volume di prenotazioni";
                body = "Il " + eventData.getOrDefault("date", "?") + " sono previste " + 
                       eventData.getOrDefault("reservationCount", "0") + " prenotazioni";
                properties.put("date", eventData.getOrDefault("date", "").toString());
                properties.put("reservation_count", eventData.getOrDefault("reservationCount", "").toString());
                break;

            case "REVENUE_REPORT":
                title = "Report ricavi settimanale";
                body = "Ricavi questa settimana: €" + eventData.getOrDefault("totalRevenue", "0");
                properties.put("total_revenue", eventData.getOrDefault("totalRevenue", "").toString());
                properties.put("week_of", eventData.getOrDefault("weekOf", "").toString());
                break;

            case "CUSTOMER_CHURN_ALERT":
                title = "Alert: Clienti inattivi";
                body = eventData.getOrDefault("churnCount", "0") + 
                       " clienti non hanno prenotato negli ultimi 30 giorni";
                properties.put("churn_count", eventData.getOrDefault("churnCount", "").toString());
                properties.put("period_days", "30");
                break;

            case "RESTAURANT_PERFORMANCE":
                title = "Report performance ristorante";
                body = "Il ristorante " + eventData.getOrDefault("restaurantName", "?") + 
                       " ha rating medio di " + eventData.getOrDefault("avgRating", "0");
                properties.put("restaurant_id", eventData.getOrDefault("restaurantId", "").toString());
                properties.put("restaurant_name", eventData.getOrDefault("restaurantName", "").toString());
                properties.put("avg_rating", eventData.getOrDefault("avgRating", "").toString());
                break;

            case "SYSTEM_ALERT":
                title = "Alert di sistema";
                body = (String) eventData.getOrDefault("message", "Alert di sistema");
                properties.put("severity", eventData.getOrDefault("severity", "INFO").toString());
                break;

            default:
                log.warn("Unknown event type for agency: {}", eventType);
                return null;
        }

        return AgencyNotification.builder()
                .title(title)
                .body(body)
                .properties(properties)
                .userId(agencyUserId)
                .userType("AGENCY_USER")
                .read(false)
                .sharedRead(false)  // Notifiche personali per ogni manager
                .creationTime(Instant.now())
                .build();
    }
}
