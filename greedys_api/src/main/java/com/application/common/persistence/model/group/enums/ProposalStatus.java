package com.application.common.persistence.model.group.enums;

/**
 * Status of an agency proposal negotiation.
 */
public enum ProposalStatus {
    
    /**
     * Draft - not yet sent to agency
     */
    DRAFT("Bozza"),
    
    /**
     * Sent to agency, waiting for response
     */
    SENT("Inviata"),
    
    /**
     * Sent to agency, waiting for response (alias)
     */
    PENDING_AGENCY("In Attesa Risposta Agenzia"),
    
    /**
     * Agency responded, waiting for restaurant
     */
    PENDING_RESTAURANT("In Attesa Risposta Ristorante"),
    
    /**
     * Counter-proposal from one party
     */
    COUNTER_PROPOSAL("Controproposta"),
    
    /**
     * Active negotiation between parties
     */
    NEGOTIATING("In Negoziazione"),
    
    /**
     * Proposal is active and can be used for bookings
     */
    ACTIVE("Attiva"),
    
    /**
     * Both parties have accepted
     */
    ACCEPTED("Accettata"),
    
    /**
     * Proposal was rejected
     */
    REJECTED("Rifiutata"),
    
    /**
     * Proposal validity expired
     */
    EXPIRED("Scaduta"),
    
    /**
     * Proposal was cancelled
     */
    CANCELLED("Annullata");
    
    private final String displayName;
    
    ProposalStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if proposal can still be modified
     */
    public boolean isModifiable() {
        return this == DRAFT || this == SENT || this == PENDING_AGENCY || 
               this == PENDING_RESTAURANT || this == NEGOTIATING || this == COUNTER_PROPOSAL;
    }
    
    /**
     * Check if proposal is in a final state
     */
    public boolean isFinal() {
        return this == ACCEPTED || this == REJECTED || 
               this == EXPIRED || this == CANCELLED;
    }
    
    /**
     * Check if proposal is open for booking
     */
    public boolean isBookable() {
        return this == ACTIVE || this == ACCEPTED;
    }
}
