package com.application.restaurant.web.dto.verification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for restaurant verification requests
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
@Schema(name = "VerificationRequest", description = "Request for restaurant phone verification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequestDTO {
    
    @Schema(description = "Restaurant ID to verify", example = "123", required = true)
    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;
    
    @Schema(description = "Phone number to verify (Italian format)", example = "+393331234567", required = true)
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
}
