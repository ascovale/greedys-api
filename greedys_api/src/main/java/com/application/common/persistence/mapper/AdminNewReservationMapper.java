package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.persistence.model.reservation.Reservation;

/**
 * MapStruct mapper per la conversione tra AdminNewReservationDTO e Reservation
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdminNewReservationMapper {

    /**
     * Converte un AdminNewReservationDTO in entità Reservation
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true) // Sarà impostato dal service usando restaurantId
    @Mapping(target = "slot", ignore = true) // Sarà impostato dal service usando slotId
    @Mapping(target = "customer", ignore = true) // Sarà impostato dal service usando userId
    @Mapping(target = "table", ignore = true) // Sarà gestito dal service usando tableIds
    @Mapping(target = "date", source = "reservationDay") // Campo mappato dal DTO base
    @Mapping(target = "version", ignore = true) // Campo gestito automaticamente
    @Mapping(target = "createdAt", ignore = true) // Campo automatico
    @Mapping(target = "modifiedAt", ignore = true) // Campo automatico
    @Mapping(target = "createdBy", ignore = true) // Campo automatico
    @Mapping(target = "modifiedBy", ignore = true) // Campo automatico
    @Mapping(target = "acceptedBy", ignore = true) // Campo gestito dal service
    @Mapping(target = "acceptedAt", ignore = true) // Campo gestito dal service
    Reservation fromNewDTO(AdminNewReservationDTO adminNewReservationDTO);

    /**
     * Aggiorna un'entità Reservation esistente con i dati dal AdminNewReservationDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "slot", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    void updateEntityFromNewDTO(AdminNewReservationDTO dto, @MappingTarget Reservation entity);
}
