package com.application.admin.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.persistence.model.Admin;
import com.application.admin.web.dto.verification.AdminVerificationResult;
import com.application.admin.web.dto.verification.UserRestaurantAssociation;
import com.application.common.controller.BaseController;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.restaurant.service.google.RestaurantDataValidationService;
import com.application.restaurant.service.google.RestaurantGooglePlacesService;
import com.application.restaurant.web.dto.restaurantGoogleDTO.AdminVerificationRequestDTO;
import com.application.restaurant.web.dto.restaurantGoogleDTO.RestaurantAssociationRequestDTO;
import com.application.restaurant.web.dto.validation.RestaurantValidationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/restaurant/verification")
@CrossOrigin(origins = "*")
@Tag(name = "Admin Restaurant Verification", description = "Admin API for managing restaurant ownership verification. " +
     "These endpoints allow administrators to verify, approve, or reject user-restaurant associations.")
public class AdminRestaurantVerificationController extends BaseController {

    private final RestaurantGooglePlacesService googlePlacesService;
    private final RestaurantDataValidationService restaurantDataValidationService;

    /**
     * Admin endpoint: Verify restaurant email correlation
     * Analyzes if user email correlates with restaurant contact data
     */
    @Operation(
        summary = "Verify restaurant email correlation", 
        description = "Verifies if user email correlates with restaurant contact information. " +
                     "Uses website domain matching, phone area correlation, and name pattern analysis."
    )
    @PostMapping("/verify-email")
    public ResponseWrapper<AdminVerificationResult> verifyRestaurantEmail(
            @RequestBody RestaurantAssociationRequestDTO request) {
        
        return execute("verify restaurant email", "Email verification analysis completed", 
            new OperationSupplier<AdminVerificationResult>() {
                @Override
                public AdminVerificationResult get() {
                    return googlePlacesService.verifyRestaurantEmail(
                        request.getUserEmail(),
                        request.getPlaceId(),
                        getCurrentAdminEmail()
                    );
                }
            });
    }

    /**
     * Admin endpoint: Find restaurant contact email
     * Attempts to find restaurant contact email through available sources
     */
    @Operation(
        summary = "Find restaurant contact email", 
        description = "Attempts to find restaurant contact email through website analysis. " +
                     "Provides manual verification guidance when automated methods are limited."
    )
    @PostMapping("/find-contact-email")
    public ResponseWrapper<AdminVerificationResult> findRestaurantContactEmail(
            @RequestBody RestaurantAssociationRequestDTO request) {
        
        return execute("find restaurant contact email", "Contact email search completed", 
            new OperationSupplier<AdminVerificationResult>() {
                @Override
                public AdminVerificationResult get() {
                    return googlePlacesService.findRestaurantContactEmail(
                        request.getPlaceId(),
                        getCurrentAdminEmail()
                    );
                }
            });
    }

    /**
     * Admin endpoint: Analyze restaurant ownership verification (PATTERN MATCHING)
     * Performs automated analysis to help admin make verification decisions
     */
    @Operation(
        summary = "Analyze restaurant ownership", 
        description = "Performs automated verification analysis to determine if a user likely owns/manages a restaurant. " +
                     "Returns confidence score and recommendations for admin decision."
    )
    @PostMapping("/analyze-ownership")
    public ResponseWrapper<AdminVerificationResult> analyzeRestaurantOwnership(
            @RequestBody RestaurantAssociationRequestDTO request) {
        
        return execute("analyze restaurant ownership", "Ownership analysis completed", 
            new OperationSupplier<AdminVerificationResult>() {
                @Override
                public AdminVerificationResult get() {
                    return googlePlacesService.verifyRestaurantOwnership(
                        request.getUserEmail(),
                        request.getPlaceId(),
                        getCurrentAdminEmail() 
                    );
                }
            });
    }

