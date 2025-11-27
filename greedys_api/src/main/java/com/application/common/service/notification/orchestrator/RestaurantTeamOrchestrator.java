package com.application.common.service.notification.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.restaurant.persistence.dao.RestaurantUserNotificationDAO;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.model.RestaurantUserNotification;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.common.persistence.model.notification.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RESTAURANT TEAM ORCHESTRATOR
 * 
 * Disaggregates team-scoped RESERVATION notifications for restaurant staff.
 * 
 * KEY DIFFERENCE FROM RestaurantUserOrchestrator:
 * - **Scope**: TEAM notifications (all staff see the same notification)
 * - **readByAll**: ALWAYS true (shared across team, not personal)
 * - **Destination**: `/topic/restaurant/{restaurantId}/reservations` (team channel)
 * - **Recipients**: ALL active staff of the restaurant (no preference filtering for team scope)
 * - **Queue**: Listens to "notification.restaurant.reservations" (TEAM queue)
 * 
 * RESPONSIBILITY:
 * 1. Load ALL restaurant staff recipients (no filtering)
 * 2. Load restaurant group settings (team scope only)
 * 3. Load event type routing rules for RESERVATION events
 * 4. Calculate final channels per staff member
 * 5. Create RestaurantUserNotification records with read_by_all=true
 * 6. Apply team-specific business rules
 * 
 * ROUTING CONTEXT:
 * EventOutboxOrchestrator routes RESERVATION events here when:
 * - initiated_by=CUSTOMER → notification.restaurant.reservations (TEAM scope)
 * 
 * EXAMPLE DISAGGREGATION:
 * Input: 1 message for RESERVATION_NEW initiated by CUSTOMER for restaurant 5
 * Processing:
 *   - Load ALL 10 active staff of restaurant 5 (no preference filtering)
 *   - For each staff:
 *     - Calculate team channels: WEBSOCKET + EMAIL/PUSH/SMS per event rules
 *     - Create RestaurantUserNotification with:
 *       * read_by_all=true (TEAM SHARED)
 *       * destination="/topic/restaurant/5/reservations"
 *       * priority=HIGH (reservation events)
 *   - WebSocket delivery sent to team channel (not personal user channel)
 * Output: 20 notification records (10 staff × 2 channels, all read_by_all=true)
 * 
 * PROPERTIES:
 * - destination: "/topic/restaurant/{restaurantId}/reservations" (TEAM WebSocket channel)
 * - read_by_all: true (marks as shared team notification)
 * - priority: HIGH (reservations are important)
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Created for team-scoped RESERVATION notifications)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantTeamOrchestrator extends NotificationOrchestrator<RestaurantUserNotification> {

    @SuppressWarnings("unused")
    private final RestaurantUserNotificationDAO restaurantNotificationDAO;
    private final RUserDAO rUserDAO;
    // TODO: Iniettare RestaurantStaffService quando disponibile
    // private final RestaurantStaffService staffService;

    /**
     * Disaggregates TEAM-scoped RESERVATION messages for restaurant staff.
     * 
     * All notifications created have read_by_all=true (team shared).
     * No personal preference filtering applied.
     * 
     * @param message RabbitMQ message from notification.restaurant.reservations queue
     * @return List of disaggregated RestaurantUserNotification records (all with read_by_all=true)
     */
    @Override
    public List<RestaurantUserNotification> disaggregateAndProcess(Map<String, Object> message) {
        log.info("🏢👥 RestaurantTeamOrchestrator: starting TEAM-scoped disaggregation");
        
        List<RestaurantUserNotification> disaggregated = new ArrayList<>();

        // Extract from message
        String eventId = extractString(message, "event_id");
        String eventType = extractString(message, "event_type");
        Long restaurantId = extractLong(message, "restaurant_id");

        log.info("📊 Team Disaggregating: eventId={}, eventType={}, restaurantId={}, scope=TEAM", 
            eventId, eventType, restaurantId);

        // Load recipients (ALL staff, no filtering for team scope)
        List<Long> recipientStaffIds = loadRecipients(message);
        log.info("👥 Loaded {} staff recipients for TEAM notification at restaurant {}", 
            recipientStaffIds.size(), restaurantId);

        // Load event rules
        Map<String, Object> eventRules = loadEventTypeRules(eventType);
        @SuppressWarnings("unchecked")
        List<String> mandatoryChannels = (List<String>) eventRules.get("mandatory");
        @SuppressWarnings("unchecked")
        List<String> optionalChannels = (List<String>) eventRules.get("optional");

        // Load group settings (team scope)
        Map<String, Object> groupSettings = loadGroupSettings(message);
        @SuppressWarnings("unchecked")
        List<String> groupEnabledChannels = (List<String>) groupSettings.get("enabled_channels");

        // For each staff member, create TEAM notification
        for (Long staffId : recipientStaffIds) {
            // NO preference filtering for TEAM scope - all channels from group settings
            List<String> teamChannels = calculateFinalChannels(
                mandatoryChannels,
                optionalChannels,
                groupEnabledChannels,
                groupEnabledChannels  // Team scope: use group settings, not personal preferences
            );

            log.debug("📋 Staff {}: team channels = {} (from group settings)", staffId, teamChannels);

            // Create notification record per channel
            for (String channel : teamChannels) {
                String disaggregatedEventId = generateDisaggregatedEventId(eventId, staffId, channel);

                RestaurantUserNotification notification = createNotificationRecord(
                    disaggregatedEventId,
                    staffId,
                    channel,
                    message
                );

                // Ensure read_by_all=true for TEAM scope
                notification.setReadByAll(true);

                // Apply team-specific rules
                notification = applyTeamEventTypeRules(notification, message);

                disaggregated.add(notification);
            }
        }

        log.info("✅ Team Disaggregation complete: {} notification records from 1 event (all TEAM-scoped)", 
            disaggregated.size());
        return disaggregated;
    }

    /**
     * Loads ALL active restaurant staff as recipients for TEAM notifications.
     * 
     * TEAM scope: No filtering by preferences or roles.
     * All active staff receive team notifications about reservations.
     * 
     * @param message RabbitMQ message (contains restaurant_id)
     * @return List of ALL active staff user IDs
     */
    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        Long restaurantId = extractLong(message, "restaurant_id");
        
        // TEAM scope: Load ALL active staff (no filtering)
        log.info("📢 TEAM SCOPE: Loading ALL active staff for restaurant {}", restaurantId);
        
        // Query all restaurant users (staff members) for this restaurant
        List<RUser> staffMembers = (List<RUser>) rUserDAO.findByRestaurantId(restaurantId);
        List<Long> staffIds = new ArrayList<>();
        
        for (RUser staff : staffMembers) {
            if (staff != null && staff.getId() != null) {
                staffIds.add(staff.getId());
            }
        }
        
        log.info("✅ Loaded {} active staff members for restaurant {} (TEAM notification)", 
            staffIds.size(), restaurantId);
        
        return staffIds;
    }

    /**
     * TEAM notifications don't use individual user preferences.
     * 
     * Returns empty list (team scope ignores personal preferences).
     * Channel calculation uses only group settings.
     * 
     * @param staffId Staff user ID (ignored for team scope)
     * @return Empty list (team scope)
     */
    @Override
    protected List<String> loadUserPreferences(Long staffId) {
        log.debug("⚠️  TEAM scope: Ignoring user preferences for staffId {} (using group settings only)", staffId);
        return new ArrayList<>();  // Team scope: no personal preferences
    }

    /**
     * Loads restaurant group notification settings for TEAM scope.
     * 
     * For TEAM notifications, uses only group-level settings:
     * - All staff receive same channels
     * - No quiet hours
     * - High priority for reservations
     * 
     * @param message RabbitMQ message (contains restaurant_id)
     * @return Map with settings: enabled_channels, priority, etc
     */
    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        // TODO: Iniettare NotificationGroupSettingsService.getRestaurantSettings(restaurantId)
        // For TEAM scope, use full channel set (no quiet hours)
        log.warn("⚠️  TODO: Implement NotificationGroupSettingsService.getRestaurantSettings()");
        return Map.of(
            "enabled_channels", List.of("WEBSOCKET", "EMAIL", "PUSH", "SMS"),
            "quiet_hours_enabled", false,  // TEAM notifications ignore quiet hours
            "scope", "TEAM"
        );
    }

    /**
     * Loads event type routing rules for TEAM-scoped RESERVATION events.
     * 
     * RESERVATION events routed to team scope:
     * - RESERVATION_NEW: Customer created new reservation
     * - RESERVATION_MODIFY: Customer modified reservation
     * - RESERVATION_CANCEL: Customer cancelled reservation
     * 
     * @param eventType Event type
     * @return Map with "mandatory" and "optional" channel lists for team
     */
    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        // TEAM scope: Reservation events always get high-priority channels
        
        return switch (eventType) {
            case "RESERVATION_NEW", "RESERVATION_MODIFY", "RESERVATION_CANCEL" -> Map.of(
                "mandatory", List.of("WEBSOCKET"),  // All team gets WebSocket
                "optional", List.of("EMAIL", "PUSH", "SMS")  // Additional delivery channels
            );
            default -> Map.of(
                "mandatory", List.of("WEBSOCKET"),
                "optional", List.of("EMAIL", "PUSH", "SMS")
            );
        };
    }

    /**
     * Creates a RestaurantUserNotification record for TEAM scope.
     * 
     * Key differences from personal:
     * - read_by_all=true (always)
     * - destination="/topic/restaurant/{restaurantId}/reservations"
     * - All staff see same notification
     * 
     * @param eventId Disaggregated event ID
     * @param staffId Staff user ID (record is created for this user, but visible to all)
     * @param channel Channel type
     * @param message Original RabbitMQ message
     * @return RestaurantUserNotification record with read_by_all=true
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

        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) payload.getOrDefault("properties", new java.util.HashMap<>());

        // TEAM scope: Always readByAll, high priority for reservations
        boolean readByAll = true;  // ALWAYS true for team scope
        NotificationPriority priority = NotificationPriority.HIGH;  // Reservations are important

        // Set team-specific destination for WebSocket
        if ("WEBSOCKET".equals(channel)) {
            String teamDestination = "/topic/restaurant/" + restaurantId + "/reservations";
            props.put("destination", teamDestination);
            log.info("🌐🌐🌐 [TEAM-DESTINATION-SET] ✅ Set TEAM WebSocket destination: {} for staffId={}, eventId={}", 
                teamDestination, staffId, eventId);
        } else {
            log.info("🌐 [TEAM-NO-WEBSOCKET] Channel is NOT WEBSOCKET (channel={}), no destination set for staffId={}", 
                channel, staffId);
        }

        log.info("📝📝📝 [TEAM-RECORD-CREATE] Creating RestaurantUserNotification: eventId={}, staffId={}, channel={}, restaurantId={}, readByAll={}, destination={}", 
            eventId, staffId, channel, restaurantId, readByAll, props.get("destination"));

        // ⭐ Generate title and body from event type and payload data
        String title = generateNotificationTitle(eventType, payload);
        String body = generateNotificationBody(eventType, payload);

        return RestaurantUserNotification.builder()
            .eventId(eventId)
            .eventOutboxId(eventOutboxId)
            .userId(staffId)
            .restaurantId(restaurantId)
            .channel(NotificationChannel.valueOf(channel))
            .status(DeliveryStatus.PENDING)
            .readByAll(readByAll)  // TEAM SHARED
            .priority(priority)
            .title(title)
            .body(body)
            .eventType(eventType)
            .aggregateType(aggregateType)
            .properties(props)
            .build();
    }

    /**
     * Applies team-specific business rules for RESERVATION notifications.
     * 
     * TEAM scope rules:
     * - All staff equal visibility (no role-based filtering)
     * - High priority (reservations critical to restaurant operations)
     * - Delivery to team WebSocket channel
     * 
     * @param notification Notification record
     * @param message Original RabbitMQ message
     * @return Modified notification
     */
    private RestaurantUserNotification applyTeamEventTypeRules(
        RestaurantUserNotification notification,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");

        log.debug("📋 Applying TEAM rules for eventType={}, channel={}", 
            eventType, notification.getChannel());

        // Ensure read_by_all is true
        notification.setReadByAll(true);

        // Ensure HIGH priority for all reservation events in team scope
        if (eventType.startsWith("RESERVATION_")) {
            notification.setPriority(NotificationPriority.HIGH);
            log.debug("🔴 RESERVATION event: set HIGH priority for team scope");
        }

        return notification;
    }

    /**
     * ⭐ GENERATE NOTIFICATION TITLE
     * Creates appropriate title based on event type
     * 
     * @param eventType Type of event (RESERVATION_REQUESTED, RESERVATION_CANCELLED, etc)
     * @param payload Event payload containing details
     * @return Notification title
     */
    private String generateNotificationTitle(String eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case "RESERVATION_REQUESTED" -> "Nuova Prenotazione";
            case "RESERVATION_CANCELLED" -> "Prenotazione Cancellata";
            case "RESERVATION_MODIFIED" -> "Prenotazione Modificata";
            case "RESERVATION_CONFIRMED" -> "Prenotazione Confermata";
            case "RESERVATION_REJECTED" -> "Prenotazione Rifiutata";
            default -> "Notifica Ristorante";
        };
    }

    /**
     * ⭐ GENERATE NOTIFICATION BODY
     * Creates appropriate body text based on event type and payload
     * 
     * @param eventType Type of event
     * @param payload Event payload with reservation details
     * @return Notification body text
     */
    private String generateNotificationBody(String eventType, Map<String, Object> payload) {
        if (payload == null) {
            return "Nuova notifica dal ristorante";
        }
        
        Object paxObj = payload.get("pax");
        Object dateObj = payload.get("date");
        Object nameObj = payload.getOrDefault("customerName", payload.get("email"));
        
        int pax = paxObj instanceof Number ? ((Number) paxObj).intValue() : 1;
        String date = dateObj != null ? dateObj.toString() : "Data non specificata";
        String name = nameObj != null ? nameObj.toString() : "Cliente";
        
        return switch (eventType) {
            case "RESERVATION_REQUESTED" -> 
                String.format("Nuova prenotazione da %s per %d persone il %s", name, pax, date);
            case "RESERVATION_CANCELLED" -> 
                String.format("Prenotazione di %s per %d persone cancellata", name, pax);
            case "RESERVATION_MODIFIED" -> 
                String.format("Prenotazione di %s modificata - %d persone il %s", name, pax, date);
            case "RESERVATION_CONFIRMED" -> 
                String.format("Prenotazione di %s confermata per %d persone", name, pax);
            case "RESERVATION_REJECTED" -> 
                String.format("Prenotazione di %s rifiutata", name);
            default -> "Nuova notifica dal ristorante";
        };
    }
}
