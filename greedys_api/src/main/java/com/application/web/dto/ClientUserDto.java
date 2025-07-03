package com.application.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.application.controller.validators.PasswordMatches;
import com.application.controller.validators.ValidEmail;
import com.application.controller.validators.ValidPassword;

import io.swagger.v3.oas.annotations.media.Schema;

@PasswordMatches
@Schema(name = "ClientUserDto", description = "DTO for client user details")
public class ClientUserDto {
    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.firstName}")
    private String firstName;
    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.lastName}")
    private String lastName;
    @ValidPassword
    private String password;
    @NotNull
    @Size(min = 1)
    private String matchingPassword;
    @ValidEmail
    @NotNull
    @Size(min = 1, message = "{Size.companyUserDto.email}")
    private String email;    

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    private Integer role;

    public Integer getRole() {
        return role;
    }

    public void setRole(final Integer role) {
        this.role = role;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getMatchingPassword() {
        return matchingPassword;
    }

    public void setMatchingPassword(final String matchingPassword) {
        this.matchingPassword = matchingPassword;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CompanyUserDto [firstName=").append(firstName).append(", lastName=").append(lastName).append(", password=").append(password).append(", matchingPassword=").append(matchingPassword).append(", email=").append(email)
                .append(", role=").append(role).append("]");
        return builder.toString();
    }

}
