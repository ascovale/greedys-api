package com.application.web.dto.post.restaurant;


import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;


public class RestaurantNewReservationDTO {
	
	private Long idSlot;
	private Integer pax;
	private Integer kids=0;
	private String notes;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
	private LocalDate reservationDay; 
    private Long restaurant_id;
	private Long user_id;


	public Long getUser_id() {
		if (user_id == null) {
			throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
		}
		return user_id;
	}

    public Long getRestaurant_id() {
        return restaurant_id;
    }

    public void setRestaurant_id(Long restaurant_id) {
        this.restaurant_id = restaurant_id;
    }

	public Long getIdSlot() {
		return idSlot;
	}

	public void setIdSlot(Long idSlot) {
		this.idSlot = idSlot;
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

