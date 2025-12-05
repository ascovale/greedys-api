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
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "bookedServiceName", ignore = true)
    @Mapping(target = "bookedSlotDuration", ignore = true)
    @Mapping(target = "bookedOpeningTime", ignore = true)
    @Mapping(target = "bookedClosingTime", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "date", ignore = true)
    @Mapping(target = "reservationDateTime", source = "reservationDateTime")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    Reservation fromNewDTO(AdminNewReservationDTO adminNewReservationDTO);

    /**
     * Aggiorna un'entità Reservation esistente con i dati dal AdminNewReservationDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "bookedServiceName", ignore = true)
    @Mapping(target = "bookedSlotDuration", ignore = true)
    @Mapping(target = "bookedOpeningTime", ignore = true)
    @Mapping(target = "bookedClosingTime", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "date", ignore = true)
    @Mapping(target = "reservationDateTime", source = "reservationDateTime")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    void updateEntityFromNewDTO(AdminNewReservationDTO dto, @MappingTarget Reservation entity);
}
