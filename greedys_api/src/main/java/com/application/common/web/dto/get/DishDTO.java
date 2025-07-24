package com.application.common.web.dto.get;

import java.util.Collection;

import com.application.restaurant.persistence.model.menu.Allergen;
import com.application.restaurant.persistence.model.menu.Dish;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "DishDTO", description = "DTO for dish details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
