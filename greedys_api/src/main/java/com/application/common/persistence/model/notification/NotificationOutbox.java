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
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ⭐ LIVELLO 2 di OUTBOX: Notifiche Channel Delivery Tracking
 * 
 * Una volta che un listener riceve un evento e crea notifiche (RestaurantNotification,
 * AdminNotification, CustomerNotification), questo outbox traccia l'invio via vari canali.
 * 
 * FLOW:
 * 1. EVENT LISTENER (ReservationEventListener):
 *    - Riceve ReservationCreatedEvent da RabbitMQ
 *    - Crea RestaurantNotification per ogni staff del ristorante
 *    - Crea NotificationOutbox entry (status=PENDING) per il ChannelPoller
 *    - Commit
 * 
 * 2. NOTIFICATION CHANNEL POLLER (background, @Scheduled every 10sec):
 *    - SELECT * FROM notification_outbox WHERE status = 'PENDING' LIMIT 50
 *    - Per ogni entry:
 *      ├─ Leggi RestaurantNotification per estrarre userId
 *      ├─ Query FCM token per userId
 *      ├─ Invia Push notification via FCM
 *      ├─ Oppure: Leggi email address e invia via SMTP
 *      ├─ Oppure: Invia via WebSocket se utente online
 *      ├─ UPDATE status = 'SENT'
 *      └─ LOG
 * 
 * ✅ VANTAGGI:
 * - At-least-once delivery: Se server muore, poller ritrova entries PENDING
 * - Exponential backoff: retry_count > 0 con delay incrementale
 * - Auditability: Storico completo di quali notifiche sono state inviate
 * - Multi-channel: Una singola NotificationOutbox può generare SMS + EMAIL + PUSH
 * 
 * ⚠️ DIFFERENZA con EventOutbox:
 * - EventOutbox: Traccia PUBLISHING su RabbitMQ (evento a livello di sistema)
 * - NotificationOutbox: Traccia DELIVERY al recipient (canale specifico)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Notification Delivery Tracking)
 */
@Entity
@Table(name = "notification_outbox", indexes = {
    @Index(name = "idx_status_created", columnList = "status, created_at"),
    @Index(name = "idx_notification_type", columnList = "notification_id, notification_type"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_status_retry", columnList = "status, retry_count")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID della notifica (RestaurantNotification, AdminNotification, CustomerNotification, etc)
     * 
     * Usato per cercare il recipient (userId, userType) da notificare
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    /**
     * Tipo di notifica: RESTAURANT, ADMIN, CUSTOMER, AGENCY
     * 
     * Usato per sapere quale tabella queryare per estrarre dati del recipient
     */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    /**
     * Tipo di aggregato che ha generato l'evento
     * 
     * Es: RESERVATION, CUSTOMER, RESTAURANT, ADMIN
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /**
     * ID dell'aggregato che ha generato l'evento
     * 
     * Es: reservation_id = 123, customer_id = 456, etc.
     */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /**
     * Tipo di evento che ha scatenato la notifica
     * 
     * Es: RESERVATION_REQUESTED, CUSTOMER_REGISTERED, SERVICE_ACTIVATED
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Payload JSON con i dati della notifica
     * 
     * Es:
     * {
     *   "reservationId": 123,
     *   "customerId": 456,
     *   "restaurantId": 789,
     *   "customerEmail": "john@example.com",
     *   "reservationDate": "2025-01-25T19:00:00Z"
     * }
     */
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Status dell'invio della notifica
     * 
     * PENDING: Non ancora inviata da NotificationChannelPoller
     * SENT: Inviata con successo
     * FAILED: Fallita dopo max retry (salva lastError)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * Timestamp quando è stata creata la notifica
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp quando è stata inviata via WebSocket/Push/Email/SMS
     * 
     * NULL se status != SENT
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Numero di tentativi di invio della notifica
     * 
     * Usato per exponential backoff:
     * - retry_count=0 → retry dopo 10 sec
     * - retry_count=1 → retry dopo 1 min
     * - retry_count=2 → retry dopo 5 min
     * - retry_count≥3 → retry dopo 1 hour
     * - retry_count≥5 → UPDATE status = FAILED, log error
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Messaggio di errore se status = FAILED
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public enum Status {
        PENDING,      // Non ancora inviata
        SENT,         // Inviata con successo
        FAILED        // Fallita dopo max retry
    }

}
