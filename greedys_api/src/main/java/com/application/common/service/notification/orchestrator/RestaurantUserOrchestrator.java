package com.application.common.service.notification.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.common.service.notification.preferences.NotificationBlockService;
import com.application.common.service.notification.preferences.NotificationBlockService.NotificationContext;
import com.application.restaurant.persistence.dao.RestaurantUserNotificationDAO;
import com.application.restaurant.persistence.model.RestaurantUserNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.common.persistence.model.notification.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RESTAURANT USER ORCHESTRATOR
 * 
 * Disaggregates notifications for restaurant staff.
 * 
 * RESPONSIBILITY:
 * 1. Load restaurant staff recipients
 * 2. Load staff notification preferences
 * 3. Load restaurant group settings
 * 4. Calculate final channels per staff member
 * 5. Create RestaurantUserNotification records
 * 6. Apply restaurant-specific business rules (escalation, SMS restrictions, etc)
 * 
 * TYPE-SPECIFIC RULES:
 * - CRITICAL events (HIGH priority): escalate to manager if no ACK in 5 min
 * - SMS only sent to MANAGER role
 * - Email sent to all staff
 * - WebSocket sent to all online staff
 * - readByAll logic: shared notifications for broadcast events
 * 
 * RECIPIENT RESOLUTION:
 * - Load all active staff of the restaurant
 * - Filter by roles (MANAGER, CHEF, WAITER, etc)
 * - Consider on-duty status
 * 
 * EXAMPLE DISAGGREGATION:
 * Input: 1 message for RESERVATION_REQUESTED at restaurant 5
 * Processing:
 *   - Load 10 active staff of restaurant 5
 *   - For each staff:
 *     - Load preferences (channels: [WEBSOCKET, EMAIL])
 *     - Load restaurant settings (readByAll=true)
 *     - Load event rules (RESERVATION: mandatory=[WS], optional=[EMAIL,PUSH,SMS])
 *     - Final channels = [WEBSOCKET, EMAIL]
 *     - Create 2 notification records:
 *       * {eventId: evt-5-staff1-WEBSOCKET}
 *       * {eventId: evt-5-staff1-EMAIL}
 * Output: 20 notification records (10 staff × 2 channels)
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Extracted from EventOutboxOrchestrator)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantUserOrchestrator extends NotificationOrchestrator<RestaurantUserNotification> {

    private final RestaurantUserNotificationDAO restaurantNotificationDAO;
    private final NotificationBlockService notificationBlockService;
    // TODO: Iniettare RestaurantStaffService quando disponibile
    // private final RestaurantStaffService staffService;
    // private final RestaurantUserPreferencesService preferencesService;

    /**
     * Disaggregates message for restaurant staff.
     * 
     * @param message RabbitMQ message
     * @return List of disaggregated RestaurantUserNotification records
     */
    @Override
    public List<RestaurantUserNotification> disaggregateAndProcess(Map<String, Object> message) {
        log.info("🏢 RestaurantUserOrchestrator: starting disaggregation");
        
        List<RestaurantUserNotification> disaggregated = new ArrayList<>();

        // Extract from message
        String eventId = extractString(message, "event_id");
        String eventType = extractString(message, "event_type");
        Long restaurantId = extractLong(message, "restaurant_id");
        // aggregateType e payload sono disponibili nel message ma non usati direttamente qui

        // ═══════════════════════════════════════════════════════════════════
        // LIVELLO 0: CHECK BLOCCO GLOBALE
        // ═══════════════════════════════════════════════════════════════════
        if (notificationBlockService.isGloballyBlocked(eventType)) {
            log.info("🚫 EventType {} is globally blocked - skipping all notifications", eventType);
            return disaggregated; // Empty list
        }

        log.info("📊 Disaggregating: eventId={}, eventType={}, restaurantId={}", 
            eventId, eventType, restaurantId);

        // Load recipients
        List<Long> recipientStaffIds = loadRecipients(message);
        log.info("👥 Loaded {} staff recipients for restaurant {}", recipientStaffIds.size(), restaurantId);

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

        // For each staff member
        for (Long staffId : recipientStaffIds) {
            // Load staff preferences
            List<String> userEnabledChannels = loadUserPreferences(staffId);

            // Calculate final channels
            List<String> finalChannels = calculateFinalChannels(
                mandatoryChannels,
                optionalChannels,
                groupEnabledChannels,
                userEnabledChannels
            );

            log.debug("📋 Staff {}: final channels = {}", staffId, finalChannels);

            // ═══════════════════════════════════════════════════════════════════
            // CONTEXT per controllo blocchi (Livello 2-4)
            // ═══════════════════════════════════════════════════════════════════
            Long hubId = extractLong(message, "hub_id"); // Può essere null
            NotificationContext blockContext = hubId != null 
                ? NotificationContext.forRestaurant(restaurantId, hubId)
                : NotificationContext.forRestaurant(restaurantId);

            // Create notification record per channel
            for (String channel : finalChannels) {
                // ═══════════════════════════════════════════════════════════════════
                // CHECK BLOCCHI LIVELLO 1-4 PER CANALE
                // ═══════════════════════════════════════════════════════════════════
                if (!notificationBlockService.canSendNotification(eventType, channel, staffId, blockContext)) {
                    log.debug("🚫 Notification blocked for staff {} on channel {} for eventType {}", 
                        staffId, channel, eventType);
                    continue; // Skip this channel
                }

                String disaggregatedEventId = generateDisaggregatedEventId(eventId, staffId, channel);

                RestaurantUserNotification notification = createNotificationRecord(
                    disaggregatedEventId,
                    staffId,
                    channel,
                    message
                );

                // Apply restaurant-specific rules
                notification = applyEventTypeRules(notification, message);

                disaggregated.add(notification);
            }
        }

        log.info("✅ Disaggregation complete: {} notification records from 1 event", disaggregated.size());
        return disaggregated;
    }

    /**
     * Loads all active restaurant staff as recipients.
     * 
     * Distingue tra BROADCAST e TARGETED:
     * - BROADCAST: Carica TUTTI i staff attivi del ristorante
     * - TARGETED: Carica solo lo staff specificato in message (recipient_id)
     * 
     * @param message RabbitMQ message (contains restaurant_id, recipientType, recipient_id opzionale)
     * @return List of active staff user IDs
     */
    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        Long restaurantId = extractLong(message, "restaurant_id");
        String recipientType = (String) message.getOrDefault("recipientType", "TARGETED");
        
        if ("BROADCAST".equals(recipientType)) {
            // Load ALL active staff of this restaurant
            log.info("📢 BROADCAST: Loading ALL active staff for restaurant {}", restaurantId);
            // TODO: Iniettare RestaurantStaffService.findActiveStaffByRestaurantId(restaurantId)
            // For now, return empty list (stub)
            return new ArrayList<>();
        } else {
            // Load specific recipient from message
            log.info("🎯 TARGETED: Loading specific recipient");
            Long recipientId = extractLong(message, "recipient_id");
            return List.of(recipientId);
        }
    }

    /**
     * Loads user preferences for restaurant staff.
     * 
     * Queries user_notification_preferences table for enabled channels.
     * 
     * @param staffId Staff user ID
     * @return List of enabled channels (EMAIL, PUSH, SMS, WEBSOCKET, etc)
     */
    @Override
    protected List<String> loadUserPreferences(Long staffId) {
        // TODO: Iniettare RestaurantUserPreferencesService.getEnabledChannels(staffId)
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement RestaurantUserPreferencesService.getEnabledChannels()");
        return List.of("WEBSOCKET", "EMAIL");
    }

    /**
     * Loads restaurant group notification settings.
     * 
     * Queries notification_group_settings table for restaurant-level preferences.
     * 
     * @param message RabbitMQ message (contains restaurant_id)
     * @return Map with settings: enabled_channels, quiet_hours, priority, etc
     */
    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        // restaurantId sarà usato quando NotificationGroupSettingsService sarà implementato
        @SuppressWarnings("unused")
        Long restaurantId = extractLong(message, "restaurant_id");
        
        // TODO: Iniettare NotificationGroupSettingsService.getRestaurantSettings(restaurantId)
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement NotificationGroupSettingsService.getRestaurantSettings()");
        return Map.of(
            "enabled_channels", List.of("EMAIL", "PUSH", "SMS", "WEBSOCKET"),
            "quiet_hours_enabled", false
        );
    }

    /**
     * Loads event type routing rules for restaurant staff.
     * 
     * Examples:
     * - RESERVATION_REQUESTED: mandatory=[WEBSOCKET], optional=[EMAIL,PUSH,SMS]
     * - NEW_ORDER: mandatory=[WEBSOCKET], optional=[EMAIL,PUSH,SMS]
     * - KITCHEN_ALERT: mandatory=[WEBSOCKET,PUSH], optional=[SMS,EMAIL]
     * - TASK_ASSIGNMENT: mandatory=[], optional=[EMAIL,SMS]
     * 
     * @param eventType Event type
     * @return Map with "mandatory" and "optional" channel lists
     */
    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        // TODO: Carica da database (event_type_routing_rules)
        // Per ora, regole hardcoded:
        
        return switch (eventType) {
            case "RESERVATION_REQUESTED", "NEW_ORDER" -> Map.of(
                "mandatory", List.of("WEBSOCKET"),
                "optional", List.of("EMAIL", "PUSH", "SMS")
            );
            case "KITCHEN_ALERT" -> Map.of(
                "mandatory", List.of("WEBSOCKET", "PUSH"),
                "optional", List.of("SMS", "EMAIL")
            );
            case "TASK_ASSIGNMENT", "DIRECT_MESSAGE" -> Map.of(
                "mandatory", List.of(),
                "optional", List.of("EMAIL", "SMS", "WEBSOCKET")
            );
            default -> Map.of(
                "mandatory", List.of("WEBSOCKET"),
                "optional", List.of("EMAIL", "PUSH", "SMS")
            );
        };
    }

    /**
     * Creates a RestaurantUserNotification record.
     * 
     * @param eventId Disaggregated event ID
     * @param staffId Staff user ID
     * @param channel Channel type
     * @param message Original RabbitMQ message
     * @return RestaurantUserNotification record
     */
    @Override
    protected RestaurantUserNotification createNotificationRecord(
        String eventId,
        Long staffId,
        String channel,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");
        String aggregateType = extractString(message, "aggregate_type");
        Long restaurantId = extractLong(message, "restaurant_id");
        Long eventOutboxId = extractLong(message, "event_outbox_id");
        Map<String, Object> payload = extractPayload(message);

        // Null-safe extraction of properties
        @SuppressWarnings("unchecked")
        Map<String, String> props = (payload != null) 
            ? (Map<String, String>) payload.getOrDefault("properties", new java.util.HashMap<>())
            : new java.util.HashMap<>();

        // Determine read visibility
        boolean readByAll = determineReadByAll(eventType);
        NotificationPriority priority = determinePriority(eventType);
        
        // Null-safe extraction of title and body
        String title = (payload != null) ? (String) payload.get("title") : null;
        String body = (payload != null) ? (String) payload.get("body") : null;

        return RestaurantUserNotification.builder()
            .eventId(eventId)
            .eventOutboxId(eventOutboxId)
            .userId(staffId)
            .restaurantId(restaurantId)
            .channel(NotificationChannel.valueOf(channel))
            .status(DeliveryStatus.PENDING)
            .readByAll(readByAll)
            .priority(priority)
            .title(title)
            .body(body)
            .eventType(eventType)
            .aggregateType(aggregateType)
            .properties(props)
            .build();
    }

    /**
     * Applies restaurant-specific business rules.
     * 
     * Examples:
     * - CRITICAL priority → escalate to manager after 5 min
     * - SMS channel → only for MANAGER role
     * - KITCHEN_ALERT → only to CHEF role
     * 
     * @param notification Notification record
     * @param message Original RabbitMQ message
     * @return Modified notification
     */
    @Override
    protected RestaurantUserNotification applyEventTypeRules(
        RestaurantUserNotification notification,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");
        NotificationPriority priority = notification.getPriority();

        // Rule: SMS only for managers
        if (notification.getChannel() == NotificationChannel.SMS) {
            // TODO: Check if user has MANAGER role, if not, skip SMS
            log.debug("📱 SMS channel: verify staff is MANAGER (TODO)");
        }

        // Rule: KITCHEN_ALERT only to chefs
        if (eventType.equals("KITCHEN_ALERT")) {
            // TODO: Check if user has CHEF role, if not, skip
            log.debug("🍳 KITCHEN_ALERT: verify staff is CHEF (TODO)");
        }

        // Rule: HIGH priority → requires urgent delivery
        if (priority == NotificationPriority.HIGH) {
            log.debug("🚨 HIGH priority: urgent delivery needed");
        }

        return notification;
    }

    /**
     * Determines if notification is shared (readByAll).
     * 
     * @param eventType Event type
     * @return true if shared, false if personal
     */
    private boolean determineReadByAll(String eventType) {
        return switch (eventType) {
            case "RESERVATION_REQUESTED", "NEW_ORDER", "KITCHEN_ALERT" -> true;
            case "TASK_ASSIGNMENT", "DIRECT_MESSAGE" -> false;
            default -> false;
        };
    }

    /**
     * Determines priority based on event type.
     * 
     * @param eventType Event type
     * @return NotificationPriority
     */
    private NotificationPriority determinePriority(String eventType) {
        return switch (eventType) {
            case "KITCHEN_ALERT", "CRITICAL_RESERVATION" -> NotificationPriority.HIGH;
            case "RESERVATION_REQUESTED", "NEW_ORDER" -> NotificationPriority.HIGH;
            default -> NotificationPriority.NORMAL;
        };
    }
}
