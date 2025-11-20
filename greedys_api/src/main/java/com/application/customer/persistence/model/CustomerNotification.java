package com.application.customer.persistence.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.application.common.persistence.model.notification.ANotification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ NOTIFICA DISAGGREGATA PER CUSTOMER + CHANNEL
 * 
 * Estende ANotification e aggiunge campi specifici per il nuovo sistema RabbitListener:
 * - Per OGNI customer E OGNI channel (WEBSOCKET, EMAIL, PUSH, SMS) si crea UNA ROW
 * 
 * FLOW:
 * 1. Evento genera message su notification.customer queue
 * 2. CustomerNotificationListener riceve message
 * 3. Disaggrega per customer × channel (Es: 1 customer × 3 channels = 3 rows)
 * 4. Crea 3 CustomerNotification rows (unique eventId + channel combo)
 * 5. Persiste con status=PENDING, channel=WEBSOCKET/EMAIL/etc
 * 6. ChannelPoller legge rows per channel
 * 7. Invia via channel appropriato (WebSocket, Email, Push, SMS)
 * 8. Aggiorna status=DELIVERED/FAILED
 * 
 * ⭐ IMPORTANTE: NO SHARED READ per customer (ogni customer ha read status INDIVIDUALE)
 * 
 * Use Cases:
 * - RESERVATION_CONFIRMED (notifica personale a customer)
 * - RESERVATION_CANCELLED (notifica personale a customer)
 * - ORDER_READY (notifica personale a customer)
 * - PAYMENT_PROCESSED (notifica personale a customer)
 * - PROMOTIONAL_MESSAGE (notifica personale a customer)
 * 
 * A differenza di RESTAURANT_USER notifications che possono essere BROADCAST
 * (Es: NEW_ORDER a 10 staff → tutti vedono come READ se uno legge),
 * CUSTOMER notifications sono ALWAYS individuali.
 * 
 * Un customer non condivide notifiche con altri customers.
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "CustomerNotificationEntity")
@Table(name = "notification")
public class CustomerNotification extends ANotification {

	/**
	 * ⭐ UNIQUE IDEMPOTENCY KEY
	 * 
	 * Generato come: SHA256(aggregateType + eventId + userId + channel)
	 * 
	 * Usato da CustomerNotificationListener per idempotency check:
	 * - Se esiste già row con questo eventId → skip (duplicato da retry)
	 * - Se non esiste → crea nuova row
	 * 
	 * Garantisce che retry da RabbitMQ non crea duplicati.
	 */
	@Column(name = "event_id", unique = true, nullable = false, length = 255)
	private String eventId;

	/**
	 * ⭐ NO SHARED READ PER CUSTOMER
	 * 
	 * readByAll è sempre false per CustomerNotification.
	 * 
	 * Quando customer legge notifica:
	 * - UPDATE SOLO la row con quel userId (customerId)
	 * - Gli altri customer NON SONO affettati (non hanno accesso a questa notifica)
	 * 
	 * Questo è diverso da RestaurantUserNotification dove readByAll=true
	 * per broadcast events (Es: NEW_ORDER che notifica 5 staff, se uno legge, tutti vedono READ).
	 * 
	 * Per customer: ogni customer è isolato, non vede notifiche di altri customers.
	 */
	@Column(name = "read_by_all", nullable = false)
	@Builder.Default
	private Boolean readByAll = false;  // Always false for customer

	/**
	 * DELIVERY CHANNEL per questa disaggregazione
	 * 
	 * Enum values:
	 * - WEBSOCKET: WebSocket per real-time browser notification
	 * - EMAIL: Email notification
	 * - PUSH: Mobile push notification
	 * - SMS: SMS text message
	 * 
	 * Ogni (customerId, eventId, channel) combo = 1 row separata.
	 * 
	 * ChannelPoller queries per channel:
	 * - SELECT * FROM notification WHERE channel='WEBSOCKET' AND status='PENDING'
	 * - SELECT * FROM notification WHERE channel='EMAIL' AND status='PENDING'
	 */
	@Column(name = "channel", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private NotificationChannel channel;

	/**
	 * DELIVERY STATUS (più granulare di is_read)
	 * 
	 * - PENDING: Appena creata, in attesa di invio
	 * - DELIVERED: Inviata via channel con successo
	 * - FAILED: Errore durante invio (es: email bounce)
	 * - READ: Customer ha letto via WebSocket
	 * 
	 * Transizioni:
	 * PENDING → DELIVERED (channel send successful)
	 * PENDING → FAILED (channel send failed)
	 * DELIVERED/PENDING → READ (customer action via WebSocket)
	 */
	@Column(name = "status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private DeliveryStatus status = DeliveryStatus.PENDING;

	/**
	 * PRIORITY dell'invio
	 * 
	 * - HIGH: Immediate delivery (reservation confirmed, urgent message)
	 * - NORMAL: ~5 minuti (regular notification)
	 * - LOW: ~1 ora (promotional, optional updates)
	 * 
	 * ChannelPoller queries con ORDER BY:
	 * - SELECT * FROM notification 
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
	 * Es: RESERVATION_CONFIRMED, RESERVATION_CANCELLED, ORDER_READY, PAYMENT_PROCESSED, etc
	 * 
	 * Usato per:
	 * 1. Template selection: quale template usare per Email/Push
	 * 2. Icon/color in UI: RESERVATION_CONFIRMED → checkmark icon, CANCELLED → x icon
	 * 3. Action URL: link a pagina relativa (reservation detail, order status, etc)
	 */
	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	/**
	 * AGGREGATE TYPE (chi ha agito)
	 * 
	 * Es: RESTAURANT_USER, ADMIN, SYSTEM
	 * 
	 * Per customer notifications è generalmente chi ha fatto azione:
	 * - RESTAURANT_USER: staff ha confermato prenotazione
	 * - ADMIN: admin ha inviato messaggio promozionale
	 * - SYSTEM: sistema ha inviato notifica (es: reminder, timeout)
	 */
	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	/**
	 * CUSTOMER REFERENCE (legacy, keep for backward compatibility)
	 * 
	 * Relationship to Customer entity (if exists).
	 * Per new RabbitListener system, userId (from ANotification) è preferito.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idcustomer")
	private Customer customer;

	/**
	 * TITLE (da AEventNotification)
	 * Ereditate da ANotification → AEventNotification
	 * Es: "Prenotazione confermata", "Ordine pronto"
	 */
	// Inherited: title

	/**
	 * BODY (da AEventNotification)
	 * Es: "La tua prenotazione per 4 persone alle 19:30 è confermata"
	 */
	// Inherited: body

	/**
	 * PAYLOAD per template rendering
	 * Es: {
	 *   "reservation_id": "RES-12345",
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
	 * - Nessun altro customer è affettato
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

	/**
	 * DELIVERY CHANNEL ENUM
	 */
	public enum NotificationChannel {
		WEBSOCKET, EMAIL, PUSH, SMS
	}

	/**
	 * DELIVERY STATUS ENUM
	 */
	public enum DeliveryStatus {
		PENDING,    // Appena creata
		DELIVERED,  // Inviata via channel
		FAILED,     // Errore invio
		READ        // Customer ha letto
	}

	/**
	 * PRIORITY ENUM
	 */
	public enum NotificationPriority {
		HIGH,       // Immediate delivery
		NORMAL,     // ~5 minuti
		LOW         // ~1 ora
	}
}
