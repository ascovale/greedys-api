package com.application.customer.web.post;

import com.application.common.web.dto.post.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(name = "CustomerNewReservationDTO", description = "DTO for creating a new customer reservation")
public class CustomerNewReservationDTO extends NewBaseReservationDTO {
    private Long restaurantId;
}

