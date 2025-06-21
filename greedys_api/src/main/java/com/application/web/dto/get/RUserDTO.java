package com.application.web.dto.get;

import com.application.persistence.model.restaurant.user.RUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RUserDTO", description = "DTO for restaurant user details")
public class RUserDTO {

    private String username;
    private Long restaurantId;

    public RUserDTO() {
    }

    public RUserDTO(RUser user) {
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