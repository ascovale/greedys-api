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
 * MapStruct mapper per la conversione tra Customer e CustomerDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface CustomerDTOMapper {


    /**
     * Converte un'entità Customer in CustomerDTO
     */
    @Mapping(target = "firstName", source = "name")
    @Mapping(target = "lastName", source = "surname")
    CustomerDTO toDTO(Customer customer);

    /**
     * Converte un CustomerDTO in entità Customer
     */
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "nickName", ignore = true) // Non presente nel DTO
    @Mapping(target = "phoneNumber", ignore = true) // Non presente nel DTO
    @Mapping(target = "toReadNotification", ignore = true) // Valore di default
    @Mapping(target = "password", ignore = true) // Sarà gestita separatamente
    @Mapping(target = "allergies", ignore = true) // Sarà gestito dal service
    @Mapping(target = "reservations", ignore = true) // Sarà gestito dal service
    @Mapping(target = "reservationAsGuest", ignore = true) // Sarà gestito dal service
    @Mapping(target = "customerOptions", ignore = true) // Sarà gestito dal service
    @Mapping(target = "dateOfBirth", ignore = true) // Non presente nel DTO
    @Mapping(target = "roles", ignore = true) // Sarà gestito dal service
    @Mapping(target = "createdAt", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "createdBy", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "modifiedAt", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "modifiedBy", ignore = true) // Gestito da CustomAuditingEntityListener
    Customer toEntity(CustomerDTO customerDTO);

    /**
     * Converte un NewCustomerDTO in entità Customer (per creazione)
     */
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "id", ignore = true) // Generato automaticamente
    @Mapping(target = "nickName", ignore = true) // Non presente nel DTO
    @Mapping(target = "phoneNumber", ignore = true) // Non presente nel DTO
    @Mapping(target = "toReadNotification", ignore = true) // Valore di default
    @Mapping(target = "password", ignore = true) // Sarà gestita separatamente nel service
    @Mapping(target = "allergies", ignore = true) // Sarà gestito dal service
    @Mapping(target = "reservations", ignore = true) // Sarà gestito dal service
    @Mapping(target = "reservationAsGuest", ignore = true) // Sarà gestito dal service
    @Mapping(target = "customerOptions", ignore = true) // Sarà gestito dal service
    @Mapping(target = "dateOfBirth", ignore = true) // Non presente nel DTO
    @Mapping(target = "roles", ignore = true) // Sarà gestito dal service
    @Mapping(target = "status", ignore = true) // Sarà impostato dal service
    @Mapping(target = "createdAt", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "createdBy", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "modifiedAt", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "modifiedBy", ignore = true) // Gestito da CustomAuditingEntityListener
    Customer toEntity(NewCustomerDTO newCustomerDTO);

    /**
     * Aggiorna un'entità Customer esistente con i dati dal NewCustomerDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "password", ignore = true) // Sarà gestita separatamente
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "reservationAsGuest", ignore = true)
    @Mapping(target = "customerOptions", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "authorities", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "privileges", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "privilegesStrings", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "createdAt", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "createdBy", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "modifiedAt", ignore = true) // Gestito da CustomAuditingEntityListener
    @Mapping(target = "modifiedBy", ignore = true) // Gestito da CustomAuditingEntityListener
    void updateEntityFromDTO(NewCustomerDTO dto, @MappingTarget Customer entity);

    /**
     * Aggiorna un'entità Customer esistente con i dati dal CustomerDTO
     
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "firstName")
    @Mapping(target = "surname", source = "lastName")
    @Mapping(target = "nickName", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "toReadNotification", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "customerOptions", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "authorities", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "privileges", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "privilegesStrings", ignore = true) // Metodo derivato dall'AbstractUser
    @Mapping(target = "roles", ignore = true) // Metodo derivato dall'AbstractUser
    void updateEntityFromDTO(CustomerDTO dto, @MappingTarget Customer entity);
    */
}
