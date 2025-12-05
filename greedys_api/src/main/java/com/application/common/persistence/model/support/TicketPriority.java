package com.application.common.persistence.model.support;

/**
 * ⭐ TICKET PRIORITY ENUM
 * 
 * Priorità di un ticket di supporto:
 * - LOW: Bassa priorità, problema minore
 * - NORMAL: Priorità normale
 * - HIGH: Alta priorità, richiede attenzione
 * - URGENT: Urgente, da risolvere immediatamente
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum TicketPriority {
    /**
     * Bassa priorità - problema minore o domanda generica
     */
    LOW,
    
    /**
     * Priorità normale - standard
     */
    NORMAL,
    
    /**
     * Alta priorità - richiede attenzione tempestiva
     */
    HIGH,
    
    /**
     * Urgente - da risolvere immediatamente
     */
    URGENT
}
