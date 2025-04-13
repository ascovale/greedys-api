package com.application.web.dto.get;

import java.util.Collection;

import com.application.persistence.model.menu.Allergen;
import com.application.persistence.model.menu.Dish;

public class DishDTO {
    private Long id;
    private String name;
    private String description;
    private Collection<Allergen> allergens;

    public DishDTO(Dish dish) {
        this.id = dish.getId();
        this.name = dish.getName();
        this.description = dish.getDescription();
        this.allergens = dish.getAllergens();
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

    public Collection<Allergen> getAllergens() {
        return allergens;
    }    
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAllergens(Collection<Allergen> allergens) {
        this.allergens = allergens;
    }
}
