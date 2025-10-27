package com.application.common.web.dto.restaurant;

import com.application.common.web.dto.security.UserAuthResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(name = "RUserDTO", description = "DTO for restaurant user details")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RUserDTO extends UserAuthResponse {

    private Long id;
    private String username;
    private Long restaurantId;
    
    @Override
    public String getEmail() {
        return username; // Email and username are the same for RUser
    }
}
