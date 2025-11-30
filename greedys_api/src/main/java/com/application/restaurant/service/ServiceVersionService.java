package com.application.restaurant.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.ServiceVersion;
import com.application.common.persistence.model.reservation.ServiceVersion.VersionState;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.ServiceVersionDAO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing ServiceVersion entities.
 * Handles creation, updates, and querying of service versions.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ServiceVersionService {

    private final ServiceVersionDAO serviceVersionDAO;
    private final ServiceDAO serviceDAO;

    /**
     * Create a new service version.
     * 
     * @param serviceVersion the service version to create
     * @return the created service version with generated ID
     */
    public ServiceVersion createVersion(ServiceVersion serviceVersion) {
        if (serviceVersion == null) {
            throw new IllegalArgumentException("ServiceVersion cannot be null");
        }
        
        if (serviceVersion.getService() == null || serviceVersion.getService().getId() == null) {
            throw new IllegalArgumentException("ServiceVersion must have a valid Service reference");
        }
        
        if (serviceVersion.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("ServiceVersion must have an effective_from date");
        }
        
        if (serviceVersion.getDuration() == null || serviceVersion.getDuration() <= 0) {
            throw new IllegalArgumentException("ServiceVersion must have a valid duration in minutes");
        }
        
        if (serviceVersion.getCreatedAt() == null) {
            serviceVersion.setCreatedAt(LocalDateTime.now());
        }
        
        if (serviceVersion.getState() == null) {
            serviceVersion.setState(VersionState.ACTIVE);
        }
        
        log.info("Creating new service version for service {} effective from {}", 
            serviceVersion.getService().getId(), serviceVersion.getEffectiveFrom());
        
        return serviceVersionDAO.save(serviceVersion);
    }

    /**
     * Update an existing service version.
     * 
     * @param serviceVersion the service version to update
     * @return the updated service version
     */
    public ServiceVersion updateVersion(ServiceVersion serviceVersion) {
        if (serviceVersion == null || serviceVersion.getId() == null) {
            throw new IllegalArgumentException("ServiceVersion must have a valid ID for update");
        }
        
        serviceVersionDAO.findById(serviceVersion.getId())
            .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found with ID: " + serviceVersion.getId()));
        
        serviceVersion.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating service version {} for service {}", serviceVersion.getId(), serviceVersion.getService().getId());
        
        return serviceVersionDAO.save(serviceVersion);
    }

    /**
     * Archive a service version (change state from ACTIVE to ARCHIVED).
     * 
     * @param serviceVersionId the ID of the version to archive
     * @return the archived service version
     */
    public ServiceVersion archiveVersion(Long serviceVersionId) {
        ServiceVersion version = serviceVersionDAO.findById(serviceVersionId)
            .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found with ID: " + serviceVersionId));
        
        version.setState(VersionState.ARCHIVED);
        version.setUpdatedAt(LocalDateTime.now());
        
        log.info("Archiving service version {} for service {}", serviceVersionId, version.getService().getId());
        
        return serviceVersionDAO.save(version);
    }

    /**
     * Get the active service version for a given date.
     * 
     * @param serviceId the service ID
     * @param date the date to check
     * @return the active service version for that date, or empty if none exists
     */
    public Optional<ServiceVersion> getActiveVersionByDate(Long serviceId, LocalDate date) {
        if (serviceId == null || date == null) {
            return Optional.empty();
        }
        
        return serviceVersionDAO.findActiveVersionByServiceAndDate(serviceId, date);
    }

    /**
     * Get the active service version for today.
     * 
     * @param serviceId the service ID
     * @return the active service version for today, or empty if none exists
     */
    public Optional<ServiceVersion> getActiveVersionForToday(Long serviceId) {
        return getActiveVersionByDate(serviceId, LocalDate.now());
    }

    /**
     * Get all versions for a given service, ordered by effective_from descending.
     * 
     * @param serviceId the service ID
     * @return collection of all versions for the service
     */
    public Collection<ServiceVersion> getAllVersionsByService(Long serviceId) {
        if (serviceId == null) {
            return Collections.emptyList();
        }
        
        return serviceVersionDAO.findAllVersionsByService(serviceId);
    }

    /**
     * Get all active versions for a given service.
     * 
     * @param serviceId the service ID
     * @return collection of active versions
     */
    public Collection<ServiceVersion> getActiveVersionsByService(Long serviceId) {
        if (serviceId == null) {
            return Collections.emptyList();
        }
        
        return serviceVersionDAO.findActiveVersionsByService(serviceId);
    }

    /**
     * Get all versions that overlap with a given date range.
     * 
     * @param serviceId the service ID
     * @param startDate start of the range (inclusive)
     * @param endDate end of the range (inclusive)
     * @return collection of overlapping versions
     */
    public Collection<ServiceVersion> getVersionsOverlappingRange(Long serviceId, LocalDate startDate, LocalDate endDate) {
        if (serviceId == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        return serviceVersionDAO.findVersionsOverlappingRange(serviceId, startDate, endDate);
    }

    /**
     * Get the most recent version for a service (regardless of state).
     * 
     * @param serviceId the service ID
     * @return the most recent version, or empty if none exists
     */
    public Optional<ServiceVersion> getMostRecentVersion(Long serviceId) {
        if (serviceId == null) {
            return Optional.empty();
        }
        
        return serviceVersionDAO.findMostRecentVersionByService(serviceId);
    }

    /**
     * Migrate an existing service to a service version.
     * Creates an initial ACTIVE version starting from today with indefinite end date.
     * This is useful for backward compatibility when transitioning from the old Service-based system.
     * 
     * @param serviceId the service ID to migrate
     * @return the created initial version
     */
    public ServiceVersion migrateExistingService(Long serviceId) {
        com.application.common.persistence.model.reservation.Service service = serviceDAO.findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Service not found with ID: " + serviceId));
        
        // Check if a version already exists
        Optional<ServiceVersion> existingVersion = getMostRecentVersion(serviceId);
        if (existingVersion.isPresent()) {
            log.warn("Service {} already has versions, skipping migration", serviceId);
            return existingVersion.get();
        }
        
        // Create initial version from today onwards with no end date
        ServiceVersion initialVersion = ServiceVersion.builder()
            .service(service)
            .effectiveFrom(LocalDate.now())
            .effectiveTo(null)  // No end date
            .state(VersionState.ACTIVE)
            .duration(30)  // Default 30-minute slots
            .notes("Initial version created by migration from legacy Service")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        log.info("Migrating service {} to service version starting from {}", serviceId, LocalDate.now());
        
        return serviceVersionDAO.save(initialVersion);
    }

    /**
     * Delete a service version and all associated availability exceptions.
     * This cascades deletion to availability exceptions due to the FK constraint.
     * 
     * @param serviceVersionId the ID of the version to delete
     */
    public void deleteVersion(Long serviceVersionId) {
        ServiceVersion version = serviceVersionDAO.findById(serviceVersionId)
            .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found with ID: " + serviceVersionId));
        
        log.info("Deleting service version {} for service {}", serviceVersionId, version.getService().getId());
        
        serviceVersionDAO.deleteById(serviceVersionId);
    }

    /**
     * Check if a service version is valid for a given date.
     * 
     * @param serviceVersion the service version to check
     * @param date the date to check
     * @return true if the version is valid for that date
     */
    public boolean isValidForDate(ServiceVersion serviceVersion, LocalDate date) {
        return serviceVersion != null && serviceVersion.isValidForDate(date);
    }

}
