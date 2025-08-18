package com.application.common.web.dto.restaurant;

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

    private Long id;
    private String username;
    private Long restaurantId;
}
