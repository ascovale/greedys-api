package com.application.common.service.notification.orchestrator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.ProcessedEventDAO;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.model.ProcessedEvent;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.notification.EventOutbox.Status;
import com.application.common.type.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ EVENT OUTBOX ORCHESTRATOR (PRODUCER - LAYER 1)
 * 
 * SIMPLE PUBLISHER: Polls EventOutbox and publishes 1 GENERIC message per recipient type to RabbitMQ.
 * 
 * RESPONSIBILITY:
 * - Poll EventOutbox table for PENDING events (status = PENDING)
 * - Determine recipient type from aggregateType (RESTAURANT, CUSTOMER, AGENCY, ADMIN)
 * - Publish 1 message per recipient type to RabbitMQ
 * - Mark event as PROCESSED
 * 
 * ⭐ IMPORTANT: NO DISAGGREGATION HERE
 * - Does NOT load user preferences
 * - Does NOT load group settings
 * - Does NOT load event routing rules
 * - Does NOT disaggregate by recipient × channel
 * - Disaggregation happens in Layer 2 (NotificationOrchestrator in @RabbitListener)
 * 
 * FLOW:
 * [T0s] @Scheduled(fixedDelay=1000)
 *       EventOutboxOrchestrator.orchestrate()
 *       ├─ Poll EventOutbox: SELECT * WHERE status='PENDING' LIMIT 100
 *       ├─ For each event:
 *       │  ├─ Get aggregateType (RESTAURANT, CUSTOMER, AGENCY, ADMIN)
 *       │  ├─ Publish 1 message to RabbitMQ queue (notification.{type})
 *       │  │  {
 *       │  │    event_id: "RES-REQ-12345",
 *       │  │    event_type: "RESERVATION_REQUESTED",
 *       │  │    aggregate_type: "RESTAURANT",
 *       │  │    restaurant_id: 5,
 *       │  │    payload: {...}
 *       │  │  }
 *       │  └─ Mark as PROCESSED
 *       └─ Log results
 * 
 * [T1s+] RabbitMQ delivers to correct queue:
 *        ├─ notification.restaurant queue → RestaurantNotificationListener
 *        ├─ notification.customer queue → CustomerNotificationListener
 *        ├─ notification.agency queue → AgencyUserNotificationListener
 *        └─ notification.admin queue → AdminNotificationListener
 * 
 * [T2s+] @RabbitListener processes message:
 *        ├─ Calls BaseNotificationListener.processNotificationMessage()
 *        ├─ Gets type-specific orchestrator from factory
 *        ├─ Orchestrator DISAGGREGATES (1 message → N notifications)
 *        ├─ Listener saves all disaggregated records
 *        └─ ACK
 * 
 * MESSAGE VOLUME:
 * - Input: 1 RESERVATION_REQUESTED event
 * - EventOutboxOrchestrator publishes: 1 message (to notification.restaurant)
 * - RabbitMQ carries: 1 message (LIGHT!)
 * - RestaurantNotificationListener receives: 1 message
 * - Disaggregates to: 8 notification records (staff × channels)
 * - Database saves: 8 rows
 * 
 * BENEFITS:
 * ✅ RabbitMQ traffic is minimal (1 event = 1 message)
 * ✅ Business logic stays in layer 2 (NotificationOrchestrator)
 * ✅ Can add event-type-specific rules in future (override in orchestrator)
 * ✅ Aligns with stream processor pattern (Facebook, Netflix, Amazon)
 * 
 * FUTURE ENHANCEMENT (Optional):
 * - Add event-type-specific publishing rules in EventOutboxOrchestrator
 * - Example: CRITICAL_RESERVATION → add priority=HIGH to message
 * - Rules stored in database per event type + recipient type
 * - No breaking changes to current flow
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Simplified producer - disaggregation moved to listener)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventOutboxOrchestrator {

    private final EventOutboxDAO eventOutboxRepository;
    private final ProcessedEventDAO processedEventRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * ⭐ MAIN SCHEDULED JOB
     * 
     * Polls EventOutbox every 1 second and publishes messages to RabbitMQ.
     * 
     * @Scheduled(fixedDelay=1000, initialDelay=2000)
     * - Runs every 1000ms (1 second)
     * - Initial delay 2000ms (wait 2 seconds before first run)
     * - Allows other services to start first
     * 
     * ⭐ IDEMPOTENCY: LEVEL 1 (Event-Level)
     * For each event, tries to INSERT ProcessedEvent(eventId) with UNIQUE constraint.
     * - If success → event is new, proceed with publishing
     * - If UNIQUE violation → event already processed, SKIP (no duplicate RabbitMQ messages)
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    @Transactional
    public void orchestrate() {
        log.debug("📬 EventOutboxOrchestrator: polling EventOutbox for PENDING events");

        // Fetch PENDING events (max 100 per cycle to avoid memory issues)
        List<EventOutbox> pendingEvents = eventOutboxRepository.findByStatus("PENDING", 100);

        if (pendingEvents.isEmpty()) {
            log.debug("✓ No pending events to publish");
            return;
        }

        log.info("📬 Found {} pending events to publish", pendingEvents.size());

        // Process each event
        for (EventOutbox event : pendingEvents) {
            try {
                // ⭐ LEVEL 1 IDEMPOTENCY: Try insert ProcessedEvent with UNIQUE constraint
                ProcessedEvent processed = new ProcessedEvent();
                processed.setEventId(event.getEventId());
                processed.setStatus(ProcessingStatus.PROCESSING);
                processedEventRepository.save(processed);
                
                // If we reach here = first time processing this event
                publishEvent(event);
                markAsProcessed(event);
                
                // Mark processing as successful
                processed.setStatus(ProcessingStatus.SUCCESS);
                processedEventRepository.save(processed);
                
                log.info("✅ Published event: {} to queue: notification.{}", 
                    event.getEventId(), 
                    event.getAggregateType().toLowerCase()
                );
                
            } catch (DataIntegrityViolationException e) {
                // ⭐ UNIQUE constraint violation = eventId already in ProcessedEvent
                // This means event was already published to RabbitMQ
                // SKIP this event (no duplicate messages to RabbitMQ)
                log.info("⏭️  Event {} already processed (idempotent), skipping", event.getEventId());
                
            } catch (Exception e) {
                log.error("❌ Failed to process event: {} - {}", 
                    event.getEventId(), 
                    e.getMessage(), 
                    e
                );
                // Don't mark as processed - will retry next cycle
            }
        }

        log.info("✅ Completed orchestration cycle: {} events processed", pendingEvents.size());
    }

    /**
     * ⭐ PUBLISH EVENT TO RABBITMQ
     * 
     * Publishes 1 GENERIC message per recipient type to RabbitMQ.
     * No disaggregation, no business logic - just publish.
     * 
     * @param event EventOutbox entity
     */
    private void publishEvent(EventOutbox event) {
        // Determine target queue based on aggregateType
        String aggregateType = event.getAggregateType();
        String queueName = determineTargetQueue(aggregateType);

        // Build message
        Map<String, Object> message = buildMessage(event);

        log.debug("📤 Publishing message to queue: {}", queueName);

        // Publish to RabbitMQ (convertAndSend automatically handles serialization)
        rabbitTemplate.convertAndSend(queueName, message);

        log.debug("✓ Message published to RabbitMQ");
    }

    /**
     * ⭐ DETERMINE TARGET QUEUE
     * 
     * Routes message to correct queue based on aggregateType.
     * 
     * @param aggregateType Type (RESTAURANT, CUSTOMER, AGENCY, ADMIN)
     * @return Queue name (notification.{type})
     */
    private String determineTargetQueue(String aggregateType) {
        if (aggregateType == null) {
            throw new IllegalArgumentException("aggregateType cannot be null");
        }

        return switch (aggregateType.toUpperCase()) {
            case "RESTAURANT" -> "notification.restaurant";
            case "CUSTOMER" -> "notification.customer";
            case "AGENCY" -> "notification.agency";
            case "ADMIN" -> "notification.admin";
            case "BROADCAST" -> "notification.broadcast";  // Future: broadcast to all users
            default -> throw new IllegalArgumentException("Unknown aggregateType: " + aggregateType);
        };
    }

    /**
     * ⭐ BUILD MESSAGE FOR RABBITMQ
     * 
     * Creates a simple message with event data.
     * No disaggregation, no preference loading - just pass through.
     * 
     * Message structure:
     * {
     *   event_id: "RES-REQ-12345",
     *   event_type: "RESERVATION_REQUESTED",
     *   aggregate_type: "RESTAURANT",
     *   aggregate_id: 5,
     *   recipientType: "BROADCAST",  ⭐ NEW: BROADCAST or TARGETED (default: TARGETED)
     *   restaurant_id: 5,           // For RESTAURANT events
     *   customer_id: 42,             // For CUSTOMER events
     *   payload: {...}
     * }
     * 
     * ⭐ recipientType determines how listener disaggregates:
     *   - BROADCAST: Load ALL users of this type, send to all
     *   - TARGETED: Load specific user only (from recipient_id in payload)
     * 
     * @param event EventOutbox entity
     * @return Map ready for RabbitMQ
     */
    private Map<String, Object> buildMessage(EventOutbox event) {
        Map<String, Object> message = new HashMap<>();

        message.put("event_id", event.getEventId());
        message.put("event_type", event.getEventType());
        message.put("aggregate_type", event.getAggregateType());
        message.put("aggregate_id", event.getAggregateId());
        
        // ⭐ BROADCAST vs TARGETED: Determines disaggregation strategy in listener
        // Default: TARGETED (send to specific user)
        // Override in payload if event should be broadcast to all users of this type
        String recipientType = determineRecipientType(event);
        message.put("recipientType", recipientType);

        // Add type-specific ID field for convenience in listener
        addTypeSpecificIds(message, event);

        // Pass through payload as-is (contains custom fields)
        if (event.getPayload() != null) {
            message.put("payload", event.getPayload());
        }

        return message;
    }

    /**
     * ⭐ DETERMINE RECIPIENT TYPE (BROADCAST vs TARGETED)
     * 
     * Extracts recipientType from event payload or defaults to TARGETED.
     * 
     * @param event EventOutbox entity
     * @return "BROADCAST" or "TARGETED" (default: "TARGETED")
     */
    private String determineRecipientType(EventOutbox event) {
        String payload = event.getPayload();
        if (payload != null && !payload.isEmpty()) {
            try {
                // payload is a JSON string, parse it
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> payloadMap = mapper.readValue(payload, Map.class);
                Object recipientType = payloadMap.get("recipientType");
                if (recipientType != null) {
                    return recipientType.toString().toUpperCase();
                }
            } catch (Exception e) {
                log.debug("Could not parse payload for recipientType: {}", e.getMessage());
            }
        }
        // Default to TARGETED (specific user)
        return "TARGETED";
    }

    /**
     * ⭐ ADD TYPE-SPECIFIC ID FIELDS
     * 
     * Adds convenient ID fields based on aggregateType.
     * Makes it easier for listeners to extract relevant IDs.
     * 
     * @param message Message map
     * @param event EventOutbox entity
     */
    private void addTypeSpecificIds(Map<String, Object> message, EventOutbox event) {
        String aggregateType = event.getAggregateType();
        Long aggregateId = event.getAggregateId();

        switch (aggregateType.toUpperCase()) {
            case "RESTAURANT":
                message.put("restaurant_id", aggregateId);
                break;
            case "CUSTOMER":
                message.put("customer_id", aggregateId);
                break;
            case "AGENCY":
                message.put("agency_id", aggregateId);
                break;
            case "ADMIN":
                message.put("admin_id", aggregateId);
                break;
        }
    }

    /**
     * ⭐ MARK EVENT AS PROCESSED
     * 
     * Updates status to PROCESSED after successful publication.
     * This prevents the same event from being published multiple times.
     * 
     * @param event EventOutbox entity
     */
    private void markAsProcessed(EventOutbox event) {
        event.setStatus(Status.PROCESSED);
        eventOutboxRepository.save(event);
        log.debug("✓ Marked event as PROCESSED: {}", event.getEventId());
    }

    /**
     * ⭐ FUTURE ENHANCEMENT (Optional)
     * 
     * Can add event-type-specific publishing rules here.
     * Example: CRITICAL events get higher priority, different routing, etc.
     * 
     * Rules would be stored in database (event_type_routing_config table):
     * ┌─────────────────────────────────────────────────────────┐
     * │ event_type │ aggregate_type │ priority │ target_queue  │
     * ├─────────────────────────────────────────────────────────┤
     * │ RESERVATION_REQUESTED │ RESTAURANT │ NORMAL │ notification.restaurant │
     * │ CRITICAL_RESERVATION │ RESTAURANT │ HIGH │ notification.restaurant-critical │
     * │ URGENT_ORDER │ RESTAURANT │ HIGH │ notification.restaurant-urgent │
     * └─────────────────────────────────────────────────────────┘
     * 
     * Then loadEventTypeRules() would determine final queue and priority:
     * 
     * private void applyEventTypeRules(Map<String, Object> message, EventOutbox event) {
     *   String eventType = event.getEventType();
     *   EventTypeRule rule = loadEventTypeRules(eventType);
     *   
     *   if (rule.getPriority() == HIGH) {
     *     message.put("priority", "HIGH");
     *     message.put("escalation_enabled", true);
     *   }
     * }
     */
}
