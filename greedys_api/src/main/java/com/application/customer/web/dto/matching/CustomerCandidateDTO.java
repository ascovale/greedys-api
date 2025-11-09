package com.application.customer.web.dto.matching;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a potential customer match candidate with confidence score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCandidateDTO {
    
    private UUID id;
    private String displayName;
    private String phoneE164;
    private String email;
    private double confidence;
    private String reason;

    /**
     * Check if this candidate has high confidence (>= 0.9)
     */
    public boolean isHighConfidence() {
        return confidence >= 0.90;
    }

    /**
     * Check if this candidate has medium confidence (0.7 - 0.89)
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.70 && confidence < 0.90;
    }

    /**
     * Check if this candidate has low confidence (< 0.7)
     */
    public boolean isLowConfidence() {
        return confidence < 0.70;
    }

    /**
     * Get confidence as percentage string
     */
    public String getConfidencePercentage() {
        return String.format("%.1f%%", confidence * 100);
    }

    /**
     * Get confidence level as string
     */
    public String getConfidenceLevel() {
        if (isHighConfidence()) {
            return "HIGH";
        } else if (isMediumConfidence()) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}