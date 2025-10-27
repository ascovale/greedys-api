package com.application.common.web.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "AuthResponseDTO", description = "DTO for authentication responses")
@Data
@Builder
@NoArgsConstructor
public class AuthResponseDTO {
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String jwt;
    
    @Schema(description = "JWT refresh token for remember me functionality", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    @Schema(description = "Authenticated user information - RUserDTO, RUserHubDTO, AdminDTO, or CustomerDTO")
    private UserAuthResponse user;
    
    /**
     * Constructor for backward compatibility with existing code.
     * Accepts UserAuthResponse (RUserDTO, RUserHubDTO, AdminDTO, CustomerDTO, etc.)
     */
    public AuthResponseDTO(String jwt, UserAuthResponse user) {
        this.jwt = jwt;
        this.user = user;
    }
    
    /**
     * Constructor with refresh token
     */
    public AuthResponseDTO(String jwt, String refreshToken, UserAuthResponse user) {
        this.jwt = jwt;
        this.refreshToken = refreshToken;
        this.user = user;
    }
}
