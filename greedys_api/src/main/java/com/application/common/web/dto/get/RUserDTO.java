package com.application.common.web.dto.get;

import com.application.restaurant.persistence.model.user.RUser;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RUserDTO", description = "DTO for restaurant user details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RUserDTO {

    private String username;
    private Long restaurantId;

    public RUserDTO(RUser user) {
        this.username = user.getEmail();
        this.restaurantId = user.getRestaurant().getId();
    }
}