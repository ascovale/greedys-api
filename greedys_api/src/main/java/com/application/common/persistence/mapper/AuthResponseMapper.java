package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.post.AuthResponseDTO;

/**
 * MapStruct mapper per la conversione di AuthResponseDTO
 * Questo mapper è principalmente utilizzato per la creazione di response di autenticazione
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AuthResponseMapper {

    AuthResponseMapper INSTANCE = Mappers.getMapper(AuthResponseMapper.class);

    /**
     * Crea un AuthResponseDTO con JWT e user object
     */
    @Mapping(target = "jwt", source = "jwt")
    @Mapping(target = "user", source = "user")
    AuthResponseDTO createAuthResponse(String jwt, Object user);
}
