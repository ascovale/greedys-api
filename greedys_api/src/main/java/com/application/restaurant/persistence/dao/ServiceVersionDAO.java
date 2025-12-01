package com.application.restaurant.persistence.dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.ServiceVersion;

@Transactional(readOnly = true)
@Repository
public interface ServiceVersionDAO extends JpaRepository<ServiceVersion, Long> {

    /**
     * Find the active service version for a given service and date.
     * Returns the version that:
     * - Belongs to the given service
     * - Is in ACTIVE state
     * - Has effective_from <= date <= effective_to (or effective_to is NULL)
     * 
     * @param serviceId the service ID
     * @param date the date to check
     * @return the active service version, or empty if none exists
     */
    @Query("""
        SELECT sv FROM ServiceVersion sv
        WHERE sv.service.id = :serviceId
          AND sv.state = 'ACTIVE'
          AND sv.effectiveFrom <= :date
          AND (sv.effectiveTo IS NULL OR sv.effectiveTo >= :date)
        ORDER BY sv.effectiveFrom DESC
        LIMIT 1
    """)
    Optional<ServiceVersion> findActiveVersionByServiceAndDate(
        @Param("serviceId") Long serviceId,
        @Param("date") LocalDate date
    );

    /**
     * Find all versions for a given service, ordered by effective_from descending.
     * 
     * @param serviceId the service ID
     * @return collection of service versions
     */
    @Query("""
        SELECT sv FROM ServiceVersion sv
        WHERE sv.service.id = :serviceId
        ORDER BY sv.effectiveFrom DESC
    """)
    Collection<ServiceVersion> findAllVersionsByService(@Param("serviceId") Long serviceId);

    /**
     * Find all active versions for a given service.
     * 
     * @param serviceId the service ID
     * @return collection of active service versions
     */
    @Query("""
        SELECT sv FROM ServiceVersion sv
        WHERE sv.service.id = :serviceId
          AND sv.state = 'ACTIVE'
        ORDER BY sv.effectiveFrom DESC
    """)
    Collection<ServiceVersion> findActiveVersionsByService(@Param("serviceId") Long serviceId);

    /**
     * Find all versions that overlap with a given date range.
     * 
     * @param serviceId the service ID
     * @param startDate start of the range (inclusive)
     * @param endDate end of the range (inclusive)
     * @return collection of overlapping service versions
     */
    @Query("""
        SELECT sv FROM ServiceVersion sv
        WHERE sv.service.id = :serviceId
          AND sv.effectiveFrom <= :endDate
          AND (sv.effectiveTo IS NULL OR sv.effectiveTo >= :startDate)
        ORDER BY sv.effectiveFrom ASC
    """)
    Collection<ServiceVersion> findVersionsOverlappingRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find the most recent version for a service (regardless of active state).
     * 
     * @param serviceId the service ID
     * @return the most recent version, or empty if none exists
     */
    @Query("""
        SELECT sv FROM ServiceVersion sv
        WHERE sv.service.id = :serviceId
        ORDER BY sv.effectiveFrom DESC, sv.createdAt DESC
        LIMIT 1
    """)
    Optional<ServiceVersion> findMostRecentVersionByService(@Param("serviceId") Long serviceId);

    /**
     * Find all active service versions for a restaurant.
     * Used by customers to see available services for booking.
     * 
     * @param restaurantId the restaurant ID
     * @return collection of active service versions
     */
    @Query("""
        SELECT sv FROM ServiceVersion sv
        WHERE sv.service.restaurant.id = :restaurantId
          AND sv.state = 'ACTIVE'
          AND (sv.effectiveTo IS NULL OR sv.effectiveTo >= CURRENT_DATE)
        ORDER BY sv.service.name, sv.effectiveFrom DESC
    """)
    Collection<ServiceVersion> findActiveByRestaurantId(@Param("restaurantId") Long restaurantId);

}
