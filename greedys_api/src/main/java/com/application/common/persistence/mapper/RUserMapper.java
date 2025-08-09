package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.restaurant.RUserDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.web.dto.staff.NewRUserDTO;

/**
 * MapStruct mapper per la conversione tra RUser e RUserDTO/NewRUserDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RUserMapper {


    /**
     * Converte un'entità RUser in RUserDTO
     */
    @Mapping(target = "username", source = "email")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    RUserDTO toDTO(RUser rUser);

    /**
     * Converte un NewRUserDTO in entità RUser
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "password", ignore = true) // Sarà gestito separatamente per sicurezza
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "restaurantRoles", ignore = true) // Sarà impostato dal service
    @Mapping(target = "options", ignore = true) // Sarà impostato dal service
    @Mapping(target = "RUserHub", ignore = true) // Sarà impostato dal service
    RUser fromNewRUserDTO(NewRUserDTO newRUserDTO);

    /**
     * Converte un RUserDTO in entità RUser
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "username")
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "password", ignore = true) // Sarà gestito separatamente per sicurezza
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "restaurantRoles", ignore = true) // Sarà impostato dal service
    @Mapping(target = "options", ignore = true) // Sarà impostato dal service
    @Mapping(target = "RUserHub", ignore = true) // Sarà impostato dal service
    RUser toEntity(RUserDTO rUserDTO);

    /**
     * Aggiorna un'entità RUser esistente con i dati dal RUserDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "username")
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "restaurantRoles", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "RUserHub", ignore = true)
    void updateEntityFromDTO(RUserDTO dto, @MappingTarget RUser entity);

    /**
     * Aggiorna un'entità RUser esistente con i dati dal NewRUserDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "restaurantRoles", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "RUserHub", ignore = true)
    void updateEntityFromNewDTO(NewRUserDTO dto, @MappingTarget RUser entity);
}
