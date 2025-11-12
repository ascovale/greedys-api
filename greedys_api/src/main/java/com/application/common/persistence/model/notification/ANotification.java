package com.application.common.persistence.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ GERARCHIA 2: NOTIFICHE RECIPIENT-SPECIFICHE (con userId/userType)
 * 
 * Estende AEventNotification e aggiunge campi specifici per il RECIPIENT destinatario.
 * 
 * FLOW:
 * 1. Evento generato (ReservationRequested, CustomerRegistered, etc)
 * 2. Listener crea AEventNotification (AdminEventNotification, RestaurantEventNotification)
 * 3. Per OGNI recipient di quella entità, crea ANotification con userId/userType
 *    - Es: For Restaurant con 50 staff, crea 50 rows di RestaurantNotification (ciascuno con userId di uno staff)
 * 4. NotificationChannelSend ha: notificationId + channelType (SMS, EMAIL, PUSH, etc)
 * 
 * ✅ DIFFERENZE:
 * - AEventNotification: Entity-level (title, body, no userId) → Per restaurant, customer, admin, agency
 * - ANotification: Recipient-level (+ userId, userType) → Per singolo staff/customer/admin
 * 
 * ESEMPI:
 * 
 * EVENT: ReservationRequestedEvent
 * ↓
 * AdminEventNotification (id=1, title="Nuova prenotazione", restaurant_id=10)
 * ↓
 * For each admin in admins of restaurant:
 *   ├─ AdminNotification (id=100, user_id=50, user_type=ADMIN_USER) → extends ANotification
 *   ├─ AdminNotification (id=101, user_id=51, user_type=ADMIN_USER)
 *   └─ AdminNotification (id=102, user_id=52, user_type=ADMIN_USER)
 * 
 * ↓↓↓
 * For each notification:
 *   ├─ NotificationChannelSend (notification_id=100, channel=SMS)
 *   ├─ NotificationChannelSend (notification_id=100, channel=EMAIL)
 *   ├─ NotificationChannelSend (notification_id=100, channel=PUSH)
 *   └─ NotificationChannelSend (notification_id=100, channel=WEBSOCKET)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Recipient-Specific Notifications)
 */
@MappedSuperclass
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public abstract class ANotification extends AEventNotification {
    
    /**
     * User ID del recipient destinatario
     * 
     * Questo è l'utente SPECIFICO che riceverà la notifica:
     * - Se è admin: Admin user ID (dalla tabella admin_users)
     * - Se è customer: Customer user ID (dalla tabella customers)
     * - Se è staff ristorante: RUser ID (dalla tabella restaurant_users)
     * - Se è agency: Agency user ID
     * 
     * ⭐ IMPORTANTE: Un AEventNotification può generare N ANotification (una per recipient)
     * 
     * Esempio per ReservationRequestedEvent:
     * - AdminEventNotification (title="Nuova prenotazione", NO userId)
     * - AdminNotification #1 (user_id=50, user_type=ADMIN_USER)
     * - AdminNotification #2 (user_id=51, user_type=ADMIN_USER)
     * - AdminNotification #3 (user_id=52, user_type=ADMIN_USER)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Tipo di utente destinatario
     * 
     * Enum values:
     * - CUSTOMER: Customer user
     * - RESTAURANT_USER: Staff di ristorante (RUser)
     * - ADMIN_USER: Admin dell'app
     * - AGENCY_USER: Utente agency
     * 
     * Usato per:
     * - Multi-channel resolution: Sapere quale tabella querare per email/phone/etc.
     * - Permission checks: Se ricevi notifica SMS, sei CUSTOMER o RESTAURANT_USER
     * - Template rendering: Personalizzare messaggio per tipo utente
     */
    @Column(name = "user_type", nullable = false, length = 50)
    private String userType;

}
