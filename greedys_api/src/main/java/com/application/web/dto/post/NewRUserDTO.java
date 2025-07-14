package com.application.web.dto.post;

import com.application.controller.validators.ValidEmail;
import com.application.controller.validators.ValidPassword;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@Schema(name = "NewRUserDTO", description = "DTO for creating a new restaurant user")
public class NewRUserDTO {

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

    @NotNull
    private Long restaurantId;

    @NotNull
    private Long roleId;
   
}