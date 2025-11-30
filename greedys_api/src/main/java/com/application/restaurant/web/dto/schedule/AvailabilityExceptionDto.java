package com.application.restaurant.web.dto.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AvailabilityException - represents date-specific availability changes
 * 
 * Used to model:
 * - Full-day closures (holidays, special events, maintenance)
 * - Partial-day closures (maintenance windows, meetings)
 * - Reduced hours (special opening hours)
 * - Special events (private events, catering)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityExceptionDto {

    /**
     * Unique identifier for this exception
     */
    private Long id;

    /**
     * Which service version this exception applies to
     */
    private Long serviceVersionId;

    /**
     * Type of exception
     * 
     * Values:
     * - "FULL_DAY_CLOSURE": Completely closed (holidays, emergencies)
     * - "PARTIAL_DAY_CLOSURE": Not available during specific hours (maintenance, meetings)
     * - "REDUCED_HOURS": Open but with modified hours (e.g., opens late, closes early)
     * - "SPECIAL_EVENT": Private event, special pricing, different capacity
     * - "MAINTENANCE": Maintenance window (part of PARTIAL_DAY_CLOSURE but specific purpose)
     */
    private String exceptionType;

    /**
     * Date this exception applies to
     * 
     * Example: LocalDate.of(2025, 12, 25) for Christmas
     */
    private LocalDate exceptionDate;

    /**
     * For FULL_DAY_CLOSURE: is this a full-day closure?
     * 
     * If true: restaurant completely closed (ignore startTime/endTime)
     * If false: partial closure or reduced hours (use startTime/endTime)
     */
    private boolean isFullyClosed;

    /**
     * When does unavailability start? (for partial closures)
     * 
     * Example: LocalTime.of(14, 0) = 2:00 PM
     * Example: LocalTime.of(15, 0) = 3:00 PM for maintenance 2-4 PM
     * 
     * Only used if isFullyClosed = false
     */
    private LocalTime startTime;

    /**
     * When does unavailability end? (for partial closures)
     * 
     * Example: LocalTime.of(16, 0) = 4:00 PM
     * 
     * Only used if isFullyClosed = false
     */
    private LocalTime endTime;

    /**
     * Optional override for opening hours (for REDUCED_HOURS)
     * 
     * Example: Restaurant normally opens 12:00, but this day opens at 13:00
     * This would be LocalTime.of(13, 0)
     */
    private LocalTime overrideOpeningTime;

    /**
     * Optional override for closing hours (for REDUCED_HOURS)
     * 
     * Example: Restaurant normally closes 22:00, but this day closes at 21:00
     * This would be LocalTime.of(21, 0)
     */
    private LocalTime overrideClosingTime;

    /**
     * Reason for this exception (for admin notes)
     * 
     * Examples:
     * - "Christmas Day"
     * - "Staff training 2-4 PM"
     * - "Private event for corporate client"
     * - "Annual maintenance"
     * - "Owner's birthday celebration"
     */
    private String reason;

    /**
     * Audit: when was this exception created
     */
    private Instant createdAt;

    /**
     * Audit: who created this exception
     */
    private String createdBy;

    /**
     * Audit: when was this exception last modified
     */
    private Instant modifiedAt;

    /**
     * Audit: who last modified this exception
     */
    private String modifiedBy;

    /**
     * Validate this exception configuration
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (exceptionDate == null) {
            throw new IllegalArgumentException("exceptionDate is required");
        }
        if (exceptionType == null || exceptionType.trim().isEmpty()) {
            throw new IllegalArgumentException("exceptionType is required");
        }

        // If not fully closed, must have start/end times
        if (!isFullyClosed) {
            if (startTime == null && overrideOpeningTime == null) {
                throw new IllegalArgumentException(
                    "For partial closures or reduced hours: startTime or overrideOpeningTime required");
            }
            if (endTime == null && overrideClosingTime == null) {
                throw new IllegalArgumentException(
                    "For partial closures or reduced hours: endTime or overrideClosingTime required");
            }

            // Validate time ranges if set
            if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("startTime must be before endTime");
            }
            if (overrideOpeningTime != null && overrideClosingTime != null
                    && !overrideOpeningTime.isBefore(overrideClosingTime)) {
                throw new IllegalArgumentException("overrideOpeningTime must be before overrideClosingTime");
            }
        }
    }

    /**
     * Check if this is a full-day closure
     * 
     * @return true if completely closed
     */
    public boolean isFullDayClosureException() {
        return isFullyClosed;
    }

    /**
     * Check if this exception applies to a specific time on the date
     * 
     * @param time the time to check
     * @return true if the time falls within the exception window
     */
    public boolean appliesToTime(LocalTime time) {
        if (isFullyClosed) {
            return true; // All times affected
        }

        if (startTime != null && endTime != null) {
            return !time.isBefore(startTime) && time.isBefore(endTime);
        }

        return false;
    }

    /**
     * Get the effective operating hours for this date (if REDUCED_HOURS exception)
     * 
     * @param normalStartTime normal opening time
     * @param normalEndTime normal closing time
     * @return array: [start, end] or null if this is not a REDUCED_HOURS exception
     */
    public LocalTime[] getEffectiveHours(LocalTime normalStartTime, LocalTime normalEndTime) {
        if (isFullyClosed) {
            return null; // No hours - closed
        }

        LocalTime start = overrideOpeningTime != null ? overrideOpeningTime : normalStartTime;
        LocalTime end = overrideClosingTime != null ? overrideClosingTime : normalEndTime;

        return new LocalTime[] { start, end };
    }

}
