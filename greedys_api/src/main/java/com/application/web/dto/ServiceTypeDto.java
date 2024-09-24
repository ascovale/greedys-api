package com.application.web.dto;

import com.application.persistence.model.reservation.ServiceType;

public class ServiceTypeDto {
	Long id;
	String name;

	public ServiceTypeDto(ServiceType serviceType) {
		this.id = serviceType.getId();
		this.name = serviceType.getName();
	}

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

	public ServiceTypeDto() {
	}

	public ServiceTypeDto(String name) {
		this.name = name;
	}

	public ServiceTypeDto(Long id, String name) {
		this.id = id;
		this.name = name;
	}

}
