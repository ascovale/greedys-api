package com.application.restaurant.web.post;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewMenuDTO", description = "DTO for creating a new menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewMenuDTO {

    private String name;
    private String description;
    private Float price;
    private List<Long> serviceIds;
}
