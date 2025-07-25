package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.ServiceDto;
import com.application.common.persistence.model.reservation.Service;

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
    @Mapping(target = "open", ignore = true) // Non presente nell'entità Service
    @Mapping(target = "close", ignore = true) // Non presente nell'entità Service  
    @Mapping(target = "weekday", ignore = true) // Non presente nell'entità Service
    ServiceDto toDTO(Service service);

    /**
     * Converte un ServiceDto in entità Service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "slots", ignore = true) // Sarà gestito dal service
    @Mapping(target = "info", ignore = true) // Non presente nel DTO
    @Mapping(target = "deleted", ignore = true) // Campo gestito dal service
    @Mapping(target = "validFrom", ignore = true) // Non presente nel DTO
    @Mapping(target = "validTo", ignore = true) // Non presente nel DTO
    @Mapping(target = "active", ignore = true) // Non presente nel DTO
    @Mapping(target = "enabled", ignore = true) // Non presente nel DTO
    @Mapping(target = "color", ignore = true) // Non presente nel DTO
    @Mapping(target = "menus", ignore = true) // Sarà gestito dal service
    Service toEntity(ServiceDto serviceDto);

    /**
     * Aggiorna un'entità Service esistente con i dati dal ServiceDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "info", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "validFrom", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "menus", ignore = true)
    void updateEntityFromDTO(ServiceDto dto, @MappingTarget Service entity);
}
