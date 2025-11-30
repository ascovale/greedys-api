package com.application.common.persistence.model.reservation;

import java.time.LocalDate;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ⭐ HYBRID SCHEDULING: AvailabilityException (Date-Specific Overrides)
 * 
 * Represents exceptions/overrides to the normal ServiceVersionDay schedule.
 * Can be either:
 * 1. Full-day closure (is_fully_closed=TRUE)
 * 2. Partial-day closure (start_time + end_time)
 * 3. Opening hours override (override_opening_time + override_closing_time)
 * 
 * EXAMPLES:
 * 
 * ✅ Full-day closure (Ferragosto 2025-08-15):
 * - exception_date = 2025-08-15
 * - is_fully_closed = TRUE
 * - exception_type = CLOSURE
 * → Entire day: NO RESERVATIONS
 * 
 * ✅ Partial-day closure (Maintenance window):
 * - exception_date = 2025-07-01
 * - start_time = 14:00
 * - end_time = 15:00
 * - exception_type = MAINTENANCE
 * - is_fully_closed = FALSE
 * → Available 09:00-14:00 and 15:00-23:00
 * → NOT available 14:00-15:00 (maintenance)
 * 
 * ✅ Special opening hours (New Year's Eve):
 * - exception_date = 2025-12-31
 * - override_opening_time = 19:00 (normally 12:00)
 * - override_closing_time = 02:00 (normally 23:00)
 * - exception_type = SPECIAL_EVENT
 * → Available 19:00-02:00 (extended hours)
 * 
 * ✅ Staff shortage (limited capacity):
 * - exception_date = 2025-07-20
 * - exception_type = STAFF_SHORTAGE
 * - notes = "Only 5 reservations available"
 * → Uses ServiceVersionDay time, but with limited capacity
 * 
 * QUERY PATTERNS:
 * 
 * 1. Check if fully closed on a date:
 *    SELECT * FROM availability_exception 
 *    WHERE service_version_id = 1 AND exception_date = '2025-08-15' AND is_fully_closed = TRUE
 * 
 * 2. Get all partial closures for a week:
 *    SELECT * FROM availability_exception 
 *    WHERE service_version_id = 1 AND exception_date BETWEEN '2025-07-01' AND '2025-07-07'
 *    AND start_time IS NOT NULL
 * 
 * 3. Find all maintenance windows for a month:
 *    SELECT * FROM availability_exception 
 *    WHERE service_version_id = 1 AND exception_type = 'MAINTENANCE'
 *    AND exception_date BETWEEN '2025-07-01' AND '2025-07-31'
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Hybrid Service Scheduling with Partial Closures)
 */
@Entity
@Table(name = "availability_exception", indexes = {
    @Index(name = "idx_availability_exception_service_version_id", columnList = "service_version_id"),
    @Index(name = "idx_availability_exception_date", columnList = "exception_date"),
    @Index(name = "idx_availability_exception_service_version_date", columnList = "service_version_id,exception_date"),
    @Index(name = "idx_availability_exception_date_type", columnList = "exception_date,exception_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_version_id", nullable = false)
    private ServiceVersion serviceVersion;

    /**
     * Date of the exception (what date is this exception for?)
     */
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    /**
     * ⭐ START TIME for partial-day closure (e.g., 14:00)
     * NULL = entire day exception
     * Combined with end_time for time range
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * ⭐ END TIME for partial-day closure (e.g., 15:00)
     * Combined with start_time to define closure window
     */
    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * ⭐ OVERRIDE: Custom opening time for this date (if different from ServiceVersionDay)
     * NULL = use ServiceVersionDay.openingTime
     */
    @Column(name = "override_opening_time")
    private LocalTime overrideOpeningTime;

    /**
     * ⭐ OVERRIDE: Custom closing time for this date (if different from ServiceVersionDay)
     * NULL = use ServiceVersionDay.closingTime
     */
    @Column(name = "override_closing_time")
    private LocalTime overrideClosingTime;

    /**
     * ⭐ FLAG: Is the entire day fully closed?
     * TRUE = no reservations accepted entire day
     * FALSE = only the time window (start_time to end_time) is unavailable
     */
    @Column(name = "is_fully_closed", nullable = false)
    @Builder.Default
    private Boolean isFullyClosed = false;

    /**
     * Type of exception: CLOSURE, MAINTENANCE, FULLY_BOOKED, SPECIAL_EVENT, STAFF_SHORTAGE, CUSTOM
     */
    @Column(name = "exception_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExceptionType exceptionType;

    /**
     * Additional notes or reason for the exception
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Audit: when this exception was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit: when this exception was last modified
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ExceptionType {
        /**
         * Complete closure - no reservations accepted
         */
        CLOSURE,
        /**
         * Maintenance - no reservations accepted
         */
        MAINTENANCE,
        /**
         * Special event - possibly limited availability
         */
        SPECIAL_EVENT,
        /**
         * Staff shortage - limited availability
         */
        STAFF_SHORTAGE,
        /**
         * Fully booked - no more capacity
         */
        FULLY_BOOKED,
        /**
         * Custom exception with notes
         */
        CUSTOM
    }

    /**
     * ⭐ CHECK: Is this exception a full-day closure?
     */
    public boolean isFullDayException() {
        return isFullyClosed;
    }

    /**
     * ⭐ CHECK: Is this exception a partial-day closure (specific time window)?
     */
    public boolean isPartialDayException() {
        return !isFullyClosed && startTime != null && endTime != null;
    }

    /**
     * ⭐ CHECK: Is this exception an opening hours override?
     */
    public boolean isHoursOverride() {
        return (overrideOpeningTime != null || overrideClosingTime != null) && !isFullyClosed;
    }

    /**
     * ⭐ CHECK: Is the given time within the exception window?
     * 
     * Returns FALSE if:
     * - Exception is full-day (entire day is unavailable)
     * - Exception is partial-day AND time is within [startTime, endTime)
     * - Exception is hours override AND time is not within override hours
     */
    public boolean affectsTime(java.time.LocalTime time) {
        if (time == null) {
            return false;
        }

        // Full-day closure affects all times
        if (isFullDayException()) {
            return true;
        }

        // Partial-day closure: check if time is within window
        if (isPartialDayException()) {
            return !time.isBefore(startTime) && time.isBefore(endTime);
        }

        // Hours override: check if time is within override hours
        if (isHoursOverride()) {
            java.time.LocalTime effectiveOpen = overrideOpeningTime != null ? overrideOpeningTime : null;
            java.time.LocalTime effectiveClose = overrideClosingTime != null ? overrideClosingTime : null;
            
            if (effectiveOpen != null && effectiveClose != null) {
                return time.isBefore(effectiveOpen) || time.isAfter(effectiveClose);
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "AvailabilityException{" +
                "id=" + id +
                ", serviceVersionId=" + (serviceVersion != null ? serviceVersion.getId() : null) +
                ", exceptionDate=" + exceptionDate +
                ", exceptionType=" + exceptionType +
                ", isFullyClosed=" + isFullyClosed +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}

