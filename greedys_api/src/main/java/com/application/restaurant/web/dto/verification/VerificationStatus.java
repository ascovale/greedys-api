package com.application.restaurant.web.dto.verification;

/**
 * Enumeration for restaurant verification status
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
public enum VerificationStatus {
    /**
     * No verification has been started
     */
    NOT_STARTED,
    
    /**
     * Verification is pending, waiting for code verification
     */
    PENDING,
    
    /**
     * Phone number has been successfully verified
     */
    VERIFIED,
    
    /**
     * Verification failed due to incorrect code or other error
     */
    FAILED,
    
    /**
     * Verification has expired
     */
    EXPIRED,
    
    /**
     * Verification was cancelled by user or system
     */
    CANCELLED
}
