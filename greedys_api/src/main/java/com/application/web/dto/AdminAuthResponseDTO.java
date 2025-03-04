package com.application.web.dto;

import com.application.web.dto.get.AdminDTO;

public class AdminAuthResponseDTO {
    
    private String jwt;
    private AdminDTO user;

    public AdminAuthResponseDTO(String jwt, AdminDTO user) {
        this.jwt = jwt;
        this.user = user;
    }

    public AdminDTO getUser() {
        return user;
    }

    public String getJwt() {
        return jwt;
    }
}
