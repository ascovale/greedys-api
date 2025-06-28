package com.application.web.dto.post.restaurant;

import com.application.web.dto.post.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RestaurantNewReservationDTO", description = "DTO for creating a new restaurant reservation")
public class RestaurantNewReservationDTO extends NewBaseReservationDTO {
    private Long customerId;

    public Long getCustomerId() {
        if (customerId == null) {
            throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
        }
        return customerId;
    }
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

}

