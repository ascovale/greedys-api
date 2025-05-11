package com.application.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthRequestDTO", description = "DTO for authentication requests")
public class AuthRequestDTO {
    @Schema(description = "Nome utente", example = "user123")
    private String username;

    @Schema(description = "Password dell'utente", example = "password123")
    private String password;

    public AuthRequestDTO() {}

    public AuthRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}