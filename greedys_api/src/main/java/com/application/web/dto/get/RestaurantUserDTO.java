package com.application.web.dto.get;

import com.application.persistence.model.restaurant.user.RestaurantUser;


public class RestaurantUserDTO {

    private Long user_id;
    private Long restaurant_id;

    public RestaurantUserDTO(RestaurantUser user) {
        this.user_id = user.getId();
        this.restaurant_id = user.getRestaurant().getId();
    }

    public Long getUser_id() {
        return user_id;
    }

    public Long getRestaurant_id() {
        return restaurant_id;
    }

}
