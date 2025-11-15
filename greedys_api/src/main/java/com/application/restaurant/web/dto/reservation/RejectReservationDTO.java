package com.application.restaurant.web.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for rejecting a reservation.
 * 
 * Contains rejection reason.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectReservationDTO {
    
    /**
     * Reason for rejection (e.g., "fully booked", "no suitable slot", "customer request").
     * Optional but recommended.
     */
    private String reason;
}
