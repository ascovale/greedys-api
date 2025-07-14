package com.application.web.dto.post;

import com.application.controller.validators.ValidEmail;
import com.application.web.dto.RestaurantImageDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(name = "NewRestaurantDTO", description = "DTO for creating a new restaurant")
public class NewRestaurantDTO {
	//TODO: Veridicare tutti i dati con Validators
    @NotNull
    @Size(min = 1, message = "{Size.restaurantDto.name}")
    private String name;
    private String address;
    private String post_code;
    private String pi;
	private RestaurantImageDto restaurantImage;
    @ValidEmail
    @NotNull
    @Size(min = 1, message = "{Size.restaurantDto.email}")
    private String email;
	private String ownerName;
	private String ownerSurname;
	private String phoneNumber;
	private String password;

}
