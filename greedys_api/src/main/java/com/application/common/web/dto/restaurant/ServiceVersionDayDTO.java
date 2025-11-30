package com.application.common.web.dto.restaurant;

import java.time.DayOfWeek;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ServiceVersionDay - Weekly recurring schedule
 * 
 * One record per day of week (7 total per ServiceVersion)
 * Contains opening/closing times, breaks, and max reservations
 */
@Schema(name = "ServiceVersionDayDTO", description = "Weekly recurring schedule for a day of week")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersionDayDTO {

    @Schema(description = "Record ID")
    private Long id;

    @Schema(description = "Day of week (SUNDAY to SATURDAY)")
    private DayOfWeek dayOfWeek;

    @Schema(description = "Opening time for this day (e.g., 09:00). Null means closed")
    private LocalTime openingTime;

    @Schema(description = "Closing time for this day (e.g., 23:00)")
    private LocalTime closingTime;

    @Schema(description = "If true, restaurant is fully closed this day")
    private Boolean isClosed;

    @Schema(description = "Maximum concurrent reservations for this day (e.g., 50 covers)")
    private Integer maxReservations;

    @Schema(description = "Duration of each slot in minutes (e.g., 30)")
    private Integer slotDuration;

    @Schema(description = "Break start time (e.g., 14:00 for siesta). Null if no break")
    private LocalTime breakStart;

    @Schema(description = "Break end time (e.g., 15:30)")
    private LocalTime breakEnd;

}
