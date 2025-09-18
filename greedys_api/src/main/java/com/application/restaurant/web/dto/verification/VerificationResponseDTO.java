package com.application.restaurant.web.dto.verification;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for restaurant verification responses
 * 
 * @author Restaurant Verification Team
 * @since 1.0
 */
@Schema(name = "VerificationResponse", description = "Response for restaurant phone verification operations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponseDTO {
    
    @Schema(description = "Indicates if the operation was successful", example = "true")
    private boolean success;
    
    @Schema(description = "Current verification status")
    private VerificationStatus status;
    
    @Schema(description = "Response message", example = "Verification code sent successfully")
    private String message;
    
    @Schema(description = "Twilio verification SID for tracking", example = "VE12345...")
    private String verificationSid;
    
    @Schema(description = "Masked phone number", example = "+39***1234")
    private String phoneNumber;
    
    @Schema(description = "When verification was created")
    private LocalDateTime createdAt;
    
    @Schema(description = "When verification expires")
    private LocalDateTime expiresAt;
    
    @Schema(description = "When verification was completed (if successful)")
    private LocalDateTime verifiedAt;
    
    @Schema(description = "Number of verification attempts made", example = "1")
    private Integer attempts;
    
    @Schema(description = "Number of attempts remaining", example = "2")
    private Integer attemptsRemaining;
}
