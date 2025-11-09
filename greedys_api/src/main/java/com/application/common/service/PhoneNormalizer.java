package com.application.common.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for normalizing phone numbers to E.164 format
 * Simplified implementation focused on Italian phone numbers
 */
@Service
@Slf4j
public class PhoneNormalizer {

    // Pattern for validating E.164 format
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    
    // Pattern for Italian mobile numbers
    private static final Pattern ITALIAN_MOBILE = Pattern.compile("^(\\+39|0039|39)?\\s?3[0-9]{2}\\s?[0-9]{3}\\s?[0-9]{3,4}$");
    
    // Pattern for Italian landline numbers  
    private static final Pattern ITALIAN_LANDLINE = Pattern.compile("^(\\+39|0039|39)?\\s?0[0-9]{1,4}\\s?[0-9]{4,8}$");

    /**
     * Convert raw phone number to E.164 format
     * 
     * @param raw    Raw phone number string
     * @param region Default region code (e.g., "IT", "US") 
     * @return E.164 formatted phone number or null if invalid
     */
    public String toE164(String raw, String region) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            // Clean the input - remove spaces, dashes, parentheses
            String cleaned = raw.replaceAll("[\\s\\-\\(\\)\\.]", "");
            
            // Handle Italian numbers specifically
            if ("IT".equalsIgnoreCase(region)) {
                return normalizeItalianNumber(cleaned);
            }
            
            // For other regions, do basic normalization
            return normalizeInternationalNumber(cleaned, region);
            
        } catch (Exception e) {
            log.debug("Failed to normalize phone number '{}' with region '{}': {}", raw, region, e.getMessage());
            return null;
        }
    }

    /**
     * Convert raw phone number to E.164 format using default Italian region
     */
    public String toE164(String raw) {
        return toE164(raw, "IT");
    }

    /**
     * Normalize Italian phone number to E.164
     */
    private String normalizeItalianNumber(String cleaned) {
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }

        // Remove country code prefixes if present
        if (cleaned.startsWith("+39")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("0039")) {
            cleaned = cleaned.substring(4);
        } else if (cleaned.startsWith("39") && cleaned.length() > 8) {
            cleaned = cleaned.substring(2);
        }

        // Validate Italian mobile (starts with 3)
        if (cleaned.matches("^3[0-9]{8,9}$")) {
            return "+39" + cleaned;
        }
        
        // Validate Italian landline (starts with 0)
        if (cleaned.matches("^0[0-9]{5,10}$")) {
            return "+39" + cleaned;
        }

        log.debug("Invalid Italian phone number format: {}", cleaned);
        return null;
    }

    /**
     * Basic normalization for international numbers
     */
    private String normalizeInternationalNumber(String cleaned, String region) {
        // If already starts with +, validate and return
        if (cleaned.startsWith("+")) {
            return isValidE164(cleaned) ? cleaned : null;
        }

        // Add default country codes based on region
        String countryCode = getCountryCode(region);
        if (countryCode != null) {
            String candidate = "+" + countryCode + cleaned;
            return isValidE164(candidate) ? candidate : null;
        }

        return null;
    }

    /**
     * Get country code for region
     */
    private String getCountryCode(String region) {
        return switch (region.toUpperCase()) {
            case "IT" -> "39";
            case "US", "CA" -> "1"; 
            case "GB" -> "44";
            case "FR" -> "33";
            case "DE" -> "49";
            case "ES" -> "34";
            default -> null;
        };
    }

    /**
     * Extract last N digits from E.164 phone number
     */
    public String getLastDigits(String e164Phone, int digits) {
        if (e164Phone == null || !e164Phone.startsWith("+")) {
            return null;
        }

        // Remove the + sign and get last digits
        String numbersOnly = e164Phone.substring(1);
        
        if (numbersOnly.length() < digits) {
            return numbersOnly;
        }
        
        return numbersOnly.substring(numbersOnly.length() - digits);
    }

    /**
     * Get last 6 digits of phone number (useful for partial matching)
     */
    public String getLastSixDigits(String e164Phone) {
        return getLastDigits(e164Phone, 6);
    }

    /**
     * Check if phone number is valid E.164 format
     */
    public boolean isValidE164(String phone) {
        if (phone == null) {
            return false;
        }
        return E164_PATTERN.matcher(phone).matches();
    }

    /**
     * Format phone number for display (national format)
     */
    public String formatForDisplay(String e164Phone, String region) {
        if (e164Phone == null || !isValidE164(e164Phone)) {
            return e164Phone;
        }

        // Simple formatting for Italian numbers
        if ("IT".equalsIgnoreCase(region) && e164Phone.startsWith("+39")) {
            String national = e164Phone.substring(3);
            
            // Format mobile numbers (3xx xxx xxxx)
            if (national.startsWith("3") && national.length() >= 9) {
                return String.format("%s %s %s", 
                    national.substring(0, 3),
                    national.substring(3, 6),
                    national.substring(6));
            }
            
            // Format landline numbers (0xx xxxx xxxx)
            if (national.startsWith("0")) {
                if (national.length() >= 8) {
                    return String.format("%s %s %s", 
                        national.substring(0, 2),
                        national.substring(2, 6),
                        national.substring(6));
                }
            }
        }

        return e164Phone;
    }

    /**
     * Format phone number for display using Italian format
     */
    public String formatForDisplay(String e164Phone) {
        return formatForDisplay(e164Phone, "IT");
    }
}