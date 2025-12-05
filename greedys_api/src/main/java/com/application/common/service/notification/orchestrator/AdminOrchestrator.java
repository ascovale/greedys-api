package com.application.common.service.notification.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.admin.persistence.dao.AdminNotificationDAO;
import com.application.admin.persistence.model.AdminNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.common.persistence.model.notification.NotificationPriority;
import com.application.common.service.notification.preferences.NotificationBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ ADMIN ORCHESTRATOR
 * 
 * Disaggregates notifications for system admins.
 * 
 * RESPONSIBILITY:
 * 1. Load admin recipients
 * 2. Load admin notification preferences
 * 3. Load admin group settings
 * 4. Calculate final channels per admin
 * 5. Create AdminNotification records
 * 6. Apply admin-specific business rules
 * 
 * TYPE-SPECIFIC RULES:
 * - All admins receive system-critical notifications
 * - Priority-based routing: CRITICAL → all admins, NORMAL → ops team
 * - SMS for critical incidents
 * - Slack integration for incident management
 * - Email for audit trail
 * 
 * RECIPIENT RESOLUTION:
 * - Load all active system admins
 * - Consider admin level (SUPER_ADMIN, ADMIN, OPS_LEAD)
 * - Consider on-duty status
 * 
 * EXAMPLE DISAGGREGATION:
 * Input: 1 message for SYSTEM_ERROR for all admins
 * Processing:
 *   - Load 5 active admins
 *   - For each admin:
 *     - Load preferences (channels: [EMAIL, PUSH, SMS])
 *     - Load admin settings (readByAll=true)
 *     - Load event rules (SYSTEM_ERROR: mandatory=[EMAIL,SMS], optional=[PUSH,SLACK])
 *     - Final channels = [EMAIL, SMS, PUSH, SLACK]
 *     - Create 4 notification records per admin
 * Output: 20 notification records (5 admins × 4 channels)
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Extracted from EventOutboxOrchestrator)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminOrchestrator extends NotificationOrchestrator<AdminNotification> {

    private final AdminNotificationDAO adminNotificationDAO;
    private final NotificationBlockService notificationBlockService;
    // TODO: Iniettare AdminService quando disponibile
    // private final AdminService adminService;
    // private final AdminPreferencesService preferencesService;

    /**
     * Disaggregates message for admins.
     * 
     * @param message RabbitMQ message
     * @return List of disaggregated AdminNotification records
     */
    @Override
    public List<AdminNotification> disaggregateAndProcess(Map<String, Object> message) {
        log.info("👨‍💼 AdminOrchestrator: starting disaggregation");
        
        List<AdminNotification> disaggregated = new ArrayList<>();

        // Extract from message
        String eventId = extractString(message, "event_id");
        String eventType = extractString(message, "event_type");
        // aggregateType e payload sono disponibili nel message ma non usati direttamente qui

        // ═══════════════════════════════════════════════════════════════════
        // LIVELLO 0: CHECK BLOCCO GLOBALE
        // ═══════════════════════════════════════════════════════════════════
        if (notificationBlockService.isGloballyBlocked(eventType)) {
            log.info("🚫 EventType {} is globally blocked - skipping all notifications", eventType);
            return disaggregated; // Empty list
        }

        log.info("📊 Disaggregating: eventId={}, eventType={}", 
            eventId, eventType);

        // Load recipients
        List<Long> recipientAdminIds = loadRecipients(message);
        log.info("👥 Loaded {} admin recipients", recipientAdminIds.size());

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

        // For each admin
        for (Long adminId : recipientAdminIds) {
            // Load admin preferences
            List<String> userEnabledChannels = loadUserPreferences(adminId);

            // Calculate final channels
            List<String> finalChannels = calculateFinalChannels(
                mandatoryChannels,
                optionalChannels,
                groupEnabledChannels,
                userEnabledChannels
            );

            log.debug("📋 Admin {}: final channels = {}", adminId, finalChannels);

            // Create notification record per channel
            for (String channel : finalChannels) {
                // ═══════════════════════════════════════════════════════════════════
                // CHECK BLOCCHI LIVELLO 1-4 PER CANALE
                // Admin non ha organization/hub context, passa null
                // ═══════════════════════════════════════════════════════════════════
                if (!notificationBlockService.canSendNotification(eventType, channel, adminId, null)) {
                    log.debug("🚫 Notification blocked for admin {} on channel {} for eventType {}", 
                        adminId, channel, eventType);
                    continue; // Skip this channel
                }

                String disaggregatedEventId = generateDisaggregatedEventId(eventId, adminId, channel);

                AdminNotification notification = createNotificationRecord(
                    disaggregatedEventId,
                    adminId,
                    channel,
                    message
                );

                // Apply admin-specific rules
                notification = applyEventTypeRules(notification, message);

                disaggregated.add(notification);
            }
        }

        log.info("✅ Disaggregation complete: {} notification records from 1 event", disaggregated.size());
        return disaggregated;
    }

    /**
     * Loads recipients based on message type: BROADCAST (all admins) or TARGETED (specific user).
     * 
     * @param message RabbitMQ message (contains recipient_id, recipientType)
     * @return List of recipient user IDs
     */
    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        String recipientType = (String) message.getOrDefault("recipientType", "TARGETED");
        
        if ("BROADCAST".equals(recipientType)) {
            log.info("📢 BROADCAST: Loading ALL active system admins");
            // TODO: Inject AdminService.findActiveAdmins()
            return new ArrayList<>();
        } else {
            log.info("🎯 TARGETED: Loading specific recipient");
            Long recipientId = extractLong(message, "recipient_id");
            return List.of(recipientId);
        }
    }

    /**
     * Loads admin notification preferences.
     * 
     * Queries user_notification_preferences table for enabled channels.
     * 
     * @param adminId Admin user ID
     * @return List of enabled channels (EMAIL, PUSH, SMS, WEBSOCKET, SLACK, etc)
     */
    @Override
    protected List<String> loadUserPreferences(Long adminId) {
        // TODO: Iniettare AdminPreferencesService.getEnabledChannels(adminId)
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement AdminPreferencesService.getEnabledChannels()");
        return List.of("EMAIL", "PUSH", "SMS", "WEBSOCKET", "SLACK");
    }

    /**
     * Loads admin group notification settings.
     * 
     * Queries notification_group_settings table for admin-level preferences.
     * 
     * @param message RabbitMQ message
     * @return Map with settings: enabled_channels, incident_tracking, etc
     */
    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        // TODO: Iniettare NotificationGroupSettingsService.getAdminSettings()
        // For now, return default (stub)
        log.warn("⚠️  TODO: Implement NotificationGroupSettingsService.getAdminSettings()");
        return Map.of(
            "enabled_channels", List.of("EMAIL", "PUSH", "SMS", "WEBSOCKET", "SLACK"),
            "incident_tracking_enabled", true,
            "pagerduty_integration", true
        );
    }

    /**
     * Loads event type routing rules for admins.
     * 
     * Examples:
     * - SYSTEM_ERROR: mandatory=[EMAIL,SMS], optional=[PUSH,SLACK]
     * - DATABASE_ALERT: mandatory=[EMAIL], optional=[PUSH,SMS,SLACK]
     * - SECURITY_INCIDENT: mandatory=[SMS,SLACK], optional=[EMAIL,PUSH]
     * - RESOURCE_QUOTA: mandatory=[EMAIL], optional=[SLACK,PUSH]
     * - SUPPORT_*: Support ticket events routed to notification.admin queue
     * 
     * @param eventType Event type
     * @return Map with "mandatory" and "optional" channel lists
     */
    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        // TODO: Carica da database (event_type_routing_rules)
        // Per ora, regole hardcoded:
        
        return switch (eventType) {
            // System critical events
            case "SYSTEM_ERROR", "CRITICAL_INCIDENT" -> Map.of(
                "mandatory", List.of("EMAIL", "SMS"),
                "optional", List.of("PUSH", "SLACK", "WEBSOCKET")
            );
            case "DATABASE_ALERT", "SERVICE_DOWN" -> Map.of(
                "mandatory", List.of("EMAIL"),
                "optional", List.of("PUSH", "SMS", "SLACK")
            );
            case "SECURITY_INCIDENT" -> Map.of(
                "mandatory", List.of("SMS", "SLACK"),
                "optional", List.of("EMAIL", "PUSH")
            );
            case "RESOURCE_QUOTA" -> Map.of(
                "mandatory", List.of("EMAIL"),
                "optional", List.of("SLACK", "PUSH", "WEBSOCKET")
            );
            
            // Support ticket events (from notification.admin queue)
            case "SUPPORT_TICKET_CREATED" -> Map.of(
                "mandatory", List.of("EMAIL", "WEBSOCKET"),
                "optional", List.of("PUSH", "SLACK")
            );
            case "SUPPORT_TICKET_ESCALATED" -> Map.of(
                "mandatory", List.of("EMAIL", "SMS"),
                "optional", List.of("PUSH", "SLACK", "WEBSOCKET")
            );
            case "SUPPORT_TICKET_RESOLVED" -> Map.of(
                "mandatory", List.of("EMAIL"),
                "optional", List.of("PUSH", "WEBSOCKET")
            );
            case "SUPPORT_TICKET_REOPENED" -> Map.of(
                "mandatory", List.of("EMAIL", "WEBSOCKET"),
                "optional", List.of("PUSH", "SLACK")
            );
            
            default -> Map.of(
                "mandatory", List.of("EMAIL"),
                "optional", List.of("PUSH", "SLACK", "WEBSOCKET")
            );
        };
    }

    /**
     * Creates an AdminNotification record.
     * 
     * @param eventId Disaggregated event ID
     * @param adminId Admin user ID
     * @param channel Channel type
     * @param message Original RabbitMQ message
     * @return AdminNotification record
     */
    @Override
    protected AdminNotification createNotificationRecord(
        String eventId,
        Long adminId,
        String channel,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");
        String aggregateType = extractString(message, "aggregate_type");
        Long eventOutboxId = extractLong(message, "event_outbox_id");
        Map<String, Object> payload = extractPayload(message);

        // Null-safe extraction of properties
        @SuppressWarnings("unchecked")
        Map<String, String> props = (payload != null) 
            ? (Map<String, String>) payload.getOrDefault("properties", new java.util.HashMap<>())
            : new java.util.HashMap<>();

        NotificationPriority priority = determinePriority(eventType);
        
        // Null-safe extraction of title and body
        String title = (payload != null) ? (String) payload.get("title") : null;
        String body = (payload != null) ? (String) payload.get("body") : null;

        return AdminNotification.builder()
            .eventId(eventId)
            .eventOutboxId(eventOutboxId)
            .userId(adminId)
            .channel(NotificationChannel.valueOf(channel))
            .status(DeliveryStatus.PENDING)
            .priority(priority)
            .title(title)
            .body(body)
            .eventType(eventType)
            .aggregateType(aggregateType)
            .properties(props)
            .build();
    }

    /**
     * Applies admin-specific business rules.
     * 
     * Examples:
     * - CRITICAL priority → track incident + create ticket
     * - SECURITY_INCIDENT → trigger incident response workflow
     * - Always track for audit trail
     * 
     * @param notification Notification record
     * @param message Original RabbitMQ message
     * @return Modified notification
     */
    @Override
    protected AdminNotification applyEventTypeRules(
        AdminNotification notification,
        Map<String, Object> message
    ) {
        String eventType = extractString(message, "event_type");
        NotificationPriority priority = notification.getPriority();

        // Rule: HIGH priority events are tracked with detailed status
        if (priority == NotificationPriority.HIGH) {
            log.debug("🚨 HIGH PRIORITY: detailed tracking enabled");
        }

        // Rule: SECURITY_INCIDENT → escalate immediately
        if (eventType.equals("SECURITY_INCIDENT")) {
            notification.setPriority(NotificationPriority.HIGH);
            log.debug("🔒 SECURITY_INCIDENT: high priority enabled");
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
        if (eventType == null) return NotificationPriority.NORMAL;
        
        return switch (eventType) {
            // Critical system events
            case "SYSTEM_ERROR", "CRITICAL_INCIDENT", "SECURITY_INCIDENT", "SERVICE_DOWN" -> NotificationPriority.HIGH;
            case "DATABASE_ALERT", "RESOURCE_QUOTA" -> NotificationPriority.NORMAL;
            
            // Support ticket events
            case "SUPPORT_TICKET_ESCALATED" -> NotificationPriority.HIGH;
            case "SUPPORT_TICKET_CREATED", "SUPPORT_TICKET_REOPENED" -> NotificationPriority.NORMAL;
            case "SUPPORT_TICKET_RESOLVED" -> NotificationPriority.LOW;
            
            default -> NotificationPriority.NORMAL;
        };
    }
}
