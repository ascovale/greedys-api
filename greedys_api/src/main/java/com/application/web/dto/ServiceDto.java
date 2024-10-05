package com.application.web.dto;

import java.time.LocalTime;
import java.util.stream.Collectors;
import java.util.Set;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.model.reservation.Service;

public class ServiceDto {
	LocalTime open;
	LocalTime close;
	String name;
	Weekday weekday;
	Set<ServiceTypeDto> serviceTypes;
	
	public ServiceDto(Service service) {
		this.name = service.getName();
		this.serviceTypes = service.getServiceType().stream()
			.map(ServiceTypeDto::new)
			.collect(Collectors.toSet());

	}

    public Set<ServiceTypeDto> getServiceType() {
		return serviceTypes;
	}

	public void setServiceType(Set<ServiceTypeDto> serviceTypes) {
		this.serviceTypes = serviceTypes;
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

	public Weekday getWeekday() {
		return weekday;
	}

	public void setWeekday(Weekday weekday) {
		this.weekday = weekday;
	}

}