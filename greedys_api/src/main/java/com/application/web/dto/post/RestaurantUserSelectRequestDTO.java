package com.application.web.dto.post;

public class RestaurantUserSelectRequestDTO {
    private String hubToken;
    private Long restaurantId;
    public String getHubToken() { return hubToken; }
    public void setHubToken(String hubToken) { this.hubToken = hubToken; }
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
}
