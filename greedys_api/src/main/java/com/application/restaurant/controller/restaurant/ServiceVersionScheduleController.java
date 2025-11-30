package com.application.restaurant.controller.restaurant;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.ServiceVersionScheduleService;
import com.application.restaurant.web.dto.schedule.ServiceVersionDayDto;
import com.application.restaurant.web.dto.schedule.ServiceVersionSlotConfigDto;
import com.application.restaurant.web.dto.schedule.AvailabilityExceptionDto;
import com.application.restaurant.web.dto.schedule.TimeSlotDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for managing restaurant service version schedules.
 * 
 * This is the new scheduling API based on ServiceVersionDay architecture.
 * It replaces the legacy Slot-based controllers with a more flexible and maintainable design.
 * 
 * <strong>Legacy Controller Mappings:</strong>
 * <ul>
 *   <li>RestaurantSlotController.newSlot() → ServiceVersionScheduleController.createServiceVersion()</li>
 *   <li>RestaurantSlotController.cancelSlot() → ServiceVersionScheduleController.deactivateSlot()</li>
 *   <li>CustomerSlotController.getAllSlotsByRestaurantId() → CustomerServiceVersionScheduleController.getActiveScheduleForRestaurant()</li>
 *   <li>SlotTransitionController.changeSlotSchedule() → ServiceVersionScheduleController.updateSlotConfig()</li>
 *   <li>SlotTransitionController.getActiveSlotsForService() → ServiceVersionScheduleController.getActiveTimeSlots()</li>
 *   <li>SlotTransitionController.deactivateSlot() → ServiceVersionScheduleController.deactivateSchedule()</li>
 * </ul>
 * 
 * @see RestaurantSlotController (DEPRECATED - use this controller instead)
 * @see SlotTransitionController (DEPRECATED - use this controller instead)
 * @see CustomerSlotController (DEPRECATED - use CustomerServiceVersionScheduleController instead)
 */
@Tag(
    name = "Service Version Schedules (NEW)", 
    description = "NEW API for managing restaurant service schedules. Replaces legacy Slot controllers."
)
@RestController
@RequestMapping("/restaurant/schedule")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('RESTAURANT')")
@RequiredArgsConstructor
@Slf4j
public class ServiceVersionScheduleController extends BaseController {

    private final ServiceVersionScheduleService scheduleService;

    /**
     * Get weekly schedule for a service version (7 days).
     * 
     * @param serviceVersionId ID of the service version
     * @param restaurantId ID of the restaurant (owner verification)
     * @return List of 7 ServiceVersionDayDto (one per day of week)
     */
    @GetMapping("/service-version/{serviceVersionId}")
    @Operation(
        summary = "Get weekly schedule for service version",
        description = "Retrieve the complete weekly schedule (7 days) for a specific service version"
    )
    @ReadApiResponses
    public ResponseEntity<List<ServiceVersionDayDto>> getWeeklySchedule(
            @PathVariable Long serviceVersionId,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeList("get weekly schedule for service version", () ->
            scheduleService.getWeeklySchedule(serviceVersionId, restaurantId, rUser.getId())
        );
    }

    /**
     * Get active time slots for a specific service version on a given date.
     * 
     * Replaces: SlotTransitionController.getActiveSlotsForService()
     * 
     * @param serviceVersionId ID of the service version
     * @param date Target date (YYYY-MM-DD format)
     * @param restaurantId ID of the restaurant
     * @return List of available time slots
     */
    @GetMapping("/active-slots/service-version/{serviceVersionId}")
    @Operation(
        summary = "Get active time slots for date",
        description = "Retrieve all active time slots for a service version on a specific date"
    )
    @ReadApiResponses
    public ResponseEntity<List<TimeSlotDto>> getActiveTimeSlots(
            @PathVariable Long serviceVersionId,
            @Parameter(description = "Date (YYYY-MM-DD)") @RequestParam LocalDate date,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeList("get active time slots for date", () ->
            scheduleService.getActiveTimeSlotsForDate(serviceVersionId, date, restaurantId, rUser.getId())
        );
    }

