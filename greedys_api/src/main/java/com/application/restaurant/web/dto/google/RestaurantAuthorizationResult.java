package com.application.restaurant.web.dto.google;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of restaurant authorization process (STEP 1)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAuthorizationResult {
    private boolean success;
    private String message;
    private OwnerData owner;
    private List<RestaurantData> authorizedRestaurants;
}
