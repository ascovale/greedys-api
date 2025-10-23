package com.application.common.persistence.model.notification.context;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Context condiviso per notifiche relative a messaggi/chat.
 * 
 * Usato da Customer e Restaurant per notifiche di nuovi messaggi.
 * Admin potrebbe usarlo per moderazione.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatContext {
    
    /**
     * ID della conversazione
     */
    @Column(name = "ctx_chat_id")
    private Long chatId;
    
    /**
     * Nome del mittente
     */
    @Column(name = "ctx_sender_name", length = 100)
    private String senderName;
    
    /**
     * Tipo di mittente
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ctx_sender_type", length = 20)
    private SenderType senderType;
    
    /**
     * Preview del messaggio (primi 200 caratteri)
     */
    @Column(name = "ctx_message_preview", length = 200)
    private String messagePreview;
    
    /**
     * Numero messaggi non letti in questa chat
     */
    @Column(name = "ctx_unread_count")
    @Builder.Default
    private Integer unreadCount = 1;
    
    /**
     * Tipo di mittente messaggio
     */
    public enum SenderType {
        /**
         * Messaggio da cliente
         */
        CUSTOMER,
        
        /**
         * Messaggio da staff ristorante
         */
        RESTAURANT_STAFF,
        
        /**
         * Messaggio automatico di sistema
         */
        SYSTEM
    }
}
