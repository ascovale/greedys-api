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
 * ⭐ LIVELLO 2 di OUTBOX: Notification Publishing
 * 
 * Quando il listener processa un evento e crea notifiche (RestaurantNotification, 
 * CustomerNotification, AdminNotification), questo table traccia QUANDO le pubblichiamo
 * a RabbitMQ per il ChannelPoller.
 * 
 * FLOW:
 * 1. LISTENER (riceve evento da RabbitMQ):
 *    - Crea RestaurantNotification/CustomerNotification/AdminNotification
 *    - Crea NotificationOutbox (status=PENDING, notificationId=id_creato)
 *    - UPDATE event_outbox SET processed_by = 'LISTENER_NAME'
 * 
 * 2. NOTIFICATION OUTBOX POLLER (background, @Scheduled every 5 sec):
 *    - SELECT * FROM notification_outbox WHERE status = 'PENDING' LIMIT 100
 *    - Per ogni notifica:
 *      ├─ Publish a RabbitMQ (exchange: notification-channel-send, routing key per channel)
 *      ├─ UPDATE status = 'PUBLISHED'
 *      └─ LOG
 * 
 * 3. CHANNEL POLLER (background, @Scheduled every 10 sec):
 *    - SELECT * FROM notification_channel_send WHERE is_sent IS NULL
 *    - Per ogni canale:
 *      ├─ Send via SMS/Email/Push/WebSocket/Slack
 *      ├─ UPDATE is_sent = true/false
 *      └─ LOG result
 * 
 * ✅ VANTAGGI:
 * - Separazione: EventOutbox → RabbitMQ (event-stream)
 *              NotificationOutbox → RabbitMQ (notification-channel-send)
 * - Ogni listener pubblica indipendentemente
 * - Se ChannelPoller muore, le notifiche aspettano in notification_channel_send
 * - Se RabbitMQ muore, NotificationOutboxPoller riprova
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Notification Publishing Outbox)
 */
@Entity
@Table(name = "notification_outbox", indexes = {
    @Index(name = "idx_status_created", columnList = "status, created_at"),
    @Index(name = "idx_notification_id", columnList = "notification_id"),
    @Index(name = "idx_aggregate", columnList = "aggregate_type, aggregate_id"),
    @Index(name = "idx_retry", columnList = "status, retry_count")
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
     * Reference alla notifica creata (RestaurantNotification, CustomerNotification, AdminNotification)
     * 
     * POLYMORPHIC: Punta a una delle 3 subclass di ANotification
     * 
     * ⭐ USO: Quando il ChannelPoller processa, sa quale notifica inviare
     * 
     * Flusso:
     * 1. Listener crea RestaurantNotification con id=1
     * 2. Listener crea NotificationOutbox(notificationId=1, status=PENDING)
     * 3. NotificationOutboxPoller legge e pubblica a RabbitMQ
     * 4. ChannelPoller riceve, legge RestaurantNotification(id=1), estrae title/body, etc
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    /**
     * Tipo di notifica per polymorphic query
     * 
     * Enum: RESTAURANT, CUSTOMER, ADMIN
     * 
     * Permette a ChannelPoller di sapere quale tabella querare:
     * SELECT * FROM restaurant_notification WHERE id = ?
     * SELECT * FROM customer_notification WHERE id = ?
     * SELECT * FROM admin_notification WHERE id = ?
     */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

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
        PENDING,
        PUBLISHED,
        FAILED
    }
}
