package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.restaurant.service.ServiceVersionScheduleService;
import com.application.restaurant.web.dto.schedule.ServiceVersionDayDto;
import com.application.restaurant.web.dto.schedule.TimeSlotDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Customer-facing controller for restaurant schedule and time slot information.
 * 
 * This controller replaces the deprecated CustomerSlotController with
 * the new ServiceVersion-based scheduling architecture.
 * 
 * <strong>Legacy Endpoint Mappings:</strong>
 * <ul>
 *   <li>CustomerSlotController.getAllSlotsByRestaurantId() → getActiveScheduleForRestaurant()</li>
 *   <li>CustomerSlotController.getSlotById() → getTimeSlotDetails()</li>
 * </ul>
 * 
 * @see CustomerSlotController (DEPRECATED)
 */
@Tag(
    name = "Customer Restaurant Schedule", 
    description = "View restaurant availability and time slots for booking"
)
@RestController
@RequestMapping("/customer/restaurant")
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceVersionScheduleController extends BaseController {

    private final ServiceVersionScheduleService scheduleService;

    /**
     * Get available time slots for a restaurant on a specific date.
     * 
     * Replaces: CustomerSlotController.getAllSlotsByRestaurantId()
     * 
     * @param restaurantId ID of the restaurant
     * @param date Target date (defaults to today if not specified)
     * @return List of available time slots
     */
    @GetMapping("/{restaurantId}/schedule")
    @Operation(
        summary = "Get restaurant schedule",
        description = "Retrieve available time slots for a restaurant on a specific date"
    )
    @ReadApiResponses
    public ResponseEntity<List<TimeSlotDto>> getActiveScheduleForRestaurant(
            @PathVariable Long restaurantId,
            @Parameter(description = "Date (YYYY-MM-DD), defaults to today")
            @RequestParam(required = false) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        log.info("Customer requesting schedule for restaurant {} on {}", restaurantId, targetDate);
        
        return executeList("get restaurant schedule", () ->
            scheduleService.getActiveTimeSlotsForRestaurant(restaurantId, targetDate)
        );
    }

    /**
     * Get weekly schedule overview for a restaurant's service.
     * 
     * @param restaurantId ID of the restaurant
     * @param serviceVersionId ID of the service version
     * @return Weekly schedule (7 days)
     */
    @GetMapping("/{restaurantId}/schedule/weekly/{serviceVersionId}")
    @Operation(
        summary = "Get weekly schedule",
        description = "Retrieve the weekly schedule template for a specific service"
    )
    @ReadApiResponses
    public ResponseEntity<List<ServiceVersionDayDto>> getWeeklyScheduleForService(
            @PathVariable Long restaurantId,
            @PathVariable Long serviceVersionId) {
        
        log.info("Customer requesting weekly schedule for service version {} at restaurant {}", 
                 serviceVersionId, restaurantId);
        
        return executeList("get weekly schedule", () ->
            scheduleService.getWeeklyScheduleForCustomer(serviceVersionId, restaurantId)
        );
    }

    /**
     * Get details of a specific time slot.
     * 
     * Replaces: CustomerSlotController.getSlotById()
     * 
     * In the new architecture, slots don't have persistent IDs.
     * They are identified by serviceVersion + date + time.
     * 
     * @param restaurantId ID of the restaurant
     * @param serviceVersionId ID of the service version
     * @param date Date of the slot
     * @param time Start time of the slot
     * @return Time slot details
     */
    @GetMapping("/{restaurantId}/schedule/timeslot")
    @Operation(
        summary = "Get time slot details",
        description = "Retrieve details of a specific time slot (availability, capacity, etc.)"
    )
    @ReadApiResponses
    public ResponseEntity<TimeSlotDto> getTimeSlotDetails(
            @PathVariable Long restaurantId,
            @Parameter(description = "Service version ID") @RequestParam Long serviceVersionId,
            @Parameter(description = "Date (YYYY-MM-DD)") @RequestParam LocalDate date,
            @Parameter(description = "Start time (HH:mm)") @RequestParam LocalTime time) {
        
        log.info("Customer requesting slot details: restaurant={}, serviceVersion={}, date={}, time={}", 
                 restaurantId, serviceVersionId, date, time);
        
        return execute("get time slot details", () ->
            scheduleService.getTimeSlotDetails(serviceVersionId, restaurantId, date, time)
        );
    }

    /**
     * Get available time slots for a specific service on a date.
     * 
     * @param restaurantId ID of the restaurant
     * @param serviceVersionId ID of the service version
     * @param date Target date
     * @return List of available time slots for the service
     */
    @GetMapping("/{restaurantId}/schedule/service/{serviceVersionId}")
    @Operation(
        summary = "Get service time slots",
        description = "Retrieve available time slots for a specific service on a date"
    )
    @ReadApiResponses
    public ResponseEntity<List<TimeSlotDto>> getTimeSlotsForService(
            @PathVariable Long restaurantId,
            @PathVariable Long serviceVersionId,
            @Parameter(description = "Date (YYYY-MM-DD)") @RequestParam LocalDate date) {
        
        log.info("Customer requesting slots for service {} on {} at restaurant {}", 
                 serviceVersionId, date, restaurantId);
        
        return executeList("get service time slots", () ->
            scheduleService.getActiveTimeSlotsForServiceVersion(serviceVersionId, restaurantId, date)
        );
    }
}
