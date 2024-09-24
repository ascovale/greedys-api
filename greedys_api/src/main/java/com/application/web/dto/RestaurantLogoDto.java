package com.application.web.dto;

public class RestaurantLogoDto {
	String path;
	public RestaurantLogoDto(String path) {
		this.path=path;
	}
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}