package com.application.restaurant.web.post;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewDishDTO", description = "DTO for creating a new dish")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewDishDTO {

    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("restaurantId")
    private Long restaurantId;

}
