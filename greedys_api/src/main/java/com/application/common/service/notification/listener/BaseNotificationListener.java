package com.application.common.service.notification.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.notification.ANotification;
import com.application.common.service.notification.dto.NotificationEventPayloadDTO;
import com.application.common.service.notification.orchestrator.NotificationOrchestrator;
import com.application.common.service.notification.orchestrator.NotificationOrchestratorFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ ABSTRACT BASE CLASS FOR ALL NOTIFICATION LISTENERS
 * 
 * Estrae la logica comune dai 4 listener specifici (Restaurant, Customer, Agency, Admin).
 * 
 * RESPONSABILITÀ:
 * 1. Parse message from RabbitMQ
 * 2. Idempotency check
 * 3. Delegate disaggregation to type-specific NotificationOrchestrator
 * 4. Save disaggregated notifications to DB
 * 5. Handle ACK/NACK and transaction management
 * 
 * FLUSSO:
 * [RabbitMQ Message]
 *     ↓
 * [Parse Message] → extract eventId, eventType, userId, payload
 *     ↓
 * [Idempotency Check] → if exists, basicAck and skip
 *     ↓
 * [Get Orchestrator] → NotificationOrchestratorFactory.getOrchestrator(userType)
 *     ↓
 * [Disaggregate] → orchestrator.disaggregateAndProcess(message)
 *                  Returns: List<T extends ANotification>
 *     ↓
 * [Persist] → Save all disaggregated notifications to DB
 *     ↓
 * [ACK] → Confirm to RabbitMQ
 * 
 * INHERITANCE MODEL:
 * BaseNotificationListener<T>
 * ├── RestaurantNotificationListener extends BaseNotificationListener<RestaurantUserNotification>
 * ├── CustomerNotificationListener extends BaseNotificationListener<CustomerNotification>
 * ├── AgencyUserNotificationListener extends BaseNotificationListener<AgencyUserNotification>
 * └── AdminNotificationListener extends BaseNotificationListener<AdminNotification>
 * 
 * GENERICS:
 * T = Notification model class (RestaurantUserNotification, CustomerNotification, etc)
 * D = DAO interface for persisting notifications
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Refactored from 4 listener classes)
 */
@Slf4j
public abstract class BaseNotificationListener<T extends ANotification> {

