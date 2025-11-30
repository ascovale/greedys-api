package com.application.common.persistence.model.reservation;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ⭐ HYBRID SCHEDULING: ServiceVersionDay (Weekly Recurring Schedule)
 * 
 * Represents the schedule for a specific day of the week within a ServiceVersion.
 * Each ServiceVersion has 7 records (one per day: SUN-SAT).
 * 
 * ARCHITECTURE:
 * - ServiceVersion = valid date range (e.g., summer season 2025-06-01 to 2025-08-31)
 * - ServiceVersionDay = recurring weekly pattern within that version
 * - AvailabilityException = date-specific overrides (e.g., closed 2025-07-15)
 * 
 * EXAMPLES:
 * 
 * Summer 2025 (Jun-Aug):
 * ├─ ServiceVersion id=1, effectiveFrom=2025-06-01, effectiveTo=2025-08-31
 * │  ├─ ServiceVersionDay MONDAY: 09:00-23:00, max_reservations=10
 * │  ├─ ServiceVersionDay TUESDAY: 09:00-23:00, max_reservations=10
 * │  ├─ ServiceVersionDay WEDNESDAY: 09:00-23:00, max_reservations=10
 * │  ├─ ServiceVersionDay THURSDAY: 09:00-23:00, max_reservations=10
 * │  ├─ ServiceVersionDay FRIDAY: 09:00-23:00, max_reservations=12
 * │  ├─ ServiceVersionDay SATURDAY: 11:00-23:00, max_reservations=15
 * │  └─ ServiceVersionDay SUNDAY: CLOSED
 * │
 * └─ AvailabilityException date=2025-07-15 (Ferragosto): IS_FULLY_CLOSED=TRUE
 * 
 * Winter 2025 (Sep-May):
 * ├─ ServiceVersion id=2, effectiveFrom=2025-09-01, effectiveTo=2026-05-31
 * │  ├─ ServiceVersionDay MONDAY: 12:00-14:30, 19:00-22:00, break 14:30-19:00
 * │  ├─ ServiceVersionDay TUESDAY: CLOSED
 * │  ├─ ServiceVersionDay WEDNESDAY: 12:00-14:30, 19:00-22:00, break 14:30-19:00
 * │  ├─ ServiceVersionDay THURSDAY: 12:00-14:30, 19:00-22:00, break 14:30-19:00
 * │  ├─ ServiceVersionDay FRIDAY: 12:00-14:30, 19:00-23:00, break 14:30-19:00
 * │  ├─ ServiceVersionDay SATURDAY: 19:00-23:30, max_reservations=14
 * │  └─ ServiceVersionDay SUNDAY: 12:00-16:00, max_reservations=10
 * │
 * └─ AvailabilityException date=2025-12-25 (Christmas): IS_FULLY_CLOSED=TRUE
 * └─ AvailabilityException date=2025-12-01 start_time=14:00 end_time=15:00: MAINTENANCE (partial)
 * 
 * ✅ QUERYABLE:
 * SELECT * FROM service_version_day 
 * WHERE service_version_id = 1 AND day_of_week = 1 (MONDAY)
 * 
 * ✅ FLEXIBLE: Can model lunch/dinner as break_start/break_end
 * ✅ EFFICIENT: No JSON parsing, direct DB queries
 * ✅ SCALABLE: Easy to add seasonal variations
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Hybrid Service Scheduling)
 */
@Entity
@Table(name = "service_version_day", indexes = {
    @Index(name = "idx_service_version_day_service_version_id", columnList = "service_version_id"),
    @Index(name = "idx_service_version_day_dayofweek", columnList = "day_of_week")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_service_version_day_version_dayofweek", columnNames = {"service_version_id", "day_of_week"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersionDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_version_id", nullable = false)
    private ServiceVersion serviceVersion;

    /**
     * ⭐ Day of week (0=SUN, 1=MON, 2=TUE, 3=WED, 4=THU, 5=FRI, 6=SAT)
     * Stored as int for compatibility, but mapped to Java DayOfWeek
     */
    @Column(name = "day_of_week", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private DayOfWeek dayOfWeek;

    /**
     * Opening time for this day (e.g., 09:00)
     * NULL means the restaurant is closed (or use isClosed=true)
     */
    @Column(name = "opening_time")
    private LocalTime openingTime;

    /**
     * Closing time for this day (e.g., 23:00)
     * Only meaningful if openingTime is not NULL
     */
    @Column(name = "closing_time")
    private LocalTime closingTime;

    /**
     * Flag: if TRUE, restaurant is completely closed this day
     * (This takes precedence over openingTime/closingTime)
     */
    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    /**
     * Maximum number of concurrent reservations for this day
     * NULL means unlimited
     */
    @Column(name = "max_reservations")
    private Integer maxReservations;

    /**
     * Duration of each reservation slot in minutes (e.g., 30)
     * Used for slot generation
     */
    @Column(name = "slot_duration", nullable = false)
    @Builder.Default
    private Integer slotDuration = 30;

    /**
     * ⭐ BREAK TIME: Start time for break (e.g., 14:00 for siesta)
     * 
     * Use case: Restaurant has lunch 12:00-14:30 and dinner 19:00-23:00
     * ├─ opening_time = 12:00
     * ├─ closing_time = 23:00
     * ├─ break_start = 14:30
     * └─ break_end = 19:00
     * 
     * Meaning: Available 12:00-14:30 and 19:00-23:00
     * No reservations between 14:30-19:00
     */
    @Column(name = "break_start")
    private LocalTime breakStart;

    /**
     * Break end time (e.g., 15:30)
     * Reservations resume after this time
     */
    @Column(name = "break_end")
    private LocalTime breakEnd;

    /**
     * Audit: when this record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit: when this record was last modified
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * ⭐ CHECK: Is this day open?
     */
    public boolean isOpen() {
        return !isClosed && openingTime != null && closingTime != null;
    }

    /**
     * ⭐ CHECK: Does this day have a break?
     */
    public boolean hasBreak() {
        return breakStart != null && breakEnd != null;
    }

    /**
     * ⭐ CHECK: Is the given time within break hours?
     */
    public boolean isInBreak(LocalTime time) {
        if (!hasBreak()) {
            return false;
        }
        return time.isAfter(breakStart) && time.isBefore(breakEnd);
    }

    /**
     * ⭐ CHECK: Is the given time within opening hours (excluding breaks)?
     */
    public boolean isWithinOpeningHours(LocalTime time) {
        if (!isOpen() || time == null) {
            return false;
        }
        
        // Before opening
        if (time.isBefore(openingTime)) {
            return false;
        }
        
        // After closing
        if (time.isAfter(closingTime)) {
            return false;
        }
        
        // Within break
        if (isInBreak(time)) {
            return false;
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "ServiceVersionDay{" +
                "id=" + id +
                ", serviceVersionId=" + (serviceVersion != null ? serviceVersion.getId() : null) +
                ", dayOfWeek=" + dayOfWeek +
                ", openingTime=" + openingTime +
                ", closingTime=" + closingTime +
                ", isClosed=" + isClosed +
                ", breakStart=" + breakStart +
                ", breakEnd=" + breakEnd +
                '}';
    }
}
