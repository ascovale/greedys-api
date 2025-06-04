package com.application.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponseDTO", description = "DTO for authentication responses")
public class AuthResponseDTO {
    private String jwt;
    private Object user;

    public AuthResponseDTO(String jwt, Object user) {
        this.jwt = jwt;
        this.user = user;
    }

    public Object getUser() {
        return user;
    }

    public String getJwt() {
        return jwt;
    }
}
