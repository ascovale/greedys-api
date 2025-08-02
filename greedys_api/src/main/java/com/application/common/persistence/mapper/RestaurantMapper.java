package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.web.dto.restaurant.NewRestaurantDTO;

/**
 * MapStruct mapper per la conversione tra Restaurant e RestaurantDTO/NewRestaurantDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RestaurantMapper {

    RestaurantMapper INSTANCE = Mappers.getMapper(RestaurantMapper.class);

    /**
     * Converte un'entità Restaurant in RestaurantDTO
     */
    @Mapping(target = "post_code", source = "postCode")
    @Mapping(target = "pi", source = "vatNumber") // Per retrocompatibilità
    @Mapping(target = "restaurantImage", ignore = true) // Gestito separatamente
    RestaurantDTO toDTO(Restaurant restaurant);

    /**
     * Converte un NewRestaurantDTO in entità Restaurant
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postCode", source = "post_code")
    @Mapping(target = "vatNumber", source = "vatNumber")
    @Mapping(target = "creationDate", ignore = true) // Sarà impostato dal service
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "services", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "dishes", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "RUsers", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "restaurantImages", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "latitude", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "longitude", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "restaurantTypes", ignore = true) // Non mappato nei DTO base
    Restaurant fromNewRestaurantDTO(NewRestaurantDTO newRestaurantDTO);

    /**
     * Converte un RestaurantDTO in entità Restaurant
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postCode", source = "post_code")
    @Mapping(target = "vatNumber", source = "vatNumber")
    @Mapping(target = "creationDate", ignore = true) // Sarà impostato dal service
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "services", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "dishes", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "RUsers", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "restaurantImages", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "latitude", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "longitude", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "restaurantTypes", ignore = true) // Non mappato nei DTO base
    Restaurant toEntity(RestaurantDTO restaurantDTO);

    /**
     * Aggiorna un'entità Restaurant esistente con i dati dal RestaurantDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postCode", source = "post_code")
    @Mapping(target = "vatNumber", source = "vatNumber")
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "dishes", ignore = true)
    @Mapping(target = "RUsers", ignore = true)
    @Mapping(target = "restaurantImages", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "restaurantTypes", ignore = true)
    void updateEntityFromDTO(RestaurantDTO dto, @MappingTarget Restaurant entity);

    /**
     * Aggiorna un'entità Restaurant esistente con i dati dal NewRestaurantDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postCode", source = "post_code")
    @Mapping(target = "vatNumber", source = "vatNumber")
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "dishes", ignore = true)
    @Mapping(target = "RUsers", ignore = true)
    @Mapping(target = "restaurantImages", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "restaurantTypes", ignore = true)
    void updateEntityFromNewDTO(NewRestaurantDTO dto, @MappingTarget Restaurant entity);
}
