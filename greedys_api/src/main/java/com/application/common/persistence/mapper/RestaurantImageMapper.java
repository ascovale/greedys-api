package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.Image;
import com.application.common.web.dto.restaurant.RestaurantImageDto;

/**
 * MapStruct mapper per la conversione tra Image e RestaurantImageDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RestaurantImageMapper {


    /**
     * Converte un'entità Image in RestaurantImageDto
     */
    RestaurantImageDto toDTO(Image image);

    /**
     * Converte un RestaurantImageDto in entità Image
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    Image toEntity(RestaurantImageDto restaurantImageDto);

    /**
     * Aggiorna un'entità Image esistente con i dati dal RestaurantImageDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateEntityFromDTO(RestaurantImageDto dto, @MappingTarget Image entity);
}
