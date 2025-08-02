package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.admin.web.dto.service.AdminNewServiceDTO;
import com.application.common.persistence.model.reservation.Service;

/**
 * MapStruct mapper per la conversione tra AdminNewServiceDTO e Service
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AdminNewServiceMapper {

    AdminNewServiceMapper INSTANCE = Mappers.getMapper(AdminNewServiceMapper.class);

    /**
     * Converte un AdminNewServiceDTO in entità Service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service usando restaurant field
    @Mapping(target = "serviceTypes", ignore = true) // Sarà gestito dal service usando serviceType field
    @Mapping(target = "slots", ignore = true) // Sarà gestito dal service
    @Mapping(target = "deleted", ignore = true) // Campo gestito dal service
    @Mapping(target = "active", ignore = true) // Valore di default
    @Mapping(target = "enabled", ignore = true) // Valore di default
    @Mapping(target = "color", ignore = true) // Non presente nel DTO
    @Mapping(target = "menus", ignore = true) // Sarà gestito dal service
    Service fromNewDTO(AdminNewServiceDTO adminNewServiceDTO);

    /**
     * Aggiorna un'entità Service esistente con i dati dal AdminNewServiceDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "serviceTypes", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "menus", ignore = true)
    void updateEntityFromNewDTO(AdminNewServiceDTO dto, @MappingTarget Service entity);
}
