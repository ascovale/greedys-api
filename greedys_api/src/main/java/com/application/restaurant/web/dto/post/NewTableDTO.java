package com.application.restaurant.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewTableDTO", description = "DTO for creating a new table")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTableDTO {
    private Long idRestaurant;
    private String name;
    private Long roomId;
    private int capacity;
    private int positionX;
    private int positionY;
}
