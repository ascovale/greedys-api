package com.application.web.dto;

import com.application.web.dto.get.CustomerDTO;

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
