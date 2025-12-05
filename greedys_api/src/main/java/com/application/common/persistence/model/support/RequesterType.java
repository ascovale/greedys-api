package com.application.common.persistence.model.support;

/**
 * ⭐ REQUESTER TYPE ENUM
 * 
 * Tipo di utente che ha aperto il ticket.
 * Serve per routing al bot/agente corretto.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum RequesterType {
    /**
     * Cliente finale
     */
    CUSTOMER,
    
    /**
     * Staff ristorante
     */
    RESTAURANT,
    
    /**
     * Staff agenzia
     */
    AGENCY,
    
    /**
     * Amministratore (raro)
     */
    ADMIN
}
