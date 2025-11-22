package com.application.common.service.notification.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.customer.persistence.dao.CustomerNotificationDAO;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.customer.persistence.model.CustomerNotification.DeliveryStatus;
import com.application.customer.persistence.model.CustomerNotification.NotificationChannel;
import com.application.customer.persistence.model.CustomerNotification.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ CUSTOMER ORCHESTRATOR
 * 
 * Disaggregates notifications for customers.
 * 
 * RESPONSIBILITY:
 * 1. Load customer recipient (just 1 customer)
 * 2. Load customer notification preferences
 * 3. Load customer group settings
 * 4. Calculate final channels
 * 5. Create CustomerNotification records
 * 6. Apply customer-specific business rules
 * 
 * TYPE-SPECIFIC RULES:
 * - Customer is isolated (readByAll=false always)
 * - No SMS (optional)
 * - Email for confirmations
 * - Push for order updates
 * - WebSocket for real-time dashboard updates
 * - Archive notifications older than 30 days
 * 
 * RECIPIENT RESOLUTION:
 * - Recipient is always the customer from the message
 * - Single customer, not a group
 * 
 * EXAMPLE DISAGGREGATION:
 * Input: 1 message for RESERVATION_CONFIRMED for customer 42
 * Processing:
 *   - Recipient: customer 42
 *   - Load preferences (channels: [EMAIL, PUSH, WEBSOCKET])
 *   - Load customer settings (readByAll=false)
 *   - Load event rules (RESERVATION_CONFIRMED: mandatory=[EMAIL], optional=[PUSH,WS])
 *   - Final channels = [EMAIL, PUSH, WEBSOCKET]
 *   - Create 3 notification records:
 *     * {eventId: evt-cust42-EMAIL}
 *     * {eventId: evt-cust42-PUSH}
 *     * {eventId: evt-cust42-WEBSOCKET}
 * Output: 3 notification records
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Extracted from EventOutboxOrchestrator)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerOrchestrator extends NotificationOrchestrator<CustomerNotification> {

    private final CustomerNotificationDAO customerNotificationDAO;
    // TODO: Iniettare CustomerPreferencesService quando disponibile
    // private final CustomerPreferencesService preferencesService;

    /**
     * Disaggregates message for customer.
     * 
     * @param message RabbitMQ message
     * @return List of disaggregated CustomerNotification records
     */
    @Override
    public List<CustomerNotification> disaggregateAndProcess(Map<String, Object> message) {
        log.info("👤 CustomerOrchestrator: starting disaggregation");
        
        List<CustomerNotification> disaggregated = new ArrayList<>();

        // Extract from message
        String eventId = extractString(message, "event_id");
        String eventType = extractString(message, "event_type");
        String aggregateType = extractString(message, "aggregate_type");
        Long customerId = extractLong(message, "customer_id");
        Map<String, Object> payload = extractPayload(message);

        log.info("📊 Disaggregating: eventId={}, eventType={}, customerId={}", 
            eventId, eventType, customerId);

        // Load recipients (just the customer)
        List<Long> recipients = loadRecipients(message);
        if (recipients.isEmpty()) {
            log.warn("⚠️  No customers to notify for this event");
            return disaggregated;
        }

        // Load event rules
        Map<String, Object> eventRules = loadEventTypeRules(eventType);
        @SuppressWarnings("unchecked")
        List<String> mandatoryChannels = (List<String>) eventRules.get("mandatory");
        @SuppressWarnings("unchecked")
        List<String> optionalChannels = (List<String>) eventRules.get("optional");

        // Load group settings
        Map<String, Object> groupSettings = loadGroupSettings(message);
        @SuppressWarnings("unchecked")
        List<String> groupEnabledChannels = (List<String>) groupSettings.get("enabled_channels");

        // For the customer
        Long customerId_actual = recipients.get(0);
        
        // Load customer preferences
        List<String> userEnabledChannels = loadUserPreferences(customerId_actual);

        // Calculate final channels
        List<String> finalChannels = calculateFinalChannels(
            mandatoryChannels,
            optionalChannels,
            groupEnabledChannels,
            userEnabledChannels
        );

        log.debug("📋 Customer {}: final channels = {}", customerId_actual, finalChannels);

        // Create notification record per channel
        for (String channel : finalChannels) {
            String disaggregatedEventId = generateDisaggregatedEventId(eventId, customerId_actual, channel);

            CustomerNotification notification = createNotificationRecord(
                disaggregatedEventId,
                customerId_actual,
                channel,
                message
            );

            // Apply customer-specific rules
            notification = applyEventTypeRules(notification, message);

            disaggregated.add(notification);
        }

        log.info("✅ Disaggregation complete: {} notification records from 1 event", disaggregated.size());
        return disaggregated;
    }

    /**
     * Loads customer as recipient.
     * 
     * Distingue tra BROADCAST e TARGETED:
     * - BROADCAST: Carica TUTTI i customers attivi
     * - TARGETED: Carica solo il customer specificato (customer_id da message)
     * 
     * @param message RabbitMQ message (contains customer_id, recipientType)
     * @return List with customer ID(s)
     */
    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        String recipientType = (String) message.getOrDefault("recipientType", "TARGETED");
        
        if ("BROADCAST".equals(recipientType)) {
            // Load ALL active customers
            log.info("📢 BROADCAST: Loading ALL active customers");
            // TODO: Iniettare CustomerService.findAllActiveCust omers()
            // For now, return empty list (stub)
            return new ArrayList<>();
        } else {
            // Load specific customer from message
            Long customerId = extractLong(message, "customer_id");
            log.debug("👤 Customer {}: loaded as recipient", customerId);
            return List.of(customerId);
        }
    }

    /**
     * Loads customer notification preferences.
     * 
     * Queries user_notification_preferences table for enabled channels.
     * 
     * @param customerId Customer user ID
     * @return List of enabled channels (EMAIL, PUSH, SMS, WEBSOCKET, etc)
     */
    @Override
    protected List<String> loadUserPreferences(Long customerId) {
        // TODO: Iniettare CustomerPreferencesService.getEnabledChannels(customerId)
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement CustomerPreferencesService.getEnabledChannels()");
        return List.of("EMAIL", "PUSH", "WEBSOCKET");
    }

    /**
     * Loads customer group notification settings.
     * 
     * For customers, settings are global or customer-specific.
     * 
     * @param message RabbitMQ message
     * @return Map with settings: enabled_channels, quiet_hours, etc
     */
    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        // TODO: Iniettare NotificationGroupSettingsService.getCustomerSettings()
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement NotificationGroupSettingsService.getCustomerSettings()");
        return Map.of(
            "enabled_channels", List.of("EMAIL", "PUSH", "WEBSOCKET"),
            "quiet_hours_enabled", true,
            "quiet_hours_start", "22:00",
            "quiet_hours_end", "08:00"
        );
    }

    /**
     * Loads event type routing rules for customers.
     * 
     * Examples:
     * - RESERVATION_CONFIRMED: mandatory=[EMAIL], optional=[PUSH,WEBSOCKET]
     * - ORDER_READY: mandatory=[PUSH], optional=[EMAIL,WEBSOCKET]
     * - RESERVATION_REMINDER: mandatory=[], optional=[EMAIL,PUSH]
     * - ORDER_STATUS_UPDATE: mandatory=[WEBSOCKET], optional=[PUSH]
     * 
     * @param eventType Event type
     * @return Map with "mandatory" and "optional" channel lists
     */
    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        // TODO: Carica da database (event_type_routing_rules)
        // Per ora, regole hardcoded:
        
        return switch (eventType) {
            case "RESERVATION_CONFIRMED" -> Map.of(
                "mandatory", List.of("EMAIL"),
                "optional", List.of("PUSH", "WEBSOCKET")
            );
            case "ORDER_READY" -> Map.of(
                "mandatory", List.of("PUSH"),
                "optional", List.of("EMAIL", "WEBSOCKET")
            );
            case "ORDER_STATUS_UPDATE" -> Map.of(
                "mandatory", List.of("WEBSOCKET"),
                "optional", List.of("PUSH", "EMAIL")
            );
            case "RESERVATION_REMINDER" -> Map.of(
                "mandatory", List.of(),
                "optional", List.of("EMAIL", "PUSH", "WEBSOCKET")
            );
            default -> Map.of(
                "mandatory", List.of("WEBSOCKET"),
                "optional", List.of("EMAIL", "PUSH")
            );
        };
    }

    /**
     * Creates a CustomerNotification record.
     * 
     * @param eventId Disaggregated event ID
     * @param customerId Customer user ID
     * @param channel Channel type
     * @param message Original RabbitMQ message
     * @return CustomerNotification record
     */
    @Override
    protected CustomerNotification createNotificationRecord(
        String eventId,
        Long customerId,
        String channel,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");
        String aggregateType = extractString(message, "aggregate_type");
        Map<String, Object> payload = extractPayload(message);

        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) payload.getOrDefault("properties", new java.util.HashMap<>());

        NotificationPriority priority = determinePriority(eventType);

        return CustomerNotification.builder()
            .eventId(eventId)
            .userId(customerId)
            .channel(NotificationChannel.valueOf(channel))
            .status(DeliveryStatus.PENDING)
            .priority(priority)
            .title((String) payload.get("title"))
            .body((String) payload.get("body"))
            .eventType(eventType)
            .aggregateType(aggregateType)
            .properties(props)
            .build();
    }

    /**
     * Applies customer-specific business rules.
     * 
     * Examples:
     * - Archive notifications older than 30 days
     * - No SMS for customers (except confirmations)
     * - Track customer engagement (click-through rates)
     * 
     * @param notification Notification record
     * @param message Original RabbitMQ message
     * @return Modified notification
     */
    @Override
    protected CustomerNotification applyEventTypeRules(
        CustomerNotification notification,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");

        // Rule: Track as high-priority if confirmation
        if (eventType.equals("RESERVATION_CONFIRMED") || eventType.equals("ORDER_READY")) {
            notification.setPriority(NotificationPriority.HIGH);
            log.debug("✅ Marked as HIGH priority: {}", eventType);
        }

        // Rule: Disable SMS for customers (optional - can be overridden by preferences)
        if (notification.getChannel() == NotificationChannel.SMS) {
            log.debug("📱 SMS for customers: currently disabled (use PUSH instead)");
        }

        return notification;
    }

    /**
     * Determines priority based on event type.
     * 
     * @param eventType Event type
     * @return NotificationPriority
     */
    private NotificationPriority determinePriority(String eventType) {
        return switch (eventType) {
            case "RESERVATION_CONFIRMED", "ORDER_READY" -> NotificationPriority.HIGH;
            case "ORDER_STATUS_UPDATE", "RESERVATION_REMINDER" -> NotificationPriority.NORMAL;
            default -> NotificationPriority.NORMAL;
        };
    }
}
