package com.application.restaurant.web.dto.reservation;

import com.application.common.web.dto.reservations.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RestaurantNewReservationDTO", description = "DTO for creating a new restaurant reservation")
public class RestaurantNewReservationDTO extends NewBaseReservationDTO {
    
    private String userEmail;
    private String userPhoneNumber;

}

