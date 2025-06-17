package com.application.persistence.model.notification;

import com.application.persistence.model.restaurant.user.RestaurantUser;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class RestaurantUserFcmToken extends FcmToken {

    @ManyToOne
    @JoinColumn(nullable = false)
    private RestaurantUser restaurantUser;

    public RestaurantUserFcmToken(RestaurantUser restaurantUser, String fcmToken, String deviceId) {
        super(fcmToken, deviceId);
        this.restaurantUser = restaurantUser;
    }
  
    public RestaurantUser getRestaurantUser() { return restaurantUser; }

}