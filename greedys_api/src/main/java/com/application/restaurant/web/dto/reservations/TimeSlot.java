package com.application.restaurant.web.dto.reservations;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for available time slots
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    private LocalDateTime dateTime;
    private int availableSeats;
    private int duration; // in minutes
    private String reservationId;
}
