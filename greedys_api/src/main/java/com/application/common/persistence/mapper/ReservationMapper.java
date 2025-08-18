package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.admin.web.dto.reservation.AdminNewReservationDTO;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.reservations.NewBaseReservationDTO;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.web.dto.reservations.CustomerNewReservationDTO;
import com.application.restaurant.web.dto.reservation.RestaurantNewReservationDTO;

/**
 * MapStruct mapper per la conversione tra Reservation e i vari DTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ReservationMapper {

    /**
     * Converte un'entità Reservation in ReservationDTO
     */
    @Mapping(target = "name", source = "userName")
    @Mapping(target = "reservationDay", source = "date")
    @Mapping(target = "restaurant", source = "slot.service.restaurant.id")
    @Mapping(target = "phone", ignore = true) // Non presente in entità
    @Mapping(target = "email", ignore = true) // Non presente in entità
    @Mapping(target = "slot.service.restaurantId", source = "slot.service.restaurant.id")
    @Mapping(target = "slot.service.serviceType", ignore = true) // Troppo complesso per MapStruct
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "createdByUserType", source = "createdByUserType")
    @Mapping(target = "createdAt", source = "createdAt")
    ReservationDTO toDTO(Reservation reservation);

    /**
     * Converte un CustomerNewReservationDTO in entità Reservation
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", source = "userName")
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "slot", ignore = true) // Sarà gestito dal service
    @Mapping(target = "restaurant", ignore = true) // Sarà gestito dal service
    @Mapping(target = "customer", ignore = true) // Sarà gestito dal service
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "createdByUserType", ignore = true)
    @Mapping(target = "modifiedByUserType", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    Reservation fromCustomerNewReservationDTO(CustomerNewReservationDTO dto);

    /**
     * Converte un RestaurantNewReservationDTO in entità Reservation
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", source = "userName")
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "slot", ignore = true) // Sarà gestito dal service
    @Mapping(target = "restaurant", ignore = true) // Sarà gestito dal service
    @Mapping(target = "customer", ignore = true) // Sarà gestito dal service
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "createdByUserType", ignore = true)
    @Mapping(target = "modifiedByUserType", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    Reservation fromRestaurantNewReservationDTO(RestaurantNewReservationDTO dto);

    /**
     * Converte un AdminNewReservationDTO in entità Reservation
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", source = "userName")
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "slot", ignore = true) // Sarà gestito dal service
    @Mapping(target = "restaurant", ignore = true) // Sarà gestito dal service
    @Mapping(target = "customer", ignore = true) // Sarà gestito dal service
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "createdByUserType", ignore = true)
    @Mapping(target = "modifiedByUserType", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    Reservation fromAdminNewReservationDTO(AdminNewReservationDTO dto);

    /**
     * Metodo generico per mappare dalla classe base NewBaseReservationDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", source = "userName")
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "slot", ignore = true) // Sarà gestito dal service
    @Mapping(target = "restaurant", ignore = true) // Sarà gestito dal service
    @Mapping(target = "customer", ignore = true) // Sarà gestito dal service
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "createdByUserType", ignore = true)
    @Mapping(target = "modifiedByUserType", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    Reservation fromNewBaseReservationDTO(NewBaseReservationDTO dto);

    /**
     * Aggiorna un'entità Reservation esistente con i dati dal ReservationDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", source = "name")
    @Mapping(target = "date", source = "reservationDay")
    @Mapping(target = "slot", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "table", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "createdByUserType", ignore = true)
    @Mapping(target = "modifiedByUserType", ignore = true)
    @Mapping(target = "acceptedBy", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    void updateEntityFromDTO(ReservationDTO dto, @MappingTarget Reservation entity);
}
