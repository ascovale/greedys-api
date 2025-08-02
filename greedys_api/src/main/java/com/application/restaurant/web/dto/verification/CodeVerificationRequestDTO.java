package com.application.restaurant.web.dto.verification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OTP code verification requests
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
@Schema(name = "CodeVerificationRequest", description = "Request for OTP code verification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeVerificationRequestDTO {
    
    @Schema(description = "OTP verification code", example = "123456", required = true)
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "Verification code must be 4-6 digits")
    private String code;
}
