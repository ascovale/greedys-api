package com.application.restaurant.service.google;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.application.admin.persistence.dao.RestaurantValidationDAO;
import com.application.admin.persistence.model.Admin;
import com.application.admin.persistence.model.RestaurantValidation;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.web.dto.google.RestaurantData;
import com.application.restaurant.web.dto.validation.RestaurantValidationResult;
import com.application.restaurant.web.dto.validation.ValidationError;
import com.google.maps.model.PlaceDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for validating restaurant data against Google Places API
 * Checks if local restaurant data matches current Google Places data
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantDataValidationService {

    private final RestaurantDAO restaurantDAO;
    private final RestaurantValidationDAO restaurantValidationDAO;
    private final GooglePlacesSearchService searchService;
    private final RestaurantDataExtractor dataExtractor;


    /**
     * Validates a restaurant by searching it on Google Maps and comparing data.
     * If multiple or no results are found, returns invalid with error.
     * If exactly one result is found, compares the data fields.
     * Also saves the validation result to the database.
     *
     * @param restaurantId The ID of the restaurant to validate
     * @param admin The admin performing the validation (optional)
     * @return RestaurantValidationResult with validation status and errors
     */
    public RestaurantValidationResult validateRestaurant(Long restaurantId, Admin admin) {
        //TODO aggiungere Twilio verify
        //matching dominio sito web
        //business api quando sarà attiva
        //aggiungere anche validazione forzata e dscrizione motivo
        if (restaurantId == null) {
            return RestaurantValidationResult.builder()
                    .valid(false)
                    .message("Restaurant ID cannot be null")
                    .errors(List.of(ValidationError.builder()
                            .field("restaurantId")
                            .currentValue(null)
                            .googleValue(null)
                            .message("Restaurant ID cannot be null")
                            .build()))
                    .build();
        }

        try {
            Optional<Restaurant> restaurantOpt = restaurantDAO.findById(restaurantId);
            if (restaurantOpt.isEmpty()) {
                return RestaurantValidationResult.builder()
                        .valid(false)
                        .message("Restaurant not found with ID: " + restaurantId)
                        .errors(List.of(ValidationError.builder()
                                .field("restaurantId")
                                .currentValue(restaurantId.toString())
                                .googleValue(null)
                                .message("Restaurant not found with ID: " + restaurantId)
                                .build()))
                        .build();
            }

            return validateRestaurant(restaurantOpt.get(), admin);

        } catch (Exception e) {
            log.error("Error finding restaurant with ID {}: {}", restaurantId, e.getMessage());
            return RestaurantValidationResult.builder()
                    .valid(false)
                    .message("Error finding restaurant: " + e.getMessage())
                    .errors(List.of(ValidationError.builder()
                            .field("system")
                            .currentValue(restaurantId.toString())
                            .googleValue(null)
                            .message("System error: " + e.getMessage())
                            .build()))
                    .build();
        }
    }

    /**
     * Validates a restaurant by searching it on Google Maps and comparing data.
     * If multiple or no results are found, returns invalid with error.
     * If exactly one result is found, compares the data fields.
     * Also saves the validation result to the database.
     *
     * @param restaurant The local restaurant to validate
     * @param admin The admin performing the validation (optional)
     * @return RestaurantValidationResult with validation status and errors
     */
    public RestaurantValidationResult validateRestaurant(Restaurant restaurant, Admin admin) {
        RestaurantValidationResult result = validateRestaurant(restaurant);
        
        // Save validation result to database
        saveValidationResult(restaurant, result, admin);
        
        return result;
    }

    /**
     * Validates a restaurant by searching it on Google Maps and comparing data.
     * If multiple or no results are found, returns invalid with error.
     * If exactly one result is found, compares the data fields.
     *
     * @param restaurant The local restaurant to validate
     * @return RestaurantValidationResult with validation status and errors
     */
    public RestaurantValidationResult validateRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            return RestaurantValidationResult.builder()
                    .valid(false)
                    .message("Restaurant cannot be null")
                    .errors(List.of(ValidationError.builder()
                            .field("restaurant")
                            .currentValue(null)
                            .googleValue(null)
                            .message("Restaurant cannot be null")
                            .build()))
                    .build();
        }

        try {
            // Search for restaurants on Google Maps using local data
            List<PlaceDetails> foundPlaces = searchService.findRestaurantsOnMapFromRestaurant(restaurant);

            if (foundPlaces == null || foundPlaces.isEmpty()) {
                return RestaurantValidationResult.builder()
                        .valid(false)
                        .message("No matching restaurant found on Google Maps")
                        .errors(List.of(ValidationError.builder()
                                .field("search")
                                .currentValue(restaurant.getName() + " - " + restaurant.getAddress())
                                .googleValue(null)
                                .message("No matching restaurant found on Google Maps")
                                .build()))
                        .build();
            }

            if (foundPlaces.size() > 1) {
                return RestaurantValidationResult.builder()
                        .valid(false)
                        .message("Multiple restaurants found on Google Maps, cannot uniquely identify")
                        .errors(List.of(ValidationError.builder()
                                .field("search")
                                .currentValue(restaurant.getName() + " - " + restaurant.getAddress())
                                .googleValue("Found " + foundPlaces.size() + " results")
                                .message("Multiple restaurants found on Google Maps")
                                .build()))
                        .build();
            }

            // Only one result found, proceed to validate data
            PlaceDetails googlePlace = foundPlaces.get(0);
            RestaurantData googleData = dataExtractor.extractRestaurantData(googlePlace);

            List<ValidationError> errors = compareRestaurantData(restaurant, googleData);

            boolean isValid = errors.isEmpty();
            String message = isValid ? "Restaurant data is valid" :
                    String.format("Found %d data mismatches", errors.size());

            return RestaurantValidationResult.builder()
                    .valid(isValid)
                    .message(message)
                    .placeId(googlePlace.placeId)
                    .localRestaurant(convertToRestaurantData(restaurant))
                    .googleRestaurant(googleData)
                    .errors(errors)
                    .build();

        } catch (Exception e) {
            log.error("Error validating restaurant: {}", e.getMessage());
            return RestaurantValidationResult.builder()
                    .valid(false)
                    .message("Error during validation: " + e.getMessage())
                    .errors(List.of(ValidationError.builder()
                            .field("system")
                            .currentValue(null)
                            .googleValue(null)
                            .message("System error: " + e.getMessage())
                            .build()))
                    .build();
        }
    }

    /**
     * Validates restaurant data by comparing local data with Google Places data
     * 
     * @param placeId Google Places ID of the restaurant
     * @return RestaurantValidationResult with validation status and any mismatches
     */
    public RestaurantValidationResult validateRestaurantData(Restaurant restaurant, String placeId) {
        try {
            // Find local restaurant
            if (restaurant == null) {
                return RestaurantValidationResult.builder()
                        .valid(false)
                        .message("Restaurant not found in local database")
                        .placeId(placeId)
                        .errors(List.of(ValidationError.builder()
                                .field("restaurant")
                                .currentValue(null)
                                .googleValue(null)
                                .message("Restaurant not found in local database")
                                .build()))
                        .build();
            }

            // Get current data from Google Places
            PlaceDetails googlePlace = searchService.getPlaceDetailsByPlaceId(placeId);
            if (googlePlace == null) {
                return RestaurantValidationResult.builder()
                        .valid(false)
                        .message("Restaurant not found on Google Places")
                        .placeId(placeId)
                        .errors(List.of(ValidationError.builder()
                                .field("google_places")
                                .currentValue(placeId)
                                .googleValue(null)
                                .message("Restaurant not found on Google Places")
                                .build()))
                        .build();
            }

            // Extract Google data
            RestaurantData googleData = dataExtractor.extractRestaurantData(googlePlace);

            // Compare data and collect mismatches
            List<ValidationError> errors = compareRestaurantData(restaurant, googleData);

            boolean isValid = errors.isEmpty();
            String message = isValid ? "Restaurant data is valid" : 
                            String.format("Found %d data mismatches", errors.size());

            return RestaurantValidationResult.builder()
                    .valid(isValid)
                    .message(message)
                    .placeId(placeId)
                    .localRestaurant(convertToRestaurantData(restaurant))
                    .googleRestaurant(googleData)
                    .errors(errors)
                    .build();

        } catch (Exception e) {
            log.error("Error validating restaurant data for placeId {}: {}", placeId, e.getMessage());
            return RestaurantValidationResult.builder()
                    .valid(false)
                    .message("Error during validation: " + e.getMessage())
                    .placeId(placeId)
                    .errors(List.of(ValidationError.builder()
                            .field("system")
                            .currentValue(null)
                            .googleValue(null)
                            .message("System error: " + e.getMessage())
                            .build()))
                    .build();
        }
    }

    /**
     * Assigns a placeId to a Restaurant and saves it to the database
     * 
     * @param restaurant The restaurant to update
     * @param placeId The Google Places ID to assign
     * @return The updated Restaurant entity
     */
    public Restaurant assignPlaceIdToRestaurant(Restaurant restaurant, String placeId) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        
        if (placeId == null || placeId.trim().isEmpty()) {
            throw new IllegalArgumentException("PlaceId cannot be null or empty");
        }
        
        log.info("Assigning placeId {} to restaurant {}", placeId, restaurant.getName());
        
        restaurant.setPlaceId(placeId);
        Restaurant savedRestaurant = restaurantDAO.save(restaurant);
        
        log.info("Successfully assigned placeId {} to restaurant {} (ID: {})", 
                placeId, savedRestaurant.getName(), savedRestaurant.getId());
        
        return savedRestaurant;
    }

    /**
     * Admin verification: searches for restaurant on Google Places using restaurant data,
     * then validates the found data against local restaurant data
     * 
     * @param restaurant The restaurant to verify
     * @return RestaurantValidationResult with validation status and any mismatches
     */
    public RestaurantValidationResult adminVerifyRestaurantData(Restaurant restaurant) {
        if (restaurant == null) {
            return RestaurantValidationResult.builder()
                    .valid(false)
                    .message("Restaurant cannot be null")
                    .errors(List.of(ValidationError.builder()
                            .field("restaurant")
                            .currentValue(null)
                            .googleValue(null)
                            .message("Restaurant cannot be null")
                            .build()))
                    .build();
        }

        try {
            // Search for restaurant on Google Places using name and address
            String searchAddress = restaurant.getAddress();
            PlaceDetails foundPlace = searchService.findRestaurantOnMaps(restaurant.getName(), searchAddress);
            
            if (foundPlace == null) {
                return RestaurantValidationResult.builder()
                        .valid(false)
                        .message("Restaurant not found on Google Places with provided name and address")
                        .errors(List.of(ValidationError.builder()
                                .field("search")
                                .currentValue(restaurant.getName() + " - " + searchAddress)
                                .googleValue(null)
                                .message("Restaurant not found on Google Places")
                                .build()))
                        .build();
            }

            // Now validate using the found placeId
            return validateRestaurantData(restaurant, foundPlace.placeId);

        } catch (Exception e) {
            log.error("Error during admin verification for restaurant {}: {}", restaurant.getName(), e.getMessage());
            return RestaurantValidationResult.builder()
                    .valid(false)
                    .message("Error during admin verification: " + e.getMessage())
                    .errors(List.of(ValidationError.builder()
                            .field("system")
                            .currentValue(null)
                            .googleValue(null)
                            .message("System error: " + e.getMessage())
                            .build()))
                    .build();
        }
    }

    /**
     * Compares local restaurant data with Google data
     */
    private List<ValidationError> compareRestaurantData(Restaurant localRestaurant, RestaurantData googleData) {
        List<ValidationError> errors = new ArrayList<>();

        // Compare name
        if (!Objects.equals(normalizeString(localRestaurant.getName()), 
                           normalizeString(googleData.getName()))) {
            errors.add(ValidationError.builder()
                    .field("name")
                    .currentValue(localRestaurant.getName())
                    .googleValue(googleData.getName())
                    .message("Restaurant name mismatch")
                    .build());
        }

        // Compare address
        if (!Objects.equals(normalizeString(localRestaurant.getAddress()), 
                           normalizeString(googleData.getAddress()))) {
            errors.add(ValidationError.builder()
                    .field("address")
                    .currentValue(localRestaurant.getAddress())
                    .googleValue(googleData.getAddress())
                    .message("Restaurant address mismatch")
                    .build());
        }

        // Compare phone number
        if (!Objects.equals(normalizePhoneNumber(localRestaurant.getPhoneNumber()), 
                           normalizePhoneNumber(googleData.getPhoneNumber()))) {
            errors.add(ValidationError.builder()
                    .field("phoneNumber")
                    .currentValue(localRestaurant.getPhoneNumber())
                    .googleValue(googleData.getPhoneNumber())
                    .message("Restaurant phone number mismatch")
                    .build());
        }

        // Compare website
        if (!Objects.equals(normalizeUrl(localRestaurant.getWebsite()), 
                           normalizeUrl(googleData.getWebsite()))) {
            errors.add(ValidationError.builder()
                    .field("website")
                    .currentValue(localRestaurant.getWebsite())
                    .googleValue(googleData.getWebsite())
                    .message("Restaurant website mismatch")
                    .build());
        }

        return errors;
    }

    /**
     * Converts Restaurant entity to RestaurantData for comparison
     */
    private RestaurantData convertToRestaurantData(Restaurant restaurant) {
        RestaurantData data = new RestaurantData();
        data.setPlaceId(restaurant.getPlaceId());
        data.setName(restaurant.getName());
        data.setAddress(restaurant.getAddress());
        data.setPhoneNumber(restaurant.getPhoneNumber());
        data.setWebsite(restaurant.getWebsite());
        return data;
    }

    /**
     * Normalizes strings for comparison (trim, lowercase, remove extra spaces)
     */
    private String normalizeString(String str) {
        if (str == null) return null;
        return str.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Normalizes phone numbers for comparison (remove spaces, dashes, parentheses)
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[\\s\\-\\(\\)\\+]", "");
    }

    /**
     * Normalizes URLs for comparison (remove trailing slashes, convert to lowercase)
     */
    private String normalizeUrl(String url) {
        if (url == null) return null;
        String normalized = url.toLowerCase().trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Saves validation result to the database
     */
    private void saveValidationResult(Restaurant restaurant, RestaurantValidationResult result, Admin admin) {
        try {
            RestaurantValidation.RestaurantValidationBuilder builder = RestaurantValidation.builder()
                    .restaurant(restaurant)
                    .validatedBy(admin)
                    .overallMessage(result.getMessage());

            // Set validation status
            if (result.isValid()) {
                builder.status(RestaurantValidation.ValidationStatus.VALID);
            } else {
                builder.status(RestaurantValidation.ValidationStatus.INVALID);
            }

            // Set place ID or error
            if (result.getPlaceId() != null) {
                builder.placeId(result.getPlaceId());
            } else {
                // Look for place ID related errors
                result.getErrors().stream()
                        .filter(error -> "search".equals(error.getField()) || "google_places".equals(error.getField()))
                        .findFirst()
                        .ifPresent(error -> builder.placeIdError(error.getMessage()));
            }

            // Process validation errors for each field
            for (ValidationError error : result.getErrors()) {
                switch (error.getField()) {
                    case "name":
                        builder.nameValid(false)
                                .nameCurrentValue(error.getCurrentValue())
                                .nameGoogleValue(error.getGoogleValue())
                                .nameError(error.getMessage());
                        break;
                    case "address":
                        builder.addressValid(false)
                                .addressCurrentValue(error.getCurrentValue())
                                .addressGoogleValue(error.getGoogleValue())
                                .addressError(error.getMessage());
                        break;
                    case "phoneNumber":
                        builder.phoneValid(false)
                                .phoneCurrentValue(error.getCurrentValue())
                                .phoneGoogleValue(error.getGoogleValue())
                                .phoneError(error.getMessage());
                        break;
                    case "website":
                        builder.websiteValid(false)
                                .websiteCurrentValue(error.getCurrentValue())
                                .websiteGoogleValue(error.getGoogleValue())
                                .websiteError(error.getMessage());
                        break;
                    case "system":
                        builder.status(RestaurantValidation.ValidationStatus.ERROR);
                        break;
                }
            }

            // Set valid fields (if no error was found for that field)
            boolean nameHasError = result.getErrors().stream().anyMatch(e -> "name".equals(e.getField()));
            boolean addressHasError = result.getErrors().stream().anyMatch(e -> "address".equals(e.getField()));
            boolean phoneHasError = result.getErrors().stream().anyMatch(e -> "phoneNumber".equals(e.getField()));
            boolean websiteHasError = result.getErrors().stream().anyMatch(e -> "website".equals(e.getField()));

            if (!nameHasError && result.getLocalRestaurant() != null && result.getGoogleRestaurant() != null) {
                builder.nameValid(true)
                        .nameCurrentValue(result.getLocalRestaurant().getName())
                        .nameGoogleValue(result.getGoogleRestaurant().getName());
            }
            if (!addressHasError && result.getLocalRestaurant() != null && result.getGoogleRestaurant() != null) {
                builder.addressValid(true)
                        .addressCurrentValue(result.getLocalRestaurant().getAddress())
                        .addressGoogleValue(result.getGoogleRestaurant().getAddress());
            }
            if (!phoneHasError && result.getLocalRestaurant() != null && result.getGoogleRestaurant() != null) {
                builder.phoneValid(true)
                        .phoneCurrentValue(result.getLocalRestaurant().getPhoneNumber())
                        .phoneGoogleValue(result.getGoogleRestaurant().getPhoneNumber());
            }
            if (!websiteHasError && result.getLocalRestaurant() != null && result.getGoogleRestaurant() != null) {
                builder.websiteValid(true)
                        .websiteCurrentValue(result.getLocalRestaurant().getWebsite())
                        .websiteGoogleValue(result.getGoogleRestaurant().getWebsite());
            }

            RestaurantValidation validation = builder.build();
            restaurantValidationDAO.save(validation);
            
            log.info("Saved validation result for restaurant {} with status {}", 
                    restaurant.getName(), validation.getStatus());

        } catch (Exception e) {
            log.error("Error saving validation result for restaurant {}: {}", 
                    restaurant.getName(), e.getMessage());
        }
    }

    /**
     * Gets the most recent validation result for a restaurant
     */
    public Optional<RestaurantValidation> getLastValidationResult(Restaurant restaurant) {
        return restaurantValidationDAO.findTopByRestaurantOrderByValidationDateDesc(restaurant);
    }

    /**
     * Gets all validation results for a restaurant
     */
    public List<RestaurantValidation> getAllValidationResults(Restaurant restaurant) {
        return restaurantValidationDAO.findByRestaurantOrderByValidationDateDesc(restaurant);
    }

    /**
     * Gets validation results by status
     */
    public List<RestaurantValidation> getValidationsByStatus(RestaurantValidation.ValidationStatus status) {
        return restaurantValidationDAO.findByStatusOrderByValidationDateDesc(status);
    }

    /**
     * Gets all validations that have errors
     */
    public List<RestaurantValidation> getValidationsWithErrors() {
        return restaurantValidationDAO.findValidationsWithErrors();
    }

    /**
     * Checks if a restaurant has been validated recently (within 24 hours)
     */
    public boolean hasRecentValidation(Restaurant restaurant) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        return restaurantValidationDAO.existsRecentValidation(restaurant, oneDayAgo);
    }
}
