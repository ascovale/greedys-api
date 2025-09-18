package com.application.restaurant.web.dto.restaurantGoogleDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for admin verification requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminVerificationRequestDTO {
    private String userEmail;
    private String placeId;
    private String restaurantName;
    private String adminEmail; 
    private String verificationNotes;
    private boolean approved;
}
