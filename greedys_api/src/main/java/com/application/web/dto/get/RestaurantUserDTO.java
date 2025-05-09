package com.application.web.dto.get;

import com.application.persistence.model.restaurant.user.RestaurantUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RestaurantUserDTO", description = "DTO for restaurant user details")
public class RestaurantUserDTO {

    private String username;
    private Long restaurantId;

    public RestaurantUserDTO() {
    }

    public RestaurantUserDTO(RestaurantUser user) {
        this.username = user.getEmail();
        this.restaurantId = user.getRestaurant().getId();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }
}