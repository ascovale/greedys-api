package com.application.restaurant.web.dto.schedule;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for TimeSlot - represents a computed available time slot for customer reservations
 * 
 * These are NOT stored in the database, but COMPUTED on-demand from:
 * - ServiceVersionSlotConfig (how to generate slots)
 * - ServiceVersionDay (which days are open)
 * - AvailabilityException (special closures/reduced hours)
 * - Reservation data (current bookings)
 * 
 * This DTO is used in API responses to show customers available times to book.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDto {

    /**
     * Unique identifier for this time slot
     * 
     * Format: "sv_{serviceVersionId}_slot_{number}_{date}"
     * Example: "sv_1_slot_001_2025-01-15"
     * 
     * Not a database ID, but a computed identifier for this specific slot instance
     */
    private String id;

    /**
     * Which service version this slot belongs to
     */
    private Long serviceVersionId;

    /**
     * When does this slot start?
     * 
     * Example: 2025-01-15T12:00:00
     * Includes both date and time
     */
    private LocalDateTime slotStart;

    /**
     * When does this slot end?
     * 
     * Example: 2025-01-15T13:00:00
     * Duration = slotEnd - slotStart
     */
    private LocalDateTime slotEnd;

    /**
     * Maximum capacity for this slot
     * 
     * Example: 4 for a 2-seat table
     * This is usually constant (from ServiceVersionSlotConfig)
     */
    private Integer totalCapacity;

    /**
     * How many seats are still available?
     * 
     * Example: 3 (1 already booked, 3 remaining)
     * This is REAL-TIME, computed when slot is fetched
     */
    private Integer availableCapacity;

    /**
     * How many reservations are currently in this slot?
     * 
     * Example: 1
     * bookingCount = totalCapacity - availableCapacity
     */
    private Integer bookingCount;

    /**
     * Is this slot available for new reservations?
     * 
     * true if:
     * - availableCapacity > 0 (not fully booked)
     * - Not within an exception window (closure, maintenance)
     * - Date is not fully closed
     * 
     * false otherwise
     */
    private boolean isAvailable;

    /**
     * Which slot config generated this slot?
     * 
     * Reference to ServiceVersionSlotConfig.id
     * Useful for tracking which rules created this slot
     */
    private Long generatedFromConfigId;

    /**
     * Get duration of this slot in minutes
     * 
     * @return duration = slotEnd - slotStart in minutes
     */
    public int getDurationMinutes() {
        if (slotStart == null || slotEnd == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.MINUTES.between(slotStart, slotEnd);
    }

    /**
     * Get occupancy percentage
     * 
     * @return occupancy from 0 to 100 percent
     */
    public int getOccupancyPercent() {
        if (totalCapacity == null || totalCapacity == 0) {
            return 0;
        }
        return (bookingCount * 100) / totalCapacity;
    }

    /**
     * Check if this slot is nearly full
     * 
     * @param thresholdPercent alert threshold (e.g., 80%)
     * @return true if occupancy >= threshold
     */
    public boolean isNearlyfull(int thresholdPercent) {
        return getOccupancyPercent() >= thresholdPercent;
    }

}
