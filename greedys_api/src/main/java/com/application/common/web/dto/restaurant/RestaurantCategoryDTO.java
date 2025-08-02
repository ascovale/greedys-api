package com.application.common.web.dto.restaurant;

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
    private String name;
    private String description;
}
