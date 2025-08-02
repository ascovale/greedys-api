package com.application.restaurant.web.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewMenuDishDTO", description = "DTO for associating a dish with a menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewMenuDishDTO {

    private Long menuId;
    private Long dishId; 
    private Double price;

}
