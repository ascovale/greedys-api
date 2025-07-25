package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.get.DishDTO;
import com.application.restaurant.persistence.model.menu.Dish;
import com.application.restaurant.web.dto.post.NewDishDTO;

/**
 * MapStruct mapper per la conversione tra Dish e DishDTO/NewDishDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface DishMapper {

    DishMapper INSTANCE = Mappers.getMapper(DishMapper.class);

    /**
     * Converte un'entità Dish in DishDTO
     */
    DishDTO toDTO(Dish dish);

    /**
     * Converte un NewDishDTO in entità Dish
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "photoLinks", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "menuDishes", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Verrà impostato dal service
    @Mapping(target = "allergens", ignore = true)
    Dish fromNewDishDTO(NewDishDTO newDishDTO);

    /**
     * Converte un DishDTO in entità Dish
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "photoLinks", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "menuDishes", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    Dish toEntity(DishDTO dishDTO);

    /**
     * Aggiorna un'entità Dish esistente con i dati dal DishDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "photoLinks", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "menuDishes", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "allergens", ignore = true)
    void updateEntityFromDTO(DishDTO dto, @MappingTarget Dish entity);

    /**
     * Aggiorna un'entità Dish esistente con i dati dal NewDishDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "photoLinks", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "menuDishes", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "allergens", ignore = true)
    void updateEntityFromNewDTO(NewDishDTO dto, @MappingTarget Dish entity);
}
