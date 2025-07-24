package com.application.restaurant.web.dto.post;

import com.application.common.web.dto.post.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RestaurantNewReservationDTO", description = "DTO for creating a new restaurant reservation")
public class RestaurantNewReservationDTO extends NewBaseReservationDTO {
    
    private String userEmail;
    private String userPhoneNumber;

}

