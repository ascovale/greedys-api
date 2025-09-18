package com.application.common.web.dto.restaurant;

import com.application.restaurant.persistence.model.Table;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "SimpleTableDTO", description = "Simplified DTO for table details without room reference")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTableDTO {
    
    private Long id;
    private String name;
    private int capacity;
    private int positionX;
    private int positionY;

    public SimpleTableDTO(Table table) {
        this.id = table.getId();
        this.name = table.getName();
        this.capacity = table.getCapacity();
        this.positionX = table.getPositionX();
        this.positionY = table.getPositionY();
    }
}
