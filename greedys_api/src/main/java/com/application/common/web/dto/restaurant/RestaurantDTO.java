package com.application.common.web.dto.restaurant;

import com.application.common.controller.validators.ValidEmail;
import com.application.common.controller.validators.ValidVatNumber;
import com.application.restaurant.persistence.model.Restaurant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RestaurantDTO", description = "DTO for restaurant details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {
	private Long id; 
    @NotNull
    @Size(min = 1, message = "{Size.restaurantDto.name}")
    @Schema(description = "Restaurant name", example = "Trattoria Da Mario")
    private String name;
    
    @Schema(description = "Restaurant address", example = "Via Roma 123, Milano")
    private String address;
    
    @Schema(description = "Postal code", example = "20121")
    private String post_code;
    
    @ValidVatNumber(allowNull = true)
    @Schema(description = "International VAT number following VIES format", 
            example = "IT12345678901")
    private String vatNumber;
    
    @Deprecated
    @Schema(description = "Legacy field, use vatNumber instead", hidden = true)
    private String pi;
    
	private RestaurantImageDto restaurantImage;
    
    @ValidEmail
    @NotNull
    @Size(min = 1, message = "{Size.restaurantDto.email}")
    @Schema(description = "Restaurant contact email", example = "info@trattoriadamario.it")
    private String email;

	public RestaurantDTO(Restaurant restaurant) {
		this.id = restaurant.getId();
		this.name = restaurant.getName();
		this.address = restaurant.getAddress();
		this.post_code = restaurant.getPostCode();
		this.vatNumber = restaurant.getVatNumber();
		this.pi = restaurant.getVatNumber(); // For backward compatibility
		this.email = restaurant.getEmail();
	}

	/**
	 * Legacy getter for backward compatibility
	 * @deprecated Use getVatNumber() instead
	 */
	@Deprecated
	public String getpi() {
		return vatNumber;
	}

	/**
	 * Legacy setter for backward compatibility
	 * @deprecated Use setVatNumber() instead
	 */
	@Deprecated
	public void setpI(String pi) {
		this.vatNumber = pi;
		this.pi = pi;
	}

	/**
	 * Custom setter to keep legacy field in sync
	 */
	public void setVatNumber(String vatNumber) {
		this.vatNumber = vatNumber;
		this.pi = vatNumber; // Keep legacy field in sync
	}
}
