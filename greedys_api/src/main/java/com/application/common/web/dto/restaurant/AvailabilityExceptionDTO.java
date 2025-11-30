package com.application.common.web.dto.restaurant;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AvailabilityException entity
 */
@Schema(name = "AvailabilityExceptionDTO", description = "DTO for availability exception details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityExceptionDTO {

    @Schema(description = "Availability exception ID")
    private Long id;

    @Schema(description = "Associated service version ID")
    private Long serviceVersionId;

    @Schema(description = "Date of the exception")
    private LocalDate exceptionDate;

    @Schema(description = "Type of exception: CLOSURE, MAINTENANCE, SPECIAL_EVENT, STAFF_SHORTAGE, CUSTOM")
    private String exceptionType;

    @Schema(description = "Additional notes or reason for the exception")
    private String notes;

    @Schema(description = "When this exception was created")
    private LocalDateTime createdAt;

    @Schema(description = "When this exception was last updated")
    private LocalDateTime updatedAt;

}
