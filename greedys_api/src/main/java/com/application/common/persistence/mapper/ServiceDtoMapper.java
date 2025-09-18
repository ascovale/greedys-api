package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.persistence.model.reservation.Service;
import com.application.common.web.dto.restaurant.ServiceDTO;

/**
 * MapStruct mapper per la conversione tra Service e ServiceDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN,
    uses = {ServiceTypeDtoMapper.class}
)
public interface ServiceDtoMapper {

    ServiceDtoMapper INSTANCE = Mappers.getMapper(ServiceDtoMapper.class);

    /**
     * Converte un'entità Service in ServiceDto
     */
    @Mapping(target = "serviceType", source = "serviceTypes")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    ServiceDTO toDTO(Service service);

    /**
     * Converte un ServiceDto in entità Service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "slots", ignore = true) // Sarà gestito dal service
    @Mapping(target = "deleted", ignore = true) // Campo gestito dal service
    @Mapping(target = "enabled", ignore = true) // Non presente nel DTO
    @Mapping(target = "color", ignore = true) // Non presente nel DTO
    @Mapping(target = "menus", ignore = true) // Sarà gestito dal service
    @Mapping(target = "serviceTypes", source = "serviceType")
    Service toEntity(ServiceDTO serviceDto);

    /**
     * Aggiorna un'entità Service esistente con i dati dal ServiceDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "serviceTypes", source = "serviceType")
    void updateEntityFromDTO(ServiceDTO dto, @MappingTarget Service entity);
}
