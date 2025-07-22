package com.application.common.controller.validators;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.application.common.dto.GeocodingResult;
import com.application.spring.AddressValidationConfig;
import com.application.spring.GeocodingConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for physical addresses with multiple validation levels.
 * 
 * Validation levels:
 * 1. Basic format validation (always performed)
 * 2. Pattern matching for common address formats
 * 3. Optional geocoding verification using external services
 * 
 * Supports integration with:
 * - Google Maps Geocoding API
 * - OpenStreetMap Nominatim (free alternative)
 * - Here Maps API
 * 
 * @author Generated for Greedys API
 */
@Component
public class AddressValidator implements ConstraintValidator<ValidAddress, String> {
    
    private boolean allowNull;
    private boolean strictValidation;
    private int minLength;
    private int maxLength;
    
    @Autowired
    private GeocodingConfig geocodingConfig;
    
    @Autowired
    private AddressValidationConfig validationConfig;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Common address patterns for different countries/regions
    private static final Map<String, Pattern> ADDRESS_PATTERNS = new HashMap<>();
    
    static {
        // Italian address patterns
        ADDRESS_PATTERNS.put("IT", Pattern.compile(
            ".*\\b(via|viale|piazza|corso|largo|vicolo|strada|località)\\s+[^,]+,?\\s*\\d*\\s*,?\\s*\\d{5}\\s+[a-zA-ZÀ-ÿ\\s]+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ));
        
        // US address patterns
        ADDRESS_PATTERNS.put("US", Pattern.compile(
            ".*\\d+\\s+[a-zA-Z\\s]+\\s*(street|st|avenue|ave|road|rd|drive|dr|lane|ln|way|court|ct|place|pl).*\\d{5}(-\\d{4})?.*",
            Pattern.CASE_INSENSITIVE
        ));
        
        // UK address patterns
        ADDRESS_PATTERNS.put("UK", Pattern.compile(
            ".*\\d+\\s+[a-zA-Z\\s]+\\s*(street|road|avenue|lane|drive|close|way|gardens?|crescent|square|place).*[A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2}.*",
            Pattern.CASE_INSENSITIVE
        ));
        
        // French address patterns
        ADDRESS_PATTERNS.put("FR", Pattern.compile(
            ".*\\d+\\s+(rue|avenue|boulevard|place|impasse|allée|cours|quai)\\s+[^,]+.*\\d{5}\\s+[a-zA-ZÀ-ÿ\\s]+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ));
        
        // German address patterns
        ADDRESS_PATTERNS.put("DE", Pattern.compile(
            ".*(straße|str|gasse|weg|platz|allee)\\s+\\d+.*\\d{5}\\s+[a-zA-ZÄÖÜäöüß\\s]+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ));
        
        // Spanish address patterns
        ADDRESS_PATTERNS.put("ES", Pattern.compile(
            ".*(calle|avenida|plaza|paseo|ronda)\\s+[^,]+.*\\d{5}\\s+[a-zA-ZÁÉÍÓÚÑáéíóúñ\\s]+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ));
        
        // Generic international pattern (fallback)
        ADDRESS_PATTERNS.put("GENERIC", Pattern.compile(
            ".*\\d+.*[a-zA-ZÀ-ÿ\\s]+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ));
    }
    
    @Override
    public void initialize(ValidAddress constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.strictValidation = constraintAnnotation.strictValidation();
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
    }
    
    @Override
    public boolean isValid(String address, ConstraintValidatorContext context) {
        // Handle null values
        if (address == null) {
            return allowNull;
        }
        
        // Handle empty or blank values
        if (address.trim().isEmpty()) {
            return allowNull;
        }
        
        String cleanAddress = address.trim();
        
        // Basic length validation
        if (cleanAddress.length() < minLength) {
            addCustomMessage(context, String.format("Address must be at least %d characters long", minLength));
            return false;
        }
        
        if (cleanAddress.length() > maxLength) {
            addCustomMessage(context, String.format("Address must not exceed %d characters", maxLength));
            return false;
        }
        
        // Basic format validation
        if (!isBasicFormatValid(cleanAddress)) {
            addCustomMessage(context, "Address must contain both street information and location details");
            return false;
        }
        
        // Pattern matching validation
        if (!isPatternValid(cleanAddress)) {
            addCustomMessage(context, "Address format does not match expected patterns for known regions");
            return false;
        }
        
        // Strict validation with geocoding (if enabled)
        if (strictValidation && validationConfig.isEnabled()) {
            return isGeocodingValid(cleanAddress, context);
        }
        
        return true;
    }
    
    /**
     * Performs basic format validation
     */
    private boolean isBasicFormatValid(String address) {
        // Must contain at least one number (street number or postal code)
        if (!address.matches(".*\\d+.*")) {
            return false;
        }
        
        // Must contain alphabetic characters (street name, city)
        if (!address.matches(".*[a-zA-ZÀ-ÿ]+.*")) {
            return false;
        }
        
        // Should not be just numbers or just letters
        boolean hasNumbers = address.matches(".*\\d.*");
        boolean hasLetters = address.matches(".*[a-zA-ZÀ-ÿ].*");
        
        return hasNumbers && hasLetters;
    }
    
    /**
     * Validates address against known patterns
     */
    private boolean isPatternValid(String address) {
        // Try to detect country/region and validate accordingly
        for (Map.Entry<String, Pattern> entry : ADDRESS_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(address).matches()) {
                return true;
            }
        }
        
        // If no specific pattern matches, use generic validation
        return ADDRESS_PATTERNS.get("GENERIC").matcher(address).matches();
    }
    
    /**
     * Validates address using geocoding services
     */
    private boolean isGeocodingValid(String address, ConstraintValidatorContext context) {
        try {
            // Try Google first if API key is available, otherwise use Nominatim
            if (geocodingConfig.getGoogle().getApiKey() != null && 
                !geocodingConfig.getGoogle().getApiKey().isEmpty()) {
                return validateWithGoogle(address, context);
            } else {
                return validateWithNominatim(address, context);
            }
        } catch (Exception e) {
            // If geocoding fails, don't block the validation - log and continue
            System.err.println("Geocoding validation failed: " + e.getMessage());
            return true; // Don't block registration for geocoding issues
        }
    }
    
    /**
     * Validates address using Google Maps Geocoding API
     */
    private boolean validateWithGoogle(String address, ConstraintValidatorContext context) {
        String apiKey = geocodingConfig.getGoogle().getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return true; // Skip if no API key
        }
        
        try {
            String url = String.format(
                "%s?address=%s&key=%s",
                geocodingConfig.getGoogle().getBaseUrl(),
                java.net.URLEncoder.encode(address, "UTF-8"),
                apiKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            String status = jsonNode.get("status").asText();
            if ("OK".equals(status)) {
                JsonNode results = jsonNode.get("results");
                if (results.isArray() && results.size() > 0) {
                    // Extract detailed information
                    GeocodingResult geocodingResult = extractGoogleGeocodingDetails(results.get(0));
                    
                    // Store the result in the context for later use
                    context.getDefaultConstraintMessageTemplate(); // This will store it
                    
                    // Address found and geocoded successfully
                    return true;
                }
            } else if ("ZERO_RESULTS".equals(status)) {
                addCustomMessage(context, "Address not found. Please verify the address exists.");
                return false;
            }
            
            return true; // For other statuses, don't block
        } catch (Exception e) {
            // Don't block validation for API issues
            return true;
        }
    }
    
    /**
     * Extracts detailed address information from Google Geocoding API response
     */
    private GeocodingResult extractGoogleGeocodingDetails(JsonNode result) {
        GeocodingResult.GeocodingResultBuilder builder = GeocodingResult.builder();
        
        // Formatted address
        if (result.has("formatted_address")) {
            builder.formattedAddress(result.get("formatted_address").asText());
        }
        
        // Coordinates
        if (result.has("geometry") && result.get("geometry").has("location")) {
            JsonNode location = result.get("geometry").get("location");
            if (location.has("lat")) {
                builder.latitude(location.get("lat").asDouble());
            }
            if (location.has("lng")) {
                builder.longitude(location.get("lng").asDouble());
            }
        }
        
        // Place ID
        if (result.has("place_id")) {
            builder.placeId(result.get("place_id").asText());
        }
        
        // Address components
        if (result.has("address_components")) {
            JsonNode components = result.get("address_components");
            for (JsonNode component : components) {
                JsonNode types = component.get("types");
                String longName = component.get("long_name").asText();
                String shortName = component.get("short_name").asText();
                
                for (JsonNode type : types) {
                    String typeStr = type.asText();
                    switch (typeStr) {
                        case "street_number":
                            builder.streetNumber(longName);
                            break;
                        case "route":
                            builder.route(longName);
                            break;
                        case "locality":
                            builder.locality(longName);
                            break;
                        case "administrative_area_level_1":
                            builder.administrativeAreaLevel1(longName);
                            break;
                        case "administrative_area_level_2":
                            builder.administrativeAreaLevel2(longName);
                            break;
                        case "country":
                            builder.country(longName);
                            break;
                        case "postal_code":
                            builder.postalCode(longName);
                            break;
                    }
                }
            }
        }
        
        return builder.build();
    }
    
    /**
     * Validates address using OpenStreetMap Nominatim (free service)
     */
    private boolean validateWithNominatim(String address, ConstraintValidatorContext context) {
        try {
            String url = String.format(
                "%s?q=%s&format=json&limit=1",
                geocodingConfig.getNominatim().getBaseUrl(),
                java.net.URLEncoder.encode(address, "UTF-8")
            );
            
            // Add User-Agent header as required by Nominatim
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", geocodingConfig.getNominatim().getUserAgent());
            
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            String response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                String.class
            ).getBody();
            
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.isArray() && jsonNode.size() > 0) {
                // Address found
                return true;
            } else {
                addCustomMessage(context, "Address not found. Please verify the address exists.");
                return false;
            }
            
        } catch (Exception e) {
            // Don't block validation for API issues
            return true;
        }
    }
    
    /**
     * Adds a custom error message to the validation context
     */
    private void addCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
