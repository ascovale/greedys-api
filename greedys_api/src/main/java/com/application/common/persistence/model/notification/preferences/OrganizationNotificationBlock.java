package com.application.common.persistence.model.notification.preferences;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;

/**
 * LIVELLO 2 - BLOCCHI A LIVELLO ORGANIZATION (Restaurant/Agency)
 * 
 * Restaurant o Agency può bloccare canali o eventi per TUTTI i suoi utenti.
 * 
 * Logica: Se non esiste record → tutto abilitato (default)
 *         Se esiste → applica blocchi specificati
 * 
 * Esempi:
 * - Restaurant 5 blocca SMS per tutti i suoi staff
 * - Agency 3 blocca notifiche CHAT durante quiet hours
 * - Restaurant 10 blocca EMAIL per eventi SOCIAL
 */
@Entity
@Table(name = "organization_notification_block", indexes = {
    @Index(name = "idx_onb_org", columnList = "organization_type, organization_id"),
    @Index(name = "idx_onb_event_type", columnList = "event_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_onb_org_event", columnNames = {"organization_type", "organization_id", "event_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationNotificationBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo organizzazione: RESTAURANT, AGENCY
     */
    @Column(name = "organization_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrganizationType organizationType;

    /**
     * ID dell'organizzazione (restaurant_id o agency_id)
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

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

    public enum OrganizationType {
        RESTAURANT,
        AGENCY
    }
}
