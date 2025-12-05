package com.application.common.persistence.model.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ServiceVersion represents a specific version of a Service with versioned configuration.
 * Each Service can have multiple versions, each with its own opening hours, duration, and slot generation parameters.
 * Versions are valid within an effective_from/effective_to date range.
 */
@Entity
@Table(name = "service_version", indexes = {
    @Index(name = "idx_service_version_service_id", columnList = "service_id"),
    @Index(name = "idx_service_version_effective_from", columnList = "effective_from"),
    @Index(name = "idx_service_version_state", columnList = "state"),
    @Index(name = "idx_service_version_service_from_state", columnList = "service_id,effective_from,state")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    /**
     * Opening hours configuration stored as JSON.
     * Example: {"monday": "09:00-23:00", "tuesday": "09:00-23:00", ...}
     */
    @Column(name = "opening_hours", columnDefinition = "JSON")
    private JsonNode openingHours;

    /**
     * Duration of each reservation slot in minutes
     */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    /**
     * ⭐ HYBRID SCHEDULING: Slot configuration (replaces slotGenerationParams JSON)
     * One-to-one relationship with ServiceVersionSlotConfig
     * Defines how slots are generated (start_time, end_time, duration, buffer, max_concurrent)
     * 
     * DEPRECATED: Old slotGenerationParams JSON field removed - use slotConfig instead
     */
    @OneToMany(mappedBy = "serviceVersion", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @Builder.Default
    private Set<ServiceVersionSlotConfig> slotConfigs = new HashSet<>();

    /**
     * Additional notes or description about this version
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * State of this service version: ACTIVE or ARCHIVED
     */
    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VersionState state = VersionState.ACTIVE;

    /**
     * Start date for this version's validity (inclusive)
     */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * End date for this version's validity (inclusive). NULL means ongoing/no end date.
     */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /**
     * Audit: when this version was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit: when this version was last modified
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * One-to-many relationship with AvailabilityException entities
     */
    @OneToMany(mappedBy = "serviceVersion", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Builder.Default
    private Set<AvailabilityException> availabilityExceptions = new HashSet<>();

    /**
     * ⭐ HYBRID SCHEDULING: One-to-many relationship with ServiceVersionDay entities
     * Each ServiceVersion has 7 records (one per day of week: SUN-SAT)
     * Allows queryable, type-safe day-of-week scheduling without JSON parsing
     */
    @OneToMany(mappedBy = "serviceVersion", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Builder.Default
    private Set<ServiceVersionDay> serviceDays = new HashSet<>();

    // NOTE: Reservations no longer reference ServiceVersion directly.
    // Reservations now reference Service + snapshot fields (bookedServiceName, bookedSlotDuration, etc.)
    // See: Reservation.java - service field and booked* snapshot fields

    public enum VersionState {
        ACTIVE,
        ARCHIVED
    }

    /**
     * Checks if this version is currently valid for a given date.
     * 
     * @param date the date to check
     * @return true if this version is valid for the given date, false otherwise
     */
    public boolean isValidForDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        
        if (state != VersionState.ACTIVE) {
            return false;
        }
        
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveTo != null && date.isAfter(effectiveTo)) {
            return false;
        }
        
        return true;
    }

    /**
     * Checks if this version is currently overlapping with the given date range.
     * 
     * @param startDate start of the range (inclusive)
     * @param endDate end of the range (inclusive)
     * @return true if this version overlaps with the given range, false otherwise
     */
    public boolean overlapsWithRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        
        if (startDate.isAfter(endDate)) {
            return false;
        }
        
        // Version starts after range ends
        if (effectiveFrom.isAfter(endDate)) {
            return false;
        }
        
        // Version ends before range starts (only if effectiveTo is set)
        if (effectiveTo != null && effectiveTo.isBefore(startDate)) {
            return false;
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "ServiceVersion{" +
                "id=" + id +
                ", service=" + (service != null ? service.getId() : null) +
                ", duration=" + duration +
                ", state=" + state +
                ", effectiveFrom=" + effectiveFrom +
                ", effectiveTo=" + effectiveTo +
                '}';
    }
}
