package com.application.common.persistence.model.notification;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ GERARCHIA 2: NOTIFICHE RECIPIENT-SPECIFICHE (con userId polimomorfico)
 * 
 * Estende AEventNotification e aggiunge campi specifici per il RECIPIENT destinatario.
 * 
 * REFACTORING con JOINED inheritance:
 * - PRIMA: ANotification aveva (user_id: Long, user_type: String)
 *   - Problema: user_id è ambiguo (quale tabella? customer? ruser? admin?)
 * 
 * - DOPO: Sottoclassi tipizzate di ANotification
 *   - CustomerNotification extends ANotification (+ customerId FK a Customer.id)
 *   - RestaurantNotification extends ANotification (+ rUserId FK a RUser.id)
 *   - AdminNotification extends ANotification (+ adminId FK to Admin.id)
 *   - AgencyNotification extends ANotification (+ agencyUserId FK to AgencyUser.id)
 * 
 * FLOW:
 * 1. Evento generato (ReservationRequested, CustomerRegistered, etc)
 * 2. Listener crea AEventNotification (AdminEventNotification, RestaurantEventNotification)
 * 3. Per OGNI recipient di quella entità, crea sottoclasse di ANotification con userId tipizzato
 *    - Es: For Restaurant con 50 staff, crea 50 rows di RestaurantNotification (ciascuno con rUserId di uno staff)
 * 4. NotificationChannelSend ha: notificationId + channelType (SMS, EMAIL, PUSH, etc)
 * 
 * ✅ VANTAGGI:
 * - Type-safe: userId referenzia la colonna giusta (customer_id, ruser_id, admin_id, agency_user_id)
 * - No ambiguity: Sappiamo esattamente quale utente riceve la notifica
 * - Migrare da user_type string a class hierarchy
 * - WebSocket routing: Usa class type invece di user_type string
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
     * User ID del recipient (generico, tipo-specifico implementato in sottoclassi)
     * 
     * Questo campo viene usato da:
     * - NotificationWebSocketSender.sendNotificationInternal() → passa a send()
     * - ChannelPoller.sendNotification() → usato per recipient lookup
     * - ReadStatusService.markXxxNotificationAsRead() → controlla ownership
     * 
     * Sottoclassi implementano mappando a loro FK specifiche:
     * - CustomerNotification: userId = customerId
     * - RestaurantUserNotification: userId = user_id (RUser)
     * - AdminNotification: userId = user_id (Admin)
     * - AgencyUserNotification: userId = user_id (AgencyUser)
     */
    @jakarta.persistence.Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Ritorna il recipient ID specifico della sottoclasse
     * - CustomerNotification: restituisce customerId (FK to Customer)
     * - RestaurantNotification: restituisce rUserId (FK to RUser)
     * - AdminNotification: restituisce adminId (FK to Admin)
     * - AgencyNotification: restituisce agencyUserId (FK to AgencyUser)
     */
    public abstract Long getRecipientId();

    /**
     * Ritorna il tipo di recipient (CUSTOMER, RESTAURANT_USER, ADMIN_USER, AGENCY_USER)
     * Derivato dal class type, non da una colonna user_type
     */
    public abstract String getRecipientType();
}