package com.application.web.dto.post;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NewServiceDTO", description = "DTO for creating a new service")
public class NewServiceDTO {

	private String name;
	private Long restaurant;
	private Long serviceType;
	private String info;
	private LocalDate validFrom;
	private LocalDate validTo;

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public void setValidTo(LocalDate validTo) {
		this.validTo = validTo;
	}

	public LocalDate getValidTo() {
		return validTo;

	}

    public Long getServiceType() {
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

	public Long getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Long restaurant) {
		this.restaurant = restaurant;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}