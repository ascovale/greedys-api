package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.restaurant.RestaurantCategoryDTO;
import com.application.restaurant.persistence.model.RestaurantCategory;

/**
 * MapStruct mapper per la conversione tra RestaurantCategory e RestaurantCategoryDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RestaurantCategoryMapper {


    /**
     * Converte un'entità RestaurantCategory in RestaurantCategoryDTO
     */
    RestaurantCategoryDTO toDTO(RestaurantCategory restaurantCategory);

    /**
     * Converte un RestaurantCategoryDTO in entità RestaurantCategory
     */
    @Mapping(target = "id", ignore = true)
    RestaurantCategory toEntity(RestaurantCategoryDTO restaurantCategoryDTO);

    /**
     * Aggiorna un'entità RestaurantCategory esistente con i dati dal RestaurantCategoryDTO
     */
    @Mapping(target = "id", ignore = true)
  //  @Mapping(target = "restaurants", ignore = true)
    void updateEntityFromDTO(RestaurantCategoryDTO dto, @MappingTarget RestaurantCategory entity);
}
