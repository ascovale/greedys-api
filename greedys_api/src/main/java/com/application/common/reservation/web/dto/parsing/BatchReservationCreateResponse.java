package com.application.common.reservation.web.dto.parsing;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for batch reservation creation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchReservationCreateResponse {
    
    private UUID restaurantId;
    private List<ReservationCreationResult> results;
    private int totalProcessed;
    private int successCount;
    private int errorCount;
    private long processingTime; // Processing time in milliseconds
    private String error; // Overall error if batch failed completely
    private BatchCreationStats stats;

    /**
     * Check if batch processing was successful
     */
    public boolean isSuccessful() {
        return error == null && successCount > 0;
    }

    /**
     * Check if all reservations were created successfully
     */
    public boolean isCompleteSuccess() {
        return error == null && errorCount == 0 && successCount == totalProcessed;
    }

    /**
     * Get success rate percentage
     */
    public double getSuccessRate() {
        if (totalProcessed == 0) return 0.0;
        return (double) successCount * 100.0 / totalProcessed;
    }

    /**
     * Get error rate percentage
     */
    public double getErrorRate() {
        if (totalProcessed == 0) return 0.0;
        return (double) errorCount * 100.0 / totalProcessed;
    }

    /**
     * Get processing summary
     */
    public String getSummary() {
        return String.format("Processed %d reservations in %d ms: %d successful (%.1f%%), %d errors",
                totalProcessed, processingTime, successCount, getSuccessRate(), errorCount);
    }

    /**
     * Get list of successful reservation IDs
     */
    public List<Long> getSuccessfulReservationIds() {
        if (results == null) return List.of();
        return results.stream()
                .filter(ReservationCreationResult::isSuccessful)
                .map(ReservationCreationResult::getReservationId)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Get list of failed reservations with reasons
     */
    public List<String> getFailureReasons() {
        if (results == null) return List.of();
        return results.stream()
                .filter(result -> !result.isSuccessful())
                .map(ReservationCreationResult::getError)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Statistics for batch creation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchCreationStats {
        private int customersMatched;
        private int customersCreated;
        private int reservationsCreated;
        private int validationErrors;
        private int customerMatchErrors;
        private int reservationCreationErrors;
        private double avgProcessingTimePerReservation;
        private String mostCommonError;
    }
}