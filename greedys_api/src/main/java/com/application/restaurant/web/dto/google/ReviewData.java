package com.application.restaurant.web.dto.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Review data from Google Places API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewData {
    private String authorName;
    private Integer rating;
    private String text;
    private Long time;
}
