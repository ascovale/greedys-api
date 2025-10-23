package com.application.common.persistence.model.notification;

/**
 * Status enum per notifiche.
 * 
 * ⚠️ NOTA: Attualmente non usato nelle entità (RestaurantNotification, CustomerNotification, AdminNotification)
 * perché le entità estendono ANotification che ha solo campo "read" (Boolean).
 * 
 * Questo enum è mantenuto per:
 * 1. Metodi DAO che potrebbero essere implementati in futuro (se si aggiungono campi status/retry/priority)
 * 2. Compatibilità con codice legacy
 * 
 * MIGRAZIONE FUTURA:
 * - Se si vuole implementare lo status, aggiungere campo "status" a ANotification
 * - Se non si vuole implementare, rimuovere i metodi DAO che lo usano
 */
public enum NotificationStatus {
    /**
     * Notifica in attesa di essere pubblicata
     */
    PENDING,
    
    /**
     * Notifica pubblicata (visibile al destinatario)
     */
    PUBLISHED,
    
    /**
     * Notifica inviata con successo tramite canale (email/push)
     */
    SENT,
    
    /**
     * Invio fallito (retry possibile)
     */
    FAILED,
    
    /**
     * Notifica letta dal destinatario
     */
    READ
}
