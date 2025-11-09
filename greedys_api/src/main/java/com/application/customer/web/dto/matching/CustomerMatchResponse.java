package com.application.customer.web.dto.matching;

import java.util.List;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the complete response from customer matching operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMatchResponse {
    
    @Builder.Default
    private List<CustomerCandidateDTO> candidates = new ArrayList<>();
    
    private CustomerMatchDecisionDTO decision;

    /**
     * Check if any candidates were found
     */
    public boolean hasCandidates() {
        return candidates != null && !candidates.isEmpty();
    }

    /**
     * Get the number of candidates found
     */
    public int getCandidateCount() {
        return candidates != null ? candidates.size() : 0;
    }

    /**
     * Get the best candidate (highest confidence)
     */
    public CustomerCandidateDTO getBestCandidate() {
        if (!hasCandidates()) {
            return null;
        }
        
        return candidates.stream()
            .max((c1, c2) -> Double.compare(c1.getConfidence(), c2.getConfidence()))
            .orElse(null);
    }

    /**
     * Get candidates with high confidence (>= 0.9)
     */
    public List<CustomerCandidateDTO> getHighConfidenceCandidates() {
        if (!hasCandidates()) {
            return new ArrayList<>();
        }
        
        return candidates.stream()
            .filter(CustomerCandidateDTO::isHighConfidence)
            .toList();
    }

    /**
     * Get candidates with medium confidence (0.7 - 0.89)
     */
    public List<CustomerCandidateDTO> getMediumConfidenceCandidates() {
        if (!hasCandidates()) {
            return new ArrayList<>();
        }
        
        return candidates.stream()
            .filter(CustomerCandidateDTO::isMediumConfidence)
            .toList();
    }

    /**
     * Add a candidate to the response
     */
    public void addCandidate(CustomerCandidateDTO candidate) {
        if (this.candidates == null) {
            this.candidates = new ArrayList<>();
        }
        this.candidates.add(candidate);
    }
}