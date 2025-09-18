package com.application.admin.web.dto.verification;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of admin verification process
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminVerificationResult {
    private boolean success;
    private String message;
    private String verificationId;
    
    // Verification details
    private String userEmail;
    private String placeId;
    private String restaurantName;
    private boolean ownershipVerified;
    private int confidenceScore; // 0-100
    
    // Verification methods used
    private List<String> verificationMethods;
    private String primaryVerificationMethod;
    
    // Google verification data
    private boolean googleBusinessProfileMatch;
    private boolean googleMapsOwnershipClaimed;
    private boolean emailMatchesBusinessContact;
    
    // Manual verification data
    private String adminNotes;
    private LocalDateTime verificationDate;
    private String verifiedBy;
    
    // Recommendations
    private boolean recommendApproval;
    private String rejectionReason;
    private List<String> additionalVerificationSteps;
}
