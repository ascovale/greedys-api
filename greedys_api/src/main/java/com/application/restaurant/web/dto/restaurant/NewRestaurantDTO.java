package com.application.restaurant.web.dto.restaurant;

import com.application.common.controller.validators.ValidAddress;
import com.application.common.controller.validators.ValidCity;
import com.application.common.controller.validators.ValidEmail;
import com.application.common.controller.validators.ValidPhoneNumber;
import com.application.common.controller.validators.ValidVatNumber;
import com.application.common.web.dto.restaurant.RestaurantImageDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NewRestaurantDTO", description = "DTO for creating a new restaurant")
public class NewRestaurantDTO {
    @NotNull
    @Size(min = 1, message = "{Size.restaurantDto.name}")
    @Schema(description = "Restaurant name", example = "Trattoria Da Mario")
    private String name;
    
    @NotNull
    @ValidAddress(allowNull = false, strictValidation = true, minLength = 10, maxLength = 200)
    @Schema(description = "Restaurant address (street and number only)", example = "Via Roma 123")
    private String address;
    
    @NotNull
    @Size(min = 1, message = "City is required")
    @ValidCity(country = "IT", strictValidation = false, allowNull = false)
    @Schema(description = "City name", example = "Milano")
    private String city;
    
    @Schema(description = "Postal code (optional, will be auto-filled from geocoding)", example = "20121")
    private String post_code;
    
    @Schema(description = "State or Province (optional, will be auto-filled from geocoding)", example = "Lombardy")
    private String stateProvince;
    
    @Schema(description = "Country (optional, will be auto-filled from geocoding)", example = "Italy")
    private String country;
    
    @ValidVatNumber(allowNull = true)
    @Schema(description = "International VAT/Tax number following country-specific format", 
            example = "IT12345678901", 
            pattern = "^[A-Z]{2}[A-Z0-9]+$")
    private String vatNumber;
    
	private RestaurantImageDto restaurantImage;
    
    @ValidEmail
    @NotNull
    @Size(min = 1, message = "{Size.restaurantDto.email}")
    @Schema(description = "Restaurant contact email", example = "info@trattoriadamario.it")
    private String email;
    
	@Schema(description = "Owner first name", example = "Mario")
	private String ownerName;
	
	@Schema(description = "Owner last name", example = "Rossi")
	private String ownerSurname;
	
	@ValidPhoneNumber(country = "IT", allowNull = true, requireInternational = false)
	@Schema(description = "Restaurant phone number", example = "+39 02 1234567")
	private String phoneNumber;
	
	@Schema(description = "Initial password for restaurant account")
	private String password;

}
