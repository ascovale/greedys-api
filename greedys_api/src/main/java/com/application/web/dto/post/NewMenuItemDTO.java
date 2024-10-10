package com.application.web.dto.post;

import java.util.Collection;

import com.application.persistence.model.menu.MenuItem.Allergen;

public class NewMenuItemDTO {
    
    private String name;
    private String description;
    private Collection<Allergen> allergen;
    private Long restaurantId;

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }
    
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Collection<Allergen> getAllergen() {
        return allergen;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAllergen(Collection<Allergen> allergen) {
        this.allergen = allergen;
    }

}
