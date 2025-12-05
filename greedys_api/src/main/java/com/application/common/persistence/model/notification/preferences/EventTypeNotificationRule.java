package com.application.common.persistence.model.notification.preferences;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * LIVELLO 1 - REGOLE EVENTO (Admin definisce)
 * 
 * Admin definisce per ogni EventType:
 * - Canali obbligatori (utente NON può disabilitare)
 * - Se l'utente può disabilitare la notifica
 * 
 * Logica: Se non esiste record → tutto optional, utente può disabilitare
 *         Se esiste con user_can_disable=false → utente DEVE ricevere
 *         Se esiste con mandatory_channels → quei canali sono forzati
 * 
 * Esempi:
 * - RESERVATION_CONFIRMED: mandatory_channels=["EMAIL"], user_can_disable=false
 * - SOCIAL_NEW_FOLLOWER: user_can_disable=true (utente può spegnere)
 * - SECURITY_INCIDENT: mandatory_channels=["EMAIL","SMS"], user_can_disable=false
 */
@Entity
@Table(name = "event_type_notification_rule", indexes = {
    @Index(name = "idx_etnr_event_type", columnList = "event_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_etnr_event_type", columnNames = {"event_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTypeNotificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * EventType per cui vale la regola (es: "RESERVATION_CONFIRMED")
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Canali obbligatori (JSON array): ["EMAIL", "PUSH"]
     * Utente riceve SEMPRE su questi canali, non può disabilitarli
     */
    @Column(name = "mandatory_channels", length = 200)
    @Builder.Default
    private String mandatoryChannels = "[]";

    /**
     * Se false, l'utente NON può disabilitare questa notifica
     * (riceverà sempre, almeno sui canali mandatory)
     */
    @Column(name = "user_can_disable", nullable = false)
    @Builder.Default
    private Boolean userCanDisable = true;

    /**
     * Descrizione della regola (per UI admin)
     */
    @Column(name = "description", length = 500)
    private String description;

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
