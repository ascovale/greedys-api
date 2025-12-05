package com.application.common.persistence.model.notification.preferences;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * LIVELLO 0 - BLOCCO GLOBALE ADMIN
 * 
 * Admin può bloccare globalmente un EventType per TUTTI gli utenti.
 * Se presente un record per un eventType, quell'evento NON genera notifiche.
 * 
 * Logica: Se esiste record → BLOCCATO, Se non esiste → ABILITATO (default)
 * 
 * Esempi:
 * - Bloccare tutte le notifiche SOCIAL durante manutenzione
 * - Disabilitare CHAT_TYPING_INDICATOR globalmente
 * - Bloccare MARKETING_* per compliance GDPR temporanea
 */
@Entity
@Table(name = "global_notification_block", indexes = {
    @Index(name = "idx_gnb_event_type", columnList = "event_type"),
    @Index(name = "idx_gnb_active", columnList = "active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_gnb_event_type", columnNames = {"event_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalNotificationBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * EventType bloccato (es: "SOCIAL_NEW_POST", "CHAT_*", "RESERVATION_*")
     * Supporta pattern con wildcard: "SOCIAL_*" blocca tutti gli eventi SOCIAL
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Se true, il blocco è attivo. Se false, il blocco è disabilitato (soft-delete)
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Motivo del blocco (per audit)
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Admin che ha creato il blocco
     */
    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    /**
     * Data inizio blocco (opzionale, per blocchi schedulati)
     */
    @Column(name = "block_start")
    private Instant blockStart;

    /**
     * Data fine blocco (opzionale, per blocchi temporanei)
     */
    @Column(name = "block_end")
    private Instant blockEnd;

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
