package com.application.reservation.web.dto.parsing;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for batch reservation creation input
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchReservationCreateInput {
    
    @NotNull(message = "Restaurant ID is required")
    private UUID restaurantId;
    
    @NotEmpty(message = "At least one parsed reservation is required")
    @Valid
    private List<ParsedReservationDTO> parsedReservations;
    
    @Builder.Default
    private boolean autoConfirmMatches = false; // Auto-confirm medium confidence customer matches
    
    @Builder.Default
    private boolean createMissingCustomers = true; // Create new customers if no match found
    
    @Builder.Default
    private boolean validateDateTime = true; // Validate reservation date/time availability
    
    @Builder.Default
    private boolean allowPastDates = false; // Allow creating reservations for past dates
    
    private String createdBy; // User ID or system identifier creating the reservations
    
    private String batchNote; // Optional note for the entire batch
    
    private String source; // Source of the batch (e.g., "phone", "email", "whatsapp", "import")

    /**
     * Check if input is valid
     */
    public boolean isValid() {
        return restaurantId != null && 
               parsedReservations != null && 
               !parsedReservations.isEmpty() &&
               parsedReservations.stream().allMatch(ParsedReservationDTO::isValid);
    }

    /**
     * Get count of valid parsed reservations
     */
    public long getValidReservationCount() {
        if (parsedReservations == null) return 0;
        return parsedReservations.stream()
                .filter(ParsedReservationDTO::isValid)
                .count();
    }

    /**
     * Get count of high confidence reservations
     */
    public long getHighConfidenceCount() {
        if (parsedReservations == null) return 0;
        return parsedReservations.stream()
                .filter(ParsedReservationDTO::isHighConfidence)
                .count();
    }

    /**
     * Get average confidence score
     */
    public double getAverageConfidence() {
        if (parsedReservations == null || parsedReservations.isEmpty()) return 0.0;
        return parsedReservations.stream()
                .mapToDouble(ParsedReservationDTO::getConfidence)
                .average()
                .orElse(0.0);
    }

    /**
     * Check if batch should proceed with creation
     */
    public boolean shouldProceed() {
        return isValid() && getValidReservationCount() > 0;
    }

    /**
     * Get batch summary
     */
    public String getBatchSummary() {
        return String.format("Batch for restaurant %s: %d reservations (%.1f%% valid, avg confidence %.2f)",
                restaurantId, 
                parsedReservations != null ? parsedReservations.size() : 0,
                getValidReservationCount() * 100.0 / (parsedReservations != null ? parsedReservations.size() : 1),
                getAverageConfidence());
    }
}