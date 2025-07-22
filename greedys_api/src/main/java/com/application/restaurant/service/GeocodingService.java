package com.application.restaurant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.application.common.dto.GeocodingResult;
import com.application.restaurant.model.Restaurant;
import com.application.spring.GeocodingConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for geocoding addresses and enriching restaurant data
 */
@Service
public class GeocodingService {
    
    @Autowired
    private GeocodingConfig geocodingConfig;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Geocodes an address and returns detailed information
     */
    public GeocodingResult geocodeAddress(String address) {
        return geocodeAddress(address, null);
    }
    
    /**
     * Geocodes an address with city context for better accuracy
     */
    public GeocodingResult geocodeAddress(String address, String city) {
        // Build more specific query if city is provided
        String searchQuery = address;
        if (city != null && !city.trim().isEmpty()) {
            searchQuery = address + ", " + city;
        }
        
        // Try Google first if API key is available
        if (geocodingConfig.getGoogle().getApiKey() != null && 
            !geocodingConfig.getGoogle().getApiKey().isEmpty()) {
            return geocodeWithGoogle(searchQuery);
        } else {
            return geocodeWithNominatim(searchQuery);
        }
    }
    
    /**
     * Updates restaurant with geocoding information
     */
    public void enrichRestaurantWithGeocodingData(Restaurant restaurant) {
        if (restaurant.getAddress() == null || restaurant.getAddress().trim().isEmpty()) {
            return;
        }
        
        // Use city context if available for better accuracy
        GeocodingResult result = geocodeAddress(restaurant.getAddress(), restaurant.getCity());
        
        if (result != null && result.isComplete()) {
            // Verify the city matches (case-insensitive)
            if (restaurant.getCity() != null && result.getFullCityName() != null) {
                if (!restaurant.getCity().trim().equalsIgnoreCase(result.getFullCityName().trim())) {
                    System.out.println("Warning: Provided city (" + restaurant.getCity() + 
                        ") doesn't match geocoded city (" + result.getFullCityName() + ")");
                }
            }
            
            // Update city if not provided or if geocoding provides better data
            if (restaurant.getCity() == null || restaurant.getCity().trim().isEmpty()) {
                restaurant.setCity(result.getFullCityName());
            }
            
            // Update state/province
            if (restaurant.getStateProvince() == null || restaurant.getStateProvince().trim().isEmpty()) {
                restaurant.setStateProvince(result.getStateProvince());
            }
            
            // Update country
            if (restaurant.getCountry() == null || restaurant.getCountry().trim().isEmpty()) {
                restaurant.setCountry(result.getCountry());
            }
            
            // Update postal code if geocoding provides one and it's missing
            if (restaurant.getPostCode() == null || restaurant.getPostCode().trim().isEmpty()) {
                restaurant.setPostCode(result.getPostalCode());
            }
            
            // Always update coordinates
            restaurant.setLatitude(result.getLatitude());
            restaurant.setLongitude(result.getLongitude());
            
            // Log if postal codes don't match
            if (restaurant.getPostCode() != null && !result.postalCodeMatches(restaurant.getPostCode())) {
                System.out.println("Warning: Provided postal code (" + restaurant.getPostCode() + 
                    ") doesn't match geocoded postal code (" + result.getPostalCode() + ")");
            }
        }
    }
    
    /**
     * Geocodes using Google Maps API
     */
    private GeocodingResult geocodeWithGoogle(String address) {
        try {
            String url = String.format(
                "%s?address=%s&key=%s",
                geocodingConfig.getGoogle().getBaseUrl(),
                java.net.URLEncoder.encode(address, "UTF-8"),
                geocodingConfig.getGoogle().getApiKey()
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            String status = jsonNode.get("status").asText();
            if ("OK".equals(status)) {
                JsonNode results = jsonNode.get("results");
                if (results.isArray() && results.size() > 0) {
                    return extractGoogleGeocodingDetails(results.get(0));
                }
            }
        } catch (Exception e) {
            System.err.println("Google geocoding failed: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Geocodes using OpenStreetMap Nominatim
     */
    private GeocodingResult geocodeWithNominatim(String address) {
        try {
            String url = String.format(
                "%s?q=%s&format=json&limit=1&addressdetails=1",
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
                return extractNominatimGeocodingDetails(jsonNode.get(0));
            }
        } catch (Exception e) {
            System.err.println("Nominatim geocoding failed: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extracts details from Google Geocoding API response
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
     * Extracts details from Nominatim response
     */
    private GeocodingResult extractNominatimGeocodingDetails(JsonNode result) {
        GeocodingResult.GeocodingResultBuilder builder = GeocodingResult.builder();
        
        // Formatted address
        if (result.has("display_name")) {
            builder.formattedAddress(result.get("display_name").asText());
        }
        
        // Coordinates
        if (result.has("lat")) {
            builder.latitude(Double.parseDouble(result.get("lat").asText()));
        }
        if (result.has("lon")) {
            builder.longitude(Double.parseDouble(result.get("lon").asText()));
        }
        
        // Address details
        if (result.has("address")) {
            JsonNode address = result.get("address");
            
            if (address.has("house_number")) {
                builder.streetNumber(address.get("house_number").asText());
            }
            if (address.has("road")) {
                builder.route(address.get("road").asText());
            }
            if (address.has("city") || address.has("town") || address.has("village")) {
                String city = address.has("city") ? address.get("city").asText() :
                             address.has("town") ? address.get("town").asText() :
                             address.get("village").asText();
                builder.locality(city);
            }
            if (address.has("state")) {
                builder.administrativeAreaLevel1(address.get("state").asText());
            }
            if (address.has("country")) {
                builder.country(address.get("country").asText());
            }
            if (address.has("postcode")) {
                builder.postalCode(address.get("postcode").asText());
            }
        }
        
        return builder.build();
    }
}
