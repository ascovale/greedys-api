package com.application.web.dto.get;

import com.application.persistence.model.menu.RestaurantMenu;

import java.util.Collection;

public class RestaurantMenuDTO {

    private Long id;
    private String name;
    private String description;
    private float  price;
    private Collection<MenuItemDTO> menuItems;

    public RestaurantMenuDTO(RestaurantMenu restaurantMenu) {
        this.id = restaurantMenu.getId();
        this.name = restaurantMenu.getName();
        this.description = restaurantMenu.getDescription();
        this.price = restaurantMenu.getPrice();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public float getPrice() {
        return price;
    }

    public Collection<MenuItemDTO> getMenuItems() {
        return menuItems;
    }

    
}
