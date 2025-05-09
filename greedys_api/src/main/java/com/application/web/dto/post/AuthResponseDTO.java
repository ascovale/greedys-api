package com.application.web.dto.post;

import com.application.web.dto.get.CustomerDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponseDTO", description = "DTO for authentication responses")
public class AuthResponseDTO {
    
    private String jwt;
    private CustomerDTO user;

    public AuthResponseDTO(String jwt, CustomerDTO user) {
        this.jwt = jwt;
        this.user = user;
    }

    public CustomerDTO getUser() {
        return user;
    }

    public String getJwt() {
        return jwt;
    }
}
