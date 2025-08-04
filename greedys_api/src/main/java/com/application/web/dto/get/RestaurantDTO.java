package com.application.web.dto.get;

import com.application.controller.validators.ValidEmail;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.web.dto.RestaurantImageDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "RestaurantDTO", description = "DTO for restaurant details")
public class RestaurantDTO {
	private Long id; 
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

	public RestaurantDTO(Restaurant restaurant) {
		this.id = restaurant.getId();
		this.name = restaurant.getName();
		this.address = restaurant.getAddress();
		this.post_code = restaurant.getPostCode();
		this.pi = restaurant.getPI();
		this.email = restaurant.getEmail();
	}
    
    public RestaurantImageDto getRestaurantImage() {
		return restaurantImage;
	}

	public void setRestaurantImage(RestaurantImageDto restaurantImage) {
		this.restaurantImage = restaurantImage;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPost_code() {
		return post_code;
	}

	public void setPost_code(String post_code) {
		this.post_code = post_code;
	}

	public String getpi() {
		return pi;
	}

	public void setpI(String pi) {
		this.pi = pi;
	}

}
