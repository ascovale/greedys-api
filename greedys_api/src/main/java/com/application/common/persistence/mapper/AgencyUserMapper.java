package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.agency.persistence.model.user.AgencyUser;
import com.application.agency.web.dto.AgencyUserDTO;

/**
 * MapStruct mapper per la conversione tra AgencyUser e AgencyUserDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AgencyUserMapper {

    /**
     * Converte un'entità AgencyUser in AgencyUserDTO
     */
    @Mapping(target = "username", source = "email")
    @Mapping(target = "agencyId", source = "agency.id")
    AgencyUserDTO toDTO(AgencyUser agencyUser);

    /**
     * Converte un AgencyUserDTO in entità AgencyUser
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "username")
    @Mapping(target = "agency", ignore = true) // Sarà impostato dal service
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "agencyUserHub", ignore = true) // Sarà impostato dal service
    AgencyUser toEntity(AgencyUserDTO dto);

    /**
     * Aggiorna un'entità AgencyUser esistente con i dati dal AgencyUserDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "username")
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "agencyUserHub", ignore = true)
    void updateEntityFromDTO(AgencyUserDTO dto, @MappingTarget AgencyUser entity);
}
