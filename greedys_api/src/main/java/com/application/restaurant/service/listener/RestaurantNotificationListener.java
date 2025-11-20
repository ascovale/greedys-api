package com.application.restaurant.service.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.restaurant.persistence.dao.RestaurantUserNotificationDAO;
import com.application.restaurant.persistence.model.RestaurantUserNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.common.persistence.model.notification.NotificationPriority;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RABBITLISTENER PER RESTAURANT STAFF NOTIFICATIONS
 * 
 * Ascolta sulla queue: notification.restaurant
 * 
 * FLUSSO:
 * 1. RabbitMQ invia message su queue notification.restaurant
 * 2. Listener riceve message (MANUAL ACK)
 * 3. Verifica idempotency: existsByEventId(eventId)
 *    - Se esiste → skip (retry duplicato), ACK
 *    - Se non esiste → procedi
 * 4. Disaggrega:
 *    - Per ogni recipient (restaurant staff)
 *    - Per ogni enabled channel (WEBSOCKET, EMAIL, PUSH, SMS)
 *    - Crea RestaurantUserNotification row
 * 5. Salva disaggregazioni nel DB
 * 6. ACK message (conferma a RabbitMQ)
 * 
 * ⭐ IMPORTANTE: MANUAL ACK
 * - ACK solo dopo SUCCESSO (notifiche salvate nel DB)
 * - NACK se errore → RabbitMQ riqueue message
 * 
 * ⭐ TRANSACTIONAL: @Transactional
 * - Tutte le disaggregazioni salvate ATOMICAMENTE
 * - Se errore during save → rollback + NACK + requeue
 * 
 * ⭐ RETRYABLE: @Retryable (maxAttempts=3, delay=1000ms)
 * - Retry automatico se errore transiente
 * - Dopo 3 tentativi → fallisce → NACK
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantNotificationListener {

    private final RestaurantUserNotificationDAO notificationDAO;
    // TODO: Iniettare RestaurantStaffService e RestaurantUserPreferencesService quando esistono
    // private final RestaurantStaffService staffService;
    // private final RestaurantUserPreferencesService prefsService;

    /**
     * ⭐ MAIN LISTENER METHOD
     * 
     * @RabbitListener(queues="notification.restaurant", ackMode=MANUAL)
     * - Ascolta su queue notification.restaurant
     * - MANUAL ACK: NOI controlliamo quando ACK/NACK
     * 
     * @Transactional
     * - Tutte le persist sono atomiche
     * 
     * @Retryable(maxAttempts=3, delay=1000ms)
     * - Retry automatico su errore transiente
     * 
     * @Payload message: Map con dati evento
     *   {
     *     aggregate_type: "CUSTOMER" (chi ha agito),
     *     event_type: "RESERVATION_REQUESTED",
     *     restaurant_id: 5,
     *     payload: {...},
     *     event_id: "RES-REQ-12345-2025-01-20T10:30:00Z"
     *   }
     * 
     * @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Per MANUAL ACK/NACK
     * @Param channel: RabbitMQ Channel per ACK/NACK
     */
    @RabbitListener(
        queues = "notification.restaurant",
        ackMode = "MANUAL"
    )
    @Transactional
    @Retryable(
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 1000)
    )
    public void onNotificationMessage(
        @Payload Map<String, Object> message,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        try {
            log.info("📩 RestaurantNotificationListener: received message on queue notification.restaurant");
            
            // ⭐ ESTRAI DATI
            String eventId = (String) message.get("event_id");
            String eventType = (String) message.get("event_type");
            String aggregateType = (String) message.get("aggregate_type");
            Long restaurantId = ((Number) message.get("restaurant_id")).longValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            
            log.info("🔍 Processing event: eventId={}, eventType={}, aggregateType={}, restaurantId={}", 
                eventId, eventType, aggregateType, restaurantId);
            
            // ⭐ IDEMPOTENCY CHECK
            if (notificationDAO.existsByEventId(eventId)) {
                log.warn("⚠️  Duplicate eventId detected: {}. Skipping (already processed)", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // ⭐ CARICA SETTINGS (regole per RESTAURANT_USER type)
            // Questo determina se readByAll=true/false per questo eventType
            RestaurantGroupSettings settings = loadGroupSettings(eventType);
            
            // ⭐ CARICA RECIPIENTS (staff che ricevono)
            // Per RESERVATION_REQUESTED:
            // - Se è broadcast (readByAll=true) → TUTTI staff
            // - Se è unicast (readByAll=false) → staff specifico da payload
            java.util.List<Long> recipientStaffIds = loadRecipients(restaurantId, eventType, payload);
            
            // ⭐ CARICA CHANNELS ENABLED (per ogni staff)
            // Quali channels ha enabled questo staff? [WEBSOCKET, EMAIL, PUSH]
            // TODO: Iniettare RestaurantUserPreferencesService
            Map<Long, java.util.List<NotificationChannel>> staffChannels = getEnabledChannelsStub(recipientStaffIds);
            
            // ⭐ DISAGGREGA: Per ogni (staff × channel), crea 1 row
            int disaggregationCount = 0;
            for (Long staffId : recipientStaffIds) {
                java.util.List<NotificationChannel> enabledChannels = staffChannels.getOrDefault(staffId, java.util.List.of());
                
                for (NotificationChannel channel_enum : enabledChannels) {
                    // Crea unique eventId per questa disaggregazione
                    String disaggregatedEventId = generateDisaggregatedEventId(eventId, staffId, channel_enum);
                    
                    @SuppressWarnings("unchecked")
                    Map<String, String> props = (Map<String, String>) payload.getOrDefault("properties", new HashMap<>());
                    
                    // Crea RestaurantUserNotification row
                    RestaurantUserNotification notification = RestaurantUserNotification.builder()
                        .eventId(disaggregatedEventId)
                        .userId(staffId)
                        .restaurantId(restaurantId)
                        .channel(channel_enum)
                        .status(DeliveryStatus.PENDING)
                        .readByAll(settings.isReadByAll())  // true per RESERVATION_REQUESTED, false per TASK_ASSIGNMENT
                        .priority(settings.getPriority())   // HIGH per urgent, NORMAL altrimenti
                        .title((String) payload.get("title"))
                        .body((String) payload.get("body"))
                        .eventType(eventType)
                        .aggregateType(aggregateType)
                        .properties(props)
                        .build();
                    
                    notificationDAO.save(notification);
                    disaggregationCount++;
                }
            }
            
            log.info("✅ Successfully created {} disaggregated notifications for eventId={}", disaggregationCount, eventId);
            
            // ⭐ MANUAL ACK: Solo dopo successo
            channel.basicAck(deliveryTag, false);
            log.info("✔️  Message ACK'd successfully");
            
        } catch (Exception e) {
            log.error("❌ Error processing notification message: {}", e.getMessage(), e);
            try {
                // ⭐ MANUAL NACK: Su errore, requeue
                channel.basicNack(deliveryTag, false, true);
                log.info("❌ Message NACK'd and requeued");
            } catch (Exception nackError) {
                log.error("Failed to NACK message", nackError);
            }
            // Rilanciamo per @Retryable
            throw new RuntimeException("Failed to process notification: " + e.getMessage(), e);
        }
    }

    /**
     * ⭐ LOAD GROUP SETTINGS (RESTAURANT_USER type rules)
     * 
     * Legge le regole per il tipo utente RESTAURANT_USER:
     * - Quali eventType hanno shared read (readByAll=true)?
     * - Quale priority per questo eventType?
     * 
     * Esempio:
     * - RESERVATION_REQUESTED → readByAll=true, priority=HIGH
     * - NEW_ORDER → readByAll=true, priority=HIGH
     * - TASK_ASSIGNMENT → readByAll=false, priority=NORMAL
     * - KITCHEN_ALERT → readByAll=true, priority=HIGH
     * 
     * @param eventType Event type (es: RESERVATION_REQUESTED)
     * @return RestaurantGroupSettings con readByAll e priority
     */
    private RestaurantGroupSettings loadGroupSettings(String eventType) {
        // TODO: Carica da database (notification_group_settings)
        // Per ora, hardcoded logic:
        
        RestaurantGroupSettings settings = new RestaurantGroupSettings();
        
        switch (eventType) {
            case "RESERVATION_REQUESTED":
            case "NEW_ORDER":
            case "KITCHEN_ALERT":
                settings.setReadByAll(true);
                settings.setPriority(NotificationPriority.HIGH);
                break;
                
            case "TASK_ASSIGNMENT":
            case "DIRECT_MESSAGE":
                settings.setReadByAll(false);
                settings.setPriority(NotificationPriority.NORMAL);
                break;
                
            default:
                settings.setReadByAll(false);
                settings.setPriority(NotificationPriority.NORMAL);
        }
        
        return settings;
    }

    /**
     * If readByAll=true (broadcast):
     * - TUTTI gli staff del restaurant
     * - Es: RESERVATION_REQUESTED → notifica a 10 staff
     * 
     * Se readByAll=false (unicast):
     * - Staff specifico da payload o context
     * - Es: TASK_ASSIGNMENT → solo staff con task_assigned_id
     * 
     * @param restaurantId Restaurant
     * @param eventType Event type
     * @param payload Event payload
     * @return List di staff IDs
     */
    private java.util.List<Long> loadRecipients(Long restaurantId, String eventType, Map<String, Object> payload) {
        switch (eventType) {
            case "RESERVATION_REQUESTED":
            case "NEW_ORDER":
            case "KITCHEN_ALERT":
                // BROADCAST: tutti staff del restaurant
                // TODO: Iniettare RestaurantStaffService
                return findActiveStaffByRestaurantIdStub(restaurantId);
                
            case "TASK_ASSIGNMENT":
                // UNICAST: solo staff a cui è assegnato
                Long assignedStaffId = ((Number) payload.get("assigned_staff_id")).longValue();
                return java.util.List.of(assignedStaffId);
                
            case "DIRECT_MESSAGE":
                // UNICAST: staff specifico
                Long recipientStaffId = ((Number) payload.get("recipient_staff_id")).longValue();
                return java.util.List.of(recipientStaffId);
                
            default:
                return java.util.List.of();
        }
    }

    /**
     * ⭐ GENERATE DISAGGREGATED EVENT ID
     * 
     * Crea unique eventId per ogni (staff × channel) combo.
     * 
     * Format: {eventId}_{staffId}_{channel}_{timestamp}
     * 
     * Esempio:
     * - Original: "RES-REQ-12345-2025-01-20T10:30:00Z"
     * - Staff 50, WEBSOCKET:
     *   "RES-REQ-12345-2025-01-20T10:30:00Z_50_WEBSOCKET_1705750200000"
     * 
     * Usato per idempotency: se RabbitMQ ritenta, stesso eventId → skip
     * 
     * @param eventId Original event ID
     * @param staffId Staff user ID
     * @param channel Channel
     * @return Disaggregated event ID
     */
    private String generateDisaggregatedEventId(String eventId, Long staffId, NotificationChannel channel) {
        return String.format("%s_%d_%s_%d", 
            eventId, 
            staffId, 
            channel.name(), 
            System.currentTimeMillis()
        );
    }

    /**
     * ⭐ STUB: Get enabled channels per staff
     * TODO: Sostituire con RestaurantUserPreferencesService.getEnabledChannels()
     */
    private Map<Long, java.util.List<NotificationChannel>> getEnabledChannelsStub(java.util.List<Long> staffIds) {
        Map<Long, java.util.List<NotificationChannel>> result = new java.util.HashMap<>();
        for (Long staffId : staffIds) {
            // Default: WEBSOCKET + EMAIL
            result.put(staffId, java.util.List.of(NotificationChannel.WEBSOCKET, NotificationChannel.EMAIL));
        }
        return result;
    }

    /**
     * ⭐ STUB: Find active staff by restaurant
     * TODO: Sostituire con RestaurantStaffService.findActiveStaffByRestaurantId()
     */
    private java.util.List<Long> findActiveStaffByRestaurantIdStub(Long restaurantId) {
        // Placeholder: ritorna lista vuota per ora
        return java.util.List.of();
    }

    /**
     * Helper class per group settings
     */
    public static class RestaurantGroupSettings {
        private boolean readByAll;
        private NotificationPriority priority;
        
        public boolean isReadByAll() { return readByAll; }
        public void setReadByAll(boolean readByAll) { this.readByAll = readByAll; }
        
        public NotificationPriority getPriority() { return priority; }
        public void setPriority(NotificationPriority priority) { this.priority = priority; }
    }
}
