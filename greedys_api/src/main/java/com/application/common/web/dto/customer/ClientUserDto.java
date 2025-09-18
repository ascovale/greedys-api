package com.application.common.web.dto.customer;

import com.application.common.controller.validators.PasswordMatches;
import com.application.common.controller.validators.ValidEmail;
import com.application.common.controller.validators.ValidPassword;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PasswordMatches
@Schema(name = "ClientUserDto", description = "DTO for client user details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private Integer role;
}
