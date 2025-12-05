package com.application.common.persistence.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ⭐ CHAT NOTIFICATION ENTITY
 * 
 * Notifica per eventi di chat (messaggi, typing, presenza).
 * 
 * Estende ANotification con campi specifici per chat:
 * - conversationId
 * - messageId
 * - senderName
 * - messagePreview
 * - webSocketDestination
 * 
 * FLOW:
 * ChatService → EventOutbox → RabbitMQ → ChatOrchestrator → ChatNotification
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "chat_notification", indexes = {
    @Index(name = "idx_chat_notif_user", columnList = "user_id"),
    @Index(name = "idx_chat_notif_conversation", columnList = "conversation_id"),
    @Index(name = "idx_chat_notif_event_type", columnList = "event_type"),
    @Index(name = "idx_chat_notif_created", columnList = "creation_time DESC")
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotification extends ANotification {

    /**
     * Tipo di evento (CHAT_MESSAGE_RECEIVED, CHAT_TYPING_INDICATOR, etc.)
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * ID della conversazione
     */
    @Column(name = "conversation_id")
    private Long conversationId;

    /**
     * ID del messaggio (se applicabile)
     */
    @Column(name = "message_id")
    private Long messageId;

    /**
     * Nome del mittente (per preview)
     */
    @Column(name = "sender_name", length = 100)
    private String senderName;

    /**
     * Preview del messaggio (troncato)
     */
    @Column(name = "message_preview", length = 200)
    private String messagePreview;

    /**
     * Canale di notifica (WEBSOCKET, PUSH)
     */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /**
     * Event ID disaggregato
     */
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    /**
     * Destinazione WebSocket per questo evento
     */
    @Column(name = "websocket_destination", length = 200)
    private String webSocketDestination;

    @Override
    public Long getRecipientId() {
        return getUserId();
    }

    @Override
    public String getRecipientType() {
        return "CHAT_USER";
    }
}
