package com.application.common.web.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "AuthRequestDTO", description = "DTO for authentication requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {
    @Schema(description = "Nome utente", example = "user123")
    private String username;

    @Schema(description = "Password dell'utente", example = "password123")
    private String password;
}
