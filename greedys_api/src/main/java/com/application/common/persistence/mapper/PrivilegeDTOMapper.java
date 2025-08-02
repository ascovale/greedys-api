package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.security.PrivilegeDTO;
import com.application.customer.persistence.model.Privilege;

/**
 * MapStruct mapper per la conversione tra Privilege e PrivilegeDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface PrivilegeDTOMapper {

    PrivilegeDTOMapper INSTANCE = Mappers.getMapper(PrivilegeDTOMapper.class);

    /**
     * Converte un'entità Privilege in PrivilegeDTO
     */
    PrivilegeDTO toDTO(Privilege privilege);

    /**
     * Converte un PrivilegeDTO in entità Privilege
     */
    @Mapping(target = "roles", ignore = true) // Sarà gestito dal service per evitare cicli
    Privilege toEntity(PrivilegeDTO privilegeDTO);

    /**
     * Aggiorna un'entità Privilege esistente con i dati dal PrivilegeDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true) // Sarà gestito dal service per evitare cicli
    void updateEntityFromDTO(PrivilegeDTO dto, @MappingTarget Privilege entity);
}
