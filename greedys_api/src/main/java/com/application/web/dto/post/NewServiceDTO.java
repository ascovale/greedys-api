package com.application.web.dto.post;

import java.util.Date;

public class NewServiceDTO {

	private String name;
	private Long restaurant;
	private Long serviceType;
	private String info;
	private Date validFrom;
	private Date validTo;

	public Date getValidFrom() {
		return validFrom;
	}

	public void setDateValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getValidTo() {
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