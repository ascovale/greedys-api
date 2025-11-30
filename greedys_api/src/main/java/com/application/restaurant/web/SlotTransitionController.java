package com.application.restaurant.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.persistence.model.reservation.Slot;
import com.application.common.persistence.model.reservation.SlotChangePolicy;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.service.SlotTransitionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * DEPRECATED: Use ServiceVersionScheduleController instead.
 * 
 * This controller is based on the legacy Slot architecture.
 * Will be removed in v3.0 (planned for Q2 2025).
 * 
 * <strong>Method Mapping:</strong>
 * <ul>
 *   <li>changeSlotSchedule() → ServiceVersionScheduleController.updateSlotConfig()</li>
 *   <li>getActiveSlotsForService() → ServiceVersionScheduleController.getActiveTimeSlots()</li>
 *   <li>deactivateSlot() → ServiceVersionScheduleController.deactivateSchedule()</li>
 *   <li>reactivateSlot() → ServiceVersionScheduleController.reactivateSchedule()</li>
 * </ul>
 * 
 * REST Controller for managing slot schedule transitions and changes.
 * Handles slot modifications while preserving existing reservation integrity.
 * 
 * @deprecated Since v2.0, use {@link ServiceVersionScheduleController}
 * @see ServiceVersionScheduleController
 */
@Deprecated(since = "2.0", forRemoval = true)
@Tag(name = "Slot Transitions (DEPRECATED)", description = "DEPRECATED - Use Service Version Schedules API instead")
@RestController
@RequestMapping("/api/restaurant/slot-transitions")
@PreAuthorize("hasRole('RESTAURANT')")
public class SlotTransitionController {

    @Autowired
    private SlotTransitionService slotTransitionService;

