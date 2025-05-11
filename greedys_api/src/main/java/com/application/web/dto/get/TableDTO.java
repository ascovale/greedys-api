package com.application.web.dto.get;

import com.application.persistence.model.restaurant.Table;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TableDTO", description = "DTO for table details")
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

        public int getPositionY() {
            return positionY;
        }

        public int getPositionX() {
            return positionX;
        }

        public int getCapacity() {
            return capacity;
        }

        public RoomDTO getRoom() {
            return room;
        }
    
        public Long getId() {
            return id;
        }
    
        public String getName() {
            return name;
        }
    
}
