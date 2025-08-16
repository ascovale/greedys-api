package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.admin.persistence.model.Admin;
import com.application.admin.web.dto.admin.AdminDTO;
import com.application.admin.web.dto.admin.NewAdminDTO;

/**
 * MapStruct mapper per la conversione tra Admin e AdminDTO/NewAdminDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdminMapper {
    /**
     * Converte un'entità Admin in AdminDTO
     * MapStruct mappa automaticamente i campi con lo stesso nome
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "firstName", source = "name")
    @Mapping(target = "lastName", source = "surname")
    @Mapping(target = "email", source = "email")
    AdminDTO toDTO(Admin admin);

    /**
     * Converte un NewAdminDTO in entità Admin
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "adminRoles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    Admin toEntity(NewAdminDTO newAdminDTO);

    /**
     * Aggiorna un'entità Admin esistente con i dati dal NewAdminDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "adminRoles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    void updateEntityFromNewDTO(NewAdminDTO dto, @MappingTarget Admin entity);

    
}
