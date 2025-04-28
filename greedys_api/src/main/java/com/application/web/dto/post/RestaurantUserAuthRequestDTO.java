package com.application.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO per la richiesta di autenticazione")
public class RestaurantUserAuthRequestDTO {
    @Schema(description = "Nome utente", example = "user123")
    private String username;

    private Long restaurantId;

    @Schema(description = "Password dell'utente", example = "password123")
    private String password;

    public RestaurantUserAuthRequestDTO() {}

    public RestaurantUserAuthRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public RestaurantUserAuthRequestDTO(String username, String password,Long restaurantId) {
        this.restaurantId = restaurantId;
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

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }
}