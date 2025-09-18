package com.application.restaurant.web.dto.reservations;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for reservation data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationData {
    private String reservationId;
    private String placeId;
    private LocalDateTime dateTime;
    private int partySize;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String status;
    private String confirmationNumber;
    private String specialRequests;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
