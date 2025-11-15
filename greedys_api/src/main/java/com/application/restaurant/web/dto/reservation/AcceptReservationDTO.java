package com.application.restaurant.web.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for accepting a reservation.
 * 
 * Contains optional details like table number and notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptReservationDTO {
    
    /**
     * Table number where the reservation will be seated.
     * Optional.
     */
    private Integer tableNumber;
    
    /**
     * Additional notes for the staff (e.g., "near window", "high chair needed").
     * Optional.
     */
    private String notes;
}
