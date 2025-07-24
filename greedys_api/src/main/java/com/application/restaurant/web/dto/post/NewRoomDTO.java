package com.application.restaurant.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewRoomDTO", description = "DTO for creating a new room")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewRoomDTO {
    private Long idRestaurant;
    private String name;
    private Long restaurantId;
}
