package com.application.restaurant.web.controller.verification;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.ApiResponse;
import com.application.restaurant.service.verification.RestaurantTwilioVerificationService;
import com.application.restaurant.web.dto.verification.CodeVerificationRequestDTO;
import com.application.restaurant.web.dto.verification.VerificationRequestDTO;
import com.application.restaurant.web.dto.verification.VerificationResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for restaurant phone verification operations
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/restaurant/verification")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Restaurant Verification", description = "Restaurant phone number verification operations")
public class RestaurantVerificationController {

    private final RestaurantTwilioVerificationService verificationService;

    @Operation(
        summary = "Initiate phone verification",
        description = "Sends an OTP code to the restaurant's phone number for verification"
    )
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> initiateVerification(
            @Valid @RequestBody VerificationRequestDTO request) {
        
        log.info("Received verification initiation request for restaurant ID: {}", request.getRestaurantId());
        
        try {
            VerificationResponseDTO response = verificationService.initiatePhoneVerification(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response, "Verification initiated successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMessage(), "VERIFICATION_FAILED"));
            }
            
        } catch (Exception e) {
            log.error("Error initiating verification for restaurant ID: {}", request.getRestaurantId(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to initiate verification: " + e.getMessage(), "VERIFICATION_ERROR"));
        }
    }

    @Operation(
        summary = "Verify OTP code",
        description = "Verifies the OTP code sent to the restaurant's phone number"
    )
    @PostMapping("/{restaurantId}/verify")
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> verifyCode(
            @Parameter(description = "Restaurant ID") 
            @PathVariable @NotNull Long restaurantId,
            
            @Parameter(description = "OTP verification code") 
            @RequestParam @NotBlank String code) {
        
        log.info("Received verification code for restaurant ID: {}", restaurantId);
        
        try {
            VerificationResponseDTO response = verificationService.verifyOtpCode(restaurantId, code);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response, "Phone number verified successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMessage(), "VERIFICATION_FAILED"));
            }
            
        } catch (Exception e) {
            log.error("Error verifying code for restaurant ID: {}", restaurantId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to verify code: " + e.getMessage(), "VERIFICATION_ERROR"));
        }
    }

    @Operation(
        summary = "Get verification status",
        description = "Retrieves the current verification status for a restaurant"
    )
    @GetMapping("/{restaurantId}/status")
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> getVerificationStatus(
            @Parameter(description = "Restaurant ID") 
            @PathVariable @NotNull Long restaurantId) {
        
        log.debug("Getting verification status for restaurant ID: {}", restaurantId);
        
        try {
            VerificationResponseDTO response = verificationService.getVerificationStatus(restaurantId);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Verification status retrieved"));
            
        } catch (Exception e) {
            log.error("Error getting verification status for restaurant ID: {}", restaurantId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get verification status: " + e.getMessage(), "STATUS_ERROR"));
        }
    }

    @Operation(
        summary = "Cancel verification",
        description = "Cancels an active verification for a restaurant"
    )
    @PostMapping("/{restaurantId}/cancel")
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> cancelVerification(
            @Parameter(description = "Restaurant ID") 
            @PathVariable @NotNull Long restaurantId) {
        
        log.info("Cancelling verification for restaurant ID: {}", restaurantId);
        
        try {
            VerificationResponseDTO response = verificationService.cancelVerification(restaurantId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response, "Verification cancelled successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMessage(), "CANCELLATION_FAILED"));
            }
            
        } catch (Exception e) {
            log.error("Error cancelling verification for restaurant ID: {}", restaurantId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to cancel verification: " + e.getMessage(), "CANCELLATION_ERROR"));
        }
    }
}
