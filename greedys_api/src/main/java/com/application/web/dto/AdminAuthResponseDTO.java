package com.application.web.dto;

import com.application.web.dto.get.AdminDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminAuthResponseDTO", description = "DTO for admin authentication response")
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
