package com.application.web.dto.post.customer;

import com.application.web.dto.post.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@Schema(name = "CustomerNewReservationDTO", description = "DTO for creating a new customer reservation")
public class CustomerNewReservationDTO extends NewBaseReservationDTO {
    private Long restaurantId;

    public Long getRestaurantId() {
        return restaurantId;
    }
    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }
}

