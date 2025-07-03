package com.application.web.dto.get;

import java.time.LocalDate;

import com.application.controller.validators.ValidEmail;
import com.application.persistence.model.reservation.Reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReservationDTO", description = "DTO for reservation details")
public class ReservationDTO {

	private Long id;
	private SlotDTO slot;
	private Integer pax;
	private Integer kids = 0;
	private String name;
	private String surname;
	private String phone;
	@ValidEmail
	private String email;
	private String notes;
	private LocalDate reservationDay;
	private Long restaurant;

	private Boolean isAccepted;
	private Boolean isRejected;

	public ReservationDTO(Reservation reservation) {

		this.slot = new SlotDTO(reservation.getSlot());
		this.id = reservation.getId();
		this.pax = reservation.getPax();
		this.kids = reservation.getKids();
		if (reservation.getCustomer() != null) {
			this.name = reservation.getCustomer().getName();
			this.surname = reservation.getCustomer().getSurname();
			this.email = reservation.getCustomer().getEmail();
			this.phone = reservation.getCustomer().getPhoneNumber();
		}
		this.notes = reservation.getNotes();
		this.reservationDay = reservation.getDate();
		this.restaurant = reservation.getSlot().getService().getRestaurant().getId();
		this.isAccepted = reservation.getAccepted();
		this.isRejected = reservation.getRejected();
	}

	public Long getId() {
		return id;
	}

	public Long getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Long restaurant) {
		this.restaurant = restaurant;
	}

	public ReservationDTO() {
	}

	public SlotDTO getSlot() {
		return slot;
	}

	public void setSlot(SlotDTO slot) {
		this.slot = slot;
	}

	public Integer getPax() {
		return pax;
	}

	public void setPax(Integer pax) {
		this.pax = pax;
	}

	public Integer getKids() {
		return kids;
	}

	public void setKids(Integer kids) {
		this.kids = kids;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalDate getReservationDay() {
		return reservationDay;
	}

	public void setReservationDay(LocalDate reservationDay) {
		this.reservationDay = reservationDay;
	}

	public Boolean isAccepted() {
		return isAccepted;
	}

	public Boolean isRejected() {
		return isRejected;
	}

}
