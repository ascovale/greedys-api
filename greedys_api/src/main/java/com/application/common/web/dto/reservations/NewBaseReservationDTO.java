package com.application.common.web.dto.reservations;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NewBaseReservationDTO", description = "Base DTO for creating a reservation")
public abstract class NewBaseReservationDTO {

    @Schema(description = "Name of the reservation holder")
    private String userName;

    @Schema(description = "Slot ID for the reservation")
    private Long idSlot;

    @Schema(description = "Number of adults")
    private Integer pax;

    @Schema(description = "Number of kids")
    @Default
    private Integer kids = 0;

    @Schema(description = "Notes for the reservation")
    private String notes;

    @Schema(description = "Reservation date")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDay;
}
