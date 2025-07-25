package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.get.MenuDTO;
import com.application.restaurant.persistence.model.menu.Menu;
import com.application.restaurant.web.dto.post.NewMenuDTO;

/**
 * MapStruct mapper per la conversione tra Menu e MenuDTO/NewMenuDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface MenuMapper {

    MenuMapper INSTANCE = Mappers.getMapper(MenuMapper.class);

    /**
     * Converte un'entità Menu in MenuDTO
     */
    @Mapping(target = "serviceIds", ignore = true) // Troppo complesso per mapping automatico
    @Mapping(target = "menuDishDTOs", ignore = true) // Gestito dal costruttore del DTO
    MenuDTO toDTO(Menu menu);

    /**
     * Converte un NewMenuDTO in entità Menu
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) // Sarà impostato dal service
    @Mapping(target = "services", ignore = true) // Sarà gestito dal service
    Menu fromNewMenuDTO(NewMenuDTO newMenuDTO);

    /**
     * Converte un MenuDTO in entità Menu
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "services", ignore = true)
    Menu toEntity(MenuDTO menuDTO);

    /**
     * Aggiorna un'entità Menu esistente con i dati dal MenuDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "services", ignore = true)
    void updateEntityFromDTO(MenuDTO dto, @MappingTarget Menu entity);

    /**
     * Aggiorna un'entità Menu esistente con i dati dal NewMenuDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "services", ignore = true)
    void updateEntityFromNewDTO(NewMenuDTO dto, @MappingTarget Menu entity);
}
