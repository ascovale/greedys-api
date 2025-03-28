package com.application.web.dto.post;

import java.util.List;

import com.application.persistence.model.menu.MenuItem.Allergen;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NewMenuItemDTO {
    
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("allergen")
    private List<Allergen> allergen;
    @JsonProperty("restaurantId")
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

    public List<Allergen> getAllergen() {
        return allergen;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAllergen(List<Allergen> allergen) {
        this.allergen = allergen;
    }

}
