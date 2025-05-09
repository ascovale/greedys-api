package com.application.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NewMenuDishDTO", description = "DTO for associating a dish with a menu")
public class NewMenuDishDTO {

    private Long menuId;
    private Long dishId; 
    private Double price;

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
