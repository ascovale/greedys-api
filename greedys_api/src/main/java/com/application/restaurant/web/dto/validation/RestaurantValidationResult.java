package com.application.restaurant.web.dto.validation;

import java.util.List;

import com.application.restaurant.web.dto.google.RestaurantData;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of restaurant data validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantValidationResult {
    
    /**
     * Whether the restaurant data is valid (no mismatches found)
     */
    private boolean valid;
    
    /**
     * General message about validation result
     */
    private String message;
    
    /**
     * Google Places ID of the restaurant
     */
    private String placeId;
    
    /**
     * Local restaurant data (from database)
     */
    private RestaurantData localRestaurant;
    
    /**
     * Google Places restaurant data (from API)
     */
    private RestaurantData googleRestaurant;
    
    /**
     * List of validation errors/mismatches found
     */
    private List<ValidationError> errors;
}
