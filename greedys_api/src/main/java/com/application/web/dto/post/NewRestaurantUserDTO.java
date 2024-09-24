
package com.application.web.dto.post;

import jakarta.validation.constraints.NotNull;

public class NewRestaurantUserDTO {

    @NotNull
    private Long restaurantId;
    @NotNull
    private Long userId;

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}