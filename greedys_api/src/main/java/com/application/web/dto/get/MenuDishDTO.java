package com.application.web.dto.get;

import com.application.persistence.model.menu.Dish;
import com.application.persistence.model.menu.MenuDish;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(name = "MenuDishDTO", description = "DTO for menu dish details")
public class MenuDishDTO {

    private DishDTO dishDTO;
    private Double price;
    
    public MenuDishDTO(MenuDish menuDish) {
        this.dishDTO = new DishDTO(menuDish.getDish());
        this.price = menuDish.getPrice();  
    }

    public MenuDishDTO(Dish dish, Double price) {
        this.dishDTO = new DishDTO(dish);
        this.price = price;
    }

    public DishDTO getDishDTO() {
        return dishDTO;
    }

    public Double getPrice() {
        return price;
    }

    public void setDishDTO(DishDTO dishDTO) {
        this.dishDTO = dishDTO;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
    
}
