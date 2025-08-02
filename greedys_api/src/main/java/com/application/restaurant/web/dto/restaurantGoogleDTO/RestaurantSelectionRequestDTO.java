package com.application.restaurant.web.dto.restaurantGoogleDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to select a specific restaurant (STEP 2)
 * Uses the same Access Token from STEP 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSelectionRequestDTO {
    private String accessToken;    // Same token from STEP 1 with all scopes
    private String email;
    private String placeId;
}
