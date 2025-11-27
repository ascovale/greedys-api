package com.application.common.persistence.model.notification;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ GERARCHIA 1: NOTIFICHE LEGATE A EVENTI (senza userId/userType)
 * 
 * Questa è la base per notifiche che:
 * - Sono generate da EVENTI di dominio (ReservationRequested, CustomerRegistered, etc)
 * - Vanno a ENTITÀ (Restaurant, Customer, Admin, Agency)
 * - Possono avere MULTIPLI RECIPIENT (100+ staff di un ristorante)
 * - Non hanno userId/userType a questo livello (sono entity-level)
 * 
 * SOTTOCLASSI:
 * - RestaurantEventNotification extends AEventNotification (per evento → restaurant)
 * - CustomerEventNotification extends AEventNotification (per evento → customer)
 * - AdminEventNotification extends AEventNotification (per evento → admin team)
 * - AgencyEventNotification extends AEventNotification (per evento → agency)
 * 
 * FLOW:
 * 1. ReservationRequestedEvent generato
 * 2. AdminNotificationListener crea AdminEventNotification (title, body, NO userId)
 * 3. NotificationOutbox persiste questo
 * 4. ChannelPoller legge AdminEventNotification e NotificationRecipient
 * 5. NotificationRecipient ha: notificationId, userId, userType (ADMIN_USER)
 * 6. Per ogni recipient: crea NotificationChannelSend (SMS, EMAIL, PUSH per quello user)
 * 
 * ✅ VANTAGGI:
 * - AEventNotification: generico per entity-level (no userId)
 * - ANotification: estende AEventNotification + aggiunge userId/userType per recipient-specific
 * - Un evento → Multipli event notifications → N recipients per notification
 * - Puoi avere notifiche senza recipient specifico (announcement a tutta l'app)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Event-Driven Notifications)
 */
@MappedSuperclass
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public abstract class AEventNotification {
    
    /**
     * Primary Key ID
     * 
     * Auto-generated per ogni notification
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * Titolo della notifica
     * 
     * Es: "Nuova prenotazione richiesta", "Cliente registrato", "Servizio attivato"
     */
    @Column(name = "title", nullable = false)
    String title;

    /**
     * ⭐ EVENT ID - Foreign Key to originating domain event
     * 
     * Links this notification back to the EventOutbox row that triggered it.
     * 
     * Critical for:
     * - Audit trail: Trace which event generated this notification
     * - Correlation: Join with EventOutbox to get event details
     * - Deduplication: Detect if same event already processed
     * - Analytics: Understand event → notification flow
     * - Debugging: Trace event source when troubleshooting
     * 
     * Usage:
     *   SELECT n.*, e.event_type, e.aggregate_type, e.payload
     *   FROM a_event_notification n
     *   JOIN event_outbox e ON n.event_outbox_id = e.id
     *   WHERE n.id = ?
     * 
     * Populated by NotificationListener when creating notification from EventOutbox message:
     *   eventNotification.setEventOutboxId(eventOutbox.getId());
     * 
     * @see com.application.common.persistence.model.EventOutbox
     */
    @Column(name = "event_outbox_id", nullable = false)
    private Long eventOutboxId;

    /**
     * Corpo/descrizione della notifica
     * 
     * Es: "Tavolo per 4 persone alle 19:30", "John Doe ha creato un account", etc.
     */
    @Column(name = "body", nullable = false)
    String body;

    /**
     * Proprietà dinamiche per template rendering
     * 
     * Es: {
     *   "customer_name": "John Doe",
     *   "party_size": "4",
     *   "requested_time": "2025-01-20T19:30:00Z",
     *   "restaurant_name": "Trattoria del Mare"
     * }
     * 
     * Usato da:
     * - EmailNotificationChannel: Sostituire placeholder in template HTML
     * - SMSNotificationChannel: Personalizzare messaggio SMS
     * - WebSocketNotificationChannel: Usare per UI context
     * 
     * ⭐ NOTE: foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
     * - notification_id references MULTIPLE tables (restaurant_user_notification, customer_notification, admin_notification, agency_user_notification)
     * - Cannot use standard FK constraint pointing to single table
     * - Integrity managed at application level
     */
    @ElementCollection
    @CollectionTable(
        name = "notification_event_properties", 
        joinColumns = @JoinColumn(name = "notification_id"),
        foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT)
    )
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value")
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();

    /**
     * Flag generico di lettura (entity-level)
     * 
     * ⚠️ NOTA: Per notifiche con multipli recipient, usa NotificationRecipient.read_by_user_id
     * 
     * Questo è per notifiche entity-level (announcement, system message)
     */
    @Column(name = "is_read")
    @Builder.Default
    private Boolean read = false;

    /**
     * ⭐ SHARED READ PATTERN (First-Act Logic)
     * 
     * Se true: quando UN recipient agisce, TUTTI gli altri vedono "GESTITO"
     * 
     * Use Cases:
     * - ReservationRequest: BROADCAST (primo staff che accetta, tutti vedono)
     * - CustomerRegistration: BROADCAST (primo admin che verifica, tutti vedono)
     * - SystemAlert: INDIVIDUAL (tutti devono leggere indipendentemente)
     * 
     * Per broadcast:
     * - NotificationAction.actionType registra CHI ha fatto (first-to-act)
     * - NotificationRecipient.read_by_user_id registra chi ha letto
     * - Se sharedRead=true: UPDATE notificationRecipient SET read=true WHERE notificationId=? AND NOT SELF
     * 
     * Default: false (individual read per ogni recipient)
     */
    @Column(name = "shared_read")
    @Builder.Default
    private Boolean sharedRead = false;

    /**
     * User ID che ha letto/agito per primo (se sharedRead=true)
     * 
     * Usato per mostrare "Accettato da [Nome]" agli altri recipient
     */
    @Column(name = "read_by_user_id")
    private Long readByUserId;

    /**
     * Timestamp quando la notifica è stata letta/elaborata (entity-level)
     * 
     * Per multipli recipient: Vedi NotificationRecipient.read_at
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Timestamp di creazione della notifica
     * 
     * Gestito automaticamente da Hibernate @CreationTimestamp
     */
    @Column(name = "creation_time", updatable = false)
    @CreationTimestamp
    private Instant creationTime;

    /**
     * ⭐ SHARED READ SCOPE (Dynamic Multi-Recipient Behavior)
     * 
     * Determines scope of shared read propagation:
     * 
     * NONE (default):
     *   - Individual read state per recipient
     *   - 95% of notifications
     *   - No propagation when one user marks as read
     *   - Example: Email to single staff member
     * 
     * RESTAURANT:
     *   - All staff in same restaurant see shared read status
     *   - When one RUser reads, all RUsers in restaurant#5 see as read
     *   - Query: UPDATE WHERE restaurant_id = ?
     * 
     * RESTAURANT_HUB:
     *   - All staff across hub see shared read status
     *   - When one RUser reads, all RUser + RUserHub in hub#10 see as read
     *   - Query: UPDATE WHERE restaurant_user_hub_id = ?
     * 
     * RESTAURANT_HUB_ALL:
     *   - Admin broadcast: mark ALL as read immediately
     *   - No conditions, all rows updated
     *   - Query: UPDATE WHERE restaurant_user_hub_id = ? (unconditional)
     * 
     * Same scopes exist for Agency (AGENCY, AGENCY_HUB, AGENCY_HUB_ALL)
     * but stored with RESTAURANT prefix (strategy-level enum mapping)
     * 
     * @see com.application.common.service.notification.strategy.SharedReadScope
     */
    @Column(name = "shared_read_scope", nullable = false)
    @Builder.Default
    private String sharedReadScope = "NONE";

}
