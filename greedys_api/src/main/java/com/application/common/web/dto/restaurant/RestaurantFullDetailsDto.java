package com.application.common.web.dto.restaurant;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RestaurantFullDetailsDto", description = "DTO for full restaurant details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantFullDetailsDto {
	private Long id;
	private String name;
	private String address;
	private String postCode;
	private String vatNumber;
	private String restaurantSelectedImage;
	private List<RestaurantImageDto> restaurantOtherImages;
	private String description;
	private double latitude;
	private double longitude;
	private RestaurantLogoDto restaurantLogo;

	@NotNull
	@Size(min = 1, message = "{Size.RUserDto.email}")
	private String email;

	private Integer role;
}
