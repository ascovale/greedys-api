package com.application.common.persistence.dao.audit;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.audit.ScheduleAuditLog;
import com.application.common.persistence.model.audit.ScheduleAuditLog.EntityType;

/**
 * Repository for ScheduleAuditLog - tracks schedule configuration changes.
 */
@Repository
public interface ScheduleAuditLogDAO extends JpaRepository<ScheduleAuditLog, Long> {

    /**
     * Find all audit logs for a specific service
     */
    @Query("""
        SELECT sal FROM ScheduleAuditLog sal
        WHERE sal.serviceId = :serviceId
        ORDER BY sal.changedAt DESC
    """)
    Collection<ScheduleAuditLog> findByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Find all audit logs for a specific restaurant
     */
    @Query("""
        SELECT sal FROM ScheduleAuditLog sal
        WHERE sal.restaurantId = :restaurantId
        ORDER BY sal.changedAt DESC
    """)
    Collection<ScheduleAuditLog> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Find audit logs for a specific entity
     */
    @Query("""
        SELECT sal FROM ScheduleAuditLog sal
        WHERE sal.entityType = :entityType
          AND sal.entityId = :entityId
        ORDER BY sal.changedAt DESC
    """)
    Collection<ScheduleAuditLog> findByEntity(
        @Param("entityType") EntityType entityType,
        @Param("entityId") Long entityId
    );

    /**
     * Find audit logs for a service within a date range
     */
    @Query("""
        SELECT sal FROM ScheduleAuditLog sal
        WHERE sal.serviceId = :serviceId
          AND sal.changedAt >= :startDate
          AND sal.changedAt <= :endDate
        ORDER BY sal.changedAt DESC
    """)
    Collection<ScheduleAuditLog> findByServiceIdAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find audit logs by user
     */
    @Query("""
        SELECT sal FROM ScheduleAuditLog sal
        WHERE sal.userId = :userId
        ORDER BY sal.changedAt DESC
    """)
    Collection<ScheduleAuditLog> findByUserId(@Param("userId") Long userId);

    /**
     * Find recent audit logs for a restaurant (last N days)
     */
    @Query("""
        SELECT sal FROM ScheduleAuditLog sal
        WHERE sal.restaurantId = :restaurantId
          AND sal.changedAt >= :since
        ORDER BY sal.changedAt DESC
    """)
    Collection<ScheduleAuditLog> findRecentByRestaurantId(
        @Param("restaurantId") Long restaurantId,
        @Param("since") LocalDateTime since
    );
}
