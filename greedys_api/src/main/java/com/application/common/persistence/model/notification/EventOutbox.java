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
 * ⭐ LIVELLO 1 di OUTBOX: Event Sourcing per tutti gli eventi di dominio
 * 
 * Quando un aggregate (Reservation, Customer, Restaurant) emette un evento,
 * viene salvato QUI PRIMA di essere pubblicato su RabbitMQ.
 * 
 * Questo garantisce:
 * ✅ At-least-once delivery: Se il service muore dopo persistenza, il poller lo ritrova
 * ✅ Idempotency: Listeners possono verificare se l'evento è già stato processato
 * ✅ Auditing: Storico completo di cosa è successo nel sistema
 * 
 * FLOW:
 * 1. SERVICE (ReservationService):
 *    - Crea reservation in DB (TRANSAZIONE 1)
 *    - Crea ReservationRequestedEvent
 *    - Persiste a EventOutbox (status=PENDING, same TX)
 *    - Commit
 * 
 * 2. OUTBOX POLLER (background, @Scheduled every 5 sec):
 *    - SELECT * FROM event_outbox WHERE status = 'PENDING' LIMIT 100
 *    - Per ogni evento:
 *      ├─ Publish a RabbitMQ (exchange: event-stream)
 *      ├─ UPDATE status = 'PROCESSED'
 *      └─ LOG
 * 
 * 3. EVENT LISTENER (AdminNotificationListener, RestaurantNotificationListener, etc):
 *    - Riceve evento da RabbitMQ
 *    - Idempotency check: SELECT * FROM event_outbox WHERE event_id = ? AND processed_by = ?
 *    - Se già processato: RETURN (skip)
 *    - Se nuovo: Crea notifiche (RestaurantNotification, AdminNotification, etc) 
 *    - Crea NotificationOutbox entries (LIVELLO 2)
 *    - UPDATE event_outbox SET processed_by = 'ADMIN_NOTIFICATION_LISTENER'
 * 
 * ✅ VANTAGGI DOPPIO OUTBOX:
 * - LIVELLO 1 (EventOutbox): Pubblica SEMPRE a RabbitMQ (è "affidabile" per il sistema)
 * - LIVELLO 2 (NotificationOutbox): Una volta ricevuto l'evento, QUANDO creiamo le notifiche
 * 
 * ⚠️ NOTA: Un singolo evento può essere ascoltato da MULTIPLI listener:
 *   - AdminNotificationListener crea AdminNotification
 *   - CustomerNotificationListener crea CustomerNotification
 *   - RestaurantNotificationListener crea RestaurantNotification
 * 
 * Ogni listener ha suo own processed_by, quindi può elaborare indipendentemente.
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Event Sourcing Foundation)
 */
@Entity
@Table(name = "event_outbox", indexes = {
    @Index(name = "idx_status_created", columnList = "status, created_at"),
    @Index(name = "idx_event_type_aggregate", columnList = "event_type, aggregate_type, aggregate_id"),
    @Index(name = "idx_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_processed_by", columnList = "processed_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID univoco per l'evento a livello di dominio
     * 
     * Usato per idempotency check nei listener
     * Formato: UUID o {aggregateType}_{aggregateId}_{timestamp}
     */
    @Column(name = "event_id", nullable = false, length = 100, unique = true)
    private String eventId;

    /**
     * Tipo di evento: RESERVATION_REQUESTED, RESERVATION_CONFIRMED, CUSTOMER_REGISTERED, etc.
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

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
     * Payload JSON dell'evento
     * 
     * Contiene tutti i dati necessari per il listener:
     * {
     *   "reservationId": 123,
     *   "customerId": 456,
     *   "restaurantId": 789,
     *   "requestedTime": "2025-01-20T14:30:00Z",
     *   "numberOfPeople": 4,
     *   ...
     * }
     */
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Status dell'elaborazione del evento
     * 
     * PENDING: Non ancora pubblicato su RabbitMQ
     * PROCESSED: Pubblicato con successo su RabbitMQ
     * FAILED: Fallito dopo retry (salva lastError)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * Nome del listener che ha processato l'evento
     * 
     * Es: ADMIN_NOTIFICATION_LISTENER, RESTAURANT_NOTIFICATION_LISTENER
     * 
     * ⭐ IMPORTANTE: Un SINGOLO evento può essere processato da MULTIPLI listener!
     * 
     * Es per ReservationRequestedEvent:
     * - AdminNotificationListener crea AdminNotification
     * - RestaurantNotificationListener crea RestaurantNotification
     * - CustomerNotificationListener crea CustomerNotification
     * 
     * Ogni listener aggiorna il suo processed_by indipendentemente.
     * 
     * Se NULL: Non ancora processato da nessuno.
     * Se "ADMIN_NOTIFICATION_LISTENER": Già processato, non riprocessa.
     */
    @Column(name = "processed_by", length = 100)
    private String processedBy;

    /**
     * Timestamp quando è stato creato l'evento nel dominio
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp quando è stato pubblicato su RabbitMQ
     * 
     * NULL se status != PROCESSED
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Timestamp quando il listener ha processato l'evento
     * 
     * NULL se processed_by = NULL
     */
    @Column(name = "processed_at")
    private Instant processedAt;

    /**
     * Numero di tentativi di pubblicazione su RabbitMQ
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
    }

    public enum Status {
        PENDING,      // Non ancora pubblicato su RabbitMQ
        PROCESSED,    // Pubblicato con successo
        FAILED        // Fallito dopo max retry
    }

}
