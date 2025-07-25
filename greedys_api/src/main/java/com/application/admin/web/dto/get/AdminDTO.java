package com.application.admin.web.dto.get;

import com.application.admin.persistence.model.Admin;
import com.application.common.controller.validators.PasswordMatches;
import com.application.common.controller.validators.ValidEmail;

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
@Schema(name = "AdminDTO", description = "DTO for admin details")
public class AdminDTO {

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

    public AdminDTO(Admin user) {
        this.id = user.getId();
        this.firstName = user.getName();
        this.lastName = user.getSurname();
        this.email = user.getEmail();
    }
}
