package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.persistence.model.reservation.Service;
import com.application.common.web.dto.restaurant.RestaurantServizioDto;

/**
 * MapStruct mapper per la conversione tra Service e RestaurantServizioDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RestaurantServiceMapper {

    RestaurantServiceMapper INSTANCE = Mappers.getMapper(RestaurantServiceMapper.class);

    /**
     * Converte un'entità Service in RestaurantServizioDto
     */
    @Mapping(target = "startDate", source = "validFrom")
    @Mapping(target = "endDate", source = "validTo")
    @Mapping(target = "description", source = "info")
    @Mapping(target = "reservations", ignore = true) // Sarà gestito dal service se necessario
    RestaurantServizioDto toDTO(Service service);

    /**
     * Converte un RestaurantServizioDto in entità Service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "slots", ignore = true) // Sarà gestito dal service
    @Mapping(target = "serviceTypes", ignore = true) // Sarà gestito dal service
    @Mapping(target = "deleted", ignore = true) // Campo gestito dal service
    @Mapping(target = "validFrom", source = "startDate")
    @Mapping(target = "validTo", source = "endDate")
    @Mapping(target = "info", source = "description")
    @Mapping(target = "active", ignore = true) // Valore di default
    @Mapping(target = "enabled", ignore = true) // Valore di default
    @Mapping(target = "color", ignore = true) // Non presente nel DTO
    @Mapping(target = "menus", ignore = true) // Sarà gestito dal service
    Service toEntity(RestaurantServizioDto restaurantServizioDto);

    /**
     * Aggiorna un'entità Service esistente con i dati dal RestaurantServizioDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "serviceTypes", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "validFrom", source = "startDate")
    @Mapping(target = "validTo", source = "endDate")
    @Mapping(target = "info", source = "description")
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "menus", ignore = true)
    void updateEntityFromDTO(RestaurantServizioDto dto, @MappingTarget Service entity);
}
