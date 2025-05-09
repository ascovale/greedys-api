package com.application.web.dto.get;

import com.application.persistence.model.restaurant.Room;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RoomDTO", description = "DTO for room details")
public class RoomDTO {

    private Long id;
    private String name;

    public RoomDTO(Room room) {
        this.id = room.getId();
        this.name = room.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
}
