package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.security.RoleDTO;
import com.application.customer.persistence.model.Role;

/**
 * MapStruct mapper per la conversione tra Role e RoleDTO
 */
@Mapper(
    componentModel = "spring",
    uses = PrivilegeDTOMapper.class, // Usa il mapper per i privilegi
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RoleDTOMapper {

    RoleDTOMapper INSTANCE = Mappers.getMapper(RoleDTOMapper.class);

    /**
     * Converte un'entità Role in RoleDTO
     */
    RoleDTO toDTO(Role role);

    /**
     * Converte un RoleDTO in entità Role
     */
    Role toEntity(RoleDTO roleDTO);

    /**
     * Aggiorna un'entità Role esistente con i dati dal RoleDTO
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDTO(RoleDTO dto, @MappingTarget Role entity);
}
