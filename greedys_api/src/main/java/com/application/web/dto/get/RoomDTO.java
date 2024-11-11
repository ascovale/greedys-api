package com.application.web.dto.get;

import com.application.persistence.model.restaurant.Room;

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
