package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.reservation.AvailabilityException;
import com.application.common.web.dto.restaurant.AvailabilityExceptionDTO;

/**
 * MapStruct mapper for converting between AvailabilityException entity and AvailabilityExceptionDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AvailabilityExceptionMapper {

    /**
     * Convert AvailabilityException entity to AvailabilityExceptionDTO
     */
    @Mapping(target = "serviceVersionId", source = "serviceVersion.id")
    @Mapping(target = "exceptionType", expression = "java(entity.getExceptionType() != null ? entity.getExceptionType().toString() : null)")
    AvailabilityExceptionDTO toDTO(AvailabilityException entity);

    /**
     * Convert AvailabilityExceptionDTO to AvailabilityException entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "serviceVersion", ignore = true) // Will be set by service layer
    @Mapping(target = "exceptionType", ignore = true) // Will be set by service layer
    @Mapping(target = "createdAt", ignore = true) // Will be set by service layer
    @Mapping(target = "updatedAt", ignore = true) // Will be set by service layer
    AvailabilityException toEntity(AvailabilityExceptionDTO dto);

    /**
     * Update existing AvailabilityException entity with data from AvailabilityExceptionDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "serviceVersion", ignore = true)
    @Mapping(target = "exceptionType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(AvailabilityExceptionDTO dto, @MappingTarget AvailabilityException entity);

}
