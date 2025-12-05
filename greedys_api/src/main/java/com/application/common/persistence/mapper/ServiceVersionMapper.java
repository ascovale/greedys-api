package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.reservation.ServiceVersion;
import com.application.common.web.dto.restaurant.ServiceVersionDTO;

/**
 * MapStruct mapper for converting between ServiceVersion entity and ServiceVersionDTO
 * 
 * NOTE: ServiceVersion is now used for temporal scheduling only (effectiveFrom/To dates).
 * Reservations no longer reference ServiceVersion directly - they reference Service + snapshot fields.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ServiceVersionMapper {

    /**
     * Convert ServiceVersion entity to ServiceVersionDTO
     */
    @Mapping(target = "serviceId", source = "service.id")
    @Mapping(target = "serviceName", source = "service.name")
    @Mapping(target = "state", expression = "java(entity.getState() != null ? entity.getState().toString() : null)")
    ServiceVersionDTO toDTO(ServiceVersion entity);

    /**
     * Convert ServiceVersionDTO to ServiceVersion entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true) // Will be set by service layer
    @Mapping(target = "state", ignore = true) // Will be set by service layer
    @Mapping(target = "createdAt", ignore = true) // Will be set by service layer
    @Mapping(target = "updatedAt", ignore = true) // Will be set by service layer
    @Mapping(target = "availabilityExceptions", ignore = true) // Not mapped in basic DTO
    @Mapping(target = "serviceDays", ignore = true) // Not mapped in basic DTO
    @Mapping(target = "slotConfigs", ignore = true) // Not mapped in basic DTO
    @Mapping(target = "openingHours", ignore = true) // Legacy JSON field, replaced by serviceDays
    ServiceVersion toEntity(ServiceVersionDTO dto);

    /**
     * Update existing ServiceVersion entity with data from ServiceVersionDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "availabilityExceptions", ignore = true)
    @Mapping(target = "serviceDays", ignore = true)
    @Mapping(target = "slotConfigs", ignore = true)
    @Mapping(target = "openingHours", ignore = true)
    void updateEntityFromDTO(ServiceVersionDTO dto, @MappingTarget ServiceVersion entity);

}
