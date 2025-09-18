package com.application.restaurant.web.dto.restaurantGoogleDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for search requests (for testing only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDTO {
    private String restaurantName;
    private String address;
}
