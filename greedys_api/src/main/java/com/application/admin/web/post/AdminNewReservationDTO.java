package com.application.admin.web.post;

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
    private Long restaurant_id;
    private Long user_id;
    private Boolean accept;
    private Boolean seated;
    private Boolean noShow;
    private Boolean rejected;

    public Boolean isAnonymous() {
        return user_id == null;
    }
    
    public Long getUser_id() {
        if (user_id == null) {
            throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
        }
        return user_id;
    }
}

