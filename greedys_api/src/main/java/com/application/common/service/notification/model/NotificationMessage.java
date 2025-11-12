package com.application.common.service.notification.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello di notifica da inviare via canale (Email, SMS, WebSocket, Firebase, Slack)
 * 
 * Creato dal NotificationOutbox e mandato ai ChannelPoller per l'invio su ogni canale
 * 
 * Proprietà:
 * - notificationId: ID della notificazione nel database
 * - recipientId: ID del destinatario (Customer ID, RUser ID, Admin ID, Agency ID)
 * - recipientType: Tipo di destinatario (CUSTOMER, RESTAURANT_USER, ADMIN_USER, AGENCY_USER)
 * - channel: Canale di invio (EMAIL, SMS, PUSH, WEBSOCKET, SLACK)
 * - type: Tipo di notifica (CONFIRMATION, REMINDER, PAYMENT, etc.)
 * - title: Titolo della notifica
 * - body: Corpo del messaggio
 * - metadata: Dati aggiuntivi (Map<String, String>)
 * - timestamp: Quando è stata creata
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long notificationId;          // ID della notificazione in DB
    private Long recipientId;             // ID del destinatario
    private String recipientType;         // CUSTOMER | RESTAURANT_USER | ADMIN_USER | AGENCY_USER
    private String channel;               // EMAIL | SMS | PUSH | WEBSOCKET | SLACK
    private String type;                  // CONFIRMATION, REMINDER, PAYMENT, REWARD, etc.
    private String title;                 // Oggetto/Titolo notifica
    private String body;                  // Corpo messaggio
    
    @JsonProperty("metadata")
    private Map<String, String> metadata; // Dati aggiuntivi (eventId, reservationId, phone, email, etc)
    
    private Instant timestamp;            // Quando è stata creata la notificazione
    
    // ============ Helper Methods ============
    
    public String getEventId() {
        return metadata != null ? metadata.get("eventId") : null;
    }

    public String getPhoneNumber() {
        return metadata != null ? metadata.get("phone") : null;
    }

    public String getEmail() {
        return metadata != null ? metadata.get("email") : null;
    }
    
    public boolean hasPhoneNumber() {
        return getPhoneNumber() != null && !getPhoneNumber().isEmpty();
    }
    
    public boolean hasEmail() {
        return getEmail() != null && !getEmail().isEmpty();
    }
}
