package com.application.restaurant.service.google;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.application.restaurant.persistence.model.Restaurant;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlacesSearchResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Simple service for exact restaurant searches using Google Places API
 */
@Service
@ConditionalOnProperty(name = "google.maps.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class GooglePlacesSearchService {

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    /**
     * Searches for restaurants with exact name and address
     */
    public List<PlaceDetails> findRestaurantsOnMaps(String restaurantName, String address) throws Exception {
        GeoApiContext context = createContext();

        // Direct search - exact name and address
        String searchQuery = restaurantName;
        if (address != null && !address.trim().isEmpty()) {
            searchQuery += " " + address;
        }

        PlacesSearchResponse searchResponse = PlacesApi.textSearchQuery(context, searchQuery).await();

        List<PlaceDetails> restaurants = new ArrayList<>();
        if (searchResponse.results != null) {
            int maxResults = Math.min(searchResponse.results.length, 5);

            for (int i = 0; i < maxResults; i++) {
                String placeId = searchResponse.results[i].placeId;
                PlaceDetails details = PlacesApi.placeDetails(context, placeId).await();
                
                // Only add if it's actually a restaurant
                if (isRestaurant(details)) {
                    restaurants.add(details);
                }
            }
        }

        return restaurants;
    }

    /**
     * Finds first restaurant result
     */
    public PlaceDetails findRestaurantOnMaps(String restaurantName, String address) throws Exception {
        List<PlaceDetails> restaurants = findRestaurantsOnMaps(restaurantName, address);
        return restaurants.isEmpty() ? null : restaurants.get(0);
    }

    /**
     * Gets details of a specific restaurant via placeId
     */
    public PlaceDetails getPlaceDetailsByPlaceId(String placeId) throws Exception {
        GeoApiContext context = createContext();
        return PlacesApi.placeDetails(context, placeId).await();
    }

    /**
     * Searches for restaurants on maps using Restaurant entity data
     * Takes restaurant name and constructs full address from Restaurant fields
     * If search fails, throws RuntimeException with restaurant name and address details
     */
    public List<PlaceDetails> findRestaurantsOnMapFromRestaurant(Restaurant restaurant) throws Exception {
        if (restaurant == null) {
            throw new RuntimeException("Ristorante non fornito");
        }

        String restaurantName = restaurant.getName();
        if (restaurantName == null || restaurantName.trim().isEmpty()) {
            throw new RuntimeException("Nome ristorante non disponibile");
        }

        // Costruisce l'indirizzo completo dai campi del ristorante
        StringBuilder addressBuilder = new StringBuilder();
        
        if (restaurant.getAddress() != null && !restaurant.getAddress().trim().isEmpty()) {
            addressBuilder.append(restaurant.getAddress());
        }
        
        if (restaurant.getCity() != null && !restaurant.getCity().trim().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(restaurant.getCity());
        }
        
        if (restaurant.getStateProvince() != null && !restaurant.getStateProvince().trim().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(restaurant.getStateProvince());
        }
        
        if (restaurant.getPostCode() != null && !restaurant.getPostCode().trim().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(" ");
            }
            addressBuilder.append(restaurant.getPostCode());
        }
        
        if (restaurant.getCountry() != null && !restaurant.getCountry().trim().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(restaurant.getCountry());
        }

        String fullAddress = addressBuilder.toString();
        
        try {
            List<PlaceDetails> restaurants = findRestaurantsOnMaps(restaurantName, fullAddress);
            
            if (restaurants.isEmpty()) {
                String errorMessage = String.format("Ristorante non trovato su Google Maps - Nome: %s, Indirizzo: %s", 
                    restaurantName, 
                    fullAddress.isEmpty() ? "Non disponibile" : fullAddress);
                throw new RuntimeException(errorMessage);
            }
            
            return restaurants;
            
        } catch (Exception e) {
            String errorMessage = String.format("Errore durante la ricerca del ristorante su Google Maps - Nome: %s, Indirizzo: %s. Errore: %s", 
                restaurantName, 
                fullAddress.isEmpty() ? "Non disponibile" : fullAddress,
                e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Checks if a place is a restaurant based on its types
     */
    public boolean isRestaurant(PlaceDetails place) {
        if (place.types == null) {
            return false;
        }
        
        for (var type : place.types) {
            String typeStr = type.toString().toLowerCase();
            if (typeStr.contains("restaurant") || 
                typeStr.contains("food") || 
                typeStr.contains("meal_takeaway") ||
                typeStr.contains("meal_delivery") ||
                typeStr.contains("cafe") ||
                typeStr.contains("bar") ||
                typeStr.contains("bakery") ||
                typeStr.contains("pizzeria")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates GeoApiContext with API key
     */
    private GeoApiContext createContext() {
        return new GeoApiContext.Builder()
                .apiKey(googleMapsApiKey)
                .build();
    }
}
