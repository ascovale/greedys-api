package com.application.restaurant.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.audit.ScheduleAuditLog.EntityType;
import com.application.common.persistence.model.reservation.AvailabilityException;
import com.application.common.service.audit.AuditService;
import com.application.restaurant.persistence.dao.AvailabilityExceptionDAO;
import com.application.restaurant.persistence.model.user.RUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing AvailabilityException entities.
 * Handles creation, deletion, and querying of availability exceptions.
 * 
 * NOTE: All mutating operations are audited via AuditService for compliance
 * with enterprise audit requirements (TheFork/OpenTable pattern).
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AvailabilityExceptionService {

    private final AvailabilityExceptionDAO availabilityExceptionDAO;
    private final AuditService auditService;

    /**
     * Create a new availability exception.
     * 
     * @param exception the availability exception to create
     * @return the created availability exception
     */
    public AvailabilityException createException(AvailabilityException exception) {
        if (exception == null) {
            throw new IllegalArgumentException("AvailabilityException cannot be null");
        }
        
        if (exception.getServiceVersion() == null || exception.getServiceVersion().getId() == null) {
            throw new IllegalArgumentException("AvailabilityException must have a valid ServiceVersion reference");
        }
        
        if (exception.getExceptionDate() == null) {
            throw new IllegalArgumentException("AvailabilityException must have an exception_date");
        }
        
        if (exception.getExceptionType() == null) {
            throw new IllegalArgumentException("AvailabilityException must have an exception_type");
        }
        
        if (exception.getCreatedAt() == null) {
            exception.setCreatedAt(LocalDateTime.now());
        }
        
        log.info("Creating availability exception for service version {} on date {} with type {}", 
            exception.getServiceVersion().getId(), exception.getExceptionDate(), exception.getExceptionType());
        
        AvailabilityException saved = availabilityExceptionDAO.save(exception);
        
        // Audit exception creation
        Long serviceId = exception.getServiceVersion().getService() != null 
            ? exception.getServiceVersion().getService().getId() 
            : null;
        Long restaurantId = exception.getServiceVersion().getService() != null 
            && exception.getServiceVersion().getService().getRestaurant() != null
            ? exception.getServiceVersion().getService().getRestaurant().getId() 
            : null;
        
        auditService.auditScheduleCreated(
            EntityType.AVAILABILITY_EXCEPTION,
            saved.getId(),
            serviceId,
            restaurantId,
            getCurrentUserId(),
            saved,
            "Exception created: " + saved.getExceptionType() + " on " + saved.getExceptionDate()
        );
        
        return saved;
    }

    /**
     * Update an existing availability exception.
     * 
     * @param exception the availability exception to update
     * @return the updated availability exception
     */
    public AvailabilityException updateException(AvailabilityException exception) {
        if (exception == null || exception.getId() == null) {
            throw new IllegalArgumentException("AvailabilityException must have a valid ID for update");
        }
        
        AvailabilityException oldException = availabilityExceptionDAO.findById(exception.getId())
            .orElseThrow(() -> new IllegalArgumentException("AvailabilityException not found with ID: " + exception.getId()));
        
        exception.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating availability exception {} for service version {}", 
            exception.getId(), exception.getServiceVersion().getId());
        
        AvailabilityException saved = availabilityExceptionDAO.save(exception);
        
        // Audit exception update
        Long serviceId = exception.getServiceVersion().getService() != null 
            ? exception.getServiceVersion().getService().getId() 
            : null;
        Long restaurantId = exception.getServiceVersion().getService() != null 
            && exception.getServiceVersion().getService().getRestaurant() != null
            ? exception.getServiceVersion().getService().getRestaurant().getId() 
            : null;
        
        auditService.auditScheduleUpdated(
            EntityType.AVAILABILITY_EXCEPTION,
            saved.getId(),
            serviceId,
            restaurantId,
            getCurrentUserId(),
            oldException,
            saved,
            "Exception updated on " + saved.getExceptionDate()
        );
        
        return saved;
    }

    /**
     * Delete an availability exception by ID.
     * 
     * @param exceptionId the ID of the exception to delete
     */
    public void deleteException(Long exceptionId) {
        AvailabilityException exception = availabilityExceptionDAO.findById(exceptionId)
            .orElseThrow(() -> new IllegalArgumentException("AvailabilityException not found with ID: " + exceptionId));
        
        log.info("Deleting availability exception {} for service version {}", 
            exceptionId, exception.getServiceVersion().getId());
        
        // Capture data for audit BEFORE deletion
        Long serviceId = exception.getServiceVersion().getService() != null 
            ? exception.getServiceVersion().getService().getId() 
            : null;
        Long restaurantId = exception.getServiceVersion().getService() != null 
            && exception.getServiceVersion().getService().getRestaurant() != null
            ? exception.getServiceVersion().getService().getRestaurant().getId() 
            : null;
        
        availabilityExceptionDAO.deleteById(exceptionId);
        
        // Audit exception deletion
        auditService.auditScheduleDeleted(
            EntityType.AVAILABILITY_EXCEPTION,
            exceptionId,
            serviceId,
            restaurantId,
            getCurrentUserId(),
            exception,
            "Exception deleted: " + exception.getExceptionType() + " on " + exception.getExceptionDate()
        );
    }

    /**
     * Delete all exceptions for a service version on a specific date.
     * 
     * @param serviceVersionId the service version ID
     * @param date the exception date
     * @return number of deleted exceptions
     */
    public int deleteExceptionsByDate(Long serviceVersionId, LocalDate date) {
        if (serviceVersionId == null || date == null) {
            return 0;
        }
        
        log.info("Deleting availability exceptions for service version {} on date {}", serviceVersionId, date);
        
        // Get exceptions BEFORE deletion for audit
        Collection<AvailabilityException> exceptionsToDelete = availabilityExceptionDAO
            .findExceptionsByServiceVersionAndDate(serviceVersionId, date);
        
        int deleted = availabilityExceptionDAO.deleteExceptionsByDate(serviceVersionId, date);
        
        // Audit bulk deletion
        if (deleted > 0 && !exceptionsToDelete.isEmpty()) {
            AvailabilityException firstException = exceptionsToDelete.iterator().next();
            Long serviceId = firstException.getServiceVersion().getService() != null 
                ? firstException.getServiceVersion().getService().getId() 
                : null;
            Long restaurantId = firstException.getServiceVersion().getService() != null 
                && firstException.getServiceVersion().getService().getRestaurant() != null
                ? firstException.getServiceVersion().getService().getRestaurant().getId() 
                : null;
            
            auditService.auditScheduleDeleted(
                EntityType.AVAILABILITY_EXCEPTION,
                serviceVersionId, // using serviceVersionId as entity reference for bulk
                serviceId,
                restaurantId,
                getCurrentUserId(),
                exceptionsToDelete,
                "Bulk delete: " + deleted + " exceptions for date " + date
            );
        }
        
        return deleted;
    }

    /**
     * Delete all exceptions for a service version.
     * 
     * @param serviceVersionId the service version ID
     * @return number of deleted exceptions
     */
    public int deleteAllExceptionsByServiceVersion(Long serviceVersionId) {
        if (serviceVersionId == null) {
            return 0;
        }
        
        log.info("Deleting all availability exceptions for service version {}", serviceVersionId);
        
        // Get exceptions BEFORE deletion for audit  
        Collection<AvailabilityException> exceptionsToDelete = availabilityExceptionDAO
            .findAllByServiceVersionId(serviceVersionId);
        
        int deleted = availabilityExceptionDAO.deleteAllByServiceVersion(serviceVersionId);
        
        // Audit bulk deletion
        if (deleted > 0 && !exceptionsToDelete.isEmpty()) {
            AvailabilityException firstException = exceptionsToDelete.iterator().next();
            Long serviceId = firstException.getServiceVersion().getService() != null 
                ? firstException.getServiceVersion().getService().getId() 
                : null;
            Long restaurantId = firstException.getServiceVersion().getService() != null 
                && firstException.getServiceVersion().getService().getRestaurant() != null
                ? firstException.getServiceVersion().getService().getRestaurant().getId() 
                : null;
            
            auditService.auditScheduleDeleted(
                EntityType.AVAILABILITY_EXCEPTION,
                serviceVersionId,
                serviceId,
                restaurantId,
                getCurrentUserId(),
                exceptionsToDelete,
                "Bulk delete: all " + deleted + " exceptions for service version"
            );
        }
        
        return deleted;
    }

    /**
     * Get all exceptions for a service version on a specific date.
     * 
     * @param serviceVersionId the service version ID
     * @param date the exception date
     * @return collection of exceptions for that date
     */
    public Collection<AvailabilityException> getExceptionsByDate(Long serviceVersionId, LocalDate date) {
        if (serviceVersionId == null || date == null) {
            return Collections.emptyList();
        }
        
        return availabilityExceptionDAO.findExceptionsByServiceVersionAndDate(serviceVersionId, date);
    }

    /**
     * Get all exceptions for a service version within a date range.
     * 
     * @param serviceVersionId the service version ID
     * @param startDate start of the range (inclusive)
     * @param endDate end of the range (inclusive)
     * @return collection of exceptions within the range
     */
    public Collection<AvailabilityException> getExceptionsByDateRange(Long serviceVersionId, LocalDate startDate, LocalDate endDate) {
        if (serviceVersionId == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        return availabilityExceptionDAO.findExceptionsByServiceVersionInDateRange(serviceVersionId, startDate, endDate);
    }

    /**
     * Check if a service version has any exception on a specific date.
     * 
     * @param serviceVersionId the service version ID
     * @param date the date to check
     * @return true if at least one exception exists for that date
     */
    public boolean hasExceptionForDate(Long serviceVersionId, LocalDate date) {
        if (serviceVersionId == null || date == null) {
            return false;
        }
        
        return availabilityExceptionDAO.hasExceptionForDate(serviceVersionId, date);
    }

    /**
     * Get a specific availability exception by ID.
     * 
     * @param exceptionId the exception ID
     * @return the exception, or empty if not found
     */
    public Optional<AvailabilityException> getExceptionById(Long exceptionId) {
        if (exceptionId == null) {
            return Optional.empty();
        }
        
        return availabilityExceptionDAO.findById(exceptionId);
    }

    /**
     * Check if a date is closed (has a CLOSURE exception) for a service version.
     * 
     * @param serviceVersionId the service version ID
     * @param date the date to check
     * @return true if the date has a CLOSURE exception
     */
    public boolean isDateClosed(Long serviceVersionId, LocalDate date) {
        if (serviceVersionId == null || date == null) {
            return false;
        }
        
        Collection<AvailabilityException> exceptions = getExceptionsByDate(serviceVersionId, date);
        return exceptions.stream()
            .anyMatch(e -> e.getExceptionType() == AvailabilityException.ExceptionType.CLOSURE);
    }

    /**
     * Check if a date is under maintenance (has a MAINTENANCE exception) for a service version.
     * 
     * @param serviceVersionId the service version ID
     * @param date the date to check
     * @return true if the date has a MAINTENANCE exception
     */
    public boolean isDateUnderMaintenance(Long serviceVersionId, LocalDate date) {
        if (serviceVersionId == null || date == null) {
            return false;
        }
        
        Collection<AvailabilityException> exceptions = getExceptionsByDate(serviceVersionId, date);
        return exceptions.stream()
            .anyMatch(e -> e.getExceptionType() == AvailabilityException.ExceptionType.MAINTENANCE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Get current user ID from security context.
     * Returns null if no authenticated user (for system operations).
     */
    private Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof RUser) {
                return ((RUser) principal).getId();
            }
            // Handle other user types if needed
            log.debug("Unknown principal type for audit: {}", principal.getClass().getName());
            return null;
        } catch (Exception e) {
            log.warn("Could not determine current user for audit: {}", e.getMessage());
            return null;
        }
    }

}
