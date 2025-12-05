package com.application.common.persistence.model.chat;

/**
 * ⭐ CONVERSATION TYPE ENUM
 * 
 * Tipo di conversazione chat:
 * - DIRECT: 1:1 tra due utenti
 * - GROUP: N:N gruppo di utenti
 * - SUPPORT: Ticket di supporto
 * - RESERVATION: Chat legata a una prenotazione
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum ConversationType {
    /**
     * Direct message 1:1 tra due utenti
     */
    DIRECT,
    
    /**
     * Chat di gruppo con N partecipanti
     */
    GROUP,
    
    /**
     * Ticket di supporto/assistenza
     */
    SUPPORT,
    
    /**
     * Chat legata a una specifica prenotazione
     */
    RESERVATION
}
