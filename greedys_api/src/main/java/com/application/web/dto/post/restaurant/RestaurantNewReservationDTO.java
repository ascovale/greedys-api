package com.application.web.dto.post.restaurant;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RestaurantNewReservationDTO", description = "DTO for creating a new restaurant reservation")
public class RestaurantNewReservationDTO {
	
	private Long idSlot;
	private Integer pax;
	private Integer kids=0;
	private String notes;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
	private LocalDate reservationDay; 
    private Long restaurantId;
	private Long userId;


	public Long getUserId() {
		if (userId == null) {
			throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
		}
		return userId;
	}

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
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

