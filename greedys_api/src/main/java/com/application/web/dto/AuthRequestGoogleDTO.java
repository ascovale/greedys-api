package com.application.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;

@Schema(name = "AuthRequestGoogleDTO", description = "DTO for Google authentication requests")
@NoArgsConstructor
public class AuthRequestGoogleDTO {
    private String token;

    public AuthRequestGoogleDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}