package com.application.customer.persistence.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.reservation.ReservationAudit;
import com.application.common.persistence.model.reservation.ReservationAudit.AuditAction;

/**
 * Data Access Object for ReservationAudit entities.
 * Provides optimized queries for audit trail retrieval and analysis.
 */
@Repository
public interface ReservationAuditDAO extends JpaRepository<ReservationAudit, Long> {

    /**
     * Find all audit records for a reservation, ordered by most recent first.
     * 
     * @param reservationId ID of the reservation
     * @return List of audit records (most recent first)
     */
    List<ReservationAudit> findByReservationIdOrderByChangedAtDesc(Long reservationId);

    /**
     * Find audit records for a reservation filtered by action type.
     * 
     * @param reservationId ID of the reservation
     * @param action Type of action to filter by
     * @return List of matching audit records
     */
    List<ReservationAudit> findByReservationIdAndAction(Long reservationId, AuditAction action);

    /**
     * Find paginated audit history for a reservation.
     * 
     * @param reservationId ID of the reservation
     * @param pageable Pagination info
     * @return Paginated audit records (most recent first)
     */
    @Query("SELECT a FROM ReservationAudit a WHERE a.reservation.id = :reservationId ORDER BY a.changedAt DESC")
    Page<ReservationAudit> findByReservationIdPaginated(Long reservationId, Pageable pageable);

    /**
     * Find all audit records created by a specific user within a date range.
     * Used for staff activity audit trails.
     * 
     * @param userId ID of the user
     * @param start Start datetime (inclusive)
     * @param end End datetime (inclusive)
     * @return List of audit records
     */
    List<ReservationAudit> findByChangedByAndChangedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    /**
     * Count total changes for a reservation.
     * 
     * @param reservationId ID of the reservation
     * @return Number of audit records
     */
    Long countByReservationId(Long reservationId);

    /**
     * Find restaurant-wide audit trail for a date range.
     * Used for comprehensive audit reporting across all reservations.
     * 
     * @param restaurantId ID of the restaurant
     * @param start Start datetime (inclusive)
     * @param end End datetime (inclusive)
     * @param pageable Pagination info
     * @return Paginated audit records (most recent first)
     */
    @Query("""
        SELECT a FROM ReservationAudit a
        WHERE a.reservation.restaurant.id = :restaurantId
          AND a.changedAt BETWEEN :start AND :end
        ORDER BY a.changedAt DESC
    """)
    Page<ReservationAudit> findRestaurantAuditTrail(
        Long restaurantId, 
        LocalDateTime start, 
        LocalDateTime end, 
        Pageable pageable
    );
}
