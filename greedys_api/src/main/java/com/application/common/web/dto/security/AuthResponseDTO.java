package com.application.common.web.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "AuthResponseDTO", description = "DTO for authentication responses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String jwt;
    
    @Schema(description = "JWT refresh token for remember me functionality", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    @Schema(description = "User information")
    private Object user;
    
    // Constructor per compatibilità con codice esistente
    public AuthResponseDTO(String jwt, Object user) {
        this.jwt = jwt;
        this.user = user;
    }
}
