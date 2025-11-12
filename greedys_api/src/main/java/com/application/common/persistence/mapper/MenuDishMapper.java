package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.menu.MenuDishDTO;
import com.application.restaurant.persistence.model.menu.MenuDish;
import com.application.restaurant.web.dto.menu.NewMenuDishDTO;

/**
 * MapStruct mapper per la conversione tra MenuDish e MenuDishDTO/NewMenuDishDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MenuDishMapper {


    /**
     * Converte un'entità MenuDish in MenuDishDTO
     */
    @Mapping(target = "dishDTO", ignore = true) // Gestito dal costruttore del DTO
    MenuDishDTO toDTO(MenuDish menuDish);

    /**
     * Converte un NewMenuDishDTO in entità MenuDish
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menu", ignore = true) // Sarà impostato dal service
    @Mapping(target = "dish", ignore = true) // Sarà impostato dal service
    MenuDish fromNewMenuDishDTO(NewMenuDishDTO newMenuDishDTO);

    /**
     * Converte un MenuDishDTO in entità MenuDish
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menu", ignore = true) // Sarà impostato dal service
    @Mapping(target = "dish", ignore = true) // Sarà impostato dal service
    MenuDish toEntity(MenuDishDTO menuDishDTO);

    /**
     * Aggiorna un'entità MenuDish esistente con i dati dal MenuDishDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menu", ignore = true)
    @Mapping(target = "dish", ignore = true)
    void updateEntityFromDTO(MenuDishDTO dto, @MappingTarget MenuDish entity);

    /**
     * Aggiorna un'entità MenuDish esistente con i dati dal NewMenuDishDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menu", ignore = true)
    @Mapping(target = "dish", ignore = true)
    void updateEntityFromNewDTO(NewMenuDishDTO dto, @MappingTarget MenuDish entity);
}
