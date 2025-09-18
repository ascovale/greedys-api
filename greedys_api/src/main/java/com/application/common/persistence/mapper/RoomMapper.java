package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.restaurant.RoomDTO;
import com.application.restaurant.persistence.model.Room;
import com.application.restaurant.web.dto.restaurant.NewRoomDTO;

/**
 * MapStruct mapper per la conversione tra Room e RoomDTO/NewRoomDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RoomMapper {

    /**
     * Converte un'entità Room in RoomDTO
     */
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "tables", ignore = true) // Non mappare le tables per evitare lazy loading
    RoomDTO toDTO(Room room);

    /**
     * Converte un NewRoomDTO in entità Room
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "tables", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "name", source = "name")
    Room fromNewRoomDTO(NewRoomDTO newRoomDTO);

    /**
     * Aggiorna un'entità Room esistente con i dati dal NewRoomDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "tables", ignore = true)
    @Mapping(target = "name", source = "name")
    void updateEntityFromNewDTO(NewRoomDTO dto, @MappingTarget Room entity);
}
