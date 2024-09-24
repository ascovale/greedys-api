package com.application.web.dto;


import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RestaurantFullDetailsDto {
		private Long id;
		private String name;
	    private String address;
	    private String post_code;
	    private String pI;
	    private String restaurantSelectedImage;
	    private List<RestaurantImageDto> restaurantOtherImages;
	    private String description;
	    private double latitude;
	    private double longitude;
	    private RestaurantLogoDto restaurantLogo;
	    
	    
		public RestaurantLogoDto getRestaurantLogoDto() {
			return restaurantLogo;
		}

		public void setRestaurantLogoDto(RestaurantLogoDto restaurantLogo) {
			this.restaurantLogo = restaurantLogo;
		}

		public String getRestaurantSelectedImage() {
			return restaurantSelectedImage;
		}

		public void setRestaurantSelectedImage(String restaurantSelectedImage) {
			this.restaurantSelectedImage = restaurantSelectedImage;
		}

		public List<RestaurantImageDto> getRestaurantOtherImages() {
			return restaurantOtherImages;
		}

		public void setRestaurantOtherImages(List<RestaurantImageDto> restaurantOtherImages) {
			this.restaurantOtherImages = restaurantOtherImages;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		@NotNull
	    @Size(min = 1, message = "{Size.restaurantUserDto.email}")
	    private String email;    

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

	    private Integer role;

	    public Integer getRole() {
	        return role;
	    }

	    public void setRole(final Integer role) {
	        this.role = role;
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

		public String getpI() {
			return pI;
		}

		public void setpI(String pI) {
			this.pI = pI;
		}

	}

