package com.application.restaurant.service.verification;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.spring.TwilioConfig;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.RestaurantVerificationDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantVerification;
import com.application.restaurant.web.dto.verification.VerificationRequestDTO;
import com.application.restaurant.web.dto.verification.VerificationResponseDTO;
import com.application.restaurant.web.dto.verification.VerificationStatus;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for restaurant verification using Twilio Verify API.
 * Handles phone number verification for restaurant ownership confirmation.
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RestaurantTwilioVerificationService {

    private final TwilioConfig twilioConfig;
    private final RestaurantDAO restaurantDAO;
    private final RestaurantVerificationDAO verificationDAO;

    /**
     * Initiates phone verification for restaurant ownership.
     * Sends OTP code to the restaurant's registered phone number.
     *
     * @param request verification request containing restaurant ID and phone number
     * @return verification response with status and reference ID
     * @throws IllegalArgumentException if restaurant not found or phone invalid
     */
    public VerificationResponseDTO initiatePhoneVerification(VerificationRequestDTO request) {
        log.info("Initiating phone verification for restaurant ID: {}", request.getRestaurantId());
        
        try {
            // Validate restaurant exists
            Restaurant restaurant = validateRestaurant(request.getRestaurantId());
            
            // Validate and format phone number
            String formattedPhone = validateAndFormatPhoneNumber(request.getPhoneNumber());
            
            // Check if there's an active verification
            Optional<RestaurantVerification> existingVerification = 
                verificationDAO.findActiveVerificationByRestaurant(restaurant.getId());
            
            if (existingVerification.isPresent()) {
                RestaurantVerification existing = existingVerification.get();
                if (isVerificationStillValid(existing)) {
                    log.warn("Active verification already exists for restaurant ID: {}", request.getRestaurantId());
                    return VerificationResponseDTO.builder()
                        .success(false)
                        .status(VerificationStatus.PENDING)
                        .message("Active verification already in progress. Please wait before requesting a new one.")
                        .verificationSid(existing.getVerificationSid())
                        .expiresAt(existing.getExpiresAt())
                        .build();
                } else {
                    // Mark existing verification as expired
                    existing.setStatus(VerificationStatus.EXPIRED);
                    verificationDAO.save(existing);
                }
            }
            
            // Send verification via Twilio
            Verification verification = Verification.creator(
                    twilioConfig.getVerifyServiceSid(),
                    formattedPhone,
                    "sms")
                .setLocale("it")
                .setCustomFriendlyName("Greedy's Restaurant Verification")
                .create();
            
            // Save verification record
            RestaurantVerification restaurantVerification = createVerificationRecord(
                restaurant, formattedPhone, verification.getSid());
            
            log.info("Phone verification initiated successfully for restaurant ID: {} with SID: {}", 
                request.getRestaurantId(), verification.getSid());
            
            return VerificationResponseDTO.builder()
                .success(true)
                .status(VerificationStatus.PENDING)
                .message("Verification code sent successfully")
                .verificationSid(verification.getSid())
                .phoneNumber(maskPhoneNumber(formattedPhone))
                .expiresAt(restaurantVerification.getExpiresAt())
                .build();
                
        } catch (Exception e) {
            log.error("Error initiating phone verification for restaurant ID: {}", 
                request.getRestaurantId(), e);
            
            return VerificationResponseDTO.builder()
                .success(false)
                .status(VerificationStatus.FAILED)
                .message("Failed to send verification code: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verifies the OTP code provided by the restaurant.
     *
     * @param restaurantId the restaurant ID
     * @param verificationCode the OTP code to verify
     * @return verification response with success status
     * @throws IllegalArgumentException if restaurant or verification not found
     */
    public VerificationResponseDTO verifyOtpCode(Long restaurantId, String verificationCode) {
        log.info("Verifying OTP code for restaurant ID: {}", restaurantId);
        
        try {
            // Validate restaurant exists
            Restaurant restaurant = validateRestaurant(restaurantId);
            
            // Find active verification
            RestaurantVerification restaurantVerification = verificationDAO
                .findActiveVerificationByRestaurant(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No active verification found for restaurant ID: " + restaurantId));
            
            // Check if verification is still valid
            if (!isVerificationStillValid(restaurantVerification)) {
                restaurantVerification.setStatus(VerificationStatus.EXPIRED);
                verificationDAO.save(restaurantVerification);
                
                return VerificationResponseDTO.builder()
                    .success(false)
                    .status(VerificationStatus.EXPIRED)
                    .message("Verification code has expired. Please request a new one.")
                    .build();
            }
            
            // Verify code with Twilio
            VerificationCheck verificationCheck = VerificationCheck.creator(
                    twilioConfig.getVerifyServiceSid())
                .setTo(restaurantVerification.getPhoneNumber())
                .setCode(verificationCode)
                .create();
            
            // Update verification status based on Twilio response
            VerificationStatus status = "approved".equals(verificationCheck.getStatus()) 
                ? VerificationStatus.VERIFIED 
                : VerificationStatus.FAILED;
            
            restaurantVerification.setStatus(status);
            restaurantVerification.setVerifiedAt(LocalDateTime.now());
            restaurantVerification.setAttempts(restaurantVerification.getAttempts() + 1);
            
            if (status == VerificationStatus.VERIFIED) {
                // Update restaurant verification status
                restaurant.setPhoneVerified(true);
                restaurant.setPhoneVerifiedAt(LocalDateTime.now());
                restaurantDAO.save(restaurant);
                
                log.info("Phone verification successful for restaurant ID: {}", restaurantId);
                
                return VerificationResponseDTO.builder()
                    .success(true)
                    .status(VerificationStatus.VERIFIED)
                    .message("Phone number verified successfully")
                    .verificationSid(restaurantVerification.getVerificationSid())
                    .verifiedAt(restaurantVerification.getVerifiedAt())
                    .build();
            } else {
                log.warn("Phone verification failed for restaurant ID: {} - Invalid code", restaurantId);
                
                // Check if max attempts reached
                if (restaurantVerification.getAttempts() >= twilioConfig.getMaxVerificationAttempts()) {
                    restaurantVerification.setStatus(VerificationStatus.EXPIRED);
                }
                
                verificationDAO.save(restaurantVerification);
                
                return VerificationResponseDTO.builder()
                    .success(false)
                    .status(status)
                    .message("Invalid verification code")
                    .attemptsRemaining(Math.max(0, 
                        twilioConfig.getMaxVerificationAttempts() - restaurantVerification.getAttempts()))
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error verifying OTP code for restaurant ID: {}", restaurantId, e);
            
            return VerificationResponseDTO.builder()
                .success(false)
                .status(VerificationStatus.FAILED)
                .message("Failed to verify code: " + e.getMessage())
                .build();
        }
    }

    /**
     * Cancels an active verification for a restaurant.
     *
     * @param restaurantId the restaurant ID
     * @return verification response with cancellation status
     */
    public VerificationResponseDTO cancelVerification(Long restaurantId) {
        log.info("Cancelling verification for restaurant ID: {}", restaurantId);
        
        try {
            RestaurantVerification verification = verificationDAO
                .findActiveVerificationByRestaurant(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No active verification found for restaurant ID: " + restaurantId));
            
            // Cancel with Twilio if still pending
            if (verification.getStatus() == VerificationStatus.PENDING) {
                try {
                    // Note: Twilio Verify API doesn't support canceling verifications
                    // We'll just mark it as cancelled in our database
                    log.info("Marking verification as cancelled (Twilio doesn't support direct cancellation)");
                } catch (Exception e) {
                    log.warn("Failed to cancel verification with Twilio: {}", e.getMessage());
                }
            }
            
            verification.setStatus(VerificationStatus.CANCELLED);
            verificationDAO.save(verification);
            
            log.info("Verification cancelled successfully for restaurant ID: {}", restaurantId);
            
            return VerificationResponseDTO.builder()
                .success(true)
                .status(VerificationStatus.CANCELLED)
                .message("Verification cancelled successfully")
                .build();
                
        } catch (Exception e) {
            log.error("Error cancelling verification for restaurant ID: {}", restaurantId, e);
            
            return VerificationResponseDTO.builder()
                .success(false)
                .status(VerificationStatus.FAILED)
                .message("Failed to cancel verification: " + e.getMessage())
                .build();
        }
    }

    /**
     * Gets the current verification status for a restaurant.
     *
     * @param restaurantId the restaurant ID
     * @return verification response with current status
     */
    public VerificationResponseDTO getVerificationStatus(Long restaurantId) {
        log.debug("Getting verification status for restaurant ID: {}", restaurantId);
        
        try {
            Optional<RestaurantVerification> verification = 
                verificationDAO.findActiveVerificationByRestaurant(restaurantId);
            
            if (verification.isEmpty()) {
                return VerificationResponseDTO.builder()
                    .success(true)
                    .status(VerificationStatus.NOT_STARTED)
                    .message("No verification found")
                    .build();
            }
            
            RestaurantVerification verif = verification.get();
            
            // Check if verification has expired
            if (!isVerificationStillValid(verif) && verif.getStatus() == VerificationStatus.PENDING) {
                verif.setStatus(VerificationStatus.EXPIRED);
                verificationDAO.save(verif);
            }
            
            return VerificationResponseDTO.builder()
                .success(true)
                .status(verif.getStatus())
                .message("Current verification status")
                .verificationSid(verif.getVerificationSid())
                .phoneNumber(maskPhoneNumber(verif.getPhoneNumber()))
                .createdAt(verif.getCreatedAt())
                .expiresAt(verif.getExpiresAt())
                .verifiedAt(verif.getVerifiedAt())
                .attempts(verif.getAttempts())
                .attemptsRemaining(Math.max(0, 
                    twilioConfig.getMaxVerificationAttempts() - verif.getAttempts()))
                .build();
                
        } catch (Exception e) {
            log.error("Error getting verification status for restaurant ID: {}", restaurantId, e);
            
            return VerificationResponseDTO.builder()
                .success(false)
                .status(VerificationStatus.FAILED)
                .message("Failed to get verification status: " + e.getMessage())
                .build();
        }
    }

    // ====== PACKAGE-PRIVATE HELPER METHODS FOR TESTING ======

    Restaurant validateRestaurant(Long restaurantId) {
        return restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + restaurantId));
    }

    String validateAndFormatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^\\d]", "");
        
        // Add Italian country code if not present
        if (!cleaned.startsWith("39")) {
            if (cleaned.startsWith("0")) {
                cleaned = "39" + cleaned.substring(1);
            } else {
                cleaned = "39" + cleaned;
            }
        }
        
        // Validate Italian phone number format
        if (!cleaned.matches("39[0-9]{9,10}")) {
            throw new IllegalArgumentException("Invalid Italian phone number format");
        }
        
        return "+" + cleaned;
    }

    boolean isVerificationStillValid(RestaurantVerification verification) {
        return verification.getExpiresAt().isAfter(LocalDateTime.now()) 
            && verification.getAttempts() < twilioConfig.getMaxVerificationAttempts()
            && verification.getStatus() == VerificationStatus.PENDING;
    }

    String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }
        
        int visibleDigits = 4;
        int start = phoneNumber.length() - visibleDigits;
        return phoneNumber.substring(0, 3) + "*".repeat(start - 3) + phoneNumber.substring(start);
    }

    private RestaurantVerification createVerificationRecord(Restaurant restaurant, String phoneNumber, String verificationSid) {
        RestaurantVerification verification = RestaurantVerification.builder()
            .restaurant(restaurant)
            .phoneNumber(phoneNumber)
            .verificationSid(verificationSid)
            .status(VerificationStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(twilioConfig.getVerificationExpiryMinutes()))
            .attempts(0)
            .build();
        
        return verificationDAO.save(verification);
    }
}
