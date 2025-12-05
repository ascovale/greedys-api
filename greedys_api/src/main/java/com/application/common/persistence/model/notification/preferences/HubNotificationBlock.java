package com.application.common.persistence.model.notification.preferences;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;

/**
 * LIVELLO 3 - BLOCCHI A LIVELLO HUB (RUserHub / AgencyUserHub)
 * 
 * Un Hub (persona fisica con più account) può bloccare notifiche per tutti i suoi utenti.
 * Es: Mario Rossi ha account su 3 ristoranti → blocca SMS per tutti e 3.
 * 
 * Logica: Se non esiste record → tutto abilitato (default)
 *         Se esiste → applica blocchi specificati
 * 
 * Esempi:
 * - RUserHub 42 blocca tutte le notifiche EMAIL
 * - AgencyUserHub 15 blocca SOCIAL_* su tutti i canali
 */
@Entity
@Table(name = "hub_notification_block", indexes = {
    @Index(name = "idx_hnb_hub", columnList = "hub_type, hub_id"),
    @Index(name = "idx_hnb_event_type", columnList = "event_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_hnb_hub_event", columnNames = {"hub_type", "hub_id", "event_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubNotificationBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo hub: RESTAURANT_USER_HUB, AGENCY_USER_HUB
     */
    @Column(name = "hub_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private HubType hubType;

    /**
     * ID dell'hub (restaurant_user_hub_id o agency_user_hub_id)
     */
    @Column(name = "hub_id", nullable = false)
    private Long hubId;

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

    public enum HubType {
        RESTAURANT_USER_HUB,
        AGENCY_USER_HUB
    }
}
