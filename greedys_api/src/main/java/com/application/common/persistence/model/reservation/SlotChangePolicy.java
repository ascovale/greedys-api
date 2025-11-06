package com.application.common.persistence.model.reservation;

/**
 * Definisce come gestire le modifiche agli slot quando ci sono prenotazioni future esistenti
 */
public enum SlotChangePolicy {
    /**
     * Le nuove prenotazioni usano il nuovo slot, quelle esistenti restano sul vecchio
     */
    HARD_CUT,
    
    /**
     * Invia notifica ai clienti per confermare il nuovo orario
     */
    NOTIFY_CUSTOMERS,
    
    /**
     * Migra automaticamente le prenotazioni se compatibili con i nuovi orari
     */
    AUTO_MIGRATE
}