    /**
     * Change slot schedule with specified policy for handling existing reservations
     * 
     * @deprecated Since v2.0, use ServiceVersionScheduleController.updateSlotConfig() instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping("/change-schedule")
    @Operation(summary = "Change slot schedule", 
              description = "Modify slot times with specified policy for handling existing reservations")
    public ResponseEntity<?> changeSlotSchedule(@RequestBody SlotScheduleChangeRequest request) {
        try {
            SlotDTO newSlot = slotTransitionService.changeSlotSchedule(
                request.getSlotId(),
                request.getNewStartTime(),
                request.getNewEndTime(),
                request.getEffectiveDate(),
                request.getChangePolicy()
            );
            
            return ResponseEntity.ok(new SlotScheduleChangeResponse(
                true, 
                "Slot schedule changed successfully", 
                newSlot.getId(),
                request.getChangePolicy().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SlotScheduleChangeResponse(
                false, 
                "Failed to change slot schedule: " + e.getMessage(),
                null,
                null
            ));
        }
    }

    /**
     * Get active slots for a service on a specific date
     * 
     * @deprecated Since v2.0, use ServiceVersionScheduleController.getActiveTimeSlots() instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @GetMapping("/active-slots/service/{serviceId}")
    @Operation(summary = "Get active slots for service", 
              description = "Retrieve all active slots for a service on a specific date")
    public ResponseEntity<List<Slot>> getActiveSlotsForService(
            @Parameter(description = "Service ID") @PathVariable Long serviceId,
            @Parameter(description = "Date to check (YYYY-MM-DD)") @RequestParam LocalDate date) {
        
        List<Slot> activeSlots = slotTransitionService.getActiveSlotsForDate(serviceId, date);
        return ResponseEntity.ok(activeSlots);
    }

    /**
     * Check if a slot can be safely modified
     * 
     * @deprecated Since v2.0, use ServiceVersionScheduleController methods instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @GetMapping("/can-modify/{slotId}")
    @Operation(summary = "Check if slot can be modified", 
              description = "Check if a slot can be safely modified based on existing reservations")
    public ResponseEntity<SlotModificationCheckResponse> canModifySlot(
            @Parameter(description = "Slot ID") @PathVariable Long slotId) {
        
        boolean canModify = slotTransitionService.canSlotBeModified(slotId);
        int futureReservationsCount = slotTransitionService.getFutureReservationsCount(slotId);
        
        return ResponseEntity.ok(new SlotModificationCheckResponse(
            canModify,
            futureReservationsCount,
            canModify ? "Slot can be modified safely" : 
                      "Slot has " + futureReservationsCount + " future reservations"
        ));
    }

    /**
     * Deactivate a slot starting from a specific date
     * 
     * @deprecated Since v2.0, use ServiceVersionScheduleController.deactivateSchedule() instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping("/deactivate/{slotId}")
    @Operation(summary = "Deactivate slot", 
              description = "Deactivate a slot starting from a specific date")
    public ResponseEntity<?> deactivateSlot(
            @Parameter(description = "Slot ID") @PathVariable Long slotId,
            @Parameter(description = "Deactivation date") @RequestParam LocalDate fromDate) {
        
        try {
            slotTransitionService.deactivateSlot(slotId, fromDate);
            return ResponseEntity.ok(new SimpleResponse(true, "Slot deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SimpleResponse(
                false, "Failed to deactivate slot: " + e.getMessage()));
        }
    }

    /**
     * Reactivate a deactivated slot
     * 
     * @deprecated Since v2.0, use ServiceVersionScheduleController.reactivateSchedule() instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping("/reactivate/{slotId}")
    @Operation(summary = "Reactivate slot", 
              description = "Reactivate a previously deactivated slot")
    public ResponseEntity<?> reactivateSlot(
            @Parameter(description = "Slot ID") @PathVariable Long slotId) {
        
        try {
            slotTransitionService.reactivateSlot(slotId);
            return ResponseEntity.ok(new SimpleResponse(true, "Slot reactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SimpleResponse(
                false, "Failed to reactivate slot: " + e.getMessage()));
        }
    }

    // Request/Response DTOs

    public static class SlotScheduleChangeRequest {
        private Long slotId;
        private LocalTime newStartTime;
        private LocalTime newEndTime;
        private LocalDate effectiveDate;
        private SlotChangePolicy changePolicy;

        // Getters and setters
        public Long getSlotId() { return slotId; }
        public void setSlotId(Long slotId) { this.slotId = slotId; }

        public LocalTime getNewStartTime() { return newStartTime; }
        public void setNewStartTime(LocalTime newStartTime) { this.newStartTime = newStartTime; }

        public LocalTime getNewEndTime() { return newEndTime; }
        public void setNewEndTime(LocalTime newEndTime) { this.newEndTime = newEndTime; }

        public LocalDate getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

        public SlotChangePolicy getChangePolicy() { return changePolicy; }
        public void setChangePolicy(SlotChangePolicy changePolicy) { this.changePolicy = changePolicy; }
    }

    public static class SlotScheduleChangeResponse {
        private boolean success;
        private String message;
        private Long newSlotId;
        private String policyApplied;

        public SlotScheduleChangeResponse(boolean success, String message, Long newSlotId, String policyApplied) {
            this.success = success;
            this.message = message;
            this.newSlotId = newSlotId;
            this.policyApplied = policyApplied;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Long getNewSlotId() { return newSlotId; }
        public void setNewSlotId(Long newSlotId) { this.newSlotId = newSlotId; }

        public String getPolicyApplied() { return policyApplied; }
        public void setPolicyApplied(String policyApplied) { this.policyApplied = policyApplied; }
    }

    public static class SlotModificationCheckResponse {
        private boolean canModify;
        private int futureReservationsCount;
        private String message;

        public SlotModificationCheckResponse(boolean canModify, int futureReservationsCount, String message) {
            this.canModify = canModify;
            this.futureReservationsCount = futureReservationsCount;
            this.message = message;
        }

        // Getters and setters
        public boolean isCanModify() { return canModify; }
        public void setCanModify(boolean canModify) { this.canModify = canModify; }

        public int getFutureReservationsCount() { return futureReservationsCount; }
        public void setFutureReservationsCount(int futureReservationsCount) { 
            this.futureReservationsCount = futureReservationsCount; 
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class SimpleResponse {
        private boolean success;
        private String message;

        public SimpleResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}