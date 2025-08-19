package com.application.common.web.dto.restaurant;

import com.application.restaurant.persistence.model.RestaurantCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RestaurantCategoryDTO", description = "DTO for restaurant category details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantCategoryDTO {
    private Long id;
    private String name;
    private String description;

    public RestaurantCategoryDTO(RestaurantCategory category) {
        this.id = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
    }

    public static RestaurantCategoryDTO toDTO(RestaurantCategory category) {
        return new RestaurantCategoryDTO(category);
    }
}
