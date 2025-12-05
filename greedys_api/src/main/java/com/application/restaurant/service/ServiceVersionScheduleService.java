package com.application.restaurant.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.AvailabilityException;
import com.application.common.persistence.model.reservation.ServiceVersion;
import com.application.common.persistence.model.reservation.ServiceVersionDay;
import com.application.common.persistence.model.reservation.ServiceVersionSlotConfig;
import com.application.common.persistence.model.audit.ScheduleAuditLog.EntityType;
import com.application.common.service.audit.AuditService;
import com.application.restaurant.persistence.dao.AvailabilityExceptionDAO;
import com.application.restaurant.persistence.dao.ServiceVersionDAO;
import com.application.restaurant.persistence.dao.ServiceVersionDayDAO;
import com.application.restaurant.persistence.dao.ServiceVersionSlotConfigDAO;
import com.application.restaurant.web.dto.schedule.AvailabilityExceptionDto;
import com.application.restaurant.web.dto.schedule.ServiceVersionDayDto;
import com.application.restaurant.web.dto.schedule.ServiceVersionSlotConfigDto;
import com.application.restaurant.web.dto.schedule.TimeSlotDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing restaurant service version schedules
 * 
 * This service replaces the legacy SlotService and SlotTransitionService
 * with a template-based scheduling approach using:
 * - ServiceVersionDay: Weekly schedule template (7 records per service version)
 * - ServiceVersionSlotConfig: Slot generation rules
 * - AvailabilityException: Date-specific overrides
 * - TimeSlot: Computed available slots for customers
 * 
 * KEY CONCEPTS:
 * 1. TEMPLATE-BASED: Configure once, slots generated automatically
 * 2. TEMPORAL ISOLATION: Each service version has independent schedule
 * 3. FLEXIBLE EXCEPTIONS: Support closures, reduced hours, special events
 * 4. COMPUTED SLOTS: No pre-stored slots, calculated on demand
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ServiceVersionScheduleService {

    private final ServiceVersionDayDAO serviceVersionDayDAO;
    private final ServiceVersionSlotConfigDAO serviceVersionSlotConfigDAO;
    private final AvailabilityExceptionDAO availabilityExceptionDAO;
    private final ServiceVersionDAO serviceVersionDAO;
    private final AuditService auditService;

    // ============================================
    // 1. GET WEEKLY SCHEDULE (Template)
    // ============================================

    /**
     * Get the 7-day weekly schedule template for a service version
     */
    public List<ServiceVersionDayDto> getWeeklySchedule(
            Long serviceVersionId,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Loading weekly schedule for serviceVersion={}", serviceVersionId);

        // Validate access
        validateOwnership(serviceVersionId, restaurantId, userId);

        // Load all 7 days
        Collection<ServiceVersionDay> days = serviceVersionDayDAO.findAllByServiceVersionId(serviceVersionId);

        // Convert to DTOs and sort by day of week
        return days.stream()
            .map(this::toServiceVersionDayDto)
            .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
            .collect(Collectors.toList());
    }

    // ============================================
    // 2. GET ACTIVE TIME SLOTS (Computed)
    // ============================================

    /**
     * Get computed time slots for a specific date
     */
    public List<TimeSlotDto> getActiveTimeSlotsForDate(
            Long serviceVersionId,
            LocalDate date,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Querying active slots for serviceVersion={}, date={}", serviceVersionId, date);

        // Validate access
        validateOwnership(serviceVersionId, restaurantId, userId);

        // Step 1: Check day of week schedule
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<ServiceVersionDay> daySchedule = serviceVersionDayDAO
            .findByServiceVersionAndDayOfWeek(serviceVersionId, dayOfWeek);

        if (daySchedule.isEmpty() || daySchedule.get().getIsClosed()) {
            log.debug("Day {} is closed, returning empty slots", dayOfWeek);
            return new ArrayList<>();
        }

        ServiceVersionDay day = daySchedule.get();

        // Step 2: Check for full-day closure exception
        Optional<AvailabilityException> fullDayException = availabilityExceptionDAO
            .findFullDayClosureByDate(serviceVersionId, date);

        if (fullDayException.isPresent()) {
            log.debug("Full-day closure on {}", date);
            return new ArrayList<>();
        }

        // Step 3: Load slot config
        Optional<ServiceVersionSlotConfig> configOpt = serviceVersionSlotConfigDAO
            .findByServiceVersionId(serviceVersionId);

        if (configOpt.isEmpty()) {
            log.warn("No slot config found for serviceVersion={}", serviceVersionId);
            return new ArrayList<>();
        }

        ServiceVersionSlotConfigDto config = toServiceVersionSlotConfigDto(configOpt.get());

        // Step 4: Determine operating hours (considering exceptions)
        LocalTime[] hours = getEffectiveHours(serviceVersionId, date, day);
        if (hours == null) {
            log.debug("No operating hours on {}", date);
            return new ArrayList<>();
        }

        LocalTime startTime = hours[0];
        LocalTime endTime = hours[1];

        // Step 5: Generate base slots
        List<TimeSlotDto> slots = generateTimeSlots(
            serviceVersionId,
            date,
            startTime,
            endTime,
            config,
            day);

        // Step 6: Filter by partial closures
        slots = filterByPartialClosures(slots, serviceVersionId, date);

        // Step 7: Load reservation counts and compute availability
        enrichWithReservationData(slots, date);

        log.debug("Generated {} slots for {}", slots.size(), date);
        return slots;
    }

    // ============================================
    // 3. UPDATE SLOT CONFIGURATION
    // ============================================

    /**
     * Update slot generation configuration
     */
    public ServiceVersionSlotConfigDto updateSlotConfiguration(
            Long serviceVersionId,
            ServiceVersionSlotConfigDto configDto,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Updating slot config for serviceVersion={}", serviceVersionId);

        // Validate access
        validateOwnership(serviceVersionId, restaurantId, userId);

        // Validate config
        configDto.validate();

        // Load or create config
        Optional<ServiceVersionSlotConfig> existingOpt = serviceVersionSlotConfigDAO
            .findByServiceVersionId(serviceVersionId);

        ServiceVersionSlotConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            log.debug("Updating existing slot config");
        } else {
            config = new ServiceVersionSlotConfig();
            ServiceVersion sv = serviceVersionDAO.findById(serviceVersionId)
                .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found"));
            config.setServiceVersion(sv);
            log.debug("Creating new slot config");
        }

        // Update fields (use correct field names from entity)
        config.setSlotDurationMinutes(configDto.getSlotDurationMinutes());
        config.setBufferMinutes(configDto.getBufferTimeMinutes());
        config.setStartTime(configDto.getDailyStartTime());
        config.setEndTime(configDto.getDailyEndTime());
        config.setMaxConcurrentReservations(configDto.getMaxCapacityPerSlot());

        // Save
        config = serviceVersionSlotConfigDAO.save(config);
        log.info("Slot config updated successfully for serviceVersion={}", serviceVersionId);

        // 📝 AUDIT: Log slot config update
        ServiceVersion sv = config.getServiceVersion();
        auditService.auditScheduleUpdated(
            EntityType.SLOT_CONFIG,
            config.getId(),
            sv.getService().getId(),
            sv.getService().getRestaurant().getId(),
            userId,
            null, // Old value (could capture before update)
            config,
            "Slot configuration updated"
        );

        return toServiceVersionSlotConfigDto(config);
    }

    // ============================================
    // 4. UPDATE DAY SCHEDULE
    // ============================================

    /**
     * Modify schedule for a specific day of week
     */
    public ServiceVersionDayDto updateDaySchedule(
            Long serviceVersionId,
            DayOfWeek dayOfWeek,
            ServiceVersionDayDto dayDto,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Updating day schedule for serviceVersion={}, dayOfWeek={}", serviceVersionId, dayOfWeek);

        // Validate access
        validateOwnership(serviceVersionId, restaurantId, userId);

        // Load or create day
        Optional<ServiceVersionDay> existingOpt = serviceVersionDayDAO
            .findByServiceVersionAndDayOfWeek(serviceVersionId, dayOfWeek);

        ServiceVersionDay day;
        if (existingOpt.isPresent()) {
            day = existingOpt.get();
        } else {
            day = new ServiceVersionDay();
            ServiceVersion sv = serviceVersionDAO.findById(serviceVersionId)
                .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found"));
            day.setServiceVersion(sv);
            day.setDayOfWeek(dayOfWeek);
        }

        // Update fields (use correct field names from entity)
        day.setIsClosed(dayDto.isClosed());
        day.setOpeningTime(dayDto.getOperatingStartTime());
        day.setClosingTime(dayDto.getOperatingEndTime());
        day.setBreakStart(dayDto.getBreakStart());
        day.setBreakEnd(dayDto.getBreakEnd());

        // Save
        day = serviceVersionDayDAO.save(day);
        log.info("Day schedule updated for serviceVersion={}, dayOfWeek={}", serviceVersionId, dayOfWeek);

        // 📝 AUDIT: Log day schedule update
        ServiceVersion sv = day.getServiceVersion();
        auditService.auditScheduleUpdated(
            EntityType.DAY_SCHEDULE,
            day.getId(),
            sv.getService().getId(),
            sv.getService().getRestaurant().getId(),
            userId,
            null, // Old value
            day,
            "Day schedule updated for " + dayOfWeek
        );

        return toServiceVersionDayDto(day);
    }

    // ============================================
    // 5. CREATE AVAILABILITY EXCEPTION
    // ============================================

    /**
     * Create a closure, reduced hours, or special event exception
     */
    public AvailabilityExceptionDto createAvailabilityException(
            Long serviceVersionId,
            AvailabilityExceptionDto exceptionDto,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Creating availability exception for serviceVersion={}, date={}", 
            serviceVersionId, exceptionDto.getExceptionDate());

        // Validate access
        validateOwnership(serviceVersionId, restaurantId, userId);

        // Validate exception
        exceptionDto.validate();

        // Create entity
        AvailabilityException exception = new AvailabilityException();
        ServiceVersion sv = serviceVersionDAO.findById(serviceVersionId)
            .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found"));

        exception.setServiceVersion(sv);
        exception.setExceptionDate(exceptionDto.getExceptionDate());
        // Convert string to enum
        exception.setExceptionType(AvailabilityException.ExceptionType.valueOf(exceptionDto.getExceptionType()));
        exception.setIsFullyClosed(exceptionDto.isFullyClosed());
        exception.setStartTime(exceptionDto.getStartTime());
        exception.setEndTime(exceptionDto.getEndTime());
        exception.setOverrideOpeningTime(exceptionDto.getOverrideOpeningTime());
        exception.setOverrideClosingTime(exceptionDto.getOverrideClosingTime());
        exception.setNotes(exceptionDto.getReason());

        // Save
        exception = availabilityExceptionDAO.save(exception);
        log.info("Availability exception created: id={}", exception.getId());

        // 📝 AUDIT: Log exception creation
        auditService.auditScheduleCreated(
            EntityType.AVAILABILITY_EXCEPTION,
            exception.getId(),
            sv.getService().getId(),
            sv.getService().getRestaurant().getId(),
            userId,
            exception,
            "Availability exception created for " + exceptionDto.getExceptionDate()
        );

        return toAvailabilityExceptionDto(exception);
    }

    // ============================================
    // 6. DELETE AVAILABILITY EXCEPTION
    // ============================================

    /**
     * Remove an exception
     */
    public void deleteAvailabilityException(
            Long exceptionId,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Deleting availability exception: id={}", exceptionId);

        AvailabilityException exception = availabilityExceptionDAO.findById(exceptionId)
            .orElseThrow(() -> new IllegalArgumentException("Exception not found"));

        // Validate access
        validateOwnershipByException(exception, restaurantId, userId);

        // 📝 AUDIT: Log exception deletion (before delete)
        ServiceVersion sv = exception.getServiceVersion();
        auditService.auditScheduleDeleted(
            EntityType.AVAILABILITY_EXCEPTION,
            exception.getId(),
            sv.getService().getId(),
            sv.getService().getRestaurant().getId(),
            userId,
            exception,
            "Availability exception deleted for " + exception.getExceptionDate()
        );

        availabilityExceptionDAO.delete(exception);
        log.info("Availability exception deleted: id={}", exceptionId);
    }

    // ============================================
    // 7. DEACTIVATE SCHEDULE
    // ============================================

    /**
     * Stop accepting reservations from a specific date
     */
    public void deactivateSchedule(
            Long serviceVersionId,
            LocalDate fromDate,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Deactivating schedule for serviceVersion={} from {}", serviceVersionId, fromDate);

        // Validate access
        validateOwnership(serviceVersionId, restaurantId, userId);

        ServiceVersion sv = serviceVersionDAO.findById(serviceVersionId)
            .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found"));

        // Mark as inactive
        sv.setState(ServiceVersion.VersionState.ARCHIVED);

        serviceVersionDAO.save(sv);
        
        // 📝 AUDIT: Log schedule deactivation
        auditService.auditScheduleDeactivated(
            sv.getService().getId(),
            sv.getService().getRestaurant().getId(),
            userId,
            "Schedule deactivated from " + fromDate
        );
        
        log.info("Schedule deactivated for serviceVersion={}", serviceVersionId);
    }

    // ============================================
    // 8. REACTIVATE SCHEDULE
    // ============================================

    /**
     * Resume accepting reservations
     */
    public void reactivateSchedule(
            Long serviceVersionId,
            Long restaurantId,
            Long userId) throws IllegalArgumentException {

        log.info("Reactivating schedule for serviceVersion={}", serviceVersionId);

        // Validate access
        validateOwnership(serviceVersionId, restaurantId, userId);

        ServiceVersion sv = serviceVersionDAO.findById(serviceVersionId)
            .orElseThrow(() -> new IllegalArgumentException("ServiceVersion not found"));

        // Mark as active
        sv.setState(ServiceVersion.VersionState.ACTIVE);

        serviceVersionDAO.save(sv);
        
        // 📝 AUDIT: Log schedule reactivation
        auditService.auditScheduleActivated(
            sv.getService().getId(),
            sv.getService().getRestaurant().getId(),
            userId,
            "Schedule reactivated"
        );
        
        log.info("Schedule reactivated for serviceVersion={}", serviceVersionId);
    }

    // ============================================
    // 9. CUSTOMER-FACING METHODS (Read-only)
    // ============================================

    /**
     * Get active time slots for a restaurant on a specific date.
     * Used by customers to see available booking times.
     * 
     * @param restaurantId ID of the restaurant
     * @param date Target date
     * @return List of available time slots across all active services
     */
    public List<TimeSlotDto> getActiveTimeSlotsForRestaurant(Long restaurantId, LocalDate date) {
        log.info("Getting active time slots for restaurant {} on {}", restaurantId, date);

        List<TimeSlotDto> allSlots = new ArrayList<>();

        // Get all active service versions for this restaurant
        Collection<ServiceVersion> activeVersions = serviceVersionDAO
            .findActiveByRestaurantId(restaurantId);

        for (ServiceVersion sv : activeVersions) {
            try {
                List<TimeSlotDto> serviceSlots = getActiveTimeSlotsForDate(
                    sv.getId(), date, restaurantId, null);
                allSlots.addAll(serviceSlots);
            } catch (Exception e) {
                log.warn("Error getting slots for serviceVersion {}: {}", sv.getId(), e.getMessage());
            }
        }

        // Sort by start time
        allSlots.sort((a, b) -> a.getSlotStart().compareTo(b.getSlotStart()));
        return allSlots;
    }

    /**
     * Get active time slots for a specific service version (customer view).
     * No ownership validation required - public data.
     * 
     * @param serviceVersionId ID of the service version
     * @param restaurantId ID of the restaurant
     * @param date Target date
     * @return List of available time slots
     */
    public List<TimeSlotDto> getActiveTimeSlotsForServiceVersion(
            Long serviceVersionId, 
            Long restaurantId, 
            LocalDate date) {
        log.info("Getting active time slots for serviceVersion {} on {}", serviceVersionId, date);

        // No ownership validation - this is public data for customers
        return getActiveTimeSlotsForDate(serviceVersionId, date, restaurantId, null);
    }

    /**
     * Get weekly schedule for customer view (read-only, no ownership check).
     * 
     * @param serviceVersionId ID of the service version
     * @param restaurantId ID of the restaurant (for validation)
     * @return Weekly schedule (7 days)
     */
    public List<ServiceVersionDayDto> getWeeklyScheduleForCustomer(
            Long serviceVersionId, 
            Long restaurantId) {
        log.info("Getting weekly schedule for customer: serviceVersion={}, restaurant={}", 
                 serviceVersionId, restaurantId);

        // Load all 7 days (no ownership validation - public data)
        Collection<ServiceVersionDay> days = serviceVersionDayDAO.findAllByServiceVersionId(serviceVersionId);

        return days.stream()
            .map(this::toServiceVersionDayDto)
            .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
            .collect(Collectors.toList());
    }

    /**
     * Get details of a specific time slot.
     * 
     * @param serviceVersionId ID of the service version
     * @param restaurantId ID of the restaurant
     * @param date Date of the slot
     * @param time Start time of the slot
     * @return Time slot details or null if not available
     */
    public TimeSlotDto getTimeSlotDetails(
            Long serviceVersionId, 
            Long restaurantId, 
            LocalDate date, 
            LocalTime time) {
        log.info("Getting time slot details: serviceVersion={}, date={}, time={}", 
                 serviceVersionId, date, time);

        // Get all slots for the date
        List<TimeSlotDto> slots = getActiveTimeSlotsForServiceVersion(
            serviceVersionId, restaurantId, date);

        // Find the matching slot
        return slots.stream()
            .filter(slot -> slot.getSlotStart().toLocalTime().equals(time))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Time slot not found: " + date + " " + time));
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Validate user owns this service version's restaurant
     */
    private void validateOwnership(Long serviceVersionId, Long restaurantId, Long userId)
            throws IllegalArgumentException {
        // TODO: Implement authorization logic
        // Check: userId owns restaurant with restaurantId
        // Check: restaurantId owns service with serviceVersionId's service
        log.debug("Validating ownership for serviceVersion={}, restaurant={}, user={}", 
            serviceVersionId, restaurantId, userId);
    }

    /**
     * Validate user owns exception's service version's restaurant
     */
    private void validateOwnershipByException(AvailabilityException exception, Long restaurantId, Long userId)
            throws IllegalArgumentException {
        validateOwnership(exception.getServiceVersion().getId(), restaurantId, userId);
    }

    /**
     * Get effective operating hours for a date
     */
    private LocalTime[] getEffectiveHours(Long serviceVersionId, LocalDate date, ServiceVersionDay day) {
        if (day.getIsClosed()) {
            return null;
        }

        LocalTime startTime = day.getOpeningTime();
        LocalTime endTime = day.getClosingTime();

        if (startTime == null || endTime == null) {
            return null;
        }

        // Check for reduced hours exception
        Collection<AvailabilityException> exceptions = availabilityExceptionDAO
            .findByServiceVersionAndDate(serviceVersionId, date);

        for (AvailabilityException ex : exceptions) {
            if (ex.getOverrideOpeningTime() != null) {
                startTime = ex.getOverrideOpeningTime();
            }
            if (ex.getOverrideClosingTime() != null) {
                endTime = ex.getOverrideClosingTime();
            }
        }

        return new LocalTime[] { startTime, endTime };
    }

    /**
     * Generate time slots for a date
     */
    private List<TimeSlotDto> generateTimeSlots(
            Long serviceVersionId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            ServiceVersionSlotConfigDto config,
            ServiceVersionDay day) {

        List<TimeSlotDto> slots = new ArrayList<>();

        int slotNumber = 1;
        LocalTime currentTime = startTime;

        int slotDuration = config.getSlotDurationMinutes();
        int bufferTime = config.getBufferTimeMinutes();
        int totalTimePerSlot = slotDuration + bufferTime;

        while (currentTime.plusMinutes(slotDuration).isBefore(endTime) ||
               currentTime.plusMinutes(slotDuration).equals(endTime)) {

            // Check if this time falls within a break
            if (day.hasBreak()) {
                if (!currentTime.isBefore(day.getBreakStart()) && currentTime.isBefore(day.getBreakEnd())) {
                    currentTime = day.getBreakEnd();
                    continue;
                }
            }

            LocalTime slotEnd = currentTime.plusMinutes(slotDuration);

            TimeSlotDto slot = TimeSlotDto.builder()
                .id(String.format("sv_%d_slot_%03d_%s", serviceVersionId, slotNumber, date))
                .serviceVersionId(serviceVersionId)
                .slotStart(java.time.LocalDateTime.of(date, currentTime))
                .slotEnd(java.time.LocalDateTime.of(date, slotEnd))
                .totalCapacity(config.getMaxCapacityPerSlot())
                .availableCapacity(config.getMaxCapacityPerSlot())
                .bookingCount(0)
                .isAvailable(true)
                .generatedFromConfigId(config.getId())
                .build();

            slots.add(slot);

            currentTime = currentTime.plusMinutes(totalTimePerSlot);
            slotNumber++;
        }

        return slots;
    }

    /**
     * Filter slots by partial closures and exceptions
     */
    private List<TimeSlotDto> filterByPartialClosures(
            List<TimeSlotDto> slots,
            Long serviceVersionId,
            LocalDate date) {

        Collection<AvailabilityException> exceptions = availabilityExceptionDAO
            .findByServiceVersionAndDate(serviceVersionId, date);

        return slots.stream()
            .filter(slot -> {
                for (AvailabilityException ex : exceptions) {
                    LocalTime slotTime = slot.getSlotStart().toLocalTime();
                    if (ex.getStartTime() != null && ex.getEndTime() != null) {
                        if (!slotTime.isBefore(ex.getStartTime()) && slotTime.isBefore(ex.getEndTime())) {
                            return false; // Filter out slots during exceptions
                        }
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * Enrich slots with current reservation data
     */
    private void enrichWithReservationData(List<TimeSlotDto> slots, LocalDate date) {
        for (TimeSlotDto slot : slots) {
            // TODO: Query reservations in this time slot
            // Update availableCapacity and bookingCount
            slot.setAvailable(slot.getAvailableCapacity() > 0);
        }
    }

    // ============================================
    // DTO CONVERSION HELPERS
    // ============================================

    private ServiceVersionDayDto toServiceVersionDayDto(ServiceVersionDay entity) {
        return ServiceVersionDayDto.builder()
            .id(entity.getId())
            .serviceVersionId(entity.getServiceVersion().getId())
            .dayOfWeek(entity.getDayOfWeek())
            .isClosed(entity.getIsClosed())
            .operatingStartTime(entity.getOpeningTime())
            .operatingEndTime(entity.getClosingTime())
            .breakStart(entity.getBreakStart())
            .breakEnd(entity.getBreakEnd())
            .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant(java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now())) : null)
            .createdBy("system")
            .modifiedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant(java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now())) : null)
            .modifiedBy("system")
            .build();
    }

    private ServiceVersionSlotConfigDto toServiceVersionSlotConfigDto(ServiceVersionSlotConfig entity) {
        return ServiceVersionSlotConfigDto.builder()
            .id(entity.getId())
            .serviceVersionId(entity.getServiceVersion().getId())
            .slotDurationMinutes(entity.getSlotDurationMinutes())
            .bufferTimeMinutes(entity.getBufferMinutes())
            .dailyStartTime(entity.getStartTime())
            .dailyEndTime(entity.getEndTime())
            .maxCapacityPerSlot(entity.getMaxConcurrentReservations())
            .generationRule("GENERATE_ON_DEMAND")
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .createdAt(java.time.Instant.now())
            .createdBy("system")
            .modifiedAt(java.time.Instant.now())
            .modifiedBy("system")
            .build();
    }

    private AvailabilityExceptionDto toAvailabilityExceptionDto(AvailabilityException entity) {
        return AvailabilityExceptionDto.builder()
            .id(entity.getId())
            .serviceVersionId(entity.getServiceVersion().getId())
            .exceptionType(entity.getExceptionType().name())
            .exceptionDate(entity.getExceptionDate())
            .isFullyClosed(entity.getIsFullyClosed())
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .overrideOpeningTime(entity.getOverrideOpeningTime())
            .overrideClosingTime(entity.getOverrideClosingTime())
            .reason(entity.getNotes())
            .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant(java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now())) : null)
            .createdBy("system")
            .modifiedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant(java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now())) : null)
            .modifiedBy("system")
            .build();
    }

}