    /**
     * Update slot configuration (generation rules, duration, buffer time, etc.) for a service version.
     * 
     * Replaces: SlotTransitionController.changeSlotSchedule()
     * 
     * @param slotConfigDto Configuration parameters (duration, buffer, generation rules)
     * @param restaurantId ID of the restaurant
     * @return Updated slot configuration
     */
    @PutMapping("/slot-config/{serviceVersionId}")
    @Operation(
        summary = "Update slot configuration",
        description = "Modify slot generation parameters (duration, buffer time, start/end times)"
    )
    @CreateApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    public ResponseEntity<ServiceVersionSlotConfigDto> updateSlotConfig(
            @PathVariable Long serviceVersionId,
            @RequestBody ServiceVersionSlotConfigDto slotConfigDto,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeCreate("update slot configuration", "Slot configuration updated", () ->
            scheduleService.updateSlotConfiguration(serviceVersionId, slotConfigDto, restaurantId, rUser.getId())
        );
    }

    /**
     * Update schedule for a specific day of week.
     * 
     * @param serviceVersionId ID of the service version
     * @param dayOfWeek Target day (MONDAY, TUESDAY, etc.)
     * @param dayDto Updated day schedule (isActive, operatingHours, etc.)
     * @param restaurantId ID of the restaurant
     * @return Updated day schedule
     */
    @PutMapping("/day/{serviceVersionId}")
    @Operation(
        summary = "Update day schedule",
        description = "Modify schedule for a specific day of week"
    )
    @CreateApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    public ResponseEntity<ServiceVersionDayDto> updateDaySchedule(
            @PathVariable Long serviceVersionId,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestBody ServiceVersionDayDto dayDto,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return execute("update day schedule", () ->
            scheduleService.updateDaySchedule(serviceVersionId, dayOfWeek, dayDto, restaurantId, rUser.getId())
        );
    }

    /**
     * Create an availability exception (closure, reduced hours, special event).
     * 
     * @param exceptionDto Exception details (date range, type, reason)
     * @param serviceVersionId ID of the service version
     * @param restaurantId ID of the restaurant
     * @return Created exception
     */
    @PostMapping("/exception/{serviceVersionId}")
    @Operation(
        summary = "Create availability exception",
        description = "Add a closure, reduced hours, or special event"
    )
    @CreateApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    public ResponseEntity<AvailabilityExceptionDto> createAvailabilityException(
            @PathVariable Long serviceVersionId,
            @RequestBody AvailabilityExceptionDto exceptionDto,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeCreate("create availability exception", "Exception created successfully", () ->
            scheduleService.createAvailabilityException(serviceVersionId, exceptionDto, restaurantId, rUser.getId())
        );
    }

    /**
     * Delete an availability exception.
     * 
     * @param exceptionId ID of the exception to remove
     * @param restaurantId ID of the restaurant
     */
    @DeleteMapping("/exception/{exceptionId}")
    @Operation(
        summary = "Delete availability exception",
        description = "Remove a closure or special event"
    )
    @ReadApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    public ResponseEntity<String> deleteAvailabilityException(
            @PathVariable Long exceptionId,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return execute("delete availability exception", () -> {
            scheduleService.deleteAvailabilityException(exceptionId, restaurantId, rUser.getId());
            return "Exception deleted successfully";
        });
    }

    /**
     * Deactivate schedule for a service version starting from a specific date.
     * 
     * Replaces: SlotTransitionController.deactivateSlot()
     * 
     * @param serviceVersionId ID of the service version
     * @param fromDate Date when deactivation should start
     * @param restaurantId ID of the restaurant
     */
    @PostMapping("/deactivate/{serviceVersionId}")
    @Operation(
        summary = "Deactivate service version schedule",
        description = "Stop accepting reservations for this service version starting from a date"
    )
    @ReadApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    public ResponseEntity<String> deactivateSchedule(
            @PathVariable Long serviceVersionId,
            @Parameter(description = "Date when deactivation starts") @RequestParam LocalDate fromDate,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return execute("deactivate service version schedule", () -> {
            scheduleService.deactivateSchedule(serviceVersionId, fromDate, restaurantId, rUser.getId());
            return "Service version schedule deactivated successfully";
        });
    }

    /**
     * Reactivate a deactivated service version schedule.
     * 
     * @param serviceVersionId ID of the service version
     * @param restaurantId ID of the restaurant
     */
    @PostMapping("/reactivate/{serviceVersionId}")
    @Operation(
        summary = "Reactivate service version schedule",
        description = "Resume accepting reservations for this service version"
    )
    @ReadApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    public ResponseEntity<String> reactivateSchedule(
            @PathVariable Long serviceVersionId,
            @RequestParam Long restaurantId,
            @AuthenticationPrincipal RUser rUser) {
        
        return execute("reactivate service version schedule", () -> {
            scheduleService.reactivateSchedule(serviceVersionId, restaurantId, rUser.getId());
            return "Service version schedule reactivated successfully";
        });
    }
}
