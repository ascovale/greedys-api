package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.RestaurantReservationDto;

/**
 * MapStruct mapper per la conversione tra Reservation e RestaurantReservationDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RestaurantReservationMapper {

    RestaurantReservationMapper INSTANCE = Mappers.getMapper(RestaurantReservationMapper.class);

    /**
     * Converte un'entità Reservation in RestaurantReservationDto
     */
    @Mapping(target = "restaurantId", source = "slot.service.restaurant.id")
    @Mapping(target = "serviceId", source = "slot.service.id")
    @Mapping(target = "slotId", source = "slot.id")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(reservation.getCustomer().getFirstName() + \" \" + reservation.getCustomer().getLastName())")
    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "serviceName", source = "slot.service.name")
    @Mapping(target = "slotStart", source = "slot.start")
    @Mapping(target = "slotEnd", source = "slot.end")
    @Mapping(target = "slotWeekday", source = "slot.weekday")
    RestaurantReservationDto toDTO(Reservation reservation);

    /**
     * Converte un RestaurantReservationDto in entità Reservation
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slot", ignore = true) // Sarà impostato dal service
    @Mapping(target = "customer", ignore = true) // Sarà impostato dal service
    @Mapping(target = "status", ignore = true) // Sarà gestito dal service
    @Mapping(target = "tables", ignore = true) // Sarà gestito dal service
    @Mapping(target = "deleted", ignore = true) // Campo gestito dal service
    @Mapping(target = "createdAt", ignore = true) // Campo automatico
    @Mapping(target = "updatedAt", ignore = true) // Campo automatico
    Reservation toEntity(RestaurantReservationDto restaurantReservationDto);

    /**
     * Aggiorna un'entità Reservation esistente con i dati dal RestaurantReservationDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slot", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tables", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(RestaurantReservationDto dto, @MappingTarget Reservation entity);
}
