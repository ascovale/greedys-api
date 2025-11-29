package com.application.common.persistence.dao;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.reservation.ReservationAudit;
import com.application.common.persistence.model.reservation.ReservationAudit.AuditAction;

/**
 * Data Access Object for ReservationAudit entities.
 * Provides database operations for audit trail management.
 */
@Repository
public interface ReservationAuditDAO extends CrudRepository<ReservationAudit, Long> {

    /**
     * Find all audit records for a reservation ordered by most recent first
     * 
     * @param reservationId ID of the reservation
     * @param pageable Pagination parameters
     * @return Page of audit records
     */
    Page<ReservationAudit> findByReservationIdOrderByChangedAtDesc(Long reservationId, Pageable pageable);

    /**
     * Find audit records for a specific action type
     * 
     * @param reservationId ID of the reservation
     * @param action Type of action
     * @param pageable Pagination parameters
     * @return Page of audit records
     */
    Page<ReservationAudit> findByReservationIdAndAction(Long reservationId, AuditAction action, Pageable pageable);

    /**
     * Find audit records created by a specific user
     * 
     * @param reservationId ID of the reservation
     * @param userId ID of the user
     * @param pageable Pagination parameters
     * @return Page of audit records
     */
    @Query("SELECT ra FROM ReservationAudit ra WHERE ra.reservation.id = :reservationId AND ra.changedBy.id = :userId ORDER BY ra.changedAt DESC")
    Page<ReservationAudit> findByReservationIdAndChangedByUserId(@Param("reservationId") Long reservationId, 
                                                                   @Param("userId") Long userId, Pageable pageable);

    /**
     * Find audit records within a date range
     * 
     * @param reservationId ID of the reservation
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @param pageable Pagination parameters
     * @return Page of audit records
     */
    Page<ReservationAudit> findByReservationIdAndChangedAtBetween(Long reservationId, LocalDateTime startDate, 
                                                                   LocalDateTime endDate, Pageable pageable);

    /**
     * Get complete audit trail for all reservations at a restaurant
     * 
     * @param restaurantId ID of the restaurant
     * @param pageable Pagination parameters
     * @return Page of audit records for the restaurant's reservations
     */
    @Query("SELECT ra FROM ReservationAudit ra " +
           "WHERE ra.reservation.restaurant.id = :restaurantId " +
           "ORDER BY ra.changedAt DESC")
    Page<ReservationAudit> findRestaurantAuditTrail(@Param("restaurantId") Long restaurantId, Pageable pageable);

    /**
     * Count audit records for a reservation
     * 
     * @param reservationId ID of the reservation
     * @return Count of audit records
     */
    long countByReservationId(Long reservationId);

    /**
     * Count audit records for a specific action
     * 
     * @param reservationId ID of the reservation
     * @param action Type of action
     * @return Count of audit records
     */
    long countByReservationIdAndAction(Long reservationId, AuditAction action);
}
