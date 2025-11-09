package com.application.customer.web.dto.matching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for configurable customer form schema per restaurant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFormSchemaDTO {
    
    // Required fields configuration
    @Builder.Default
    private boolean requireFirstName = true;
    
    @Builder.Default
    private boolean requireLastName = true;
    
    @Builder.Default
    private boolean requirePhone = true;
    
    @Builder.Default
    private boolean requireEmail = false;
    
    @Builder.Default
    private boolean allowNickname = false;
    
    @Builder.Default
    private boolean allowNotes = true;
    
    // Validation configuration
    @Builder.Default
    private String phoneFormat = "italian"; // "italian", "international", "any"
    
    @Builder.Default
    private boolean emailValidation = true;
    
    @Builder.Default
    private boolean phoneValidation = true;
    
    // Display configuration
    @Builder.Default
    private String firstNameLabel = "Nome";
    
    @Builder.Default
    private String lastNameLabel = "Cognome";
    
    @Builder.Default
    private String phoneLabel = "Telefono";
    
    @Builder.Default
    private String emailLabel = "Email";
    
    @Builder.Default
    private String nicknameLabel = "Soprannome";
    
    @Builder.Default
    private String notesLabel = "Note";
    
    // Placeholder text
    @Builder.Default
    private String firstNamePlaceholder = "Inserisci il nome";
    
    @Builder.Default
    private String lastNamePlaceholder = "Inserisci il cognome";
    
    @Builder.Default
    private String phonePlaceholder = "Inserisci il numero di telefono";
    
    @Builder.Default
    private String emailPlaceholder = "Inserisci l'email";
    
    @Builder.Default
    private String nicknamePlaceholder = "Inserisci un soprannome";
    
    @Builder.Default
    private String notesPlaceholder = "Note aggiuntive";
    
    /**
     * Check if the form requires any field
     */
    public boolean hasRequiredFields() {
        return requireFirstName || requireLastName || requirePhone || requireEmail;
    }
    
    /**
     * Check if the form has optional fields
     */
    public boolean hasOptionalFields() {
        return allowNickname || allowNotes;
    }
    
    /**
     * Get the total number of visible fields
     */
    public int getVisibleFieldCount() {
        int count = 0;
        if (requireFirstName || allowOptionalFirstName()) count++;
        if (requireLastName || allowOptionalLastName()) count++;
        if (requirePhone || allowOptionalPhone()) count++;
        if (requireEmail || allowOptionalEmail()) count++;
        if (allowNickname) count++;
        if (allowNotes) count++;
        return count;
    }
    
    /**
     * Check if first name is allowed but not required
     */
    public boolean allowOptionalFirstName() {
        return !requireFirstName; // Currently first name is always shown
    }
    
    /**
     * Check if last name is allowed but not required
     */
    public boolean allowOptionalLastName() {
        return !requireLastName; // Currently last name is always shown
    }
    
    /**
     * Check if phone is allowed but not required
     */
    public boolean allowOptionalPhone() {
        return !requirePhone; // Currently phone is always shown
    }
    
    /**
     * Check if email is allowed but not required
     */
    public boolean allowOptionalEmail() {
        return !requireEmail; // Currently email is always shown
    }
    
    /**
     * Validate the schema configuration
     */
    public boolean isValid() {
        // Must have at least one way to identify a customer
        return requirePhone || requireEmail || (requireFirstName && requireLastName);
    }
    
    /**
     * Get validation error message if schema is invalid
     */
    public String getValidationError() {
        if (!isValid()) {
            return "Schema must require at least phone, email, or both first and last name";
        }
        return null;
    }
}