package com.application.security.google2fa;

public class RestaurantUserAuthenticationDetails {
    private final boolean bypassPasswordCheck;
    private final Long restaurantId;
    private final String email;
    public RestaurantUserAuthenticationDetails(boolean bypassPasswordCheck, Long restaurantId, String email) {
        this.bypassPasswordCheck = bypassPasswordCheck;
        this.restaurantId = restaurantId;
        this.email = email;
    }

    public boolean isBypassPasswordCheck() {
        return bypassPasswordCheck;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getEmail() {
        return email;
    }
}