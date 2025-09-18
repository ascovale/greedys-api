package com.application.restaurant.web.dto.reservations;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for reservation requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private String placeId;
    private LocalDateTime dateTime;
    private int partySize;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String specialRequests;
}
