package com.application.common.web.dto.menu;

import java.util.Collection;
import java.util.List;

import com.application.restaurant.persistence.model.menu.Menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "MenuDTO", description = "DTO for menu details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuDTO {

    private Long id;
    private String name;
    private String description;
    private List<Long> serviceIds;
    private Collection<MenuDishDTO> menuDishDTOs;

    public MenuDTO(Menu menu) {
        this.id = menu.getId();
        this.name = menu.getName();
        this.description = menu.getDescription();
    }
}
