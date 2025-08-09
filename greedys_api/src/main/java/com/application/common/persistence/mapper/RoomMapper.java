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
    RoomDTO toDTO(Room room);

    /**
     * Converte un NewRoomDTO in entità Room
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "tables", ignore = true) // Non mappato nei DTO base
    Room fromNewRoomDTO(NewRoomDTO newRoomDTO);

    /**
     * Converte un RoomDTO in entità Room
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "tables", ignore = true) // Non mappato nei DTO base
    Room toEntity(RoomDTO roomDTO);

    /**
     * Aggiorna un'entità Room esistente con i dati dal RoomDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "tables", ignore = true)
    void updateEntityFromDTO(RoomDTO dto, @MappingTarget Room entity);

    /**
     * Aggiorna un'entità Room esistente con i dati dal NewRoomDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "tables", ignore = true)
    void updateEntityFromNewDTO(NewRoomDTO dto, @MappingTarget Room entity);
}
