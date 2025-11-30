package com.application.common.web.dto.reservations;

import java.time.LocalDateTime;

import com.application.common.controller.validators.ValidEmail;
import com.application.common.persistence.model.reservation.Reservation.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReservationDTO", description = "DTO for reservation details (no longer contains slot)")
public class ReservationDTO {

	@Schema(description = "Reservation ID", example = "101")
	private Long id;

	@Schema(description = "Reservation date and time (combined datetime)", example = "2025-01-15T19:30:00")
	private LocalDateTime reservationDateTime;

	@Schema(description = "Number of adults", example = "4")
	private Integer pax;

	@Builder.Default
	@Schema(description = "Number of children", example = "0")
	private Integer kids = 0;

	@Schema(description = "Name of reservation holder", example = "John Doe")
	private String name;

	@Schema(description = "Phone number", example = "+39...")
	private String phone;

	@ValidEmail
	@Schema(description = "Email address", example = "john@example.com")
	private String email;

	@Schema(description = "Special notes for reservation", example = "Window seat preferred")
	private String notes;

	@Schema(description = "ServiceVersion ID (versioned service config)", example = "5")
	private Long serviceVersionId;

	@Schema(description = "Restaurant ID", example = "1")
	private Long restaurant;

	@Schema(description = "ID of the customer who made the reservation", example = "103")
	private Long customerId;

	@Schema(description = "Table number if seated", example = "A5")
	private Integer tableNumber;

	@Schema(description = "Reservation status", example = "ACCEPTED")
	private Status status;

	@Schema(description = "User who created this reservation")
	private String createdBy;

	@Schema(description = "When the reservation was created")
	private LocalDateTime createdAt;

	@Schema(hidden = true)
	private LocalDateTime modifiedAt;

}
