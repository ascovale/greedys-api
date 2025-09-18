package com.application.restaurant.web.dto.reservations;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for restaurant availability information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAvailability {
    private String placeId;
    private LocalDateTime requestedDateTime;
    private int partySize;
    private boolean available;
    private List<TimeSlot> availableSlots;
    private String message;
}
