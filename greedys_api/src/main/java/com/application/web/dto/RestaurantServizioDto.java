package com.application.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RestaurantServizioDto", description = "DTO for restaurant service details")
public class RestaurantServizioDto {
	private Long id;
	private String name;
	private LocalDate startDate;
	private LocalDate endDate;
	private String description;
	private List<Reservation> reservations;
	private Restaurant restaurant;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public LocalDate getStartDate() {
		return startDate;
	}
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}
	public LocalDate getEndDate() {
		return endDate;
	}
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Reservation> getReservations() {
		return reservations;
	}
	public void setReservations(List<Reservation> reservations) {
		this.reservations = reservations;
	}
	public Restaurant getRestaurant() {
		return restaurant;
	}
	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}


}
