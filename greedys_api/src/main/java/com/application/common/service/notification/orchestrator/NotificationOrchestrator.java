package com.application.common.service.notification.orchestrator;

import java.util.List;
import java.util.Map;

import com.application.common.persistence.model.notification.ANotification;

/**
 * ⭐ ABSTRACT BASE CLASS FOR NOTIFICATION ORCHESTRATORS
 * 
 * Responsabile per disaggregare messaggi ricevuti da RabbitMQ.
 * 
 * DISAGGREGATION FLOW:
 * Input: 1 message from RabbitMQ
 *   {
 *     event_type: "RESERVATION_REQUESTED",
 *     recipient_type: "RESTAURANT",
 *     restaurant_id: 5,
 *     payload: {...}
 *   }
 * 
 * Processing: 
 * 1. Load all recipients (staff members, customers, agents, etc)
 * 2. Load user preferences for each recipient
 * 3. Load group notification settings
 * 4. Calculate final channels: Group ∩ User ∩ Event per recipient
 * 5. Create 1 notification record per (recipient × channel)
 * 
 * Output: List<T> with disaggregated notifications
 * [
 *   {eventId: evt-5-staff1-WEBSOCKET, userId: 1, channel: WEBSOCKET, ...},
 *   {eventId: evt-5-staff1-EMAIL, userId: 1, channel: EMAIL, ...},
 *   {eventId: evt-5-staff2-WEBSOCKET, userId: 2, channel: WEBSOCKET, ...},
 *   {eventId: evt-5-staff2-EMAIL, userId: 2, channel: EMAIL, ...},
 *   {eventId: evt-5-staff3-WEBSOCKET, userId: 3, channel: WEBSOCKET, ...}
 * ]
 * 
 * INHERITANCE MODEL:
 * NotificationOrchestrator<T>
 * ├── RestaurantUserOrchestrator extends NotificationOrchestrator<RestaurantUserNotification>
 * │   - Type-specific recipient loading (load restaurant staff)
 * │   - Type-specific preference loading (restaurant staff preferences)
 * │   - Type-specific event rules (escalation for critical events to managers)
 * │   - Type-specific channel logic (SMS only to managers)
 * │
 * ├── CustomerOrchestrator extends NotificationOrchestrator<CustomerNotification>
 * │   - Type-specific recipient loading (just the customer)
 * │   - Type-specific preference loading (customer preferences)
 * │   - Type-specific event rules (archive old notifications)
 * │   - Type-specific channel logic (no SMS for customers)
 * │
 * ├── AgencyUserOrchestrator extends NotificationOrchestrator<AgencyUserNotification>
 * │   - Type-specific recipient loading (load agency agents/managers)
 * │   - Type-specific preference loading (agency user preferences)
 * │   - Type-specific event rules (priority-based routing)
 * │   - Type-specific channel logic (SMS for urgent only)
 * │
 * └── AdminOrchestrator extends NotificationOrchestrator<AdminNotification>
 *     - Type-specific recipient loading (load all system admins)
 *     - Type-specific preference loading (admin preferences)
 *     - Type-specific event rules (incident tracking)
 *     - Type-specific channel logic (Slack integration)
 * 
 * GENERICS:
 * T = Notification model class (RestaurantUserNotification, CustomerNotification, etc)
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Refactored from EventOutboxOrchestrator disaggregation)
 */
public abstract class NotificationOrchestrator<T extends ANotification> {

    /**
     * Main method: Disaggregates 1 RabbitMQ message into multiple notification records.
     * 
     * Called by BaseNotificationListener after receiving message from RabbitMQ.
     * 
     * ALGORITHM:
     * 1. Load recipients list based on eventType and payload
     * 2. For each recipient:
     *    a. Load user preferences (enabled channels)
     *    b. Load group settings
     *    c. Load event type routing rules
     *    d. Calculate: Group ∩ User ∩ Event = final channels
     *    e. Create notification record per channel
     * 3. Return list of disaggregated notifications
     * 
     * @param message RabbitMQ message with event data
     * @return List<T> with disaggregated notification records
     */
    public abstract List<T> disaggregateAndProcess(Map<String, Object> message);

    /**
     * Loads recipients for this notification type.
     * 
     * Type-specific implementation:
     * - RestaurantUserOrchestrator: Load restaurant staff
     * - CustomerOrchestrator: Return single customer
     * - AgencyUserOrchestrator: Load agency agents/managers
     * - AdminOrchestrator: Load all system admins
     * 
     * @param message RabbitMQ message
     * @return List of recipient user IDs
     */
    protected abstract List<Long> loadRecipients(Map<String, Object> message);

    /**
     * Loads user preferences for recipient.
     * 
     * Type-specific implementation:
     * - Queries user_notification_preferences table
     * - Returns enabled channels for this user
     * - Considers quiet hours, role-based restrictions
     * 
     * @param userId User ID
     * @return List of enabled notification channels
     */
    protected abstract List<String> loadUserPreferences(Long userId);

