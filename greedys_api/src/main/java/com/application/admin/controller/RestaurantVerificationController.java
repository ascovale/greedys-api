package com.application.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ResponseWrapper;
import com.application.restaurant.service.verification.RestaurantTwilioVerificationService;
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
@RequestMapping("/admin/restaurant/verification")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Restaurant Verification", description = "Restaurant phone number verification operations")
public class RestaurantVerificationController extends BaseController {

    private final RestaurantTwilioVerificationService verificationService;

    @Operation(
        summary = "Initiate phone verification",
        description = "Sends an OTP code to the restaurant's phone number for verification"
    )
    @CreateApiResponses
    @PostMapping("/initiate")
    public ResponseEntity<ResponseWrapper<VerificationResponseDTO>> initiateVerification(
            @Valid @RequestBody VerificationRequestDTO request) {
        
        return executeCreate("initiate verification", () -> {
            log.info("Received verification initiation request for restaurant ID: {}", request.getRestaurantId());
            return verificationService.initiatePhoneVerification(request);
        });
    }

    @Operation(
        summary = "Verify OTP code",
        description = "Verifies the OTP code sent to the restaurant's phone number"
    )
    @CreateApiResponses
    @PostMapping("/{restaurantId}/verify")
    public ResponseEntity<ResponseWrapper<VerificationResponseDTO>> verifyCode(
            @Parameter(description = "Restaurant ID") 
            @PathVariable @NotNull Long restaurantId,
            
            @Parameter(description = "OTP verification code") 
            @RequestParam @NotBlank String code) {
        
        return executeCreate("verify code", () -> {
            log.info("Received verification code for restaurant ID: {}", restaurantId);
            return verificationService.verifyOtpCode(restaurantId, code);
        });
    }

    @Operation(
        summary = "Get verification status",
        description = "Retrieves the current verification status for a restaurant"
    )
    @ReadApiResponses
    @GetMapping("/{restaurantId}/status")
    public ResponseEntity<ResponseWrapper<VerificationResponseDTO>> getVerificationStatus(
            @Parameter(description = "Restaurant ID") 
            @PathVariable @NotNull Long restaurantId) {
        
        return execute("get verification status", () -> {
            log.debug("Getting verification status for restaurant ID: {}", restaurantId);
            return verificationService.getVerificationStatus(restaurantId);
        });
    }

    @Operation(
        summary = "Cancel verification",
        description = "Cancels an active verification for a restaurant"
    )
    @CreateApiResponses
    @PostMapping("/{restaurantId}/cancel")
    public ResponseEntity<ResponseWrapper<VerificationResponseDTO>> cancelVerification(
            @Parameter(description = "Restaurant ID") 
            @PathVariable @NotNull Long restaurantId) {
        
        return executeCreate("cancel verification", () -> {
            log.info("Cancelling verification for restaurant ID: {}", restaurantId);
            return verificationService.cancelVerification(restaurantId);
        });
    }
}
