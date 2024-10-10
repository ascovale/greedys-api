package com.application.web.dto.get;

import com.application.persistence.model.menu.MenuItem;
import com.application.persistence.model.menu.MenuItem.Allergen;

import java.util.Collection;

public class MenuItemDTO {
    private Long id;
    private String name;
    private String description;
    private Collection<Allergen> allergens;

    public MenuItemDTO(MenuItem menuItem) {
        this.id = menuItem.getId();
        this.name = menuItem.getName();
        this.description = menuItem.getDescription();
        this.allergens = menuItem.getAllergens();
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
}
