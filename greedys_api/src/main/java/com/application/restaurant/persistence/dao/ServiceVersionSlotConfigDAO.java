package com.application.restaurant.persistence.dao;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.ServiceVersionSlotConfig;

/**
 * DAO for ServiceVersionSlotConfig - manages slot generation parameters
 * 
 * REPLACES: JSON parsing of slotGenerationParams with queryable entity
 * 
 * Each ServiceVersion can have ONE or MORE SlotConfigs if supporting different
 * slot configurations for different seasons/modes
 * 
 * TYPICAL USAGE:
 * 1. Get config for a service version:
 *    slotConfigDAO.findByServiceVersionId(versionId)
 * 
 * 2. Validate before saving:
 *    config.isValid()
 * 
 * 3. Calculate slots:
 *    - Get ServiceVersionDay for target date
 *    - Apply this config to generate time slots
 *    - Exclude breaks, closed times
 */
@Transactional(readOnly = true)
@Repository
public interface ServiceVersionSlotConfigDAO extends JpaRepository<ServiceVersionSlotConfig, Long> {

    /**
     * Find slot config for a specific service version
     * 
     * @param serviceVersionId the service version ID
     * @return the slot config, or empty if not configured
     */
    @Query("""
        SELECT sc FROM ServiceVersionSlotConfig sc
        WHERE sc.serviceVersion.id = :serviceVersionId
    """)
    Optional<ServiceVersionSlotConfig> findByServiceVersionId(@Param("serviceVersionId") Long serviceVersionId);

    /**
     * Find all slot configs for a service version
     * (In case of multiple configs for different modes/seasons)
     * 
     * @param serviceVersionId the service version ID
     * @return collection of slot configs
     */
    @Query("""
        SELECT sc FROM ServiceVersionSlotConfig sc
        WHERE sc.serviceVersion.id = :serviceVersionId
        ORDER BY sc.startTime ASC
    """)
    Collection<ServiceVersionSlotConfig> findAllByServiceVersionId(@Param("serviceVersionId") Long serviceVersionId);

    /**
     * Find all slot configs for all versions of a service
     * 
     * @param serviceId the service ID
     * @return collection of slot configs
     */
    @Query("""
        SELECT sc FROM ServiceVersionSlotConfig sc
        WHERE sc.serviceVersion.service.id = :serviceId
        ORDER BY sc.serviceVersion.effectiveFrom ASC, sc.startTime ASC
    """)
    Collection<ServiceVersionSlotConfig> findAllByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Check if a slot config exists for a service version
     * 
     * @param serviceVersionId the service version ID
     * @return true if configured, false otherwise
     */
    @Query("""
        SELECT COUNT(sc) > 0 FROM ServiceVersionSlotConfig sc
        WHERE sc.serviceVersion.id = :serviceVersionId
    """)
    boolean existsByServiceVersionId(@Param("serviceVersionId") Long serviceVersionId);

    /**
     * Delete all slot configs for a service version
     * 
     * @param serviceVersionId the service version ID
     * @return number of deleted configs
     */
    @Query("""
        DELETE FROM ServiceVersionSlotConfig sc
        WHERE sc.serviceVersion.id = :serviceVersionId
    """)
    int deleteByServiceVersionId(@Param("serviceVersionId") Long serviceVersionId);
}
