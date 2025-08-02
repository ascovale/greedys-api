package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.admin.web.dto.communication.EmailRequestDTO;

/**
 * MapStruct mapper per EmailRequestDTO
 * Questo DTO è principalmente utilizzato per richieste di invio email e non ha un'entità corrispondente
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface EmailRequestMapper {

    EmailRequestMapper INSTANCE = Mappers.getMapper(EmailRequestMapper.class);

    /**
     * Clona un EmailRequestDTO in un altro EmailRequestDTO
     * Utile per trasformazioni o copie
     */
    EmailRequestDTO clone(EmailRequestDTO original);
}
