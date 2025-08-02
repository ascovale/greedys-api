package com.application.restaurant.web.dto.restaurantGoogleDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to request the list of managed restaurants (STEP 1)
 * Uses a single Access Token with all necessary scopes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRestaurantsRequestDTO {
    private String accessToken;    // Token with scopes: userinfo.email, userinfo.profile, business.manage
    private String email;
}
