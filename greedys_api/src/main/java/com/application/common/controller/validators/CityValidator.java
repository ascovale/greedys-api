package com.application.common.controller.validators;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.application.common.persistence.model.ItalianCity;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for city names with support for different countries and validation modes
 */
@Component
public class CityValidator implements ConstraintValidator<ValidCity, String> {
    
    private boolean allowNull;
    private String country;
    private boolean strictValidation;
    
    // Pattern for reasonable city names
    private static final Pattern CITY_NAME_PATTERN = Pattern.compile(
        "^[a-zA-ZÀ-ÿ\\s\\-']{2,50}$", 
        Pattern.UNICODE_CASE
    );
    
    @Override
    public void initialize(ValidCity constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.country = constraintAnnotation.country();
        this.strictValidation = constraintAnnotation.strictValidation();
    }
    
    @Override
    public boolean isValid(String city, ConstraintValidatorContext context) {
        // Handle null values
        if (city == null) {
            return allowNull;
        }
        
        // Handle empty or blank values
        if (city.trim().isEmpty()) {
            return allowNull;
        }
        
        String cleanCity = city.trim();
        
        // Basic format validation
        if (!CITY_NAME_PATTERN.matcher(cleanCity).matches()) {
            addCustomMessage(context, "City name contains invalid characters or format");
            return false;
        }
        
        // Country-specific validation
        if (strictValidation) {
            return validateCityForCountry(cleanCity, context);
        }
        
        // Lenient validation - just check basic format
        return true;
    }
    
    /**
     * Validates city against known cities for specific countries
     */
    private boolean validateCityForCountry(String city, ConstraintValidatorContext context) {
        switch (country.toUpperCase()) {
            case "IT":
            case "ITALY":
                return validateItalianCity(city, context);
            case "":
                // No country specified - try to validate as best as possible
                return validateGenericCity(city, context);
            default:
                // Unknown country - use generic validation
                return validateGenericCity(city, context);
        }
    }
    
    /**
     * Validates against known Italian cities
     */
    private boolean validateItalianCity(String city, ConstraintValidatorContext context) {
        ItalianCity italianCity = ItalianCity.findByName(city);
        if (italianCity == null) {
            addCustomMessage(context, 
                "City '" + city + "' is not recognized as a valid Italian city. " +
                "Please use one of the major Italian cities.");
            return false;
        }
        return true;
    }
    
    /**
     * Generic city validation - checks for reasonable patterns
     */
    private boolean validateGenericCity(String city, ConstraintValidatorContext context) {
        // Additional checks for generic cities
        if (city.length() < 2) {
            addCustomMessage(context, "City name must be at least 2 characters long");
            return false;
        }
        
        if (city.length() > 50) {
            addCustomMessage(context, "City name must not exceed 50 characters");
            return false;
        }
        
        // Check for suspicious patterns
        if (city.matches(".*\\d.*")) {
            addCustomMessage(context, "City name should not contain numbers");
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
