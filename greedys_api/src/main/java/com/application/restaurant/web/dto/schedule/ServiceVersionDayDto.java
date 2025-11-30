package com.application.restaurant.web.dto.schedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ServiceVersionDay - represents the schedule for one day of the week
 * 
 * Each ServiceVersion has 7 of these (Monday through Sunday).
 * These form the template for slot generation and reservation availability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceVersionDayDto {

    /**
     * Unique identifier for this day schedule record
     */
    private Long id;

    /**
     * Which service version this schedule belongs to
     */
    private Long serviceVersionId;

    /**
     * Day of week (MONDAY, TUESDAY, ..., SUNDAY)
     */
    private DayOfWeek dayOfWeek;

    /**
     * Is this day closed? (no reservations possible)
     * 
     * If true: ignore operatingStartTime/operatingEndTime, day is fully closed
     * If false: use operatingStartTime and operatingEndTime
     */
    @JsonProperty("isClosed")
    private boolean isClosed;

    /**
     * When do operations start on this day? (e.g., 12:00 for lunch)
     * 
     * Only used if isClosed = false
     * Example: LocalTime.of(12, 0) for 12:00 PM
     */
    private LocalTime operatingStartTime;

    /**
     * When do operations end on this day? (e.g., 22:00 for dinner)
     * 
     * Only used if isClosed = false
     * Example: LocalTime.of(22, 0) for 10:00 PM
     */
    private LocalTime operatingEndTime;

    /**
     * Optional break start time during the day
     * 
     * Example: LocalTime.of(14, 30) for 2:30 PM (after lunch, before dinner)
     * If null: no break on this day
     */
    private LocalTime breakStart;

    /**
     * Optional break end time during the day
     * 
     * Example: LocalTime.of(17, 30) for 5:30 PM (end of break, dinner starts)
     * Used together with breakStart to exclude a time window from availability
     */
    private LocalTime breakEnd;

    /**
     * Audit: when was this record created
     */
    private Instant createdAt;

    /**
     * Audit: who created this record (user ID or system)
     */
    private String createdBy;

    /**
     * Audit: when was this record last modified
     */
    private Instant modifiedAt;

    /**
     * Audit: who last modified this record
     */
    private String modifiedBy;

    /**
     * Helper method to check if this day is available for reservations
     * 
     * @return true if not closed and operating times are set
     */
    public boolean isAvailable() {
        return !isClosed && operatingStartTime != null && operatingEndTime != null;
    }

    /**
     * Helper method to check if there's a break on this day
     * 
     * @return true if both breakStart and breakEnd are set
     */
    public boolean hasBreak() {
        return breakStart != null && breakEnd != null;
    }

    /**
     * Helper method to get operating duration in minutes
     * 
     * @return minutes from operatingStartTime to operatingEndTime (excluding break)
     */
    public int getOperatingMinutes() {
        if (!isAvailable()) {
            return 0;
        }

        int totalMinutes = operatingEndTime.toSecondOfDay() / 60 - operatingStartTime.toSecondOfDay() / 60;

        if (hasBreak()) {
            int breakMinutes = breakEnd.toSecondOfDay() / 60 - breakStart.toSecondOfDay() / 60;
            totalMinutes -= breakMinutes;
        }

        return totalMinutes;
    }

}
