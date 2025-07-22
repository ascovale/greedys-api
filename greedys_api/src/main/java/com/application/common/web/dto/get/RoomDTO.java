package com.application.common.web.dto.get;

import com.application.restaurant.model.Room;

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

    public RoomDTO(Room room) {
        this.id = room.getId();
        this.name = room.getName();
    }
}
