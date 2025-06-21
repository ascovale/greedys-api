package com.application.web.dto;

import java.util.List;

public class RUserHubAuthResponseDTO {
    private String token;
    private List<RestaurantInfo> restaurants;

    public static class RestaurantInfo {
        private Long id;
        private String name;
        // getter/setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // getter/setter
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public List<RestaurantInfo> getRestaurants() { return restaurants; }
    public void setRestaurants(List<RestaurantInfo> restaurants) { this.restaurants = restaurants; }
}
