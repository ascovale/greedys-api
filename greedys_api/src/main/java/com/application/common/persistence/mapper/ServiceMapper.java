package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.persistence.model.reservation.Service;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.restaurant.web.dto.services.NewServiceDTO;

/**
 * MapStruct mapper per la conversione tra Service e ServiceDTO/NewServiceDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ServiceMapper {

    ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

    /**
     * Converte un'entità Service in ServiceDTO
     */
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "serviceType", ignore = true) // Gestito dal costruttore del DTO
    ServiceDTO toDTO(Service service);

    /**
     * Converte un NewServiceDTO in entità Service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "slots", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "active", ignore = true) // Sarà impostato dal service
    @Mapping(target = "color", ignore = true) // Sarà impostato dal service
    @Mapping(target = "deleted", ignore = true) // Sarà impostato dal service
    @Mapping(target = "enabled", ignore = true) // Sarà impostato dal service
    @Mapping(target = "menus", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "serviceTypes", ignore = true) // Sarà impostato dal service
    Service fromNewServiceDTO(NewServiceDTO newServiceDTO);

    /**
     * Converte un ServiceDTO in entità Service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "slots", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "color", ignore = true) // Sarà impostato dal service
    @Mapping(target = "deleted", ignore = true) // Sarà impostato dal service
    @Mapping(target = "enabled", ignore = true) // Sarà impostato dal service
    @Mapping(target = "menus", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "serviceTypes", ignore = true) // Sarà impostato dal service
    Service toEntity(ServiceDTO serviceDTO);

    /**
     * Aggiorna un'entità Service esistente con i dati dal ServiceDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "serviceTypes", ignore = true)
    void updateEntityFromDTO(ServiceDTO dto, @MappingTarget Service entity);

    /**
     * Aggiorna un'entità Service esistente con i dati dal NewServiceDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "menus", ignore = true)
    @Mapping(target = "serviceTypes", ignore = true)
    void updateEntityFromNewDTO(NewServiceDTO dto, @MappingTarget Service entity);
}
