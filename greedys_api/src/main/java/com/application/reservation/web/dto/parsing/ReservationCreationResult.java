package com.application.reservation.web.dto.parsing;

import java.time.LocalDateTime;

import com.application.customer.web.dto.matching.CustomerMatchResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the result of creating a single reservation from parsed data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreationResult {
    
    private ParsedReservationDTO parsedReservation; // Original parsed data
    private boolean processed; // Whether processing was attempted
    private boolean successful; // Whether creation was successful
    private Long reservationId; // Created reservation ID (if successful)
    private Long customerId; // Customer ID used (existing or new)
    private CustomerMatchResponse customerMatchResponse; // Customer matching results
    private String error; // Error message (if failed)
    private String message; // Success or info message
    private LocalDateTime processingTimestamp;
    private CreationSteps steps; // Detailed steps performed

    /**
     * Check if result represents a successful creation
     */
    public boolean isSuccessful() {
        return successful && error == null && reservationId != null;
    }

    /**
     * Check if customer was matched vs created new
     */
    public boolean wasCustomerMatched() {
        return customerMatchResponse != null && 
               customerMatchResponse.getBestCandidate() != null &&
               customerMatchResponse.getDecision() != null &&
               customerMatchResponse.getDecision().getType() != null &&
               (customerMatchResponse.getDecision().getType() == 
                com.application.customer.web.dto.matching.CustomerMatchDecisionDTO.DecisionType.AUTO_ATTACH ||
                customerMatchResponse.getDecision().getType() == 
                com.application.customer.web.dto.matching.CustomerMatchDecisionDTO.DecisionType.CONFIRM);
    }

    /**
     * Get customer match confidence if available
     */
    public Double getCustomerMatchConfidence() {
        if (customerMatchResponse != null && customerMatchResponse.getBestCandidate() != null) {
            return customerMatchResponse.getBestCandidate().getConfidence();
        }
        return null;
    }

    /**
     * Get overall confidence combining parsing and customer matching
     */
    public double getOverallConfidence() {
        double parsingConfidence = parsedReservation != null ? parsedReservation.getConfidence() : 0.0;
        Double matchConfidence = getCustomerMatchConfidence();
        
        if (matchConfidence != null) {
            return (parsingConfidence + matchConfidence) / 2.0;
        }
        
        return parsingConfidence;
    }

    /**
     * Get summary description of the result
     */
    public String getSummary() {
        if (!processed) {
            return "Not processed";
        }
        
        if (successful) {
            StringBuilder summary = new StringBuilder("Success: ");
            if (reservationId != null) {
                summary.append("Reservation #").append(reservationId).append(" ");
            }
            if (wasCustomerMatched()) {
                summary.append("(customer matched) ");
            } else {
                summary.append("(new customer) ");
            }
            return summary.toString().trim();
        } else {
            return "Failed: " + (error != null ? error : "Unknown error");
        }
    }

    /**
     * Get detailed processing information
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        
        info.append("Parsed: ").append(parsedReservation != null ? parsedReservation.getSummary() : "null").append("\n");
        
        if (customerMatchResponse != null) {
            info.append("Customer matching: ").append(customerMatchResponse.getCandidateCount())
                .append(" candidates, decision: ").append(customerMatchResponse.getDecision().getTypeString()).append("\n");
        }
        
        if (successful) {
            info.append("Created reservation ID: ").append(reservationId).append(", customer ID: ").append(customerId);
        } else {
            info.append("Error: ").append(error);
        }
        
        return info.toString();
    }

    /**
     * Creation steps tracking
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreationSteps {
        private boolean validationCompleted;
        private boolean customerMatchingCompleted;
        private boolean customerResolved; // Either matched or created
        private boolean reservationValidated;
        private boolean reservationCreated;
        private String failedAtStep; // Step where processing failed
        private String customerAction; // "matched", "created", "failed"
    }
}