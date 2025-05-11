package com.application.web.dto.post;

import com.application.controller.Validators.ValidEmail;
import com.application.web.dto.RestaurantImageDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

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
	private Long ownerId;
	private String phoneNumber;
	private String password;
	public NewRestaurantDTO() {
	}
	public NewRestaurantDTO(String name, String address, String email, String password) {
		this.name = name;
		this.address = address;
		this.email = email;
		this.password = password;
	}

    public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

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
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}
