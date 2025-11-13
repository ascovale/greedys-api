package com.application.common.service.notification.listener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.NotificationOutboxDAO;
import com.application.common.persistence.model.notification.NotificationOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LISTENER FOR ADMIN NOTIFICATIONS
 * 
 * Responsabilità:
 * 1. Ascolta eventi da RabbitMQ (exchange: event-stream)
 * 2. Fa idempotency check: è già stato processato? Se sì, exit
 * 3. Crea N AdminNotification (una per ogni admin)
 * 4. Persiste in notification_outbox per il ChannelPoller
 * 5. Marca evento come processato (processed_by='ADMIN_LISTENER')
 * 
 * ⭐ IDEMPOTENCY PATTERN:
 * 
 * FLOW:
 * [T0] RabbitMQ pubblica evento su event-stream
 * [T1] Listener riceve evento
 * [T2] SELECT * FROM event_outbox WHERE event_id=? AND processed_by='ADMIN_LISTENER'
 * [T3a] Se esiste: SKIP (già processato)
 * [T3b] Se non esiste: PROCESS
 *       - CREATE AdminNotification per ogni admin
 *       - INSERT notification_outbox per ogni notifica
 *       - UPDATE event_outbox SET processed_by='ADMIN_LISTENER'
 * 
 * ✅ AT-LEAST-ONCE:
 * - Se listener muore dopo aver creato notifiche ma prima di UPDATE: riprova e crea duplicati
 *   SOLUZIONE: idempotency check impedisce this
 * 
 * ⚠️ NOTA IMPORTANTE:
 * - Un SINGOLO evento può essere processato da MULTIPLI listener
 * - AdminListener crea AdminNotification
 * - RestaurantListener crea RestaurantNotification
 * - CustomerListener crea CustomerNotification
 * - Ogni listener ha suo proprio processed_by
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Admin Notification Listener)
 */
@Slf4j
@Service
@Profile("!dev-minimal")  // Disabilitato in dev-minimal perché AdminNotification non estende ANotification
@Transactional
public class AdminNotificationListener {

    private final EventOutboxDAO eventOutboxDAO;
    private final NotificationOutboxDAO notificationOutboxDAO;
    private final ObjectMapper objectMapper;