    /**
     * Admin endpoint: Approve or reject restaurant association
     * Final admin decision to approve or reject a user-restaurant association
     */
    @Operation(
        summary = "Approve/Reject restaurant association", 
        description = "Admin decision to approve or reject a user-restaurant association. " +
                     "This creates a permanent record of the verification decision."
    )
    @PostMapping("/approve-association")
    public ResponseWrapper<AdminVerificationResult> approveRestaurantAssociation(
            @RequestBody AdminVerificationRequestDTO request) {
        
        return execute("approve restaurant association", "Verification decision recorded", 
            new OperationSupplier<AdminVerificationResult>() {
                @Override
                public AdminVerificationResult get() {
                    return googlePlacesService.approveRestaurantAssociation(
                        request.getUserEmail(),
                        request.getPlaceId(),
                        request.isApproved(),
                        request.getAdminEmail() != null ? request.getAdminEmail() : getCurrentAdminEmail(),
                        request.getVerificationNotes()
                    );
                }
            });
    }

    /**
     * Admin endpoint: Get pending restaurant verifications
     * Returns list of restaurant associations that need admin review
     */
    @Operation(
        summary = "Get pending verifications", 
        description = "Returns list of user-restaurant associations that are pending admin verification. " +
                     "Admins can review these and make approval/rejection decisions."
    )
    @GetMapping("/pending")
    public ListResponseWrapper<UserRestaurantAssociation> getPendingVerifications() {
        
        return executeList("get pending verifications", () -> googlePlacesService.getPendingVerifications());
    }

    /**
     * Admin endpoint: Validate restaurant data against Google Places
     * Validates a restaurant by ID against Google Places API and saves results
     */
    @Operation(
        summary = "Validate restaurant by ID", 
        description = "Validates a restaurant's data against Google Places API by restaurant ID. " +
                     "Compares local data with Google Places data and saves validation results."
    )
    @PostMapping("/validate-restaurant/{restaurantId}")
    public ResponseWrapper<RestaurantValidationResult> validateRestaurantById(
            @PathVariable Long restaurantId) {
        
        return execute("validate restaurant with ID", "Restaurant validation completed", 
            new OperationSupplier<RestaurantValidationResult>() {
                @Override
                public RestaurantValidationResult get() {
                    return restaurantDataValidationService.validateRestaurant(restaurantId,getCurrentAdmin());
                }
            });
    }

    /**
     * Helper method to get current admin object from security context.
     * Returns null if not authenticated or not an admin.
     */
    private Admin getCurrentAdmin() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return (Admin) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * User endpoint: Request restaurant association
     * Allows users to request association with a restaurant for admin verification
     */
    @Operation(
        summary = "Request restaurant association", 
        description = "User request to associate their account with a restaurant. " +
                     "This creates a pending verification request for admin review."
    )
    @PostMapping("/request-association")
    // No @PreAuthorize - users can request associations
    public ResponseWrapper<AdminVerificationResult> requestRestaurantAssociation(
            @RequestBody RestaurantAssociationRequestDTO request) {
        
        return execute("request restaurant association", "Association request submitted", 
            new OperationSupplier<AdminVerificationResult>() {
                @Override
                public AdminVerificationResult get() {
                    // First, analyze the ownership automatically
                    AdminVerificationResult analysis = googlePlacesService.verifyRestaurantOwnership(
                        request.getUserEmail(),
                        request.getPlaceId(),
                        "SYSTEM_AUTO_ANALYSIS"
                    );
                    
                    
                    if (analysis.getConfidenceScore() >= 90) {
                        // Auto-approve high confidence requests
                        return googlePlacesService.approveRestaurantAssociation(
                            request.getUserEmail(),
                            request.getPlaceId(),
                            true,
                            "SYSTEM_AUTO_APPROVAL",
                            "Auto-approved based on high confidence score: " + analysis.getConfidenceScore()
                        );
                    } else {
                        // Mark as pending review
                        return AdminVerificationResult.builder()
                            .success(true)
                            .message("Association request submitted for admin review")
                            .userEmail(request.getUserEmail())
                            .placeId(request.getPlaceId())
                            .confidenceScore(analysis.getConfidenceScore())
                            .recommendApproval(analysis.isRecommendApproval())
                            .adminNotes("Pending admin review - confidence score: " + analysis.getConfidenceScore())
                            .build();
                    }
                }
            });
    }

    /**
     * Helper method to get current admin's email from security context
     */
    private String getCurrentAdminEmail() {
        // This assumes you are using Spring Security and the admin's email is the username (principal)
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }
        }
        return null;
    }
}
