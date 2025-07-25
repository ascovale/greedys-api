package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.admin.persistence.model.Admin;
import com.application.admin.web.dto.get.AdminDTO;
import com.application.admin.web.dto.post.NewAdminDTO;

/**
 * MapStruct mapper per la conversione tra Admin e AdminDTO/NewAdminDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdminMapper {

    AdminMapper INSTANCE = Mappers.getMapper(AdminMapper.class);

    /**
     * Converte un'entità Admin in AdminDTO
     * MapStruct mappa automaticamente i campi con lo stesso nome
     */
    @Mapping(target = "firstName", source = "name")
    @Mapping(target = "lastName", source = "surname")
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
     * Converte un AdminDTO in entità Admin (per aggiornamenti)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true) // La password non viene mappata dai DTO
    @Mapping(target = "adminRoles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    Admin toEntity(AdminDTO adminDTO);

    /**
     * Aggiorna un'entità Admin esistente con i dati dal DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "adminRoles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    void updateEntityFromDTO(AdminDTO dto, @MappingTarget Admin entity);

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