    /**
     * Loads group notification settings.
     * 
     * Type-specific implementation:
     * - Queries notification_group_settings table
     * - Returns settings for restaurant/agency/admin group
     * - Settings include: readByAll, priority, quiet hours
     * 
     * @param message RabbitMQ message (contains group ID)
     * @return Map with group settings
     */
    protected abstract Map<String, Object> loadGroupSettings(Map<String, Object> message);

    /**
     * Loads event type routing rules.
     * 
     * Rules determine:
     * - Mandatory channels (always sent regardless of preferences)
     * - Optional channels (sent only if enabled in group + user)
     * 
     * Example:
     * - RESERVATION_REQUESTED:
     *   - Mandatory: [WEBSOCKET]
     *   - Optional: [EMAIL, PUSH, SMS]
     * 
     * @param eventType Event type
     * @return Map with mandatory and optional channels
     */
    protected abstract Map<String, Object> loadEventTypeRules(String eventType);

    /**
     * Calculates final channels for a recipient.
     * 
     * ALGORITHM:
     * final = mandatory ∪ (optional ∩ group ∩ user)
     * 
     * Example:
     * - mandatory = [WEBSOCKET]
     * - optional = [EMAIL, PUSH, SMS]
     * - group.enabled = [EMAIL, PUSH, WEBSOCKET]
     * - user.enabled = [EMAIL, PUSH, WEBSOCKET, SMS]
     * - optional ∩ group ∩ user = [EMAIL, PUSH] ∩ [EMAIL, PUSH, WEBSOCKET] ∩ [EMAIL, PUSH, WEBSOCKET, SMS]
     *                           = [EMAIL, PUSH]
     * - final = [WEBSOCKET] ∪ [EMAIL, PUSH] = [WEBSOCKET, EMAIL, PUSH]
     * 
     * @param mandatory Mandatory channels from event type rules
     * @param optional Optional channels from event type rules
     * @param groupEnabled Group enabled channels
     * @param userEnabled User enabled channels
     * @return Final list of channels to send
     */
    protected List<String> calculateFinalChannels(
        List<String> mandatory,
        List<String> optional,
        List<String> groupEnabled,
        List<String> userEnabled
    ) {
        List<String> result = new java.util.ArrayList<>(mandatory);
        
        // Find intersection: optional ∩ group ∩ user
        for (String optionalChannel : optional) {
            if (groupEnabled.contains(optionalChannel) && userEnabled.contains(optionalChannel)) {
                if (!result.contains(optionalChannel)) {
                    result.add(optionalChannel);
                }
            }
        }
        
        return result;
    }

    /**
     * Creates a single notification record for (recipient × channel).
     * 
     * Type-specific implementation:
     * - RestaurantUserOrchestrator: creates RestaurantUserNotification
     * - CustomerOrchestrator: creates CustomerNotification
     * - AgencyUserOrchestrator: creates AgencyUserNotification
     * - AdminOrchestrator: creates AdminNotification
     * 
     * @param eventId Disaggregated event ID (evt-5-staff1-EMAIL)
     * @param userId Recipient user ID
     * @param channel Channel type (EMAIL, PUSH, SMS, WEBSOCKET)
     * @param message Original RabbitMQ message
     * @return Notification record (type T)
     */
    protected abstract T createNotificationRecord(
        String eventId,
        Long userId,
        String channel,
        Map<String, Object> message
    );

    /**
     * Applies type-specific event rules (optional, for future extension).
     * 
     * Examples:
     * - RestaurantUserOrchestrator:
     *   - CRITICAL_RESERVATION → escalate to manager if no ACK in 5 min
     *   - URGENT_ORDER → SMS to managers only
     * 
     * - CustomerOrchestrator:
     *   - Archive notifications older than 30 days
     * 
     * - AdminOrchestrator:
     *   - CRITICAL_INCIDENT → also send to Slack
     * 
     * @param notification Notification record
     * @param message Original RabbitMQ message
     * @return Modified notification (or same if no changes)
     */
    protected T applyEventTypeRules(T notification, Map<String, Object> message) {
        // Default: no special rules
        // Override in subclasses if needed
        return notification;
    }

    /**
     * Generates disaggregated event ID from original event ID + recipient + channel.
     * 
     * Format: {eventId}_{userId}_{channel}_{timestamp}
     * 
     * @param eventId Original event ID
     * @param userId Recipient user ID
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
     * Helper: safely extract String from map
     */
    protected String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalArgumentException("Expected string for key: " + key);
    }

    /**
     * Helper: safely extract Long from map
     */
    protected Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("Expected number for key: " + key);
    }

    /**
     * Helper: safely extract and cast payload
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractPayload(Map<String, Object> message) {
        return (Map<String, Object>) message.get("payload");
    }
}
