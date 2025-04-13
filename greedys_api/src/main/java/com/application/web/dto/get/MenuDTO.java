package com.application.web.dto.get;

import java.util.Collection;
import java.util.List;

import com.application.persistence.model.menu.Menu;

public class MenuDTO {

    private Long id;
    private String name;
    private String description;
    private List<Long> serviceIds;
    private Collection<MenuDishDTO> menuDishDTOs;

    public MenuDTO(Menu menu) {
        this.id = menu.getId();
        this.name = menu.getName();
        this.description = menu.getDescription();
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
    public List<Long> getServiceIds() {
        return serviceIds;
    }

    public void setServiceId(List<Long> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public Collection<MenuDishDTO> getMenuDishDTOs() {
        return menuDishDTOs;
    }

    public void setMenuDishDTOs(Collection<MenuDishDTO> menuDishDTOs) {
        this.menuDishDTOs = menuDishDTOs;
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

    
}
