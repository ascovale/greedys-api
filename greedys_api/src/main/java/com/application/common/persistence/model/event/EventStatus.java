package com.application.common.persistence.model.event;

/**
 * ⭐ EVENT STATUS ENUM
 * 
 * Stato dell'evento.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
public enum EventStatus {
    
    /**
     * Evento in bozza (non pubblicato)
     */
    DRAFT,
    
    /**
     * Evento pubblicato e visibile
     */
    PUBLISHED,
    
    /**
     * Evento pieno (posti esauriti)
     */
    SOLD_OUT,
    
    /**
     * Evento cancellato
     */
    CANCELLED,
    
    /**
     * Evento concluso
     */
    COMPLETED,
    
    /**
     * Evento posticipato
     */
    POSTPONED
}
