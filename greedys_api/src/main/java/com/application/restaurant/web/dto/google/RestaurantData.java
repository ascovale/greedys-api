package com.application.restaurant.web.dto.google;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Restaurant data from Google Places API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantData {
    private String placeId;
    private String name;
    private String address;
    private String phoneNumber;
    private String website;
    private Double rating;
    private Integer totalReviews;
    private List<String> types;
    private List<String> openingHours;
    private List<String> photos;
    private List<ReviewData> reviews;
    private Integer priceLevel;
}