    /**
     * Template method for processing notification messages.
     * Called by @RabbitListener in subclasses.
     * 
     * @param payload NotificationEventPayloadDTO deserialized from JSON
     * @param deliveryTag RabbitMQ delivery tag for ACK/NACK
     * @param channel RabbitMQ channel
     */
    @Transactional
    protected void processNotificationMessage(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        try {
            log.info("📩 {}: received message on queue", this.getClass().getSimpleName());
            
            // ⭐ STEP 1: Extract from DTO
            String eventId = payload.getEventId();
            String eventType = payload.getEventType();
            String recipientType = payload.getRecipientType();
            Long recipientId = payload.getRecipientId();
            Map<String, Object> data = payload.getData();
            
            log.info("🔍 Processing event: eventId={}, eventType={}, recipientType={}, recipientId={}", 
                eventId, eventType, recipientType, recipientId);
            
            // ⭐ STEP 2: Idempotency check
            if (existsByEventId(eventId)) {
                log.warn("⚠️  Duplicate eventId detected: {}. Skipping (already processed)", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // ⭐ STEP 3: Get type-specific orchestrator
            // Convert DTO back to Map for backward compatibility with orchestrator interface
            // ⚠️ IMPORTANT: Use snake_case keys to match orchestrator expectations
            Map<String, Object> message = new HashMap<>();
            message.put("event_id", eventId);
            message.put("event_type", eventType);
            message.put("recipient_type", recipientType);
            message.put("recipient_id", recipientId);
            message.put("data", data);
            
            // ⭐ Let subclass enrich the message with type-specific fields
            enrichMessageWithTypeSpecificFields(message, payload);
            
            NotificationOrchestrator<T> orchestrator = getTypeSpecificOrchestrator(message);
            
            // ⭐ STEP 4: Disaggregate - returns list of notification records
            List<T> disaggregatedNotifications = orchestrator.disaggregateAndProcess(message);
            
            log.info("✅ Orchestrator disaggregated {} notifications for eventId={}", 
                disaggregatedNotifications.size(), eventId);
            
            // ⭐ STEP 5: Persist all disaggregated notifications
            // ⭐ STEP 6: Immediately attempt WebSocket delivery (best-effort, no retry)
            // ⭐ LEVEL 2 IDEMPOTENCY: Catch DataIntegrityViolationException for duplicate notifications
            int sentCount = 0;
            for (T notification : disaggregatedNotifications) {
                try {
                    persistNotification(notification);
                    
                    // ⭐ SYNCHRONOUS WEBSOCKET SEND: happens immediately after DB persist
                    // If client is online → delivery succeeds (real-time)
                    // If client offline → send fails silently, no retry (best-effort)
                    // If service crashes between persist and send → client doesn't receive (acceptable)
                    attemptWebSocketSend(notification);
                    sentCount++;
                    
                } catch (DataIntegrityViolationException e) {
                    // ⭐ UNIQUE constraint violation = notification already exists
                    // This is IDEMPOTENT behavior - log and continue (not an error)
                    // Could happen if event is reprocessed by listener after crash
                    log.debug("⏭️  Notification already exists (idempotent), skipping: eventId={}, userId={}", 
                        eventId, notification.getUserId());
                }
            }
            
            log.info("✅ Successfully persisted {} disaggregated notifications for eventId={}, WebSocket attempts: {}", 
                disaggregatedNotifications.size(), eventId, sentCount);
            
            // ⭐ STEP 6: Manual ACK (only after success)
            channel.basicAck(deliveryTag, false);
            log.info("✔️  Message ACK'd successfully");
            
        } catch (Exception e) {
            log.error("❌ Error processing notification message: {}", e.getMessage(), e);
            try {
                // Manual NACK on error - requeue for retry
                channel.basicNack(deliveryTag, false, true);
                log.info("❌ Message NACK'd and requeued");
            } catch (Exception nackError) {
                log.error("Failed to NACK message", nackError);
            }
            // Re-throw for @Retryable to handle
            throw new RuntimeException("Failed to process notification: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the type-specific orchestrator for this listener type.
     * Implemented by subclasses to return appropriate orchestrator.
     * 
     * @param message RabbitMQ message
     * @return NotificationOrchestrator<T> for this user type
     */
    protected abstract NotificationOrchestrator<T> getTypeSpecificOrchestrator(Map<String, Object> message);

    /**
     * Enriches the message map with type-specific fields.
     * Each subclass adds the fields its orchestrator expects.
     * 
     * For RESTAURANT_TEAM: adds "restaurant_id" from recipientId
     * For CUSTOMER: adds "customer_id" from recipientId
     * etc.
     * 
     * @param message Map to enrich (already has event_id, event_type, recipient_type, recipient_id, data)
     * @param payload Original DTO for context
     */
    protected abstract void enrichMessageWithTypeSpecificFields(
        Map<String, Object> message, 
        NotificationEventPayloadDTO payload
    );

    /**
     * Checks if notification with given eventId already exists (idempotency).
     * Implemented by subclasses using type-specific DAO.
     * 
     * @param eventId Event ID to check
     * @return true if exists, false otherwise
     */
    protected abstract boolean existsByEventId(String eventId);

    /**
     * Persists notification to database.
     * Implemented by subclasses using type-specific DAO.
     * 
     * @param notification Notification to persist
     */
    protected abstract void persistNotification(T notification);

    /**
     * Attempts to send notification via WebSocket immediately after persistence.
     * 
     * WEBSOCKET DELIVERY DESIGN:
     * - BEST-EFFORT, not reliable (unlike Email/Push/SMS with retry)
     * - NO OUTBOX TABLE (idempotency via Notification table UNIQUE constraint)
     * - SYNCHRONOUS: called right after persistNotification()
     * - If client online → delivery succeeds, client receives immediately
     * - If client offline → send fails silently, NO RETRY
     * - If service crashes between persist and send → client doesn't receive (acceptable)
     * 
     * CLIENT-SIDE DEDUPLICATION:
     * - Payload includes (notificationId, eventId, timestamp)
     * - If same message arrives twice (due to bug), client deduplicates using eventId
     * 
     * @param notification Notification to send via WebSocket
     */
    protected abstract void attemptWebSocketSend(T notification);

    /**
     * Generates disaggregated event ID for tracking.
     * Format: {eventId}_{userId}_{channel}_{timestamp}
     * 
     * @param eventId Original event ID
     * @param userId User ID
     * @param channel Channel type
     * @return Disaggregated event ID
     */
    protected String generateDisaggregatedEventId(String eventId, Long userId, String channel) {
        return String.format("%s_%d_%s_%d", 
            eventId, 
            userId, 
            channel, 
            System.currentTimeMillis()
        );
    }

    /**
     * Helper to extract Long from message (handles both Long and Number)
     */
    protected Long extractLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("Expected numeric value for key: " + key);
    }

    /**
     * Helper to safely extract String value
     */
    protected String extractStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalArgumentException("Expected string value for key: " + key);
    }

    /**
     * Helper to safely extract and cast payload map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractPayload(Map<String, Object> message) {
        return (Map<String, Object>) message.get("payload");
    }

    /**
     * Helper to extract properties from payload
     */
    @SuppressWarnings("unchecked")
    protected Map<String, String> extractProperties(Map<String, Object> payload) {
        return (Map<String, String>) payload.getOrDefault("properties", new HashMap<>());
    }
}
