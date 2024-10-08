package com.application.web.dto.get;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.controller.Validators.ValidEmail;
import com.application.persistence.model.reservation.ClientInfo;
import com.application.persistence.model.reservation.Reservation;

public class ReservationDTO {

	private SlotDTO slot;
	private Integer pax;
	private Integer kids=0;
	private String name;
	private String surname;
	private String phone;
	@ValidEmail
	private String email;
	private String notes;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate reservationDay; 
	private Long restaurant;

	public ReservationDTO(Reservation reservation) {
		
		this.slot = new SlotDTO(reservation.getSlot());

		this.pax = reservation.getPax();
		this.kids = reservation.getKids();

		ClientInfo clientInfo = reservation.get_user_info();
		this.name = clientInfo.name();
		this.surname = clientInfo.surname();
		this.email = clientInfo.email();
		this.notes = reservation.getNotes();
		this.reservationDay = reservation.getDate();
		this.restaurant = reservation.getSlot().getService().getRestaurant().getId();
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
	public void setIdSlot(SlotDTO slot) {
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
	
}

