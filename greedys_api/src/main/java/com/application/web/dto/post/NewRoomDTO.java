package com.application.web.dto.post;

public class NewRoomDTO {
    private Long idRestaurant;
    private String name;

    private Long restaurantId;

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Long getIdRestaurant() {
        return idRestaurant;
    }

    public void setIdRestaurant(Long idRestaurant) {
        this.idRestaurant = idRestaurant;
    }
    public String getName() {
        return name;
    }
    
}
