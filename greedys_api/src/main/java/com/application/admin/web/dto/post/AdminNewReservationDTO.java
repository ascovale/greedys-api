package com.application.admin.web.dto.post;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.post.NewBaseReservationDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(name = "AdminNewReservationDTO", description = "DTO for creating a new admin reservation")
public class AdminNewReservationDTO extends NewBaseReservationDTO {
    private Long restaurantId;
    private Long userId;
    private Reservation.Status status;

    public Boolean isAnonymous() {
        return userId == null;
    }

}

