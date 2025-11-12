package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.reservation.ServiceType;
import com.application.common.web.dto.restaurant.ServiceTypeDto;

/**
 * MapStruct mapper per la conversione tra ServiceType e ServiceTypeDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ServiceTypeMapper {


    /**
     * Converte un'entità ServiceType in ServiceTypeDto
     */
    ServiceTypeDto toDTO(ServiceType serviceType);

    /**
     * Converte un ServiceTypeDto in entità ServiceType
     */
    @Mapping(target = "id", ignore = true)
    ServiceType toEntity(ServiceTypeDto serviceTypeDto);

    /**
     * Aggiorna un'entità ServiceType esistente con i dati dal ServiceTypeDto
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDTO(ServiceTypeDto dto, @MappingTarget ServiceType entity);
}
