package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.agency.persistence.model.user.AgencyUserHub;
import com.application.agency.web.dto.AgencyUserHubDTO;

/**
 * MapStruct mapper per la conversione tra AgencyUserHub e AgencyUserHubDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AgencyUserHubMapper {

    /**
     * Converte un'entità AgencyUserHub in AgencyUserHubDTO
     */
    @Mapping(target = "username", source = "email")
    AgencyUserHubDTO toDTO(AgencyUserHub agencyUserHub);

    /**
     * Converte un AgencyUserHubDTO in entità AgencyUserHub
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "username")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    AgencyUserHub toEntity(AgencyUserHubDTO dto);

    /**
     * Aggiorna un'entità AgencyUserHub esistente con i dati dal AgencyUserHubDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "username")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromDTO(AgencyUserHubDTO dto, @MappingTarget AgencyUserHub entity);
}
