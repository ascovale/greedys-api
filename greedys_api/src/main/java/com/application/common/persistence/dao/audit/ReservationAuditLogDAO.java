package com.application.common.persistence.dao.audit;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.audit.ReservationAuditLog;
import com.application.common.persistence.model.audit.ReservationAuditLog.ReservationAuditAction;

/**
 * Repository for ReservationAuditLog - tracks reservation changes.
 */
@Repository
public interface ReservationAuditLogDAO extends JpaRepository<ReservationAuditLog, Long> {

    /**
     * Find all audit logs for a specific reservation
     */
    @Query("""
        SELECT ral FROM ReservationAuditLog ral
        WHERE ral.reservationId = :reservationId
        ORDER BY ral.changedAt ASC
    """)
    Collection<ReservationAuditLog> findByReservationId(@Param("reservationId") Long reservationId);

    /**
     * Find all audit logs for a specific restaurant
     */
    @Query("""
        SELECT ral FROM ReservationAuditLog ral
        WHERE ral.restaurantId = :restaurantId
        ORDER BY ral.changedAt DESC
    """)
    Collection<ReservationAuditLog> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Find audit logs by action type
     */
    @Query("""
        SELECT ral FROM ReservationAuditLog ral
        WHERE ral.restaurantId = :restaurantId
          AND ral.action = :action
        ORDER BY ral.changedAt DESC
    """)
    Collection<ReservationAuditLog> findByRestaurantIdAndAction(
        @Param("restaurantId") Long restaurantId,
        @Param("action") ReservationAuditAction action
    );

    /**
     * Find audit logs within a date range
     */
    @Query("""
        SELECT ral FROM ReservationAuditLog ral
        WHERE ral.restaurantId = :restaurantId
          AND ral.changedAt >= :startDate
          AND ral.changedAt <= :endDate
        ORDER BY ral.changedAt DESC
    """)
    Collection<ReservationAuditLog> findByRestaurantIdAndDateRange(
        @Param("restaurantId") Long restaurantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find audit logs by user
     */
    @Query("""
        SELECT ral FROM ReservationAuditLog ral
        WHERE ral.userId = :userId
        ORDER BY ral.changedAt DESC
    """)
    Collection<ReservationAuditLog> findByUserId(@Param("userId") Long userId);

    /**
     * Find terms changed logs (for tracking contract modifications)
     */
    @Query("""
        SELECT ral FROM ReservationAuditLog ral
        WHERE ral.restaurantId = :restaurantId
          AND ral.action = 'TERMS_CHANGED'
        ORDER BY ral.changedAt DESC
    """)
    Collection<ReservationAuditLog> findTermsChangedByRestaurantId(
        @Param("restaurantId") Long restaurantId
    );

    /**
     * Count changes for a reservation
     */
    @Query("""
        SELECT COUNT(ral) FROM ReservationAuditLog ral
        WHERE ral.reservationId = :reservationId
    """)
    long countByReservationId(@Param("reservationId") Long reservationId);
}
