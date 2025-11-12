package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.reservations.NewBaseReservationDTO;

/**
 * MapStruct mapper per la conversione tra NewBaseReservationDTO e Reservation
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface NewBaseReservationMapper {


    /**
     * Converte un NewBaseReservationDTO in entità Reservation
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service
    @Mapping(target = "slot", ignore = true) // Sarà impostato dal service usando idSlot
    @Mapping(target = "customer", ignore = true) // Sarà impostato dal service
    @Mapping(target = "table", ignore = true) // Sarà gestito dal service
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "status", ignore = true) // Valore di default
    @Mapping(target = "version", ignore = true) // Campo gestito automaticamente
    @Mapping(target = "createdAt", ignore = true) // Campo automatico
    @Mapping(target = "modifiedAt", ignore = true) // Campo automatico
    @Mapping(target = "createdBy", ignore = true) // Campo automatico
    @Mapping(target = "modifiedBy", ignore = true) // Campo automatico
    @Mapping(target = "acceptedBy", ignore = true) // Campo gestito dal service
    @Mapping(target = "acceptedAt", ignore = true) // Campo gestito dal service
    Reservation fromNewBaseDTO(NewBaseReservationDTO newBaseReservationDTO);

    /**
     * Aggiorna un'entità Reservation esistente con i dati dal NewBaseReservationDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "slot", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    void updateEntityFromNewBaseDTO(NewBaseReservationDTO dto, @MappingTarget Reservation entity);
}
