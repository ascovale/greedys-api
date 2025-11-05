package com.application.common.web.dto.reservations;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.application.common.controller.validators.ValidEmail;
import com.application.common.persistence.model.reservation.Reservation.Status;
import com.application.common.web.dto.restaurant.SlotDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReservationDTO", description = "DTO for reservation details")
public class ReservationDTO {

	private Long id;
	private SlotDTO slot;
	private Integer pax;
	@Builder.Default
	private Integer kids = 0;
	private String name;
	private String phone;
	@ValidEmail
	private String email;
	private String notes;
	private LocalDate reservationDay;
	private Long restaurant;
	@Schema(description = "ID of the customer who made the reservation", example = "103")
	private Long customerId;
	private Status status;
	private String createdBy;
	private String createdByUserType;
	private LocalDateTime createdAt;

}
