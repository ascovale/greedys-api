package com.application.common.persistence.model.reservation;

import java.time.LocalDate;
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
 * Entity for tracking reservation modification requests.
 * 
 * Logica:
 * - Se il CUSTOMER richiede una modifica → ReservationModificationRequest creata in stato PENDING_APPROVAL
 * - Se RestaurantUser/Admin applica modifica direttamente → Modifica è applicata direttamente al Reservation (no request needed)
 * 
 * Stati possibili:
 * - PENDING_APPROVAL: Customer ha richiesto modifica, in attesa dell'approvazione dal restaurant
 * - APPROVED: RestaurantUser/Admin ha approvato la modifica
 * - REJECTED: RestaurantUser/Admin ha rifiutato la modifica
 * - APPLIED: La modifica è stata applicata al reservation originale
 * - CANCELLED: Il customer ha cancellato la richiesta
 */
@Hidden
@Entity
@Table(
    name = "reservation_modification_request",
    indexes = {
        @Index(name = "idx_mod_req_reservation", columnList = "reservation_id"),
        @Index(name = "idx_mod_req_status", columnList = "status"),
        @Index(name = "idx_mod_req_requested_at", columnList = "requested_at DESC"),
        @Index(name = "idx_mod_req_reservation_status", columnList = "reservation_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationModificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the original reservation being modified
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /**
     * Status of the modification request
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING_APPROVAL;

    // ============================================
    // ORIGINAL VALUES (for reference/comparison)
    // ============================================

    @Column(name = "original_date")
    private LocalDate originalDate;

    @Column(name = "original_datetime")
    private LocalDateTime originalDateTime;

    @Column(name = "original_pax")
    private Integer originalPax;

    @Column(name = "original_kids")
    private Integer originalKids;

    @Column(name = "original_notes", length = 500)
    private String originalNotes;

    // ============================================
    // REQUESTED NEW VALUES
    // ============================================

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Column(name = "requested_datetime")
    private LocalDateTime requestedDateTime;

    @Column(name = "requested_pax")
    private Integer requestedPax;

    @Column(name = "requested_kids")
    private Integer requestedKids;

    @Column(name = "requested_notes", length = 500)
    private String requestedNotes;

    /**
     * Reason why customer is requesting modification
     */
    @Column(name = "customer_reason", length = 500)
    private String customerReason;

    /**
     * Reason for approval/rejection (filled by restaurant staff)
     */
    @Column(name = "approval_reason", length = 500)
    private String approvalReason;

    // ============================================
    // AUDITING
    // ============================================

    /**
     * User who requested the modification (usually the customer)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private AbstractUser requestedBy;

    /**
     * Timestamp when the modification was requested
     */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /**
     * User who approved/rejected the modification (restaurant staff or admin)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private AbstractUser reviewedBy;

    /**
     * Timestamp when the modification was reviewed
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * Enum for modification request status
     */
    public enum Status {
        PENDING_APPROVAL,   // Customer richiesto modifica, in attesa approvazione
        APPROVED,           // RestaurantUser ha approvato
        REJECTED,           // RestaurantUser ha rifiutato
        APPLIED,            // La modifica è stata applicata al reservation
        CANCELLED           // Il customer ha cancellato la richiesta
    }
}
