package com.application.common.service.notification.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Parameters container for shared read operations
 * Holds all context needed by Strategy implementations
 * 
 * @author System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedReadParams {
    
    // === REQUIRED FIELDS ===
    
    /** ID of notification being marked as read */
    private Long notificationId;
    
    /** User ID who triggered the read action */
    private Long readByUserId;
    
    /** Timestamp when read occurred */
    private Instant readAt;
    
    // === RESTAURANT-SPECIFIC FIELDS ===
    
    /** Restaurant ID (for RESTAURANT scope queries) */
    private Long restaurantId;
    
    /** Restaurant User Hub ID (for RESTAURANT_HUB scopes) */
    private Long restaurantUserHubId;
    
    // === AGENCY-SPECIFIC FIELDS ===
    
    /** Agency ID (for AGENCY scope queries) */
    private Long agencyId;
    
    /** Agency User Hub ID (for AGENCY_HUB scopes) */
    private Long agencyUserHubId;
    
    // === OPTIONAL AUDIT/LOGGING FIELDS ===
    
    /** Username who triggered read (for audit trail) */
    private String readByUsername;
    
    /** String representation of scope (set by service) */
    private String scope;
    
    /** Reason for read operation (for logging) */
    private String reason;
    
    // === HELPER METHODS ===
    
    /**
     * Validates required fields for operation
     * @throws IllegalArgumentException if critical fields missing
     */
    public void validate() {
        if (notificationId == null) {
            throw new IllegalArgumentException("notificationId cannot be null");
        }
        if (readByUserId == null) {
            throw new IllegalArgumentException("readByUserId cannot be null");
        }
        if (readAt == null) {
            throw new IllegalArgumentException("readAt cannot be null");
        }
    }
    
    /**
     * Validates restaurant-specific fields
     * @throws IllegalArgumentException if required fields missing
     */
    public void validateRestaurant() {
        validate();
        if (restaurantId == null && restaurantUserHubId == null) {
            throw new IllegalArgumentException(
                "Either restaurantId or restaurantUserHubId must be provided"
            );
        }
    }
    
    /**
     * Validates agency-specific fields
     * @throws IllegalArgumentException if required fields missing
     */
    public void validateAgency() {
        validate();
        if (agencyId == null && agencyUserHubId == null) {
            throw new IllegalArgumentException(
                "Either agencyId or agencyUserHubId must be provided"
            );
        }
    }
    
    /**
     * Build a readable description of this operation
     */
    public String toDescription() {
        return String.format(
            "SharedRead[notification=%d, user=%d, scope=%s, timestamp=%s]",
            notificationId,
            readByUserId,
            scope != null ? scope : "UNKNOWN",
            readAt
        );
    }
}
