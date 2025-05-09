package com.application.web.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "image_restaurant")
@Schema(name = "RestaurantImageDto", description = "DTO for restaurant image details")
public class RestaurantImageDto {
	
	@Id
	@GeneratedValue
	private Long id;
	private String name;

	public RestaurantImageDto(){};

	public RestaurantImageDto(String name) {
		this.name = name;
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

	@Override
	public String toString() {
		return "Image [id=" + id + ", name=" + name + "]";
	}

}
