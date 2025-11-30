package com.application.common.persistence.model.reservation;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ServiceVersionSlotConfig - Slot generation configuration for a ServiceVersion
 * 
 * REPLACES: slotGenerationParams JSON field with proper, queryable entity
 * 
 * SLOT GENERATION ALGORITHM:
 * 1. Get day schedule from ServiceVersionDay (opening_time, closing_time, break times)
 * 2. Apply this SlotConfig:
 *    - startTime: when to START generating slots (e.g., 09:00)
 *    - endTime: when to STOP generating slots (e.g., 23:00) 
 *    - slotDurationMinutes: length of each slot (e.g., 30 min)
 *    - bufferMinutes: gap between slots (e.g., 5 min after each booking)
 *    - maxConcurrentReservations: max bookings in the same time slot (e.g., 10)
 * 3. Exclude breaks and closed times
 * 4. Don't generate slots in the past
 * 5. Cap slots by max_reservations from ServiceVersionDay if specified
 * 
 * EXAMPLE SCENARIO (Italian Restaurant):
 * ServiceVersion: Lunch Mode (11:00-15:00)
 * ServiceVersionDay[MONDAY]:
 *   - openingTime: 11:00, closingTime: 15:00
 *   - breakStart: NULL, breakEnd: NULL
 *   - maxReservations: 50 covers max per lunch
 * 
 * ServiceVersionSlotConfig:
 *   - startTime: 11:00
 *   - endTime: 15:00
 *   - slotDurationMinutes: 30 (30-min slots)
 *   - bufferMinutes: 5 (5-min gap between confirmations)
 *   - maxConcurrentReservations: 10 (max 10 covers per slot)
 * 
 * Generated slots: 11:00, 11:30, 12:00, 12:30, 13:00, 13:30, 14:00, 14:30
 * (stops at 14:30 to allow booking to complete by 15:00 + duration)
 * 
 * COMPARISON WITH OLD JSON FORMAT:
 * 
 * OLD (JSON, not queryable):
 *   {
 *     "start_time": "09:00",
 *     "end_time": "23:00",
 *     "interval_minutes": 30,
 *     "buffer_minutes": 0,
 *     "max_concurrent_reservations": 10
 *   }
 * 
 * NEW (Entity, queryable and type-safe):
 *   startTime: 09:00
 *   endTime: 23:00
 *   slotDurationMinutes: 30
 *   bufferMinutes: 0
 *   maxConcurrentReservations: 10
 */
@Entity
@Table(name = "service_version_slot_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersionSlotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One-to-one relationship to ServiceVersion
     * Each ServiceVersion has exactly one SlotConfig
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_version_id", nullable = false, unique = true)
    private ServiceVersion serviceVersion;

    /**
     * When to START generating slots
     * Example: 09:00 for opening at 09:00
     * 
     * NOTE: Must be <= the earliest openingTime from any ServiceVersionDay for this version
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * When to STOP generating slots
     * Example: 23:00 for closing at 23:00
     * 
     * NOTE: Must be >= the latest closingTime from any ServiceVersionDay for this version
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Duration of each generated slot in minutes
     * Examples:
     * - 15 minutes for hair salon (quick turnaround)
     * - 30 minutes for restaurant (quick service)
     * - 60 minutes for doctor (consultation)
     * - 120 minutes for massage/spa (extended service)
     * 
     * This should match or be multiple of Reservation.duration or be applied per-service-type
     */
    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;

    /**
     * Buffer/gap in minutes between slots
     * Examples:
     * - 0 minutes: back-to-back slots (e.g., restaurant)
     * - 5 minutes: small turnover time (e.g., salon)
     * - 15 minutes: cleaning/prep time (e.g., medical)
     * 
     * This is applied AFTER each slot to account for payment, setup, etc.
     */
    @Column(name = "buffer_minutes", nullable = false)
    @Builder.Default
    private Integer bufferMinutes = 0;

    /**
     * Maximum concurrent reservations in the same time slot
     * Examples:
     * - 1 for 1-on-1 service (therapist, doctor, barber)
     * - 4 for restaurant table (4-seat table)
     * - 10 for restaurant general (10 covers per time slot)
     * - 100 for cinema (100 seats available)
     * 
     * This is INDEPENDENT from ServiceVersionDay.maxReservations which caps daily total
     * System uses the MINIMUM of both constraints
     */
    @Column(name = "max_concurrent_reservations", nullable = false)
    @Builder.Default
    private Integer maxConcurrentReservations = 10;

    /**
     * Helper: Check if this slot config is valid
     * Used for validation when saving
     */
    public boolean isValid() {
        if (startTime == null || endTime == null) {
            return false;
        }
        
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            return false;
        }
        
        if (slotDurationMinutes == null || slotDurationMinutes <= 0) {
            return false;
        }
        
        if (bufferMinutes == null || bufferMinutes < 0) {
            return false;
        }
        
        if (maxConcurrentReservations == null || maxConcurrentReservations <= 0) {
            return false;
        }
        
        return true;
    }

    /**
     * Helper: Get total minutes per slot (duration + buffer)
     * Used in slot generation calculations
     */
    public Integer getSlotIntervalMinutes() {
        return slotDurationMinutes + bufferMinutes;
    }

    /**
     * Helper: Check if a given time is within the slot generation window
     */
    public boolean isWithinSlotGenerationWindow(LocalTime time) {
        if (time == null) {
            return false;
        }
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    @Override
    public String toString() {
        return "ServiceVersionSlotConfig{" +
                "id=" + id +
                ", serviceVersion=" + (serviceVersion != null ? serviceVersion.getId() : null) +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", slotDurationMinutes=" + slotDurationMinutes +
                ", bufferMinutes=" + bufferMinutes +
                ", maxConcurrentReservations=" + maxConcurrentReservations +
                '}';
    }
}
