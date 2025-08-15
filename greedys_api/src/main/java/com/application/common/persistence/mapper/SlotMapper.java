package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.web.dto.services.NewSlotDTO;

/**
 * MapStruct mapper per la conversione tra Slot e SlotDTO/NewSlotDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface SlotMapper {


    /**
     * Converte un'entità Slot in SlotDTO
     */
    @Mapping(target = "service", ignore = true) // Gestito dal costruttore del DTO
    SlotDTO toDTO(Slot slot);

    /**
     * Converte un NewSlotDTO in entità Slot
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true) // Sarà impostato dal service
    Slot fromNewSlotDTO(NewSlotDTO newSlotDTO);

    /**
     * Converte un SlotDTO in entità Slot
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true) // Sarà impostato dal service
    Slot toEntity(SlotDTO slotDTO);

    /**
     * Aggiorna un'entità Slot esistente con i dati dal NewSlotDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true)
    void updateEntityFromNewDTO(NewSlotDTO dto, @MappingTarget Slot entity);
}
