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
 * Audit log for reservation changes.
 * 
 * Tracks all modifications to reservations including:
 * - Creation
 * - Status changes (accepted, rejected, cancelled, seated, no-show)
 * - Data changes (pax, time, notes)
 * - Terms changes (when snapshot values are updated)
 * 
 * Each record captures:
 * - WHAT reservation was changed
 * - WHO changed it (customer, restaurant staff, system)
 * - WHEN it was changed
 * - WHAT was the old value
 * - WHY it was changed (optional reason)
 * - For terms changes: was customer notified? did they accept?
 */
@Entity
@Table(name = "reservation_audit_log", indexes = {
    @Index(name = "idx_reservation_audit_reservation_id", columnList = "reservation_id"),
    @Index(name = "idx_reservation_audit_restaurant_id", columnList = "restaurant_id"),
    @Index(name = "idx_reservation_audit_group_booking_id", columnList = "group_booking_id"),
    @Index(name = "idx_reservation_audit_changed_at", columnList = "changed_at"),
    @Index(name = "idx_reservation_audit_action", columnList = "action")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the reservation that was modified
     */
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    /**
     * Restaurant ID (for quick filtering)
     */
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    /**
     * Group booking ID (if this is a group booking action)
     */
    @Column(name = "group_booking_id")
    private Long groupBookingId;

    /**
     * Action performed
     */
    @Column(name = "action", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ReservationAuditAction action;

    /**
     * User who made the change
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Type of user who made the change
     */
    @Column(name = "user_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    /**
     * Specific field that was changed (for UPDATED actions)
     * e.g., "status", "pax", "bookedSlotDuration"
     */
    @Column(name = "field_changed", length = 50)
    private String fieldChanged;

    /**
     * Old value (JSON or simple string depending on field)
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * New value (JSON or simple string depending on field)
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Optional reason for the change
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * For TERMS_CHANGED: was the customer notified?
     */
    @Column(name = "customer_notified")
    private Boolean customerNotified;

    /**
     * For TERMS_CHANGED: did the customer accept?
     */
    @Column(name = "customer_accepted")
    private Boolean customerAccepted;

    /**
     * When the change was made
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /**
     * Types of audit actions for reservations
     */
    public enum ReservationAuditAction {
        /**
         * Reservation was created
         */
        CREATED,
        
        /**
         * Reservation data was updated (pax, notes, time, etc.)
         */
        UPDATED,
        
        /**
         * Reservation status changed
         */
        STATUS_CHANGED,
        
        /**
         * Reservation was cancelled
         */
        CANCELLED,
        
        /**
         * Snapshot terms were changed (e.g., duration reduced)
         */
        TERMS_CHANGED,
        
        /**
         * Table was assigned
         */
        TABLE_ASSIGNED,
        
        /**
         * Customer was seated
         */
        SEATED,
        
        /**
         * Customer didn't show up
         */
        NO_SHOW,
        
        // ─────── GROUP BOOKING ACTIONS ───────
        
        /**
         * Group booking inquiry received
         */
        GROUP_INQUIRY_RECEIVED,
        
        /**
         * Quote sent for group booking
         */
        GROUP_QUOTE_SENT,
        
        /**
         * Group booking confirmed
         */
        GROUP_CONFIRMED,
        
        /**
         * Deposit paid for group booking
         */
        GROUP_DEPOSIT_PAID,
        
        /**
         * Full payment received for group booking
         */
        GROUP_FULLY_PAID,
        
        /**
         * Group booking linked to reservation
         */
        GROUP_LINKED_TO_RESERVATION,
        
        /**
         * Group booking details updated (pax, menu, dietary, etc.)
         */
        GROUP_DETAILS_UPDATED,
        
        /**
         * Fixed price menu selected for group booking
         */
        GROUP_MENU_SELECTED,
        
        /**
         * Dietary needs updated for group booking
         */
        GROUP_DIETARY_UPDATED,
        
        // ─────── MODIFICATION REQUEST ACTIONS ───────
        
        /**
         * Customer requested modification
         */
        MODIFICATION_REQUESTED,
        
        /**
         * Modification request approved
         */
        MODIFICATION_APPROVED,
        
        /**
         * Modification request rejected
         */
        MODIFICATION_REJECTED,
        
        /**
         * Restaurant directly modified reservation
         */
        MODIFIED_BY_RESTAURANT
    }

    /**
     * Type of user who made the change
     */
    public enum UserType {
        CUSTOMER,
        RESTAURANT_USER,
        ADMIN,
        SYSTEM
    }

    @Override
    public String toString() {
        return "ReservationAuditLog{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", action=" + action +
                ", userType=" + userType +
                ", changedAt=" + changedAt +
                '}';
    }
}
