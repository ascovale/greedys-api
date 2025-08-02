package com.application.common.web.dto.reservations;

import java.time.LocalDate;

import com.application.common.controller.validators.ValidEmail;
import com.application.common.persistence.model.reservation.Reservation;
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
	private Status status;

	public ReservationDTO(Reservation reservation) {

		this.slot = new SlotDTO(reservation.getSlot());
		this.id = reservation.getId();
		this.pax = reservation.getPax();
		this.kids = reservation.getKids();
		this.name = reservation.getUserName();
		this.notes = reservation.getNotes();
		this.reservationDay = reservation.getDate();
		this.restaurant = reservation.getSlot().getService().getRestaurant().getId();
		this.status = reservation.getStatus();
		
	}

}
