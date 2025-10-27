package com.application.common.web.dto.restaurant;

import com.application.common.web.dto.security.UserAuthResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(name = "RUserHubDTO", description = "DTO for restaurant user hub details")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RUserHubDTO extends UserAuthResponse {

    @Schema(description = "User hub ID", example = "1")
    private Long id;

    @Schema(description = "Email address", example = "info@lasoffittarenovatio.it")
    private String email;

    @Schema(description = "First name", example = "Stefano")
    private String firstName;

    @Schema(description = "Last name", example = "Di Michele")
    private String lastName;

    @Schema(description = "Account status", example = "ENABLED")
    private String status;
    
    @Override
    public String getUsername() {
        return email;
    }
}
