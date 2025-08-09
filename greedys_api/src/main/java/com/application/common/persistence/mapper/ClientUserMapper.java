package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.customer.ClientUserDto;
import com.application.customer.persistence.model.Customer;

/**
 * MapStruct mapper per la conversione tra Customer e ClientUserDto
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ClientUserMapper {


    /**
     * Converte un'entità Customer in ClientUserDto
     */
    @Mapping(target = "firstName", source = "name")
    @Mapping(target = "lastName", source = "surname")
    @Mapping(target = "password", ignore = true) // Mai esporre password
    @Mapping(target = "matchingPassword", ignore = true) // Non mappato
    ClientUserDto toDTO(Customer customer);

    /**
     * Converte un ClientUserDto in entità Customer
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true) // Gestito separatamente per sicurezza
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "allergies", ignore = true) // Non mappato nei DTO base
    @Mapping(target = "reservations", ignore = true) // Non mappato nei DTO base
    Customer toEntity(ClientUserDto clientUserDto);

    /**
     * Aggiorna un'entità Customer esistente con i dati dal ClientUserDto
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    void updateEntityFromDTO(ClientUserDto dto, @MappingTarget Customer entity);
}
