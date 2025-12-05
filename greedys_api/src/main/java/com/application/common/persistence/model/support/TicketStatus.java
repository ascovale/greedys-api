package com.application.common.persistence.model.support;

/**
 * ⭐ TICKET STATUS ENUM
 * 
 * Stati del ciclo di vita di un ticket di supporto:
 * - OPEN: Appena creato, in attesa di risposta
 * - IN_PROGRESS: Assegnato a un agente, in lavorazione
 * - WAITING_CUSTOMER: In attesa di risposta del cliente
 * - ESCALATED: Escalato a livello superiore
 * - RESOLVED: Risolto ma non ancora chiuso
 * - CLOSED: Chiuso definitivamente
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum TicketStatus {
    /**
     * Ticket appena aperto, in attesa di prima risposta
     */
    OPEN,
    
    /**
     * Ticket in lavorazione da un agente
     */
    IN_PROGRESS,
    
    /**
     * In attesa di risposta dal cliente
     */
    WAITING_CUSTOMER,
    
    /**
     * Escalato a livello superiore
     */
    ESCALATED,
    
    /**
     * In attesa di risposta dal BOT (risposta automatica inviata)
     */
    WAITING_BOT,
    
    /**
     * Problema risolto, in attesa di conferma chiusura
     */
    RESOLVED,
    
    /**
     * Ticket chiuso definitivamente
     */
    CLOSED
}
