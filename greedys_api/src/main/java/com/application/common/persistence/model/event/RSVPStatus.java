package com.application.common.persistence.model.event;

/**
 * ⭐ RSVP STATUS ENUM
 * 
 * Stato della risposta a un evento (RSVP).
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum RSVPStatus {
    
    /**
     * Interessato
     */
    INTERESTED,
    
    /**
     * Parteciperà
     */
    GOING,
    
    /**
     * Forse parteciperà
     */
    MAYBE,
    
    /**
     * Non parteciperà
     */
    NOT_GOING,
    
    /**
     * In lista d'attesa
     */
    WAITLIST,
    
    /**
     * Confermato (con pagamento/check-in)
     */
    CONFIRMED,
    
    /**
     * Cancellato
     */
    CANCELLED
}
