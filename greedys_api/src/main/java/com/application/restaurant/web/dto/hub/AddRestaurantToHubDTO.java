package com.application.restaurant.web.dto.hub;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per aggiungere un ristorante ad un Hub User
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddRestaurantToHubDTO {
    
    @NotNull(message = "Restaurant ID is required")
    @Positive(message = "Restaurant ID must be positive")
    private Long restaurantId;
    
    @NotNull(message = "Role ID is required") 
    @Positive(message = "Role ID must be positive")
    private Long roleId;
}
