package com.application.reservation.web.dto.parsing;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reservation text parsing response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationParseResponse {
    
    private String originalText;
    private List<ParsedReservationDTO> parsedReservations;
    private int totalReservations;
    private double overallConfidence;
    private long processingTime; // Processing time in milliseconds
    private String error; // Error message if parsing failed
    private ReservationParseStats stats;

    /**
     * Check if parsing was successful
     */
    public boolean isSuccessful() {
        return error == null && parsedReservations != null && !parsedReservations.isEmpty();
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
     * Get count of medium confidence reservations
     */
    public long getMediumConfidenceCount() {
        if (parsedReservations == null) return 0;
        return parsedReservations.stream()
                .filter(ParsedReservationDTO::isMediumConfidence)
                .count();
    }

    /**
     * Get count of low confidence reservations
     */
    public long getLowConfidenceCount() {
        if (parsedReservations == null) return 0;
        return parsedReservations.stream()
                .filter(ParsedReservationDTO::isLowConfidence)
                .count();
    }

    /**
     * Get count of valid reservations
     */
    public long getValidReservationsCount() {
        if (parsedReservations == null) return 0;
        return parsedReservations.stream()
                .filter(ParsedReservationDTO::isValid)
                .count();
    }

    /**
     * Get overall confidence level
     */
    public String getConfidenceLevel() {
        if (overallConfidence >= 0.8) return "HIGH";
        if (overallConfidence >= 0.5) return "MEDIUM";
        return "LOW";
    }

    /**
     * Get parsing quality assessment
     */
    public String getQualityAssessment() {
        if (!isSuccessful()) return "FAILED";
        
        long validCount = getValidReservationsCount();
        double validRatio = (double) validCount / totalReservations;
        
        if (validRatio >= 0.9 && overallConfidence >= 0.8) return "EXCELLENT";
        if (validRatio >= 0.7 && overallConfidence >= 0.6) return "GOOD";
        if (validRatio >= 0.5 && overallConfidence >= 0.4) return "FAIR";
        return "POOR";
    }

    /**
     * Generate summary report
     */
    public String getSummaryReport() {
        if (!isSuccessful()) {
            return String.format("Parsing failed: %s", error);
        }

        StringBuilder report = new StringBuilder();
        report.append(String.format("Parsed %d reservations in %d ms\n", 
                totalReservations, processingTime));
        report.append(String.format("Overall confidence: %.2f (%s)\n", 
                overallConfidence, getConfidenceLevel()));
        report.append(String.format("Quality: %s\n", getQualityAssessment()));
        report.append(String.format("Valid: %d, High: %d, Medium: %d, Low: %d\n",
                getValidReservationsCount(), getHighConfidenceCount(),
                getMediumConfidenceCount(), getLowConfidenceCount()));

        return report.toString();
    }

    /**
     * Statistics about parsing results
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationParseStats {
        private int phonesFound;
        private int emailsFound;
        private int namesFound;
        private int datesFound;
        private int timesFound;
        private int peopleCounts;
        private int totalSegments;
        private double avgConfidence;
        private String mostConfidentReservation;
        private List<String> extractionWarnings;
    }
}