    public AdminNotificationListener(EventOutboxDAO eventOutboxDAO,
                                    NotificationOutboxDAO notificationOutboxDAO,
                                    ObjectMapper objectMapper) {
        this.eventOutboxDAO = eventOutboxDAO;
        this.notificationOutboxDAO = notificationOutboxDAO;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa un evento per creare AdminNotification.
     * 
     * @RabbitListener(queues = "event-stream-queue") sarà aggiunto in RabbitMQ config
     * 
     * @param eventPayload Il payload JSON dell'evento da RabbitMQ
     */
    @Transactional
    public void onEventReceived(String eventPayload) {
        try {
            log.debug("Admin listener received event: {}", eventPayload);

            // Step 1: Parse il payload per estrarre event_id e dettagli
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = objectMapper.readValue(eventPayload, Map.class);
            String eventId = (String) eventData.get("eventId");
            String eventType = (String) eventData.get("eventType");

            if (eventId == null) {
                log.warn("Event payload missing eventId: {}", eventPayload);
                return;
            }

            // Step 2: ⭐ IDEMPOTENCY CHECK
            // Verifica se questo evento è già stato processato da ADMIN_LISTENER
            boolean alreadyProcessed = eventOutboxDAO.existsByEventIdAndProcessedBy(eventId, "ADMIN_LISTENER");

            if (alreadyProcessed) {
                log.warn("Event {} already processed by ADMIN_LISTENER, skipping", eventId);
                return;  // Skip, non fare nulla
            }

            log.info("Processing event {} for admin notifications", eventId);

            // Step 3: Crea AdminNotification per ogni admin
            // TODO: Implementare la logica di creazione notifiche
            // - Leggi tutti gli admin del sistema
            // - Per ogni admin: CREATE AdminNotification
            // - Persist in adminNotificationDAO
            createAdminNotifications(eventId, eventType, eventData);

            // Step 4: ⭐ MARCA COME PROCESSATO
            // Aggiorna event_outbox con processed_by per l'idempotency check al prossimo tentativo
            eventOutboxDAO.updateProcessedBy(eventId, "ADMIN_LISTENER", Instant.now());

            log.info("Event {} successfully processed for admin notifications", eventId);

        } catch (Exception e) {
            log.error("Error processing event in AdminNotificationListener", e);
            // Non re-throwing: RabbitMQ riproverà naturalmente al prossimo ciclo poller
        }
    }

    /**
     * Crea AdminNotification per i relativi admin.
     * 
     * TODO: Questa è una placeholder - implementa secondo il tuo dominio
     * 
     * Logica suggerita:
     * - Estrai informazioni dall'evento (reservation, customer, etc)
     * - Query admin table per trovare admin responsabili
     * - Per ogni admin: CREATE AdminNotification
     * - INSERT in notification_outbox
     * 
     * @param eventId L'ID univoco dell'evento
     * @param eventType Il tipo di evento (RESERVATION_REQUESTED, etc)
     * @param eventData I dati dell'evento in formato Map
     */
    private void createAdminNotifications(String eventId, String eventType, Map<String, Object> eventData) {
        try {
            // Step 1: Estrai dati rilevanti dall'evento
            Object restaurantIdObj = eventData.get("restaurantId");
            Long restaurantId = restaurantIdObj != null ? ((Number) restaurantIdObj).longValue() : null;

            // Step 2: Crea notifica generica in base al tipo di evento
            Map<String, Object> notification = createNotificationFromEvent(eventType, eventData);

            if (notification == null) {
                log.warn("Could not create notification for event type: {}", eventType);
                return;
            }

            // Step 3: Persist la notifica in notification_outbox
            log.debug("Created AdminNotification: event_type={}", eventType);

            // Step 4: Crea entry in notification_outbox per il ChannelPoller
            NotificationOutbox outbox = NotificationOutbox.builder()
                    .notificationId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                    .notificationType("ADMIN")
                    .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                    .aggregateId(restaurantId != null ? restaurantId : 0L)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(eventData))
                    .status(NotificationOutbox.Status.PENDING)
                    .retryCount(0)
                    .build();

            notificationOutboxDAO.save(outbox);

            log.debug("Created NotificationOutbox entry: notification_id={}, status=PENDING", 
                     eventId);

        } catch (Exception e) {
            log.error("Error creating admin notifications", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Crea una AdminNotification basata sul tipo di evento.
     * 
     * TODO: Implementa secondo il tuo dominio
     * 
     * Esempi:
     * - RESERVATION_REQUESTED: "Nuova prenotazione richiesta", "Da [Customer] per [Restaurant]"
     * - CUSTOMER_REGISTERED: "Nuovo cliente registrato", "[Name] si è iscritto"
     * - PAYMENT_RECEIVED: "Pagamento ricevuto", "€100 da prenotazione #123"
     * 
     * @param eventType Il tipo di evento
     * @param eventData I dati dell'evento
     * @return Una map con i dati della notifica pronta per il persist
     */
    private Map<String, Object> createNotificationFromEvent(String eventType, Map<String, Object> eventData) {
        String title;
        String body;
        Map<String, String> properties = new HashMap<>();

        switch (eventType) {
            case "RESERVATION_REQUESTED":
                title = "Nuova prenotazione richiesta";
                body = "Da " + eventData.getOrDefault("customerName", "Cliente") + 
                       " per " + eventData.getOrDefault("restaurantName", "Ristorante");
                properties.put("reservation_id", eventData.getOrDefault("reservationId", "").toString());
                break;

            case "CUSTOMER_REGISTERED":
                title = "Nuovo cliente registrato";
                body = eventData.getOrDefault("customerName", "Cliente") + " si è iscritto";
                properties.put("customer_id", eventData.getOrDefault("customerId", "").toString());
                break;

            case "PAYMENT_RECEIVED":
                title = "Pagamento ricevuto";
                body = "€" + eventData.getOrDefault("amount", "0") + " da prenotazione";
                properties.put("payment_id", eventData.getOrDefault("paymentId", "").toString());
                break;

            default:
                log.warn("Unknown event type: {}", eventType);
                return null;
        }

        // Costruisci una map con i dati della notifica
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("properties", properties);
        notification.put("userType", "ADMIN_USER");
        notification.put("read", false);
        notification.put("creationTime", Instant.now());

        return notification;
    }
}
