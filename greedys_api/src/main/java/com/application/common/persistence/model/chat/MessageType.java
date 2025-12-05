package com.application.common.persistence.model.chat;

/**
 * ⭐ MESSAGE TYPE ENUM
 * 
 * Tipo di messaggio nella chat:
 * - TEXT: Messaggio di testo normale
 * - IMAGE: Immagine
 * - FILE: File allegato
 * - SYSTEM: Messaggio di sistema (es: "Mario è entrato nel gruppo")
 * - BOT: Risposta automatica del bot
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum MessageType {
    /**
     * Messaggio di testo normale
     */
    TEXT,
    
    /**
     * Immagine allegata
     */
    IMAGE,
    
    /**
     * File allegato generico
     */
    FILE,
    
    /**
     * Messaggio di sistema (join/leave, etc)
     */
    SYSTEM,
    
    /**
     * Risposta automatica del bot di supporto
     */
    BOT
}
