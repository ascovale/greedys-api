package com.application.common.web.dto.customer;

import com.application.common.controller.validators.PasswordMatches;
import com.application.common.controller.validators.ValidEmail;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.Customer.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PasswordMatches
@Schema(name = "CustomerDTO", description = "DTO for customer details")
public class CustomerDTO {

    @Schema(description = "ID of the customer", example = "1")
    @NotNull
    private Long id;

    @Schema(description = "First name of the customer", example = "John")
    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.firstName}")
    private String firstName;

    @Schema(description = "Last name of the customer", example = "Doe")
    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.lastName}")
    private String lastName;

    @Schema(description = "Email of the customer", example = "john.doe@example.com")
    @ValidEmail
    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.email}")
    private String email;

    @Schema(description = "Status of the customer", example = "ACTIVE")
    private Status status;

    public CustomerDTO(Customer customer) {
        this.id = customer.getId();
        this.firstName = customer.getName();
        this.lastName = customer.getSurname();
        this.email = customer.getEmail();
        this.status = customer.getStatus();
    }
}
