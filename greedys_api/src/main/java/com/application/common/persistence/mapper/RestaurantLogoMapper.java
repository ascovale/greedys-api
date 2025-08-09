package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.Image;
import com.application.common.web.dto.restaurant.RestaurantLogoDto;

/**
 * MapStruct mapper per la conversione tra Image e RestaurantLogoDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RestaurantLogoMapper {


    /**
     * Converte un'entità Image in RestaurantLogoDto
     */
    RestaurantLogoDto toDTO(Image image);

    /**
     * Converte un RestaurantLogoDto in entità Image
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    Image toEntity(RestaurantLogoDto restaurantLogoDto);

    /**
     * Aggiorna un'entità Image esistente con i dati dal RestaurantLogoDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateEntityFromDTO(RestaurantLogoDto dto, @MappingTarget Image entity);
}
