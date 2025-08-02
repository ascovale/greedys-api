package com.application.admin.verification.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for user-restaurant associations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRestaurantAssociation {
    private Long id;
    private String userEmail;
    private String placeId;
    private String restaurantName;
    private String restaurantAddress;
    private boolean verified;
    private String verificationMethod; // "google_business_api", "pattern_search", "manual_verification", "admin_approved"
    private String verificationNotes;
    private String verifiedByAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime lastChecked;
    
    // Additional verification data
    private int confidenceScore; // 0-100
    private boolean requiresManualReview;
    private String adminComments;
}
