package com.application.common.web.dto.put;

import com.application.common.controller.validators.ValidPassword;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "UpdatePasswordDTO", description = "DTO for updating a user's password")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordDTO {

    private String oldPassword;

    @ValidPassword
    private String newPassword;

    private String email;
}