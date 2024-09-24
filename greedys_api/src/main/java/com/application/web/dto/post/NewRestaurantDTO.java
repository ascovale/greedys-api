package com.application.web.dto.post;

import com.application.controller.Validators.ValidEmail;
import com.application.web.dto.RestaurantImageDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NewRestaurantDTO {

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
	private Long ownerId;

	public Long getOwnerId() {
		return ownerId;	
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
    
    public RestaurantImageDto getRestaurantImage() {
		return restaurantImage;
	}

	public void setRestaurantImage(RestaurantImageDto restaurantImage) {
		this.restaurantImage = restaurantImage;
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
