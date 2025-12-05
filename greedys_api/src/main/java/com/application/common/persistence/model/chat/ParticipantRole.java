package com.application.common.persistence.model.chat;

/**
 * ⭐ PARTICIPANT ROLE ENUM
 * 
 * Ruolo del partecipante nella conversazione:
 * - OWNER: Creatore della conversazione (può eliminare)
 * - ADMIN: Amministratore (può aggiungere/rimuovere)
 * - MEMBER: Partecipante normale
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum ParticipantRole {
    /**
     * Creatore e proprietario della conversazione
     */
    OWNER,
    
    /**
     * Amministratore con permessi di gestione
     */
    ADMIN,
    
    /**
     * Partecipante normale
     */
    MEMBER
}
