package com.application.common.web.dto.restaurant;

import java.util.List;

import com.application.restaurant.persistence.model.Room;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RoomDTO", description = "DTO for room details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {

    private Long id;
    private String name;
    private Long restaurantId;
    private List<SimpleTableDTO> tables;

    public RoomDTO(Room room) {
        this.id = room.getId();
        this.name = room.getName();
        this.restaurantId = room.getRestaurant() != null ? room.getRestaurant().getId() : null;
        // Non mappare i tavoli nel costruttore per evitare possibili problemi di lazy loading
    }
}
