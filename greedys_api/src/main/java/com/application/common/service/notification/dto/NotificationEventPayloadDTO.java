package com.application.common.service.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ NOTIFICATION EVENT PAYLOAD DTO
 * 
 * Rappresenta il payload del messaggio RabbitMQ per le notifiche.
 * Jackson deserializza automaticamente da JSON in questo DTO.
 * 
 * FLOW:
 * EventOutboxOrchestrator pubblica a RabbitMQ con payload:
 * {
 *   "eventId": "evt_123",
 *   "eventType": "RESERVATION_NEW",
 *   "recipientType": "RESTAURANT_TEAM",
 *   "recipientId": 3,
 *   "timestamp": "2025-01-20T14:30:00Z",
 *   "data": {
 *     "reservationId": 147,
 *     "customerName": "Giulia Bianchi",
 *     "partySize": 4,
 *     "time": "20:00"
 *   }
 * }
 * 
 * RestaurantTeamNotificationListener riceve automaticamente deserializzato come:
 * NotificationEventPayloadDTO oggetto tipizzato
 * 
 * @author Greedy's System
 * @since 2025-01-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEventPayloadDTO {
    
    /**
     * Unique event identifier for idempotency check
     */
    @JsonProperty("eventId")
    private String eventId;
    
    /**
     * Event type: RESERVATION_NEW, RESERVATION_MODIFIED, RESERVATION_CANCELLED, etc
     */
    @JsonProperty("eventType")
    private String eventType;
    
    /**
     * Recipient type: RESTAURANT_TEAM, RESTAURANT_USER, CUSTOMER, ADMIN, etc
     */
    @JsonProperty("recipientType")
    private String recipientType;
    
    /**
     * Recipient ID (matches user ID, not table)
     * For RESTAURANT_TEAM: restaurantId
     * For CUSTOMER: customerId
     * For ADMIN: adminId
     */
    @JsonProperty("recipientId")
    private Long recipientId;
    
    /**
     * Event timestamp (ISO 8601)
     */
    @JsonProperty("timestamp")
    private String timestamp;
    
    /**
     * Event-specific data (reservation details, etc)
     * This is a Map to be flexible for different event types
     */
    @JsonProperty("data")
    private java.util.Map<String, Object> data;
}
