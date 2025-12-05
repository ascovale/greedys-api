package com.application.common.persistence.model.notification.preferences;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;

/**
 * LIVELLO 4 - BLOCCHI A LIVELLO UTENTE SINGOLO
 * 
 * Singolo utente (Customer, RestaurantUser, AgencyUser, Admin) può bloccare notifiche.
 * Questo è il livello più basso, applicato per ultimo.
 * 
 * NOTA: L'utente può bloccare SOLO se EventTypeNotificationRule.userCanDisable=true
 * 
 * Logica: Se non esiste record → tutto abilitato (default)
 *         Se esiste → applica blocchi specificati (se permesso da livello 1)
 * 
 * Esempi:
 * - Customer 100 blocca EMAIL per SOCIAL_*
 * - RestaurantUser 50 blocca PUSH per CHAT_*
 * - Admin 5 blocca SMS per tutto tranne SECURITY_*
 */
@Entity
@Table(name = "user_notification_block", indexes = {
    @Index(name = "idx_unb_user", columnList = "user_id"),
    @Index(name = "idx_unb_event_type", columnList = "event_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_unb_user_event", columnNames = {"user_id", "event_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID utente (riferimento a abstract_user.id)
     * Può essere Customer, RestaurantUser, AgencyUser, Admin
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * EventType bloccato (es: "CHAT_*", "SOCIAL_*", oppure "*" per tutti)
     * Se null, si applica a tutti gli eventi
     */
    @Column(name = "event_type", length = 100)
    private String eventType;

    /**
     * Canali bloccati (JSON array): ["SMS", "EMAIL"]
     * Se null/vuoto, blocca tutti i canali per l'evento
     */
    @Column(name = "blocked_channels", length = 200)
    private String blockedChannels;

    /**
     * Se true, il blocco è attivo
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // === QUIET HOURS (opzionale) ===
    
    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
