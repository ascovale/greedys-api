package com.application.web.dto.post.restaurant;

import com.application.web.dto.post.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Schema(name = "RestaurantNewReservationDTO", description = "DTO for creating a new restaurant reservation")
public class RestaurantNewReservationDTO extends NewBaseReservationDTO {
    
    private String userEmail;
    private String userPhoneNumber;
    private String userName;

}

