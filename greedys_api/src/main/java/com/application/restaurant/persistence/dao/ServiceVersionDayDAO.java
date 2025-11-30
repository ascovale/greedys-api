package com.application.restaurant.persistence.dao;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.ServiceVersionDay;

/**
 * DAO for ServiceVersionDay - manages weekly recurring schedules
 * 
 * USAGE PATTERN:
 * 
 * 1. Get schedule for Monday in a specific version:
 *    findByServiceVersionAndDayOfWeek(versionId, DayOfWeek.MONDAY)
 * 
 * 2. Get all days in a version:
 *    findAllByServiceVersionId(versionId)
 * 
 * 3. Get all open days in a version:
 *    findOpenDaysByServiceVersion(versionId)
 */
@Transactional(readOnly = true)
@Repository
public interface ServiceVersionDayDAO extends JpaRepository<ServiceVersionDay, Long> {

    /**
     * Find the schedule for a specific day of week within a service version
     * 
     * @param serviceVersionId the service version ID
     * @param dayOfWeek the day of week (DayOfWeek.MONDAY, etc.)
     * @return the ServiceVersionDay, or empty if not found
     */
    @Query("""
        SELECT svd FROM ServiceVersionDay svd
        WHERE svd.serviceVersion.id = :serviceVersionId
          AND svd.dayOfWeek = :dayOfWeek
    """)
    Optional<ServiceVersionDay> findByServiceVersionAndDayOfWeek(
        @Param("serviceVersionId") Long serviceVersionId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    /**
     * Find all days in a service version
     * 
     * @param serviceVersionId the service version ID
     * @return collection of ServiceVersionDay records
     */
    @Query("""
        SELECT svd FROM ServiceVersionDay svd
        WHERE svd.serviceVersion.id = :serviceVersionId
        ORDER BY svd.dayOfWeek ASC
    """)
    Collection<ServiceVersionDay> findAllByServiceVersionId(@Param("serviceVersionId") Long serviceVersionId);

    /**
     * Find all OPEN days (not closed) in a service version
     * 
     * @param serviceVersionId the service version ID
     * @return collection of open ServiceVersionDay records
     */
    @Query("""
        SELECT svd FROM ServiceVersionDay svd
        WHERE svd.serviceVersion.id = :serviceVersionId
          AND svd.isClosed = FALSE
        ORDER BY svd.dayOfWeek ASC
    """)
    Collection<ServiceVersionDay> findOpenDaysByServiceVersion(@Param("serviceVersionId") Long serviceVersionId);

    /**
     * Find all CLOSED days in a service version
     * 
     * @param serviceVersionId the service version ID
     * @return collection of closed ServiceVersionDay records
     */
    @Query("""
        SELECT svd FROM ServiceVersionDay svd
        WHERE svd.serviceVersion.id = :serviceVersionId
          AND svd.isClosed = TRUE
        ORDER BY svd.dayOfWeek ASC
    """)
    Collection<ServiceVersionDay> findClosedDaysByServiceVersion(@Param("serviceVersionId") Long serviceVersionId);

    /**
     * Find all days with a break in a service version
     * 
     * @param serviceVersionId the service version ID
     * @return collection of ServiceVersionDay records with breaks
     */
    @Query("""
        SELECT svd FROM ServiceVersionDay svd
        WHERE svd.serviceVersion.id = :serviceVersionId
          AND svd.breakStart IS NOT NULL
          AND svd.breakEnd IS NOT NULL
        ORDER BY svd.dayOfWeek ASC
    """)
    Collection<ServiceVersionDay> findDaysWithBreak(@Param("serviceVersionId") Long serviceVersionId);

}
