package com.application.common.service.notification.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.agency.persistence.dao.AgencyUserNotificationDAO;
import com.application.agency.persistence.model.AgencyUserNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.common.persistence.model.notification.NotificationPriority;
import com.application.common.service.notification.preferences.NotificationBlockService;
import com.application.common.service.notification.preferences.NotificationBlockService.NotificationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ AGENCY USER ORCHESTRATOR
 * 
 * Disaggregates notifications for agency staff (agents, managers).
 * 
 * RESPONSIBILITY:
 * 1. Load agency staff recipients
 * 2. Load staff notification preferences
 * 3. Load agency group settings
 * 4. Calculate final channels per staff
 * 5. Create AgencyUserNotification records
 * 6. Apply agency-specific business rules
 * 
 * TYPE-SPECIFIC RULES:
 * - Priority-based routing: HIGH → managers only, NORMAL → all agents
 * - SMS for urgent events only
 * - Escalation to senior agent if no ACK in 10 min
 * - readByAll logic: shared notifications for broadcast events
 * 
 * RECIPIENT RESOLUTION:
 * - Load all active agents of the agency
 * - Separate by role (AGENT, MANAGER, SENIOR_AGENT)
 * - Consider on-duty status
 * 
 * EXAMPLE DISAGGREGATION:
 * Input: 1 message for BOOKING_REQUEST for agency 10
 * Processing:
 *   - Load 8 active staff of agency 10
 *   - For each staff:
 *     - Load preferences
 *     - Load agency settings (readByAll=true)
 *     - Load event rules (BOOKING_REQUEST: mandatory=[WS], optional=[EMAIL,PUSH])
 *     - Final channels = [WEBSOCKET, EMAIL]
 *     - Create 2 notification records per staff
 * Output: 16 notification records (8 staff × 2 channels)
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Extracted from EventOutboxOrchestrator)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgencyUserOrchestrator extends NotificationOrchestrator<AgencyUserNotification> {

    private final AgencyUserNotificationDAO agencyNotificationDAO;
    private final NotificationBlockService notificationBlockService;
    // TODO: Iniettare AgencyStaffService quando disponibile
    // private final AgencyStaffService staffService;
    // private final AgencyUserPreferencesService preferencesService;

    /**
     * Disaggregates message for agency staff.
     * 
     * @param message RabbitMQ message
     * @return List of disaggregated AgencyUserNotification records
     */
    @Override
    public List<AgencyUserNotification> disaggregateAndProcess(Map<String, Object> message) {
        log.info("🏛️ AgencyUserOrchestrator: starting disaggregation");
        
        List<AgencyUserNotification> disaggregated = new ArrayList<>();

        // Extract from message
        String eventId = extractString(message, "event_id");
        String eventType = extractString(message, "event_type");
        Long agencyId = extractLong(message, "agency_id");
        // aggregateType e payload sono disponibili nel message ma non usati direttamente qui

        // ═══════════════════════════════════════════════════════════════════
        // LIVELLO 0: CHECK BLOCCO GLOBALE
        // ═══════════════════════════════════════════════════════════════════
        if (notificationBlockService.isGloballyBlocked(eventType)) {
            log.info("🚫 EventType {} is globally blocked - skipping all notifications", eventType);
            return disaggregated; // Empty list
        }

        log.info("📊 Disaggregating: eventId={}, eventType={}, agencyId={}", 
            eventId, eventType, agencyId);

        // Load recipients
        List<Long> recipientStaffIds = loadRecipients(message);
        log.info("👥 Loaded {} staff recipients for agency {}", recipientStaffIds.size(), agencyId);

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
                ? NotificationContext.forAgency(agencyId, hubId)
                : NotificationContext.forAgency(agencyId);

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

                AgencyUserNotification notification = createNotificationRecord(
                    disaggregatedEventId,
                    staffId,
                    channel,
                    message
                );

                // Apply agency-specific rules
                notification = applyEventTypeRules(notification, message);

                disaggregated.add(notification);
            }
        }

        log.info("✅ Disaggregation complete: {} notification records from 1 event", disaggregated.size());
        return disaggregated;
    }

    /**
     * Loads recipients based on message type: BROADCAST (all staff) or TARGETED (specific user).
     * 
     * @param message RabbitMQ message (contains agency_id, recipient_id, recipientType)
     * @return List of recipient user IDs
     */
    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        Long agencyId = extractLong(message, "agency_id");
        String recipientType = (String) message.getOrDefault("recipientType", "TARGETED");
        
        if ("BROADCAST".equals(recipientType)) {
            log.info("📢 BROADCAST: Loading ALL active agency staff for agency {}", agencyId);
            // TODO: Inject AgencyStaffService.findActiveStaffByAgencyId(agencyId)
            return new ArrayList<>();
        } else {
            log.info("🎯 TARGETED: Loading specific recipient");
            Long recipientId = extractLong(message, "recipient_id");
            return List.of(recipientId);
        }
    }

    /**
     * Loads user preferences for agency staff.
     * 
     * Queries user_notification_preferences table for enabled channels.
     * 
     * @param staffId Agency staff user ID
     * @return List of enabled channels (EMAIL, PUSH, SMS, WEBSOCKET, etc)
     */
    @Override
    protected List<String> loadUserPreferences(Long staffId) {
        // TODO: Iniettare AgencyUserPreferencesService.getEnabledChannels(staffId)
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement AgencyUserPreferencesService.getEnabledChannels()");
        return List.of("WEBSOCKET", "EMAIL", "PUSH");
    }

    /**
     * Loads agency group notification settings.
     * 
     * Queries notification_group_settings table for agency-level preferences.
     * 
     * @param message RabbitMQ message (contains agency_id)
     * @return Map with settings: enabled_channels, quiet_hours, etc
     */
    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        // agencyId sarà usato quando NotificationGroupSettingsService sarà implementato
        @SuppressWarnings("unused")
        Long agencyId = extractLong(message, "agency_id");
        
        // TODO: Iniettare NotificationGroupSettingsService.getAgencySettings(agencyId)
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement NotificationGroupSettingsService.getAgencySettings()");
        return Map.of(
            "enabled_channels", List.of("EMAIL", "PUSH", "SMS", "WEBSOCKET"),
            "quiet_hours_enabled", false
        );
    }

    /**
     * Loads event type routing rules for agency staff.
     * 
     * Examples:
     * - BOOKING_REQUEST: mandatory=[WEBSOCKET], optional=[EMAIL,PUSH]
     * - NEW_BOOKING: mandatory=[WEBSOCKET], optional=[PUSH,EMAIL,SMS]
     * - ASSIGNMENT_AVAILABLE: mandatory=[], optional=[EMAIL,SMS]
     * - URGENT_BOOKING: mandatory=[WEBSOCKET,PUSH], optional=[SMS]
     * 
     * @param eventType Event type
     * @return Map with "mandatory" and "optional" channel lists
     */
    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        // TODO: Carica da database (event_type_routing_rules)
        // Per ora, regole hardcoded:
        
        return switch (eventType) {
            case "BOOKING_REQUEST", "NEW_BOOKING" -> Map.of(
                "mandatory", List.of("WEBSOCKET"),
                "optional", List.of("EMAIL", "PUSH", "SMS")
            );
            case "URGENT_BOOKING" -> Map.of(
                "mandatory", List.of("WEBSOCKET", "PUSH"),
                "optional", List.of("SMS", "EMAIL")
            );
            case "ASSIGNMENT_AVAILABLE" -> Map.of(
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
     * Creates an AgencyUserNotification record.
     * 
     * @param eventId Disaggregated event ID
     * @param staffId Staff user ID
     * @param channel Channel type
     * @param message Original RabbitMQ message
     * @return AgencyUserNotification record
     */
    @Override
    protected AgencyUserNotification createNotificationRecord(
        String eventId,
        Long staffId,
        String channel,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");
        String aggregateType = extractString(message, "aggregate_type");
        Long agencyId = extractLong(message, "agency_id");
        Long eventOutboxId = extractLong(message, "event_outbox_id");
        Map<String, Object> payload = extractPayload(message);

        // Null-safe extraction of properties
        @SuppressWarnings("unchecked")
        Map<String, String> props = (payload != null) 
            ? (Map<String, String>) payload.getOrDefault("properties", new java.util.HashMap<>())
            : new java.util.HashMap<>();

        boolean readByAll = determineReadByAll(eventType);
        NotificationPriority priority = determinePriority(eventType);
        
        // Null-safe extraction of title and body
        String title = (payload != null) ? (String) payload.get("title") : null;
        String body = (payload != null) ? (String) payload.get("body") : null;

        return AgencyUserNotification.builder()
            .eventId(eventId)
            .eventOutboxId(eventOutboxId)
            .userId(staffId)
            .agencyId(agencyId)
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
     * Applies agency-specific business rules.
     * 
     * Examples:
     * - Priority-based routing: HIGH → only managers
     * - SMS only for urgent events
     * - Escalation to senior agent after 10 min
     * 
     * @param notification Notification record
     * @param message Original RabbitMQ message
     * @return Modified notification
     */
    @Override
    protected AgencyUserNotification applyEventTypeRules(
        AgencyUserNotification notification,
        Map<String, Object> message
    ) {
        // eventType sarà usato quando si implementeranno regole specifiche per tipo evento
        @SuppressWarnings("unused")
        String eventType = extractString(message, "event_type");
        NotificationPriority priority = notification.getPriority();

        // Rule: HIGH priority only for managers
        if (priority == NotificationPriority.HIGH) {
            // TODO: Check if user has MANAGER role, if not, downgrade to NORMAL
            log.debug("📊 HIGH priority: filter to managers only (TODO)");
        }

        // Rule: SMS only for urgent
        if (notification.getChannel() == NotificationChannel.SMS) {
            if (priority != NotificationPriority.HIGH) {
                log.debug("📱 SMS for non-urgent: consider filtering (TODO)");
            }
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
            case "BOOKING_REQUEST", "NEW_BOOKING", "URGENT_BOOKING" -> true;
            case "ASSIGNMENT_AVAILABLE", "DIRECT_MESSAGE" -> false;
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
            case "URGENT_BOOKING", "CRITICAL_ASSIGNMENT" -> NotificationPriority.HIGH;
            case "BOOKING_REQUEST", "NEW_BOOKING" -> NotificationPriority.HIGH;
            default -> NotificationPriority.NORMAL;
        };
    }
}
