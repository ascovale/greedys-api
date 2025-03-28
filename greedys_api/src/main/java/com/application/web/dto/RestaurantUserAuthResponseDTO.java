package com.application.web.dto;

import com.application.web.dto.get.RestaurantUserDTO;

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
