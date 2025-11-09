package com.application.customer.web.dto.matching;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer matching input data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMatchInput {
    
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String email;
    private UUID restaurantId;

    /**
     * Get computed full name from first and last name if fullName is not provided
     */
    public String getComputedFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        
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
        
        return name.length() > 0 ? name.toString() : null;
    }

    /**
     * Check if input has sufficient data for matching
     */
    public boolean isValid() {
        return restaurantId != null && (
            hasPhone() || hasEmail() || hasName()
        );
    }

    /**
     * Check if phone is provided
     */
    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }

    /**
     * Check if email is provided
     */
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    /**
     * Check if name information is provided
     */
    public boolean hasName() {
        String computedName = getComputedFullName();
        return computedName != null && !computedName.trim().isEmpty();
    }
}