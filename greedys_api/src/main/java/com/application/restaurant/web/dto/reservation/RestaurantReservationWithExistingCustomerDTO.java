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
@Schema(name = "RestaurantReservationWithExistingCustomerDTO", description = "DTO for creating a new restaurant reservation with existing customer from agenda")
public class RestaurantReservationWithExistingCustomerDTO extends NewBaseReservationDTO {
    
    @Schema(description = "ID of existing customer from restaurant agenda. Customer email/phone will be taken from agenda, but userName can be overridden for this specific reservation.", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long customerId;

}