package com.application.common.service.notification.listener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.CustomerNotificationDAO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.NotificationOutboxDAO;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.common.persistence.model.notification.NotificationOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LISTENER FOR CUSTOMER NOTIFICATIONS
 * 
 * Responsabilità:
 * 1. Ascolta eventi da RabbitMQ
 * 2. Fa idempotency check per processed_by='CUSTOMER_LISTENER'
 * 3. Crea CustomerNotification per il cliente specifico
 * 4. Persiste in notification_outbox
 * 5. Marca evento come processato
 * 
 * ⭐ DIFFERENZA:
 * - Notifiche PERSONALI al cliente
 * - Una sola CustomerNotification per evento (non multipli)
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Slf4j
@Service
public class CustomerNotificationListener {

    private final CustomerNotificationDAO customerNotificationDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final NotificationOutboxDAO notificationOutboxDAO;
    private final ObjectMapper objectMapper;

    public CustomerNotificationListener(CustomerNotificationDAO customerNotificationDAO,
                                       EventOutboxDAO eventOutboxDAO,
                                       NotificationOutboxDAO notificationOutboxDAO,
                                       ObjectMapper objectMapper) {
        this.customerNotificationDAO = customerNotificationDAO;
        this.eventOutboxDAO = eventOutboxDAO;
        this.notificationOutboxDAO = notificationOutboxDAO;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa un evento per creare CustomerNotification.
     * 
     * @param eventPayload Il payload JSON dell'evento
     */
    @Transactional
    public void onEventReceived(String eventPayload) {
        try {
            log.debug("Customer listener received event: {}", eventPayload);

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
            boolean alreadyProcessed = eventOutboxDAO.existsByEventIdAndProcessedBy(eventId, "CUSTOMER_LISTENER");

            if (alreadyProcessed) {
                log.warn("Event {} already processed by CUSTOMER_LISTENER, skipping", eventId);
                return;
            }

            log.info("Processing event {} for customer notifications", eventId);

            // Step 3: Crea CustomerNotification
            createCustomerNotification(eventId, eventType, eventData);

            // Step 4: Marca come processato
            eventOutboxDAO.updateProcessedBy(eventId, "CUSTOMER_LISTENER", Instant.now());

            log.info("Event {} successfully processed for customer notifications", eventId);

        } catch (Exception e) {
            log.error("Error processing event in CustomerNotificationListener", e);
        }
    }

    /**
     * Crea CustomerNotification per il cliente specifico.
     * 
     * @param eventId L'ID univoco dell'evento
     * @param eventType Il tipo di evento
     * @param eventData I dati dell'evento
     */
    private void createCustomerNotification(String eventId, String eventType, Map<String, Object> eventData) {
        try {
            // Step 1: Estrai il customerId dall'evento
            Object customerIdObj = eventData.get("customerId");
            Long customerId = customerIdObj != null ? ((Number) customerIdObj).longValue() : null;

            if (customerId == null) {
                log.warn("Event {} missing customerId, skipping customer notification", eventId);
                return;
            }

            // Step 2: Crea la notifica per il cliente
            CustomerNotification notification = createNotificationFromEvent(eventType, eventData, customerId);

            if (notification == null) {
                log.warn("Could not create notification for event type: {}", eventType);
                return;
            }

            // Step 3: Persist la notifica
            CustomerNotification savedNotification = customerNotificationDAO.save(notification);

            log.debug("Created CustomerNotification: id={}, customer_id={}", 
                     savedNotification.getId(), customerId);

            // Step 4: Crea entry in notification_outbox
            NotificationOutbox outbox = NotificationOutbox.builder()
                    .notificationId(savedNotification.getId())
                    .notificationType("CUSTOMER")
                    .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                    .aggregateId(customerId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(eventData))
                    .status(NotificationOutbox.Status.PENDING)
                    .retryCount(0)
                    .build();

            notificationOutboxDAO.save(outbox);

            log.debug("Created NotificationOutbox entry: notification_id={}", savedNotification.getId());

        } catch (Exception e) {
            log.error("Error creating customer notification", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Crea una CustomerNotification basata sul tipo di evento.
     * 
     * @param eventType Il tipo di evento
     * @param eventData I dati dell'evento
     * @param customerId L'ID del cliente che riceve la notifica
     * @return Una CustomerNotification pronta per il persist
     */
    private CustomerNotification createNotificationFromEvent(String eventType, Map<String, Object> eventData, 
                                                             Long customerId) {
        String title;
        String body;
        Map<String, String> properties = new HashMap<>();

        switch (eventType) {
            case "RESERVATION_CONFIRMED":
                title = "Prenotazione confermata";
                body = "Grazie! La tua prenotazione al " + eventData.getOrDefault("restaurantName", "ristorante") + 
                       " è confermata per " + eventData.getOrDefault("confirmedTime", "?");
                properties.put("reservation_id", eventData.getOrDefault("reservationId", "").toString());
                properties.put("restaurant_name", eventData.getOrDefault("restaurantName", "").toString());
                break;

            case "RESERVATION_REJECTED":
                title = "Prenotazione non disponibile";
                body = "Purtroppo la prenotazione al " + eventData.getOrDefault("restaurantName", "ristorante") + 
                       " non è disponibile per la data/ora richiesta";
                properties.put("reservation_id", eventData.getOrDefault("reservationId", "").toString());
                break;

            case "RESERVATION_REMINDER":
                title = "Promemoria prenotazione";
                body = "Non dimenticare la tua prenotazione domani al " + 
                       eventData.getOrDefault("restaurantName", "ristorante");
                properties.put("reservation_id", eventData.getOrDefault("reservationId", "").toString());
                break;

            case "PAYMENT_RECEIVED":
                title = "Pagamento confermato";
                body = "Abbiamo ricevuto il tuo pagamento di €" + eventData.getOrDefault("amount", "0");
                properties.put("payment_id", eventData.getOrDefault("paymentId", "").toString());
                break;

            case "REWARD_EARNED":
                title = "Hai guadagnato premi!";
                body = "Complimenti! Hai accumulato " + eventData.getOrDefault("rewardPoints", "0") + " punti";
                properties.put("reward_points", eventData.getOrDefault("rewardPoints", "").toString());
                break;

            default:
                log.warn("Unknown event type for customer: {}", eventType);
                return null;
        }

        return CustomerNotification.builder()
                .title(title)
                .body(body)
                .properties(properties)
                .userId(customerId)
                .userType("CUSTOMER")
                .read(false)
                .sharedRead(false)  // Notifiche personali, non shared
                .creationTime(Instant.now())
                .build();
    }
}
