package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.customer.CustomerDTO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.web.dto.customer.NewCustomerDTO;

/**
 * MapStruct mapper per la conversione tra Customer e CustomerDTO/NewCustomerDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface CustomerMapper {


    /**
     * Converte un'entità Customer in CustomerDTO
     */
    @Mapping(target = "firstName", source = "name")
    @Mapping(target = "lastName", source = "surname")
    CustomerDTO toDTO(Customer customer);

    /**
     * Converte un NewCustomerDTO in entità Customer
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true) // La password sarà gestita dal service
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "customerOptions", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    Customer fromNewCustomerDTO(NewCustomerDTO newCustomerDTO);

    /**
     * Converte un CustomerDTO in entità Customer
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "customerOptions", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    Customer toEntity(CustomerDTO customerDTO);

    /**
     * Aggiorna un'entità Customer esistente con i dati dal CustomerDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "customerOptions", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    void updateEntityFromDTO(CustomerDTO dto, @MappingTarget Customer entity);

    /**
     * Aggiorna un'entità Customer esistente con i dati dal NewCustomerDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "customerOptions", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    void updateEntityFromNewDTO(NewCustomerDTO dto, @MappingTarget Customer entity);
}
