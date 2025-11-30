package com.application.restaurant.web.dto.schedule;

import java.time.Instant;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ServiceVersionSlotConfig - defines how slots are generated
 * 
 * REPLACES: Legacy JSON slotGenerationParams
 * 
 * One config per ServiceVersion defines:
 * - How long each slot should be (duration in minutes)
 * - How long between slots (buffer in minutes)  
 * - When slots start and end each day
 * - Maximum capacity per slot
 * - How to generate slots (on-demand, pre-computed, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceVersionSlotConfigDto {

    /**
     * Unique identifier for this slot configuration
     */
    private Long id;

    /**
     * Which service version this configuration belongs to
     */
    private Long serviceVersionId;

    /**
     * How long is each time slot? (in minutes)
     * 
     * Examples:
     * - 30 minutes for fast casual
     * - 60 minutes for fine dining
     * - 90 minutes for wine bars
     */
    private Integer slotDurationMinutes;

    /**
     * How much time between slots? (in minutes)
     * 
     * Used to prevent customer overlap and for cleaning/setup
     * 
     * Examples:
     * - 0 minutes for counter service
     * - 15 minutes for table turnover
     * - 30 minutes for deep cleaning
     */
    private Integer bufferTimeMinutes;

    /**
     * When does slot generation start each day?
     * 
     * Example: LocalTime.of(12, 0) = slots start at 12:00 PM
     * 
     * Note: This can be overridden by ServiceVersionDay for specific days
     * (e.g., Sunday might start at 11:00 AM for brunch)
     */
    private LocalTime dailyStartTime;

    /**
     * When does slot generation end each day?
     * 
     * Example: LocalTime.of(22, 0) = last slot ends by 10:00 PM
     * 
     * Note: This can be overridden by ServiceVersionDay for specific days
     */
    private LocalTime dailyEndTime;

    /**
     * Maximum number of reservations per slot
     * 
     * Examples:
     * - 4 for a 2-seat table (2 cover slot)
     * - 8 for a 4-seat table
     * - 12 for a banquet hall
     */
    private Integer maxCapacityPerSlot;

    /**
     * Rule for generating slots
     * 
     * Values:
     * - "GENERATE_ON_DEMAND": Compute slots only when requested
     * - "GENERATE_DAILY": Pre-compute all daily slots at midnight
     * - "GENERATE_WEEKLY": Pre-compute all weekly slots on Mondays
     * - "GENERATE_MONTHLY": Pre-compute all monthly slots on 1st of month
     */
    private String generationRule;

    /**
     * Optional: start time override (if different from daily pattern)
     * 
     * Example: For lunch service, slots might start at 12:00
     * Can be different from dailyStartTime
     */
    private LocalTime startTime;

    /**
     * Optional: end time override (if different from daily pattern)
     * 
     * Example: For dinner service, slots might end at 22:30
     * Can be different from dailyEndTime
     */
    private LocalTime endTime;

    /**
     * Audit: when was this config created
     */
    private Instant createdAt;

    /**
     * Audit: who created this config
     */
    private String createdBy;

    /**
     * Audit: when was this config last modified
     */
    private Instant modifiedAt;

    /**
     * Audit: who last modified this config
     */
    private String modifiedBy;

    /**
     * Validate this configuration
     * 
     * @return true if all required fields are valid
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (slotDurationMinutes == null || slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("slotDurationMinutes must be > 0");
        }
        if (bufferTimeMinutes == null || bufferTimeMinutes < 0) {
            throw new IllegalArgumentException("bufferTimeMinutes must be >= 0");
        }
        if (maxCapacityPerSlot == null || maxCapacityPerSlot <= 0) {
            throw new IllegalArgumentException("maxCapacityPerSlot must be > 0");
        }
        if (dailyStartTime == null) {
            throw new IllegalArgumentException("dailyStartTime is required");
        }
        if (dailyEndTime == null) {
            throw new IllegalArgumentException("dailyEndTime is required");
        }
        if (dailyStartTime.isAfter(dailyEndTime) || dailyStartTime.equals(dailyEndTime)) {
            throw new IllegalArgumentException("dailyStartTime must be before dailyEndTime");
        }
        if (generationRule == null || generationRule.trim().isEmpty()) {
            throw new IllegalArgumentException("generationRule is required");
        }
    }

    /**
     * Calculate total time available per slot (including buffer)
     * 
     * @return total minutes = slotDuration + bufferTime
     */
    public int getTotalTimePerSlot() {
        return slotDurationMinutes + bufferTimeMinutes;
    }

    /**
     * Calculate how many slots can fit in the operating window
     * 
     * @param operatingStartTime when operations start
     * @param operatingEndTime when operations end
     * @return number of slots that fit
     */
    public int calculateSlotsPerDay(java.time.LocalTime operatingStartTime, java.time.LocalTime operatingEndTime) {
        int operatingMinutes = operatingEndTime.toSecondOfDay() / 60 - operatingStartTime.toSecondOfDay() / 60;
        return operatingMinutes / getTotalTimePerSlot();
    }

}
