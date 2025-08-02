package com.application.restaurant.web.dto.verification;

import java.util.List;

import com.application.restaurant.web.dto.google.OwnerData;
import com.application.restaurant.web.dto.google.RestaurantData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete verification data containing restaurant and owner information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationData {
    private RestaurantData restaurant;
    private OwnerData owner;
    private List<RestaurantData> availableRestaurants; // List of available restaurants when there's ambiguity
    
    public VerificationData(RestaurantData restaurant, OwnerData owner) {
        this.restaurant = restaurant;
        this.owner = owner;
        this.availableRestaurants = null;
    }
}
