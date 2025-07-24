package com.application.common.web.dto.get;

import com.application.restaurant.persistence.model.menu.Dish;
import com.application.restaurant.persistence.model.menu.MenuDish;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "MenuDishDTO", description = "DTO for menu dish details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
