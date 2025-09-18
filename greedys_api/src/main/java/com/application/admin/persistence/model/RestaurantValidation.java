package com.application.admin.persistence.model;

import java.time.LocalDateTime;

import com.application.restaurant.persistence.model.Restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for storing restaurant validation results against Google Places API
 */
@Entity
@Table(name = "restaurant_validation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantValidation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;
    
    @NotNull
    @Builder.Default
    @Column(name = "validation_date")
    private LocalDateTime validationDate = LocalDateTime.now();
    
    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ValidationStatus status = ValidationStatus.PENDING;
    
    // Google Places ID or error message if place ID couldn't be retrieved
    @Column(name = "place_id", length = 500)
    private String placeId;
    
    @Column(name = "place_id_error", columnDefinition = "TEXT")
    private String placeIdError;
    
    // Name validation
    @Column(name = "name_valid")
    private Boolean nameValid;
    
    @Column(name = "name_current_value", length = 500)
    private String nameCurrentValue;
    
    @Column(name = "name_google_value", length = 500)
    private String nameGoogleValue;
    
    @Column(name = "name_error", columnDefinition = "TEXT")
    private String nameError;
    
    // Address validation
    @Column(name = "address_valid")
    private Boolean addressValid;
    
    @Column(name = "address_current_value", columnDefinition = "TEXT")
    private String addressCurrentValue;
    
    @Column(name = "address_google_value", columnDefinition = "TEXT")
    private String addressGoogleValue;
    
    @Column(name = "address_error", columnDefinition = "TEXT")
    private String addressError;
    
    // Phone number validation
    @Column(name = "phone_valid")
    private Boolean phoneValid;
    
    @Column(name = "phone_current_value", length = 50)
    private String phoneCurrentValue;
    
    @Column(name = "phone_google_value", length = 50)
    private String phoneGoogleValue;
    
    @Column(name = "phone_error", columnDefinition = "TEXT")
    private String phoneError;
    
    // Website validation
    @Column(name = "website_valid")
    private Boolean websiteValid;
    
    @Column(name = "website_current_value", length = 500)
    private String websiteCurrentValue;
    
    @Column(name = "website_google_value", length = 500)
    private String websiteGoogleValue;
    
    @Column(name = "website_error", columnDefinition = "TEXT")
    private String websiteError;
    
    // Overall validation result
    @Column(name = "overall_message", columnDefinition = "TEXT")
    private String overallMessage;
    
    // Admin who performed the validation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_admin_id")
    private Admin validatedBy;
    
    public enum ValidationStatus {
        PENDING,
        VALID,
        INVALID,
        ERROR,
        NOT_FOUND
    }
    
    /**
     * Checks if all data fields are valid
     */
    public boolean isAllDataValid() {
        return Boolean.TRUE.equals(nameValid) &&
               Boolean.TRUE.equals(addressValid) &&
               Boolean.TRUE.equals(phoneValid) &&
               Boolean.TRUE.equals(websiteValid);
    }
    
    /**
     * Checks if there are any validation errors
     */
    public boolean hasValidationErrors() {
        return nameError != null || addressError != null || 
               phoneError != null || websiteError != null || 
               placeIdError != null;
    }
    
    /**
     * Gets the count of invalid fields
     */
    public int getInvalidFieldsCount() {
        int count = 0;
        if (Boolean.FALSE.equals(nameValid)) count++;
        if (Boolean.FALSE.equals(addressValid)) count++;
        if (Boolean.FALSE.equals(phoneValid)) count++;
        if (Boolean.FALSE.equals(websiteValid)) count++;
        return count;
    }
}
