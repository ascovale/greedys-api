package com.application.customer.web.dto.matching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the decision on how to handle a customer match
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMatchDecisionDTO {
    
    /**
     * Decision type enum
     */
    public enum DecisionType {
        AUTO_ATTACH,  // Automatically attach to existing customer (high confidence)
        CONFIRM,      // Ask user to confirm match (medium confidence)
        CREATE_NEW    // Create new customer (low/no confidence)
    }

    private DecisionType type;
    private String reason;

    /**
     * Create an AUTO_ATTACH decision
     */
    public static CustomerMatchDecisionDTO autoAttach(String reason) {
        return CustomerMatchDecisionDTO.builder()
            .type(DecisionType.AUTO_ATTACH)
            .reason(reason)
            .build();
    }

    /**
     * Create a CONFIRM decision
     */
    public static CustomerMatchDecisionDTO confirm(String reason) {
        return CustomerMatchDecisionDTO.builder()
            .type(DecisionType.CONFIRM)
            .reason(reason)
            .build();
    }

    /**
     * Create a CREATE_NEW decision
     */
    public static CustomerMatchDecisionDTO createNew(String reason) {
        return CustomerMatchDecisionDTO.builder()
            .type(DecisionType.CREATE_NEW)
            .reason(reason)
            .build();
    }

    /**
     * Get decision type as string
     */
    public String getTypeString() {
        return type != null ? type.name() : null;
    }
}