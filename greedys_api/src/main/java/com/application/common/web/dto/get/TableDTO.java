package com.application.common.web.dto.get;

import com.application.restaurant.model.Table;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "TableDTO", description = "DTO for table details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDTO {
    
    private Long id;
    private String name;
    private RoomDTO room;
    private int capacity;
    private int positionX;
    private int positionY;

    public TableDTO(Table table) {
        this.id = table.getId();
        this.name = table.getName();
        this.room = new RoomDTO(table.getRoom());
        this.capacity = table.getCapacity();
        this.positionX = table.getPositionX();
        this.positionY = table.getPositionY();
    }
}
