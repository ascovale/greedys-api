package com.application.web.dto;

import com.application.web.dto.get.RestaurantUserDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RestaurantUserAuthResponseDTO", description = "DTO for restaurant user authentication response")
public class RestaurantUserAuthResponseDTO {
    
    private String jwt;
    private RestaurantUserDTO user;

    public RestaurantUserAuthResponseDTO(String jwt, RestaurantUserDTO user) {
        this.jwt = jwt;
        this.user = user;
    }

    public RestaurantUserDTO getUser() {
        return user;
    }

    public String getJwt() {
        return jwt;
    }
}
