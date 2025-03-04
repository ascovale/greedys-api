package com.application.web.dto.post.admin;

import com.application.controller.Validators.ValidEmail;
import com.application.controller.Validators.ValidPassword;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NewAdminDTO {

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


}
