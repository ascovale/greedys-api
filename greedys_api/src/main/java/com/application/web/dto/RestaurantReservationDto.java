package com.application.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.controller.validators.PasswordMatches;

import io.swagger.v3.oas.annotations.media.Schema;

@PasswordMatches
@Schema(name = "RestaurantReservationDto", description = "DTO for restaurant reservation details")
public class RestaurantReservationDto {
	@NotNull
	private Integer pax;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;
	private Long company;// CompanyUser companyUser;
	private Long restaurant;// Restaurant restaurant;
	private Long menu;// Menu menu;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime time;// Time

	public Integer getPax() {
		return pax;
	}

	public void setPax(Integer pax) {
		this.pax = pax;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public Long getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Long restaurant) {
		this.restaurant = restaurant;
	}

	public Long getMenu() {
		return menu;
	}

	public void setMenu(Long menu) {
		this.menu = menu;
	}

	public Long getCompany() {
		return company;
	}

	public void setCompany(Long company) {
		this.company = company;
	}
	
	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}


}
