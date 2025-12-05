package com.application.common.persistence.model.support;

/**
 * ⭐ TICKET CATEGORY ENUM
 * 
 * Categorie di ticket per routing automatico e reporting:
 * - RESERVATION: Problemi con prenotazioni
 * - PAYMENT: Problemi di pagamento
 * - ACCOUNT: Gestione account utente
 * - TECHNICAL: Problemi tecnici app/sito
 * - FEEDBACK: Feedback e suggerimenti
 * - OTHER: Altro
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum TicketCategory {
    /**
     * Problemi relativi a prenotazioni
     */
    RESERVATION,
    
    /**
     * Problemi di pagamento
     */
    PAYMENT,
    
    /**
     * Gestione account (login, profilo, etc)
     */
    ACCOUNT,
    
    /**
     * Problemi tecnici dell'app/sito
     */
    TECHNICAL,
    
    /**
     * Feedback e suggerimenti
     */
    FEEDBACK,
    
    /**
     * Altro (non categorizzato)
     */
    OTHER
}
