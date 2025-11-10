package com.application.common.reservation.web.dto.parsing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a parsed reservation candidate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedReservationDTO {
    
    private UUID restaurantId;
    private String customerName;
    private String phoneNumber;
    private String email;
    private LocalDateTime reservationDateTime;
    private Integer numberOfPeople;
    private String notes;
    private String originalSegment; // Original text segment that was parsed
    private double confidence; // Confidence score 0.0-1.0
    private List<String> extractionLog; // Log of what was extracted
    
    /**
     * Check if parsed reservation has minimum required data
     */
    public boolean isValid() {
        return restaurantId != null && 
               (hasContactInfo() && hasBasicReservationInfo());
    }

    /**
     * Check if has contact information
     */
    public boolean hasContactInfo() {
        return (phoneNumber != null && !phoneNumber.trim().isEmpty()) ||
               (email != null && !email.trim().isEmpty()) ||
               (customerName != null && !customerName.trim().isEmpty());
    }

    /**
     * Check if has basic reservation info
     */
    public boolean hasBasicReservationInfo() {
        return reservationDateTime != null || numberOfPeople != null;
    }

    /**
     * Check if has high confidence (>= 0.8)
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * Check if has medium confidence (0.5-0.79)
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.8;
    }

    /**
     * Check if has low confidence (< 0.5)
     */
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }

    /**
     * Get confidence level as string
     */
    public String getConfidenceLevel() {
        if (isHighConfidence()) return "HIGH";
        if (isMediumConfidence()) return "MEDIUM";
        return "LOW";
    }

    /**
     * Get missing required fields
     */
    public List<String> getMissingFields() {
        List<String> missing = new java.util.ArrayList<>();
        
        if (restaurantId == null) missing.add("restaurantId");
        if (!hasContactInfo()) missing.add("contactInfo");
        if (reservationDateTime == null) missing.add("dateTime");
        if (numberOfPeople == null) missing.add("numberOfPeople");
        
        return missing;
    }

    /**
     * Get validation score (0-100)
     */
    public int getValidationScore() {
        int score = 0;
        
        if (restaurantId != null) score += 10;
        if (customerName != null && !customerName.trim().isEmpty()) score += 20;
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) score += 25;
        if (email != null && !email.trim().isEmpty()) score += 15;
        if (reservationDateTime != null) score += 20;
        if (numberOfPeople != null && numberOfPeople > 0) score += 10;
        
        return Math.min(score, 100);
    }

    /**
     * Create a summary description
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (customerName != null) {
            summary.append("Nome: ").append(customerName).append(" ");
        }
        
        if (phoneNumber != null) {
            summary.append("Tel: ").append(phoneNumber).append(" ");
        }
        
        if (reservationDateTime != null) {
            summary.append("Data: ").append(reservationDateTime).append(" ");
        }
        
        if (numberOfPeople != null) {
            summary.append("Persone: ").append(numberOfPeople).append(" ");
        }
        
        return summary.toString().trim();
    }
}