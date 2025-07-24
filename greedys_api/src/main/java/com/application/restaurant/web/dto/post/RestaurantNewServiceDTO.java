package com.application.restaurant.web.dto.post;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RestaurantNewServiceDTO", description = "DTO for creating a new restaurant service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantNewServiceDTO {

	private String name;
	private Long serviceType;
	private String info;
	private LocalDate validFrom;
	private LocalDate validTo;
}