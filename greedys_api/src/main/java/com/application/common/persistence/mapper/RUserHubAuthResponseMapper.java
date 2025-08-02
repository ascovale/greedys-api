package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.security.RUserHubAuthResponseDTO;
import com.application.restaurant.persistence.model.user.RUser;

/**
 * MapStruct mapper per la conversione di RUser in RUserHubAuthResponseDTO
 * Questo mapper è specifico per la risposta di autenticazione dell'hub ristoranti
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RUserHubAuthResponseMapper {

    RUserHubAuthResponseMapper INSTANCE = Mappers.getMapper(RUserHubAuthResponseMapper.class);

    /**
     * Converte un'entità RUser in RUserHubAuthResponseDTO
     * Il token deve essere impostato separatamente dal service
     */
    @Mapping(target = "token", ignore = true) // Sarà impostato dal service
    @Mapping(target = "restaurants", expression = "java(java.util.Collections.singletonList(restaurantToInfo(rUser.getRestaurant())))")
    RUserHubAuthResponseDTO toDTO(RUser rUser);

    /**
     * Converte un Restaurant in RestaurantInfo per la risposta
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    RUserHubAuthResponseDTO.RestaurantInfo restaurantToInfo(com.application.restaurant.persistence.model.Restaurant restaurant);
}
