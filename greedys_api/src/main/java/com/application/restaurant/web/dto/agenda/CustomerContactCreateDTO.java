package com.application.restaurant.web.dto.agenda;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating customer in restaurant agenda
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContactCreateDTO {
    
    @NotNull(message = "Restaurant ID is required")
    private UUID restaurantId;
    
    private String firstName;
    private String lastName;
    
    private String phoneNumber;
    private String email;
    
    private String notes; // Note specifiche del ristorante
    private String allergies;
    private String preferences;
    private String address;
    
    @Builder.Default
    private boolean forceCreate = false; // Forza creazione anche se esiste match
    
    private String source; // Fonte del contatto (phone, email, reservation, manual)

    /**
     * Check if has minimum required data
     */
    public boolean isValid() {
        return restaurantId != null && 
               hasName() && 
               hasContactInfo();
    }

    /**
     * Check if has name information
     */
    public boolean hasName() {
        return (firstName != null && !firstName.trim().isEmpty()) ||
               (lastName != null && !lastName.trim().isEmpty());
    }

    /**
     * Check if has contact information
     */
    public boolean hasContactInfo() {
        return (phoneNumber != null && !phoneNumber.trim().isEmpty()) ||
               (email != null && !email.trim().isEmpty());
    }

    /**
     * Get display name
     */
    public String getDisplayName() {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(lastName.trim());
        }
        return name.length() > 0 ? name.toString() : "Unknown Contact";
    }

    /**
     * Get validation errors
     */
    public String getValidationError() {
        if (restaurantId == null) {
            return "Restaurant ID is required";
        }
        if (!hasName()) {
            return "At least first name or last name is required";
        }
        if (!hasContactInfo()) {
            return "At least phone number or email is required";
        }
        return null;
    }
}