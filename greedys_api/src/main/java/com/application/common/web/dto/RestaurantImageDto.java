package com.application.common.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image_restaurant")
@Schema(name = "RestaurantImageDto", description = "DTO for restaurant image details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantImageDto {
	
	@Id
	@GeneratedValue
	private Long id;
	private String name;

	public RestaurantImageDto(String name) {
		this.name = name;
	}
}
