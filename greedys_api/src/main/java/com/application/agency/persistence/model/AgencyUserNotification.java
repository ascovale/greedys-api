package com.application.agency.persistence.model;

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
 * ⭐ NOTIFICA DISAGGREGATA PER AGENCY USER + CHANNEL
 * 
 * Estende ANotification e aggiunge campi specifici per il nuovo sistema RabbitListener:
 * - Per OGNI recipient (agency user) E OGNI channel (WEBSOCKET, EMAIL, PUSH, SMS)
 *   si crea UNA ROW di AgencyUserNotification
 * 
 * FLOW:
 * 1. Evento genera message su notification.agency queue
 * 2. AgencyNotificationListener riceve message
 * 3. Disaggrega per recipient × channel (Es: 5 users × 2 channels = 10 rows)
 * 4. Crea 10 AgencyUserNotification rows (unique eventId + channel combo)
 * 5. Persiste con status=PENDING, channel=WEBSOCKET/EMAIL/etc
 * 6. ChannelPoller/DeliveryService legge rows per channel
 * 7. Invia via channel appropriato (WebSocket, Email, Push, SMS)
 * 8. Aggiorna status=DELIVERED/FAILED
 * 
 * SHARED READ LOGIC (readByAll):
 * - Se readByAll=true: quando UN agency user legge notifica WEBSOCKET
 *   → UPDATE ALL AgencyUserNotification rows WHERE agencyId=? AND eventId=? SET read_at=now
 *   → Tutti gli agency users vedono come LETTO
 * 
 * Use Cases per readByAll=true:
 * - RESERVATION_REQUESTED (broadcast a tutti gli staff agency)
 * - NEW_ORDER (broadcast a tutti gli staff)
 * - KITCHEN_ALERT (broadcast a tutti)
 * 
 * Use Cases per readByAll=false:
 * - DIRECT_MESSAGE_TO_AGENT (individuale)
 * - TASK_ASSIGNMENT (solo quell'agent)
 * - PERSONAL_NOTIFICATION (personal)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Entity
@Table(
    name = "agency_user_notification",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_agency_notification_idempotency",
            columnNames = {"event_id", "user_id", "notification_type"}
        )
    }
)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyUserNotification extends ANotification {

    /**
     * ⭐ EVENT ID FOR IDEMPOTENCY TRACKING
     * 
     * Generato come: aggregateType + eventId + userId + channel
     * 
     * Usato da AgencyNotificationListener per idempotency check:
     * - Se esiste già row con questo eventId → skip (duplicato da retry)
     * - Se non esiste → crea nuova row
     * 
     * Garantisce che retry da RabbitMQ non crea duplicati.
     * NON è UNIQUE nel DB - idempotency check avviene in listener prima del loop.
     * 
     * Esempio:
     * - Original: "AGENCY_RESERVATION_REQUESTED_12345_500_WEBSOCKET"
     * - Retry 1: stesso eventId → skip
     * - Retry 2: stesso eventId → skip
     */
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    /**
     * Agency ID (per batch operations)
     * 
     * Usato per:
     * 1. Batch read operations quando readByAll=true
     *    UPDATE agency_user_notification SET read_at=? 
     *    WHERE agency_id=? AND event_id=? AND read_by_all=true
     * 
     * 2. Querying notifiche per agency nel dashboard
     *    SELECT * FROM agency_user_notification WHERE agency_id=? ORDER BY creation_time DESC
     * 
     * Indice: INDEX idx_agency_id_creation_time (agency_id, creation_time DESC)
     */
    @Column(name = "agency_id", nullable = false)
    private Long agencyId;

    /**
     * ⭐ SHARED READ FLAG
     * 
     * true: Broadcast notification
     *   → Quando UN agency user legge, TUTTI gli agency users vedono come LETTO
     *   → Esempio: ReservationRequested è broadcast a 5 staff → uno legge → tutti vedono LETTO
     *   → UPDATE aggiorna tutte le 5 rows (per quel eventId, agencyId, channel)
     * 
     * false: Individual notification
     *   → Solo l'agency user specifico (userId) vede LETTO
     *   → Esempio: TaskAssignment a uno specifico agent → solo lui vede LETTO
     *   → UPDATE aggiorna SOLO la row con quel userId
     * 
     * Determinato da loadGroupSettings() in AgencyOrchestrator:
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
     * Esempio per eventId="123" → userId=500:
     * - AgencyUserNotification (channel=WEBSOCKET, status=PENDING) 
     * - AgencyUserNotification (channel=EMAIL, status=PENDING)
     * - AgencyUserNotification (channel=PUSH, status=PENDING)
     * → Se user ha 3 channels enabled, crea 3 rows
     * 
     * ChannelPoller queries per channel:
     * - SELECT * FROM agency_user_notification WHERE channel='WEBSOCKET' AND status='PENDING'
     * - SELECT * FROM agency_user_notification WHERE channel='EMAIL' AND status='PENDING'
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
     * - SELECT * FROM agency_user_notification WHERE status='PENDING' LIMIT 100
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
     * - SELECT * FROM agency_user_notification 
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
     * 2. Permission checks: solo AGENCY_USER può inviare a agency users
     * 3. Analytics: quante notifiche da CUSTOMER vs RESTAURANT_USER
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /**
     * ⭐ NOTE: readAt è inherited da AEventNotification (Instant)
     * 
     * SHARED READ LOGIC:
     * Se readByAll=true E user legge:
     *   → UPDATE agency_user_notification 
     *      SET read_at = NOW() 
     *      WHERE agency_id=? AND event_id=? AND read_by_all=true AND channel=?
     *   → TUTTE le rows per quel eventId/agencyId/channel vedono read_at
     * 
     * Se readByAll=false:
     *   → UPDATE SOLO la row con quel userId
     *      SET read_at = NOW() 
     *      WHERE id=?
     */

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
     * - Se readByAll=true: markAsRead() + batch UPDATE others con stesso eventId/agencyId/channel
     * - Se readByAll=false: markAsRead() ONLY this row
     * 
     * Nota: readAt è inherited da AEventNotification (Instant), accediamo via setter della base class
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
