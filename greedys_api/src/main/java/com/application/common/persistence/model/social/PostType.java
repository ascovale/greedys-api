package com.application.common.persistence.model.social;

/**
 * ⭐ POST TYPE ENUM
 * 
 * Tipo di post social.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum PostType {
    
    /**
     * Post con testo e/o immagini
     */
    REGULAR,
    
    /**
     * Post condiviso da un altro utente
     */
    SHARED,
    
    /**
     * Alias per SHARED (condivisione)
     */
    SHARE,
    
    /**
     * Post generato automaticamente (check-in, recensione, etc.)
     */
    AUTO_GENERATED,
    
    /**
     * Check-in in un ristorante
     */
    CHECKIN,
    
    /**
     * Recensione di un ristorante
     */
    REVIEW,
    
    /**
     * Post annuncio del ristorante
     */
    ANNOUNCEMENT,
    
    /**
     * Post con evento
     */
    EVENT,
    
    /**
     * Post con promozione/offerta
     */
    PROMOTION,
    
    /**
     * Story (contenuto effimero)
     */
    STORY
}
