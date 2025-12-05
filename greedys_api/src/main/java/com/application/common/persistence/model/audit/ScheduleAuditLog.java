package com.application.common.persistence.model.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit log for schedule-related changes.
 * 
 * Tracks modifications to:
 * - ServiceSlotConfig (slot generation rules)
 * - ServiceDay (weekly schedule templates)
 * - AvailabilityException (date-specific overrides)
 * 
 * Each record captures:
 * - WHAT was changed (entity_type, entity_id)
 * - WHO changed it (user_id)
 * - WHEN it was changed (changed_at)
 * - WHAT was the old value (old_value as JSON)
 * - WHY it was changed (change_reason, optional)
 */
@Entity
@Table(name = "schedule_audit_log", indexes = {
    @Index(name = "idx_schedule_audit_service_id", columnList = "service_id"),
    @Index(name = "idx_schedule_audit_restaurant_id", columnList = "restaurant_id"),
    @Index(name = "idx_schedule_audit_changed_at", columnList = "changed_at"),
    @Index(name = "idx_schedule_audit_entity", columnList = "entity_type,entity_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of entity that was modified
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    /**
     * ID of the modified entity
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Service this change belongs to (for quick filtering)
     */
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    /**
     * Restaurant this change belongs to (for quick filtering)
     */
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    /**
     * Action performed
     */
    @Column(name = "action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    /**
     * User who made the change
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * JSON snapshot of the entity BEFORE the change
     * NULL for CREATED actions
     */
    @Column(name = "old_value", columnDefinition = "JSON")
    private String oldValue;

    /**
     * JSON snapshot of the entity AFTER the change
     * NULL for DELETED actions, optional for UPDATED
     */
    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue;

    /**
     * Optional reason for the change (user-provided)
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * When the change was made
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /**
     * Types of schedule entities that can be audited
     */
    public enum EntityType {
        /**
         * Slot configuration (duration, max concurrent, buffer)
         */
        SLOT_CONFIG,
        
        /**
         * Day schedule (opening/closing times per day of week)
         */
        DAY_SCHEDULE,
        
        /**
         * Availability exception (closures, special hours)
         */
        AVAILABILITY_EXCEPTION,
        
        /**
         * Service itself (name, color, active status)
         */
        SERVICE
    }

    /**
     * Types of audit actions
     */
    public enum AuditAction {
        CREATED,
        UPDATED,
        DELETED,
        ACTIVATED,
        DEACTIVATED
    }

    @Override
    public String toString() {
        return "ScheduleAuditLog{" +
                "id=" + id +
                ", entityType=" + entityType +
                ", entityId=" + entityId +
                ", serviceId=" + serviceId +
                ", action=" + action +
                ", userId=" + userId +
                ", changedAt=" + changedAt +
                '}';
    }
}
