package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.restaurant.RUserHubDTO;
import com.application.restaurant.persistence.model.user.RUserHub;

/**
 * MapStruct mapper per la conversione tra RUserHub e RUserHubDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RUserHubMapper {

    /**
     * Converte un'entità RUserHub in RUserHubDTO
     */
    @Mapping(target = "status", expression = "java(rUserHub.getStatus() != null ? rUserHub.getStatus().toString() : null)")
    RUserHubDTO toDTO(RUserHub rUserHub);

    /**
     * Converte un RUserHubDTO in entità RUserHub
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // Non esporre password
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "credentialsExpirationDate", ignore = true)
    @Mapping(target = "accepted", ignore = true)
    RUserHub toEntity(RUserHubDTO rUserHubDTO);
}
