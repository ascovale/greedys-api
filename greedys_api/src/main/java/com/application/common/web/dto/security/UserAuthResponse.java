package com.application.common.web.dto.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base class for all user DTOs returned in authentication responses.
 * Uses @JsonTypeInfo for polymorphic deserialization - Dart/Swagger will know the exact type.
 * All user types (RUserDTO, RUserHubDTO, CustomerDTO, AdminDTO, etc.) should extend this.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "userType"
)
@Schema(
    name = "UserAuthResponse",
    discriminatorProperty = "userType",
    description = "Base response for authenticated users - actual type determined by userType field"
)
public abstract class UserAuthResponse {
    
    @Schema(
        description = "Type discriminator for polymorphic deserialization",
        example = "RESTAURANT_USER",
        allowableValues = {"RESTAURANT_USER", "RESTAURANT_HUB", "ADMIN", "CUSTOMER"}
    )
    protected String userType;
    
    /**
     * Get the user ID
     */
    public Long getId() {
        return null;
    }
    
    /**
     * Get the username/email
     */
    public String getUsername() {
        return null;
    }
    
    /**
     * Get the email
     */
    public String getEmail() {
        return null;
    }
    
    /**
     * Get the user type - used as discriminator for polymorphic deserialization.
     * OpenAPI Generator uses this for Dart code generation.
     */
    @JsonIgnore
    public String getUserType() {
        if (userType != null) {
            return userType;
        }
        // Auto-detect based on class type
        String className = this.getClass().getSimpleName();
        if (className.equals("RUserDTO")) return "RESTAURANT_USER";
        if (className.equals("RUserHubDTO")) return "RESTAURANT_HUB";
        if (className.equals("AdminDTO")) return "ADMIN";
        if (className.equals("CustomerDTO")) return "CUSTOMER";
        return "UNKNOWN";
    }
}

