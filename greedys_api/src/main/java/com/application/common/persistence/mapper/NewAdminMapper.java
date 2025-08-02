package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.admin.persistence.model.Admin;
import com.application.admin.web.dto.admin.NewAdminDTO;

/**
 * MapStruct mapper per la conversione tra NewAdminDTO e Admin
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface NewAdminMapper {

    NewAdminMapper INSTANCE = Mappers.getMapper(NewAdminMapper.class);

    /**
     * Converte un NewAdminDTO in entità Admin
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "nickName", ignore = true) // Non presente nel DTO
    @Mapping(target = "phoneNumber", ignore = true) // Non presente nel DTO
    @Mapping(target = "toReadNotification", ignore = true) // Valore di default
    @Mapping(target = "password", ignore = true) // Sarà codificata separatamente dal service
    @Mapping(target = "adminRoles", ignore = true) // Sarà gestito dal service usando role
    @Mapping(target = "status", ignore = true) // Valore di default
    Admin fromNewDTO(NewAdminDTO newAdminDTO);

    /**
     * Aggiorna un'entità Admin esistente con i dati dal NewAdminDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "adminRoles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "privalegesStrings", ignore = true) // Metodo derivato
    @Mapping(target = "authorities", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "privileges", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "privilegesStrings", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "roles", ignore = true) // Metodo derivato dall'AbstractUser
    void updateEntityFromNewDTO(NewAdminDTO dto, @MappingTarget Admin entity);
}
