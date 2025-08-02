package com.application.restaurant.web.dto.search;

import java.util.List;

import com.application.restaurant.web.dto.google.RestaurantData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of restaurant search (multiple restaurants)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultipleRestaurantSearchResult {
    private boolean found;
    private String message;
    private List<RestaurantData> restaurants;
}
