package com.application.common.persistence.model.reservation;

import java.time.LocalDateTime;

import com.application.common.persistence.model.user.AbstractUser;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit trail entity for tracking changes to reservations.
 * Stores detailed information about each modification, including what changed and who made the change.
 */
@Hidden
@Entity
@Table(
    name = "reservation_audit",
    indexes = {
        @Index(name = "idx_audit_reservation", columnList = "reservation_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_changed_at", columnList = "changed_at DESC"),
        @Index(name = "idx_audit_reservation_action", columnList = "reservation_id, action"),
        @Index(name = "idx_audit_reservation_changed_at", columnList = "reservation_id, changed_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the parent reservation being audited.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /**
     * Type of action that was performed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    /**
     * JSON array of field changes in format: [{field, oldValue, newValue}, ...]
     * Null for CREATED action (no previous state).
     */
    @Column(name = "changed_fields", columnDefinition = "JSON")
    private String changedFields;

    /**
     * Reason for the change (e.g., rejection reason, modification reason).
     * Optional - populated only when relevant.
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * Timestamp when the change was made.
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /**
     * User who made the change.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private AbstractUser changedBy;

    /**
     * Enum for audit action types.
     */
    public enum AuditAction {
        CREATED,                        // Reservation was created
        UPDATED,                        // Reservation details were updated
        STATUS_CHANGED,                 // Status changed (NOT_ACCEPTED → ACCEPTED, etc.)
        MODIFICATION_REQUESTED,         // Customer requested a modification
        MODIFICATION_APPROVED,          // Restaurant approved a modification request
        MODIFICATION_REJECTED,          // Restaurant rejected a modification request
        MODIFICATION_APPLIED,           // Approved modification was applied to reservation
        MODIFIED_BY_RESTAURANT,         // Restaurant staff modified directly (no approval needed)
        DELETED                         // Reservation was deleted
    }
}
