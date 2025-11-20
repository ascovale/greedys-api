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
     */
    @ElementCollection
    @CollectionTable(name = "notification_event_properties", joinColumns = @JoinColumn(name = "notification_id"))
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

}
