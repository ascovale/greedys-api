package com.application.restaurant.web.dto.reservation;

import java.time.Instant;

import com.application.common.web.dto.reservations.ReservationDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for WebSocket real-time reservation events.
 * 
 * Sent to all connected restaurant staff when:
 * - A new reservation arrives
 * - A reservation is accepted
 * - A reservation is rejected
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEventDTO {
    
    /**
     * Event type: RESERVATION_CREATED, RESERVATION_ACCEPTED, RESERVATION_REJECTED
     */
    private String type;
    
    /**
     * The reservation details (full DTO)
     */
    private ReservationDTO reservation;
    
    /**
     * Timestamp when the event occurred
     */
    private Instant timestamp;
}
