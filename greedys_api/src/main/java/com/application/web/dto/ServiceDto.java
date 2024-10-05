package com.application.web.dto;

import java.time.LocalTime;
import java.util.stream.Collectors;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.ServiceType;

public class ServiceDto {
	LocalTime open;
	LocalTime close;
	String name;
	Weekday weekday;
	String serviceType;
	
	public ServiceDto(Service service) {
		this.name = service.getName();
		this.serviceType = service.getServiceType().stream()
			.map(ServiceType::getName)
			.collect(Collectors.joining(", "));  // Aggiungi questa linea

	}

    public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
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

	public Weekday getWeekday() {
		return weekday;
	}

	public void setWeekday(Weekday weekday) {
		this.weekday = weekday;
	}

}