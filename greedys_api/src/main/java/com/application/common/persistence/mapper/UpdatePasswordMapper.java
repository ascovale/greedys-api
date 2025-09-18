package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.user.AbstractUser;
import com.application.common.web.dto.security.UpdatePasswordDTO;

/**
 * MapStruct mapper per la conversione tra AbstractUser e UpdatePasswordDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface UpdatePasswordMapper {


    /**
     * Aggiorna la password di un utente esistente con i dati dal UpdatePasswordDTO
     * Questo mapper è principalmente utilizzato per aggiornare la password
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "surname", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "authorities", ignore = true) // Metodo derivato
    @Mapping(target = "privileges", ignore = true) // Metodo astratto
    @Mapping(target = "privilegesStrings", ignore = true) // Metodo astratto
    @Mapping(target = "roles", ignore = true) // Metodo astratto
    @Mapping(target = "password", source = "newPassword") // Solo la nuova password viene mappata
    void updatePasswordFromDTO(UpdatePasswordDTO dto, @MappingTarget AbstractUser entity);
}
