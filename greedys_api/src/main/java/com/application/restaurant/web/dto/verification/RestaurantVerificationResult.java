package com.application.restaurant.web.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of restaurant verification process
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantVerificationResult {
    private boolean verified;
    private String message;
    private VerificationData data;
}
