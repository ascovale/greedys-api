package com.application.web.dto;

import java.time.LocalTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ServicesDto", description = "DTO for services details")
public class ServicesDto {
	String name;
	LocalTime open;
	LocalTime close;
	LocalTime slotInterval;
	LocalTime timeReservation;
	Boolean dayAfter;
	Long serviceType;
	List<String> weekdays;
	Long restaurant;

	public ServicesDto() {
	}

	public Long getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Long restaurant) {
		this.restaurant = restaurant;
	}
	
	public long getServiceType() {
		return serviceType;
	}

	public void setServiceType(Long serviceType) {
		this.serviceType = serviceType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalTime getOpen() {
		return open;
	}

	public void setOpen(LocalTime open) {
		this.open = open;
	}

	public LocalTime getClose() {
		return close;
	}

	public void setClose(LocalTime close) {
		this.close = close;
	}

	public List<String> getWeekdays() {
		return weekdays;
	}

	public void setWeekdays(List<String> weekdays) {
		System.out.println("   <<<   ServicesDto: setWeekdays   >>>"+ weekdays);
		this.weekdays = weekdays;
	}

	public LocalTime getSlotInterval() {
		return slotInterval;
	}

	public void setSlotInterval(LocalTime slotInterval) {
		this.slotInterval = slotInterval;
	}

	public LocalTime getTimeReservation() {
		return timeReservation;
	}

	public void setTimeReservation(LocalTime timeReservation) {
		this.timeReservation = timeReservation;
	}
	public Boolean getDayAfter() {
		return dayAfter;
	}

	public void setDayAfter(Boolean dayAfter) {
		this.dayAfter = dayAfter;
	}
}