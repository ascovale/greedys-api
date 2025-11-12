package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.restaurant.RestaurantFullDetailsDto;
import com.application.restaurant.persistence.model.Restaurant;

/**
 * MapStruct mapper per la conversione tra Restaurant e RestaurantFullDetailsDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {RestaurantImageMapper.class, RestaurantLogoMapper.class}
)
public interface RestaurantFullDetailsMapper {


    /**
     * Converte un'entità Restaurant in RestaurantFullDetailsDto
     */
    @Mapping(target = "restaurantSelectedImage", ignore = true) // Non presente nell'entità
    @Mapping(target = "restaurantOtherImages", source = "restaurantImages")
    @Mapping(target = "restaurantLogo", ignore = true) // Non presente nell'entità
    @Mapping(target = "role", ignore = true) // Non presente nell'entità Restaurant
    @Mapping(target = "postCode", source = "postCode") // Mapping esplicito per chiarezza
    RestaurantFullDetailsDto toDTO(Restaurant restaurant);

    /**
     * Converte un RestaurantFullDetailsDto in entità Restaurant
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vatNumber", source = "vatNumber")
    @Mapping(target = "city", ignore = true) // Non presente nel DTO
    @Mapping(target = "stateProvince", ignore = true) // Non presente nel DTO
    @Mapping(target = "country", ignore = true) // Non presente nel DTO
    @Mapping(target = "services", ignore = true) // Sarà gestito dal service
    @Mapping(target = "RUsers", ignore = true) // Sarà gestito dal service
    @Mapping(target = "restaurantImages", ignore = true) // Sarà gestito dal service
    @Mapping(target = "noShowTimeLimit", ignore = true) // Valore di default
    @Mapping(target = "restaurantTypes", ignore = true) // Sarà gestito dal service
    @Mapping(target = "waNotification", ignore = true) // Valore di default
    @Mapping(target = "telegramNotification", ignore = true) // Valore di default
    @Mapping(target = "status", ignore = true) // Valore di default
    @Mapping(target = "dishes", ignore = true) // Sarà gestito dal service
    @Mapping(target = "messageNotificationTimeAdvance", ignore = true) // Valore di default
    @Mapping(target = "creationDate", ignore = true) // Campo automatico
    @Mapping(target = "placeId", ignore = true) // Non presente nel DTO
    @Mapping(target = "priceLevel", ignore = true) // Non presente nel DTO
    @Mapping(target = "website", ignore = true) // Non presente nel DTO
    Restaurant toEntity(RestaurantFullDetailsDto restaurantFullDetailsDto);

    /**
     * Aggiorna un'entità Restaurant esistente con i dati dal RestaurantFullDetailsDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "stateProvince", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "RUsers", ignore = true)
    @Mapping(target = "restaurantImages", ignore = true)
    @Mapping(target = "noShowTimeLimit", ignore = true)
    @Mapping(target = "restaurantTypes", ignore = true)
    @Mapping(target = "waNotification", ignore = true)
    @Mapping(target = "telegramNotification", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dishes", ignore = true)
    @Mapping(target = "messageNotificationTimeAdvance", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    void updateEntityFromDTO(RestaurantFullDetailsDto dto, @MappingTarget Restaurant entity);
}
