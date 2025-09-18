package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.restaurant.ServiceSlotsDto;

/**
 * MapStruct mapper per la conversione tra Slot e ServiceSlotsDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ServiceSlotMapper {


    /**
     * Converte un'entità Slot in ServiceSlotsDto (per singolo slot)
     */
    @Mapping(target = "open", source = "start")
    @Mapping(target = "close", source = "end")
    @Mapping(target = "name", source = "service.name")
    @Mapping(target = "serviceType", expression = "java(slot.getService().getServiceTypes().stream().map(st -> st.getName()).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(target = "slots", ignore = true) // Non applicabile per singolo slot
    ServiceSlotsDto toDTO(Slot slot);

    /**
     * Converte un ServiceSlotsDto in entità Slot
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true) // Sarà impostato dal service
    @Mapping(target = "deleted", ignore = true) // Campo gestito dal service
    @Mapping(target = "start", source = "open")
    @Mapping(target = "end", source = "close")
    Slot toEntity(ServiceSlotsDto serviceSlotsDto);

    /**
     * Aggiorna un'entità Slot esistente con i dati dal ServiceSlotsDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "start", source = "open")
    @Mapping(target = "end", source = "close")
    void updateEntityFromDTO(ServiceSlotsDto dto, @MappingTarget Slot entity);
}
