package com.application.common.constants;

/**
 * Constants for token validation results across all authentication services.
 * This class centralizes token validation status constants to avoid duplication
 * across AdminRegistrationService, AdminService, CustomerAuthenticationService, and RUserService.
 */
public final class TokenValidationConstants {

    /**
     * Indicates that the provided token is invalid or not found
     */
    public static final String TOKEN_INVALID = "invalidToken";

    /**
     * Indicates that the provided token has expired
     */
    public static final String TOKEN_EXPIRED = "expired";

    /**
     * Indicates that the provided token is valid and active
     */
    public static final String TOKEN_VALID = "valid";

    // Private constructor to prevent instantiation
    private TokenValidationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
