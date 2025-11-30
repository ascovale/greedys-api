package com.application.common.web.dto.restaurant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ServiceVersion entity
 * 
 * ARCHITECTURE UPDATE:
 * - Replaced openingHours JSON with serviceDays (7 records per day of week)
 * - Replaced slotGenerationParams JSON with slotConfigs (queryable, type-safe)
 */
@Schema(name = "ServiceVersionDTO", description = "DTO for service version details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersionDTO {

    @Schema(description = "Service version ID")
    private Long id;

    @Schema(description = "Associated service ID")
    private Long serviceId;

    @Schema(description = "Service name (for convenience)")
    private String serviceName;

    @Schema(description = "Duration of each reservation slot in minutes")
    private Integer duration;

    @Schema(description = "Additional notes about this version")
    private String notes;

    @Schema(description = "State of this version: ACTIVE or ARCHIVED")
    private String state;

    @Schema(description = "Start date of version validity")
    private LocalDate effectiveFrom;

    @Schema(description = "End date of version validity (null means ongoing)")
    private LocalDate effectiveTo;

    @Schema(description = "When this version was created")
    private LocalDateTime createdAt;

    @Schema(description = "When this version was last updated")
    private LocalDateTime updatedAt;

    /**
     * HYBRID SCHEDULING: Weekly recurring schedules (7 records, one per day of week)
     * REPLACES: openingHours JSON field
     * Each record contains: dayOfWeek, openingTime, closingTime, isClosed, breaks, maxReservations
     */
    @Schema(description = "Weekly recurring schedules (7 records: SUN-SAT)")
    @Builder.Default
    private Set<ServiceVersionDayDTO> serviceDays = new HashSet<>();

    /**
     * HYBRID SCHEDULING: Slot generation configuration
     * REPLACES: slotGenerationParams JSON field
     * Contains: startTime, endTime, slotDurationMinutes, bufferMinutes, maxConcurrentReservations
     */
    @Schema(description = "Slot generation configuration (how to generate available time slots)")
    @Builder.Default
    private Set<ServiceVersionSlotConfigDTO> slotConfigs = new HashSet<>();

}
