package com.application.restaurant.web.dto.restaurantGoogleDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user restaurant association requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAssociationRequestDTO {
    private String userEmail;
    private String placeId;
    private String accessToken;
    private String verificationMethod; // "pattern_search", "manual_selection", "claim_verification"
    private String notes;
}
