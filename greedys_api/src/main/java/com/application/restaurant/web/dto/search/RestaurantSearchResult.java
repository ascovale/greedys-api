package com.application.restaurant.web.dto.search;

import com.application.restaurant.web.dto.google.RestaurantData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of restaurant search (single restaurant)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSearchResult {
    private boolean found;
    private String message;
    private RestaurantData restaurant;
}
