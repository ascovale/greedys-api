package com.application.restaurant.service.google;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.application.restaurant.web.dto.google.RestaurantData;
import com.google.maps.model.PlaceDetails;

/**
 * Simple component for converting Google Places data to RestaurantData
 */
@Component
public class RestaurantDataExtractor {

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    /**
     * Extracts restaurant data from PlaceDetails
     */
    public RestaurantData extractRestaurantData(PlaceDetails place) {
        RestaurantData restaurantData = new RestaurantData();
        restaurantData.setPlaceId(place.placeId);
        restaurantData.setName(place.name);
        restaurantData.setAddress(place.formattedAddress);
        restaurantData.setPhoneNumber(place.formattedPhoneNumber);
        restaurantData.setWebsite(place.website != null ? place.website.toString() : null);
        restaurantData.setRating(place.rating > 0 ? (double) place.rating : null);
        restaurantData.setPriceLevel(place.priceLevel != null ? place.priceLevel.ordinal() : null);
        
        // Extract types if needed
        if (place.types != null) {
            List<String> types = new ArrayList<>();
            for (var type : place.types) {
                types.add(type.toString());
            }
            restaurantData.setTypes(types);
        }
        
        // Extract photos (including logos/icons)
        if (place.photos != null && place.photos.length > 0) {
            List<String> photoUrls = new ArrayList<>();
            for (var photo : place.photos) {
                // Build photo URL using Google Places Photo API
                String photoUrl = String.format(
                    "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=%s&key=%s",
                    photo.photoReference,
                    googleMapsApiKey
                );
                photoUrls.add(photoUrl);
            }
            restaurantData.setPhotos(photoUrls);
        }

        return restaurantData;
    }
    
    /**
     * Extracts the restaurant logo (first photo) from PlaceDetails
     */
    public String extractRestaurantLogo(PlaceDetails place) {
        if (place.photos != null && place.photos.length > 0) {
            // The first photo is often the restaurant's logo or main image
            return String.format(
                "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=%s&key=%s",
                place.photos[0].photoReference,
                googleMapsApiKey
            );
        }
        return null;
    }
}

