package com.application.common.persistence.model.notification;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit log per azioni su notifiche (pattern "First-to-Act")
 * 
 * Traccia chi ha completato un'azione richiesta da una notifica broadcast.
 * 
 * Esempio:
 * - Notifica broadcast: "⚠️ Conferma prenotazione urgente"
 * - Owner clicca "Conferma" → salva NotificationAction (actor=Owner, actionType=CONFIRMED)
 * - Sistema rimuove notifica urgente da tutti gli altri staff
 * - Sistema crea notifica informativa: "✅ Owner ha confermato"
 * 
 * CONSTRAINT: Una notifica può avere al massimo 1 azione completata (first-to-act)
 */
@Entity
@Table(name = "notification_actions", indexes = {
    @Index(name = "idx_notification_action", columnList = "notification_id, action_type"),
    @Index(name = "idx_actor_acted", columnList = "actor_id, acted_at"),
    @Index(name = "idx_acted_at", columnList = "acted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID della notifica su cui è stata effettuata l'azione
     * (decoupled: può essere Customer/Restaurant/Admin notification)
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;
    
    /**
     * ID dell'utente che ha completato l'azione
     * (decoupled: può essere RUser, Customer, Admin)
     */
    @Column(name = "actor_id", nullable = false)
    private Long actorId;
    
    /**
     * Tipo di utente che ha agito (CUSTOMER, RESTAURANT_STAFF, ADMIN)
     */
    @Column(name = "actor_type", nullable = false, length = 50)
    private String actorType;
    
    /**
     * Tipo di azione completata
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;
    
    /**
     * Timestamp azione
     */
    @Column(name = "acted_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant actedAt = Instant.now();
    
    /**
     * Note opzionali (es: motivo rifiuto)
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        if (actedAt == null) {
            actedAt = Instant.now();
        }
    }
    
    /**
     * Tipi di azione possibili
     */
    public enum ActionType {
        /**
         * Azione confermata (es: prenotazione confermata)
         */
        CONFIRMED,
        
        /**
         * Azione rifiutata (es: prenotazione rifiutata)
         */
        REJECTED,
        
        /**
         * Azione posticipata (es: deciderò dopo)
         */
        POSTPONED,
        
        /**
         * Notifica dismissata senza azione
         */
        DISMISSED,
        
        /**
         * Azione custom (usa notes per dettagli)
         */
        CUSTOM
    }
}
