package com.application.admin.web.dto.reservation;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.reservations.NewBaseReservationDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "AdminNewReservationDTO", description = "DTO for creating a new admin reservation")
public class AdminNewReservationDTO extends NewBaseReservationDTO {
    private Long restaurantId;
    private Long userId;
    private Reservation.Status status;

    public Boolean isAnonymous() {
        return userId == null;
    }
}
