package com.application.web.dto.get;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.application.controller.Validators.PasswordMatches;
import com.application.controller.Validators.ValidEmail;
import com.application.persistence.model.customer.Customer;

@PasswordMatches
public class UserDTO {

    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.firstName}")
    private String firstName;

    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.lastName}")
    private String lastName;

    @ValidEmail
    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.email}")
    private String email;

    @NotNull
    private Long id;

    public UserDTO(Customer user) {
        this.id = user.getId();
        this.firstName = user.getName();
        this.lastName = user.getSurname();
        this.email = user.getEmail();
    }

    public UserDTO() {
        //TODO Auto-generated constructor stub
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
        builder.append("UserDto [firstName=").append(firstName)
                .append(", lastName=").append(lastName)
                .append(", email=").append(email);
        return builder.toString();
    }

	public Long getId() {
		return id;
	}

}
