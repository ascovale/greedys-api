package com.application.common.web.dto.reservations;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@Schema(name = "NewBaseReservationDTO", description = "Base DTO for creating a reservation (no slot, uses service + datetime)")
public abstract class NewBaseReservationDTO {

    @Schema(description = "Name of the reservation holder", example = "John Doe")
    @NotNull(message = "User name is required")
    private String userName;

    @Schema(description = "Service ID (Lunch, Dinner, etc.) - NEW", example = "2")
    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @Schema(description = "Reservation date and time - NEW", example = "2025-01-15T19:30:00")
    @NotNull(message = "Reservation date and time are required")
    private LocalDateTime reservationDateTime;

    @Schema(description = "Number of adults", example = "4")
    @NotNull(message = "Number of guests is required")
    @Positive(message = "Pax must be positive")
    private Integer pax;

    @Schema(description = "Number of kids", example = "0")
    @Default
    private Integer kids = 0;

    @Schema(description = "Special notes for the reservation", example = "Window seat preferred")
    private String notes;
}
