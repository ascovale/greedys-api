package com.application.security.google2fa;

public class CustomAuthenticationDetails {
    private final boolean bypassPasswordCheck;
    private final Long restaurantId;
    private final String email;
    public CustomAuthenticationDetails(boolean bypassPasswordCheck, Long restaurantId, String email) {
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