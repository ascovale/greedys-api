package com.application.common.web.dto.restaurant;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ServiceVersionSlotConfig - Slot generation parameters
 * 
 * Defines how available time slots are generated (start, end, duration, buffer, max concurrent)
 * REPLACES: slotGenerationParams JSON field
 */
@Schema(name = "ServiceVersionSlotConfigDTO", description = "Slot generation configuration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersionSlotConfigDTO {

    @Schema(description = "Record ID")
    private Long id;

    @Schema(description = "When to START generating slots (e.g., 09:00)")
    private LocalTime startTime;

    @Schema(description = "When to STOP generating slots (e.g., 23:00)")
    private LocalTime endTime;

    @Schema(description = "Length of each slot in minutes (e.g., 30)")
    private Integer slotDurationMinutes;

    @Schema(description = "Gap between slots in minutes (e.g., 5 for turnover)")
    private Integer bufferMinutes;

    @Schema(description = "Max concurrent reservations per slot (e.g., 10 covers)")
    private Integer maxConcurrentReservations;

}
