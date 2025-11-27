package com.application.admin.persistence.model;

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
 * ⭐ NOTIFICA DISAGGREGATA PER ADMIN USER + CHANNEL
 * 
 * Estende ANotification e aggiunge campi specifici per il nuovo sistema RabbitListener:
 * - Per OGNI admin E OGNI channel (WEBSOCKET, EMAIL, PUSH, SMS) si crea UNA ROW
 * 
 * FLOW:
 * 1. Evento genera message su notification.admin queue
 * 2. AdminNotificationListener riceve message
 * 3. Disaggrega per recipient (admin) × channel (Es: 3 admins × 2 channels = 6 rows)
 * 4. Crea 6 AdminNotification rows (unique eventId + channel combo)
 * 5. Persiste con status=PENDING, channel=WEBSOCKET/EMAIL/etc
 * 6. ChannelPoller legge rows per channel
 * 7. Invia via channel appropriato
 * 8. Aggiorna status=DELIVERED/FAILED
 * 
 * ⭐ IMPORTANTE: NO SHARED READ per admin (ogni admin ha read status INDIVIDUALE)
 * 
 * Use Cases:
 * - SYSTEM_ALERT (cada admin deve leggere indipendentemente)
 * - ADMIN_TASK (task assegnato a uno specifico admin)
 * - PLATFORM_UPDATE (update generale a tutti gli admin)
 * - ISSUE_REPORT (report assegnato a uno specifico admin)
 * 
 * Quando UN admin legge:
 * - UPDATE SOLO la row con quel userId
 * - Gli altri admin CONTINUANO A VEDERLA COME NON LETTA
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Entity
@Table(
    name = "admin_notification",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_admin_notification_idempotency",
            columnNames = {"event_id", "user_id", "notification_type"}
        )
    }
)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification extends ANotification {

    /**
     * ⭐ EVENT ID FOR IDEMPOTENCY TRACKING
     * 
     * Generato come: aggregateType + eventId + userId + channel
     * 
     * Usato da AdminNotificationListener per idempotency check:
     * - Se esiste già row con questo eventId → skip (duplicato da retry)
     * - Se non esiste → crea nuova row
     * 
     * Garantisce che retry da RabbitMQ non crea duplicati.
     * NON è UNIQUE nel DB - idempotency check avviene in listener prima del loop.
     */
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    /**
     * ⭐ NO SHARED READ PER ADMIN
     * 
     * readByAll è sempre false per AdminNotification.
     * 
     * Quando admin legge notifica:
     * - UPDATE SOLO la row con quel userId
     * - Gli altri admin CONTINUANO A VEDERLA COME NON LETTA
     * 
     * A differenza di RestaurantUserNotification o AgencyUserNotification
     * che hanno readByAll=true per broadcast events.
     * 
     * Questo perché admin notifications sono:
     * - Generalmente individuali (task, assignment, etc)
     * - O system-wide ma non richiedono "shared read" logic
     */
    @Column(name = "read_by_all", nullable = false)
    @Builder.Default
    private Boolean readByAll = false;  // Always false for admin

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
     * ChannelPoller queries per channel:
     * - SELECT * FROM admin_notification WHERE channel='WEBSOCKET' AND status='PENDING'
     * - SELECT * FROM admin_notification WHERE channel='EMAIL' AND status='PENDING'
     */
    @Column(name = "channel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    /**
     * DELIVERY STATUS (più granulare di is_read)
     * 
     * - PENDING: Appena creata, in attesa di invio
     * - DELIVERED: Inviata via channel con successo
     * - FAILED: Errore durante invio
     * - READ: Admin ha letto via WebSocket
     * 
     * Transizioni:
     * PENDING → DELIVERED (channel send successful)
     * PENDING → FAILED (channel send failed)
     * DELIVERED/PENDING → READ (admin action via WebSocket)
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    /**
     * PRIORITY dell'invio
     * 
     * - HIGH: Immediate delivery (system alert, urgent issue)
     * - NORMAL: ~5 minuti (regular notification)
     * - LOW: ~1 ora (optional updates)
     * 
     * ChannelPoller queries con ORDER BY:
     * - SELECT * FROM admin_notification 
     *   WHERE status='PENDING' 
     *   ORDER BY priority DESC, creation_time ASC
     */
    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * EVENT TYPE (cosa è successo)
     * 
     * Es: SYSTEM_ALERT, ADMIN_TASK, PLATFORM_UPDATE, ISSUE_REPORT, etc
     * 
     * Usato per:
     * 1. Template selection: quale template usare per Email/Push
     * 2. Icon/color in UI: SYSTEM_ALERT → red icon
     * 3. Action URL: link a pagina relativa
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * AGGREGATE TYPE (chi ha agito)
     * 
     * Es: ADMIN, SYSTEM
     * 
     * Per admin notifications è generalmente ADMIN o SYSTEM
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /**
     * TITLE (da AEventNotification)
     * Ereditate da ANotification → AEventNotification
     * Es: "Sistema aggiornato", "Nuovo report disponibile"
     */
    // Inherited: title

    /**
     * BODY (da AEventNotification)
     * Es: "La versione 2.1.0 è stata deployata"
     */
    // Inherited: body

    /**
     * PAYLOAD per template rendering
     * Es: {
     *   "system_name": "Greedys API",
     *   "version": "2.1.0",
     *   "deployed_at": "2025-01-20T10:30:00Z",
     *   "changes": "Fixed notification delivery, Improved performance"
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
     * ⭐ HELPER: Marca notifica come LETTA (INDIVIDUAL READ ONLY)
     * 
     * Poiché readByAll è sempre false:
     * - Questa riga viene marcata come READ
     * - Gli altri admin non sono affettati
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
