package com.application.restaurant.persistence.dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.AvailabilityException;
import com.application.common.persistence.model.reservation.AvailabilityException.ExceptionType;

/**
 * DAO for AvailabilityException - manages date-specific availability overrides
 * 
 * ARCHITECTURE: Hybrid Scheduling
 * - Supports FULL-DAY closures (is_fully_closed=TRUE)
 * - Supports PARTIAL-DAY closures (start_time + end_time)
 * - Supports HOURS OVERRIDES (override_opening_time + override_closing_time)
 * 
 * USAGE PATTERNS:
 * 
 * 1. Check if fully closed on a specific date:
 *    findFullDayClosureByDate(versionId, date)
 * 
 * 2. Get all exceptions for a date range (week, month):
 *    findByServiceVersionAndDateRange(versionId, startDate, endDate)
 * 
 * 3. Get all partial-day closures (maintenance windows, etc.):
 *    findPartialClosures(versionId)
 * 
 * 4. Get all full-day closures for the next 30 days:
 *    findFullDayClosuresByDateRange(versionId, today, today+30days)
 */
@Transactional(readOnly = true)
@Repository
public interface AvailabilityExceptionDAO extends JpaRepository<AvailabilityException, Long> {

    /**
     * Find if there's a full-day closure on a specific date
     * 
     * @param serviceVersionId the service version ID
     * @param exceptionDate the date to check
     * @return the exception if found, or empty
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate = :exceptionDate
          AND ae.isFullyClosed = TRUE
    """)
    Optional<AvailabilityException> findFullDayClosureByDate(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("exceptionDate") LocalDate exceptionDate
    );

    /**
     * Find any exception (full or partial) on a specific date
     * 
     * @param serviceVersionId the service version ID
     * @param exceptionDate the date to check
     * @return collection of exceptions on that date
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate = :exceptionDate
        ORDER BY ae.startTime ASC
    """)
    Collection<AvailabilityException> findByServiceVersionAndDate(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("exceptionDate") LocalDate exceptionDate
    );

    /**
     * Find exceptions for a specific service version on a specific date (legacy method).
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate = :date
        ORDER BY ae.createdAt DESC
    """)
    Collection<AvailabilityException> findExceptionsByServiceVersionAndDate(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("date") LocalDate date
    );

    /**
     * Find if there's any exception for a service version on a specific date.
     */
    @Query("""
        SELECT COUNT(ae) > 0 FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate = :date
    """)
    boolean hasExceptionForDate(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("date") LocalDate date
    );

    /**
     * Find all exceptions within a date range
     * 
     * @param serviceVersionId the service version ID
     * @param startDate start of range (inclusive)
     * @param endDate end of range (inclusive)
     * @return collection of exceptions within the range
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate BETWEEN :startDate AND :endDate
        ORDER BY ae.exceptionDate ASC, ae.startTime ASC
    """)
    Collection<AvailabilityException> findByServiceVersionAndDateRange(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all exceptions for a service version within a date range (legacy method).
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate >= :startDate
          AND ae.exceptionDate <= :endDate
        ORDER BY ae.exceptionDate ASC, ae.createdAt DESC
    """)
    Collection<AvailabilityException> findExceptionsByServiceVersionInDateRange(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all full-day closures within a date range
     * 
     * @param serviceVersionId the service version ID
     * @param startDate start of range (inclusive)
     * @param endDate end of range (inclusive)
     * @return collection of full-day closures
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate BETWEEN :startDate AND :endDate
          AND ae.isFullyClosed = TRUE
        ORDER BY ae.exceptionDate ASC
    """)
    Collection<AvailabilityException> findFullDayClosuresByDateRange(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all partial-day closures (maintenance windows, time-specific unavailability)
     * 
     * @param serviceVersionId the service version ID
     * @return collection of partial-day exceptions
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.isFullyClosed = FALSE
          AND ae.startTime IS NOT NULL
          AND ae.endTime IS NOT NULL
        ORDER BY ae.exceptionDate ASC, ae.startTime ASC
    """)
    Collection<AvailabilityException> findPartialClosures(@Param("serviceVersionId") Long serviceVersionId);

    /**
     * Find all exceptions of a specific type within a date range
     * 
     * @param serviceVersionId the service version ID
     * @param exceptionType the type of exception to find
     * @param startDate start of range (inclusive)
     * @param endDate end of range (inclusive)
     * @return collection of exceptions of that type
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionType = :exceptionType
          AND ae.exceptionDate BETWEEN :startDate AND :endDate
        ORDER BY ae.exceptionDate ASC
    """)
    Collection<AvailabilityException> findByTypeAndDateRange(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("exceptionType") ExceptionType exceptionType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all maintenance windows (partial closures of type MAINTENANCE)
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionType = 'MAINTENANCE'
          AND ae.exceptionDate BETWEEN :startDate AND :endDate
          AND ae.startTime IS NOT NULL
          AND ae.endTime IS NOT NULL
        ORDER BY ae.exceptionDate ASC, ae.startTime ASC
    """)
    Collection<AvailabilityException> findMaintenanceWindows(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all exceptions with opening hours overrides
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate BETWEEN :startDate AND :endDate
          AND (ae.overrideOpeningTime IS NOT NULL OR ae.overrideClosingTime IS NOT NULL)
        ORDER BY ae.exceptionDate ASC
    """)
    Collection<AvailabilityException> findHoursOverrides(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find between dates (legacy method).
     */
    @Query("""
        SELECT ae FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate >= :startDate
          AND ae.exceptionDate <= :endDate
    """)
    Collection<AvailabilityException> findBetweenDates(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Delete all exceptions for a service version on a specific date.
     */
    @Query("""
        DELETE FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
          AND ae.exceptionDate = :date
    """)
    int deleteExceptionsByDate(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("date") LocalDate date
    );

    /**
     * Delete all exceptions for a service version.
     */
    @Query("""
        DELETE FROM AvailabilityException ae
        WHERE ae.serviceVersion.id = :serviceVersionId
    """)
    int deleteAllByServiceVersion(@Param("serviceVersionId") Long serviceVersionId);

}
