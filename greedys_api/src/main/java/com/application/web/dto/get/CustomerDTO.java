package com.application.web.dto.get;

import com.application.controller.Validators.PasswordMatches;
import com.application.controller.Validators.ValidEmail;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Customer.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@PasswordMatches
@Schema(description = "DTO representing customer details")
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

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CustomerDto [firstName=").append(firstName)
                .append(", lastName=").append(lastName)
                .append(", email=").append(email);
        return builder.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
