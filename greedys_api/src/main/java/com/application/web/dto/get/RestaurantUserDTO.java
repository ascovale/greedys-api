package com.application.web.dto.get;

import com.application.persistence.model.restaurant.RestaurantUser;


public class RestaurantUserDTO {

    private Long user_id;
    private Long restaurant_id;
    private Long role_id;

    public RestaurantUserDTO(RestaurantUser user) {
        this.user_id = user.getUser().getId();
        this.restaurant_id = user.getRestaurant().getId();
        this.role_id = user.getRoles().getId();
    }

    public Long getUser_id() {
        return user_id;
    }

    public Long getRestaurant_id() {
        return restaurant_id;
    }

    public Long getRole_id() {
        return role_id;
    }
    
}
