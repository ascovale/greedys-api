package com.application.common.controller.validators;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for phone numbers with support for different countries and formats
 */
@Component
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    private boolean allowNull;
    private String country;
    private boolean requireInternational;
    
    // International phone number pattern (E.164 format)
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile(
        "^\\+[1-9]\\d{1,14}$"
    );
    
    // Italian phone patterns
    private static final Pattern ITALIAN_MOBILE_PATTERN = Pattern.compile(
        "^(\\+39\\s?)?3[0-9]{2}\\s?\\d{6,7}$"
    );
    
    private static final Pattern ITALIAN_LANDLINE_PATTERN = Pattern.compile(
        "^(\\+39\\s?)?(0[1-9]\\d{1,3})\\s?\\d{4,8}$"
    );
    
    // US phone pattern
    private static final Pattern US_PATTERN = Pattern.compile(
        "^(\\+1\\s?)?\\(?[2-9]\\d{2}\\)?[\\s\\-]?[2-9]\\d{2}[\\s\\-]?\\d{4}$"
    );
    
    // Generic clean pattern (removes common formatting)
    private static final Pattern CLEAN_PATTERN = Pattern.compile(
        "[^\\d+]"
    );
    
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.country = constraintAnnotation.country();
        this.requireInternational = constraintAnnotation.requireInternational();
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        // Handle null values
        if (phoneNumber == null) {
            return allowNull;
        }
        
        // Handle empty or blank values
        if (phoneNumber.trim().isEmpty()) {
            return allowNull;
        }
        
        String cleanPhone = phoneNumber.trim();
        
        // If international format is required, check that first
        if (requireInternational) {
            if (!cleanPhone.startsWith("+")) {
                addCustomMessage(context, "Phone number must be in international format (starting with +)");
                return false;
            }
            return INTERNATIONAL_PATTERN.matcher(cleanPhone).matches();
        }
        
        // Country-specific validation
        return validateForCountry(cleanPhone, context);
    }
    
    /**
     * Validates phone number for specific country
     */
    private boolean validateForCountry(String phoneNumber, ConstraintValidatorContext context) {
        switch (country.toUpperCase()) {
            case "IT":
            case "ITALY":
                return validateItalianPhone(phoneNumber, context);
            case "US":
            case "USA":
            case "UNITED STATES":
                return validateUSPhone(phoneNumber, context);
            case "":
                // No country specified - try international first, then generic
                return validateGenericPhone(phoneNumber, context);
            default:
                // Unknown country - use international format
                return validateGenericPhone(phoneNumber, context);
        }
    }
    
    /**
     * Validates Italian phone numbers
     */
    private boolean validateItalianPhone(String phoneNumber, ConstraintValidatorContext context) {
        // Check mobile numbers
        if (ITALIAN_MOBILE_PATTERN.matcher(phoneNumber).matches()) {
            return true;
        }
        
        // Check landline numbers
        if (ITALIAN_LANDLINE_PATTERN.matcher(phoneNumber).matches()) {
            return true;
        }
        
        addCustomMessage(context, 
            "Invalid Italian phone number. Use format: +39 xxx xxxxxxx (mobile) or +39 0xx xxxxxxxx (landline)");
        return false;
    }
    
    /**
     * Validates US phone numbers
     */
    private boolean validateUSPhone(String phoneNumber, ConstraintValidatorContext context) {
        if (US_PATTERN.matcher(phoneNumber).matches()) {
            return true;
        }
        
        addCustomMessage(context, 
            "Invalid US phone number. Use format: +1 (xxx) xxx-xxxx or +1 xxx xxx xxxx");
        return false;
    }
    
    /**
     * Generic phone validation
     */
    private boolean validateGenericPhone(String phoneNumber, ConstraintValidatorContext context) {
        // Try international format first
        if (INTERNATIONAL_PATTERN.matcher(phoneNumber).matches()) {
            return true;
        }
        
        // Clean the number and check basic criteria
        String cleanNumber = CLEAN_PATTERN.matcher(phoneNumber).replaceAll("");
        
        // Basic checks
        if (cleanNumber.length() < 7) {
            addCustomMessage(context, "Phone number too short (minimum 7 digits)");
            return false;
        }
        
        if (cleanNumber.length() > 15) {
            addCustomMessage(context, "Phone number too long (maximum 15 digits)");
            return false;
        }
        
        // Must contain only digits and + (if at start)
        if (!phoneNumber.matches("^\\+?[0-9\\s\\-\\(\\)]+$")) {
            addCustomMessage(context, "Phone number contains invalid characters");
            return false;
        }
        
        return true;
    }
    
    /**
     * Adds a custom error message to the validation context
     */
    private void addCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
