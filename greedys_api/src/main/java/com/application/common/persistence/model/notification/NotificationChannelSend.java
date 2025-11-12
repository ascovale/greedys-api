package com.application.common.persistence.model.notification;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Traccia l'invio di una notifica via un canale specifico (SMS, Email, Push, WebSocket, Slack).
 * 
 * ⭐ SEPARAZIONE DI RESPONSABILITÀ:
 * - ANotification: Cosa riceve il recipient (title, body, is_read, read_at)
 * - NotificationChannelSend: Come lo riceviamo (SMS/EMAIL/PUSH, is_sent, sent_at, attempts)
 * 
 * ⭐ VANTAGGI:
 * - Per cada notifica: N rows in notification_channel_send (1 per canale)
 * - Puoi tracciare per-channel status: SMS=SENT, EMAIL=FAILED, PUSH=SENT
 * - Retry per canale: se Email fallisce, riprova solo Email
 * - Query facile: SELECT * WHERE channel_type='EMAIL' AND is_sent=false
 * 
 * ⭐ TRANSAZIONE (REQUIRES_NEW per canale):
 * - TX 1 (SAVE): Insert notification_restaurant + insert notification_channel_send (is_sent=NULL)
 * - TX 2-N (SEND): Per ogni canale:
 *   ├─ Try send
 *   ├─ If success: UPDATE is_sent=true, sent_at=NOW()
 *   └─ If failure: UPDATE attempt_count++, last_error, last_attempt_at (no is_sent update)
 * 
 * @param <N> Notification type (RestaurantNotification, CustomerNotification, AdminNotification)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Channel Send Separation)
 */
@Entity
@Table(
    name = "notification_channel_send",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_channel_send_notification_channel",
            columnNames = {"notification_id", "channel_type"}
        )
    }
)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class NotificationChannelSend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference a ANotification (RestaurantNotification, CustomerNotification, AdminNotification)
     * 
     * ⚠️ POLYMORPHIC: ManyToOne a una superclass (JPA @Any oppure separati per tipo)
     * Per semplicità: Usa Long notification_id + String notification_type oppure 3 FK separate
     * 
     * Versione semplificata: Long notification_id + query manual
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    /**
     * Tipo di canale di invio
     * 
     * Enum: SMS, EMAIL, PUSH, WEBSOCKET, SLACK
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 50)
    private ChannelType channelType;

    /**
     * Flag: È stato inviato con successo?
     * 
     * NULL: Invio non ancora tentato o in progress
     * true: Inviato con successo
     * false: Fallito definitivamente (max retries raggiunto)
     */
    @Column(name = "is_sent")
    private Boolean sent;

    /**
     * Timestamp quando è stato inviato con successo
     * 
     * NULL se is_sent != true
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Numero di tentativi fatti per inviare
     * 
     * Incrementato ad ogni tentativo (success o failure)
     */
    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Timestamp dell'ultimo tentativo di invio
     * 
     * Aggiornato ad ogni tentativo
     */
    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    /**
     * Messaggio dell'ultimo errore di invio
     * 
     * NULL se is_sent = true
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * Timestamp di creazione
     */
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    /**
     * Timestamp ultimo aggiornamento
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * Enum per i tipi di canale supportati
     */
    public enum ChannelType {
        SMS,           // SMS via SMS Gateway
        EMAIL,         // Email via Email Service
        PUSH,          // Push notification via FCM/APNs
        WEBSOCKET,     // WebSocket real-time
        SLACK          // Slack per admin alerts
    }

}
