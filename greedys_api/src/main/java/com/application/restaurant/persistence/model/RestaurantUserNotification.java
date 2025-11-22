package com.application.restaurant.persistence.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.application.common.persistence.model.notification.ANotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.common.persistence.model.notification.NotificationPriority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ NOTIFICA DISAGGREGATA PER RESTAURANT USER + CHANNEL
 * 
 * Estende ANotification e aggiunge campi specifici per il nuovo sistema RabbitListener:
 * - Per OGNI restaurant staff E OGNI channel (WEBSOCKET, EMAIL, PUSH, SMS) si crea UNA ROW
 * 
 * FLOW:
 * 1. Evento genera message su notification.restaurant queue
 * 2. RestaurantNotificationListener riceve message
 * 3. Disaggrega per recipient (staff) × channel (Es: 10 staff × 2 channels = 20 rows)
 * 4. Crea 20 RestaurantUserNotification rows (unique eventId + channel combo)
 * 5. Persiste con status=PENDING, channel=WEBSOCKET/EMAIL/etc
 * 6. ChannelPoller legge rows per channel
 * 7. Invia via channel appropriato (WebSocket, Email, Push, SMS)
 * 8. Aggiorna status=DELIVERED/FAILED
 * 
 * SHARED READ LOGIC (readByAll):
 * - Se readByAll=true: quando UN restaurant staff legge notifica WEBSOCKET
 *   → UPDATE ALL RestaurantUserNotification rows WHERE restaurantId=? AND eventId=? SET read_at=now
 *   → Tutti gli staff vedono come LETTO
 * 
 * Use Cases per readByAll=true:
 * - RESERVATION_REQUESTED (broadcast a tutti gli staff ristorante)
 * - NEW_ORDER (broadcast a tutti gli staff)
 * - KITCHEN_ALERT (broadcast a tutti)
 * 
 * Use Cases per readByAll=false:
 * - DIRECT_MESSAGE_TO_STAFF (individuale)
 * - TASK_ASSIGNMENT (solo quell'staff)
 * - PERSONAL_NOTIFICATION (personal)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Entity
@Table(
    name = "restaurant_user_notification",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_restaurant_notification_idempotency",
            columnNames = {"event_id", "user_id", "notification_type"}
        )
    }
)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantUserNotification extends ANotification {

    /**
     * ⭐ UNIQUE IDEMPOTENCY KEY
     * 
     * Generato come: SHA256(aggregateType + eventId + userId + channel)
     * 
     * Usato da RestaurantNotificationListener per idempotency check:
     * - Se esiste già row con questo eventId → skip (duplicato da retry)
     * - Se non esiste → crea nuova row
     * 
     * Garantisce che retry da RabbitMQ non crea duplicati.
     * 
     * Esempio:
     * - Original: "RESTAURANT_RESERVATION_REQUESTED_12345_50_WEBSOCKET"
     * - Retry 1: stesso eventId → skip
     * - Retry 2: stesso eventId → skip
     */
    @Column(name = "event_id", unique = true, nullable = false, length = 255)
    private String eventId;

    /**
     * Restaurant ID (per batch operations)
     * 
     * Usato per:
     * 1. Batch read operations quando readByAll=true
     *    UPDATE restaurant_user_notification SET read_at=? 
     *    WHERE restaurant_id=? AND event_id=? AND read_by_all=true
     * 
     * 2. Querying notifiche per restaurant nel dashboard
     *    SELECT * FROM restaurant_user_notification WHERE restaurant_id=? ORDER BY creation_time DESC
     * 
     * Indice: INDEX idx_restaurant_id_creation_time (restaurant_id, creation_time DESC)
     */
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    /**
     * ⭐ SHARED READ FLAG
     * 
     * true: Broadcast notification
     *   → Quando UN restaurant staff legge, TUTTI gli restaurant staff vedono come LETTO
     *   → Esempio: ReservationRequested è broadcast a 10 staff → uno legge → tutti vedono LETTO
     *   → UPDATE aggiorna tutte le 10 rows (per quel eventId, restaurantId, channel)
     * 
     * false: Individual notification
     *   → Solo lo staff specifico (userId) vede LETTO
     *   → Esempio: TaskAssignment a uno specifico staff → solo lui vede LETTO
     *   → UPDATE aggiorna SOLO la row con quel userId
     * 
     * Determinato da loadGroupSettings() in RestaurantOrchestrator:
     * - Se eventType in ["RESERVATION_REQUESTED", "NEW_ORDER", "KITCHEN_ALERT"] → readByAll=true
     * - Altrimenti → readByAll=false
     */
    @Column(name = "read_by_all", nullable = false)
    @Builder.Default
    private Boolean readByAll = false;

    /**
     * DELIVERY CHANNEL per questa disaggregazione
     * 
     * Enum values:
     * - WEBSOCKET: WebSocket per real-time browser notification
     * - EMAIL: Email notification
     * - PUSH: Mobile push notification
     * - SMS: SMS text message
     * 
     * Ogni (userId, eventId, channel) combo = 1 row separata.
     * 
     * Esempio per eventId="123" → userId=50:
     * - RestaurantUserNotification (channel=WEBSOCKET, status=PENDING) 
     * - RestaurantUserNotification (channel=EMAIL, status=PENDING)
     * - RestaurantUserNotification (channel=PUSH, status=PENDING)
     * → Se staff ha 3 channels enabled, crea 3 rows
     * 
     * ChannelPoller queries per channel:
     * - SELECT * FROM restaurant_user_notification WHERE channel='WEBSOCKET' AND status='PENDING'
     * - SELECT * FROM restaurant_user_notification WHERE channel='EMAIL' AND status='PENDING'
     * - Invia SOLO per i channels abilitati
     */
    @Column(name = "channel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    /**
     * DELIVERY STATUS (più granulare di is_read)
     * 
     * - PENDING: Appena creata, in attesa di invio
     * - DELIVERED: Inviata via channel con successo
     * - FAILED: Errore durante invio (es: email bounce, push failed)
     * - READ: User ha letto via WebSocket
     * 
     * Transizioni:
     * PENDING → DELIVERED (channel send successful) [ChannelPoller]
     * PENDING → FAILED (channel send failed) [ChannelPoller]
     * DELIVERED/PENDING → READ (user action via WebSocket) [WebSocket handler]
     * 
     * Query comune:
     * - SELECT * FROM restaurant_user_notification WHERE status='PENDING' LIMIT 100
     *   → Per ChannelPoller batch processing
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    /**
     * PRIORITY dell'invio
     * 
     * - HIGH: Invia immediately (reservation confirmed, urgent alert)
     * - NORMAL: Invia entro 5 minuti (regular notification)
     * - LOW: Invia entro 1 ora (optional updates)
     * 
     * ChannelPoller queries con ORDER BY:
     * - SELECT * FROM restaurant_user_notification 
     *   WHERE status='PENDING' 
     *   ORDER BY priority DESC, creation_time ASC
     * → Elabora HIGH priority prima, poi NORMAL, poi LOW
     */
    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * EVENT TYPE (cosa è successo)
     * 
     * Es: RESERVATION_REQUESTED, NEW_ORDER, KITCHEN_ALERT, TASK_ASSIGNMENT, etc
     * 
     * Usato per:
     * 1. Template selection: quale template usare per Email/Push
     * 2. Icon/color in UI: RESERVATION_REQUESTED → reservation icon
     * 3. Action URL: link a pagina relativa (reservation, order, task, etc)
     * 
     * Determinato da event.eventType dalla coda RabbitMQ
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * AGGREGATE TYPE (chi ha agito)
     * 
     * Es: CUSTOMER, RESTAURANT_USER, AGENCY_USER, ADMIN
     * 
     * Usato per:
     * 1. Audit trail: sapere chi ha originato la notifica
     * 2. Permission checks: solo RESTAURANT_USER può inviare a restaurant users
     * 3. Analytics: quante notifiche da CUSTOMER vs RESTAURANT_USER
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /**
     * TITLE (da AEventNotification)
     * Ereditate da ANotification → AEventNotification
     * Es: "Nuova prenotazione richiesta", "Nuovo ordine in cucina"
     */
    // Inherited: title

    /**
     * BODY (da AEventNotification)
     * Es: "Tavolo per 4 persone alle 19:30 presso Trattoria del Mare"
     */
    // Inherited: body

    /**
     * PAYLOAD per template rendering
     * Es: {
     *   "reservation_id": "RES-12345",
     *   "customer_name": "John Doe",
     *   "party_size": "4",
     *   "requested_time": "2025-01-20T19:30:00Z",
     *   "restaurant_name": "Trattoria del Mare",
     *   "action_url": "/reservations/RES-12345"
     * }
     */
    // Inherited: properties

    /**
     * CREAZIONE TIMESTAMP
     * Gestita da @CreationTimestamp
     */
    // Inherited: creationTime

    /**
     * CREATED AT (per batch processing, performance)
     * Index: idx_channel_status_creation_time (channel, status, created_at)
     * Per query veloce: SELECT * FROM ... WHERE channel='EMAIL' AND status='PENDING' ORDER BY created_at
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * UPDATED AT (per tracking ultima modifica)
     * Index: idx_status_updated_at (status, updated_at DESC)
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * ⭐ HELPER: Marca notifica come LETTA
     * 
     * Logica:
     * - Se readByAll=true: markAsRead() + batch UPDATE others con stesso eventId/restaurantId/channel
     * - Se readByAll=false: markAsRead() ONLY this row
     */
    public void markAsRead() {
        setReadAt(java.time.Instant.now());
        this.status = DeliveryStatus.READ;
    }

    /**
     * ⭐ HELPER: Check se LETTA
     */
    public boolean isRead() {
        return getReadAt() != null && status == DeliveryStatus.READ;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
