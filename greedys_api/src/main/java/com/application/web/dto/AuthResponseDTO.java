package com.application.web.dto;

import com.application.web.dto.get.UserDTO;

public class AuthResponseDTO {
    
    private String jwt;
    private UserDTO user;

    public AuthResponseDTO(String jwt, UserDTO user) {
        this.jwt = jwt;
        this.user = user;
    }

    public UserDTO getUser() {
        return user;
    }

    public String getJwt() {
        return jwt;
    }
}
