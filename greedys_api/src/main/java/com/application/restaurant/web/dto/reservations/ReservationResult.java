package com.application.restaurant.web.dto.reservations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for reservation operation results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResult {
    private boolean success;
    private String message;
    private ReservationData reservationData;
}
