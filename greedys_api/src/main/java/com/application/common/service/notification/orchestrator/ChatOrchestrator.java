package com.application.common.service.notification.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.application.common.persistence.dao.chat.ChatConversationDAO;
import com.application.common.persistence.dao.chat.ChatParticipantDAO;
import com.application.common.persistence.model.chat.ChatConversation;
import com.application.common.persistence.model.chat.ChatParticipant;
import com.application.common.persistence.model.notification.ChatNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ CHAT ORCHESTRATOR
 * 
 * Orchestratore per la disaggregazione dei messaggi di chat.
 * Riceve eventi dalla coda notification.chat.* e li distribuisce ai destinatari.
 * 
 * FLOW:
 * 1. Riceve messaggio chat da RabbitMQ
 * 2. Carica partecipanti della conversazione
 * 3. Esclude il mittente
 * 4. Filtra per preferenze notifica
 * 5. Crea notifiche per ogni destinatario
 * 
 * QUEUES HANDLED:
 * - notification.chat.direct: Chat 1-1
 * - notification.chat.group: Chat di gruppo
 * - notification.chat.support: Chat di supporto
 * - notification.chat.reservation: Chat legate a prenotazioni
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatOrchestrator extends NotificationOrchestrator<ChatNotification> {

    private final ChatConversationDAO conversationDAO;
    private final ChatParticipantDAO participantDAO;

    @Override
    public List<ChatNotification> disaggregateAndProcess(Map<String, Object> message) {
        String eventType = extractEventType(message);
        
        List<ChatNotification> notifications = new ArrayList<>();
        
        // Carica destinatari (partecipanti escluso mittente)
        List<Long> recipients = loadRecipients(message);
        
        if (recipients.isEmpty()) {
            log.warn("⚠️ ChatOrchestrator: no recipients found for eventType={}", eventType);
            return notifications;
        }
        
        for (Long recipientId : recipients) {
            // Per chat, il canale principale è WEBSOCKET
            List<String> channels = loadUserPreferences(recipientId);
            
            if (channels.isEmpty()) {
                channels.add("WEBSOCKET"); // Default sempre abilitato
            }
            
            for (String channel : channels) {
                String disaggregatedEventId = generateDisaggregatedEventId(
                    extractEventId(message), recipientId, channel
                );
                
                ChatNotification notification = createNotificationRecord(
                    disaggregatedEventId, 
                    recipientId, 
                    channel, 
                    message
                );
                
                notifications.add(notification);
            }
        }
        
        log.info("✅ Chat disaggregation: {} → {} notifications for {} recipients", 
            eventType, notifications.size(), recipients.size());
        
        return notifications;
    }

    /**
     * Helper: Extract eventType supporting both camelCase and snake_case
     */
    private String extractEventType(Map<String, Object> message) {
        if (message.containsKey("event_type")) {
            return (String) message.get("event_type");
        }
        if (message.containsKey("eventType")) {
            return (String) message.get("eventType");
        }
        throw new IllegalArgumentException("Missing event_type/eventType in message");
    }

    /**
     * Helper: Extract eventId supporting both camelCase and snake_case
     */
    private String extractEventId(Map<String, Object> message) {
        if (message.containsKey("event_id")) {
            return (String) message.get("event_id");
        }
        if (message.containsKey("eventId")) {
            return (String) message.get("eventId");
        }
        throw new IllegalArgumentException("Missing event_id/eventId in message");
    }

    /**
     * Helper: Extract payload safely (returns null if not present)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayloadSafe(Map<String, Object> message) {
        Object payload = message.get("payload");
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        Object data = message.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return null;
    }

    /**
     * Helper: Extract Long safely (returns null if not present or not a number)
     */
    private Long extractLongSafe(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        Map<String, Object> payload = extractPayloadSafe(message);
        
        if (payload == null) {
            log.warn("⚠️ ChatOrchestrator: payload is null");
            return new ArrayList<>();
        }
        
        Long conversationId = extractLongSafe(payload, "conversationId");
        Long senderId = extractLongSafe(payload, "senderId");
        
        if (conversationId == null) {
            log.warn("⚠️ ChatOrchestrator: conversationId is null");
            return new ArrayList<>();
        }
        
        // Carica tutti i partecipanti della conversazione
        List<ChatParticipant> participants = participantDAO.findByConversationId(conversationId);
        
        List<Long> recipients = new ArrayList<>();
        for (ChatParticipant participant : participants) {
            // Escludi il mittente
            if (senderId == null || !participant.getUserId().equals(senderId)) {
                // Controlla se ha notifiche abilitate
                if (participant.getNotificationsEnabled() == null || participant.getNotificationsEnabled()) {
                    recipients.add(participant.getUserId());
                }
            }
        }
        
        log.debug("📢 Chat: found {} recipients for conversationId={} (excluding sender={})", 
            recipients.size(), conversationId, senderId);
        
        return recipients;
    }

    @Override
    protected List<String> loadUserPreferences(Long userId) {
        // Per chat, WebSocket è sempre abilitato
        // Potenzialmente anche PUSH per mobile
        List<String> channels = new ArrayList<>();
        channels.add("WEBSOCKET");
        channels.add("PUSH");
        
        // TODO: In futuro, caricare da UserNotificationPreferences
        
        return channels;
    }

    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        // Per chat, le impostazioni di gruppo sono a livello di conversazione
        Map<String, Object> payload = extractPayloadSafe(message);
        Long conversationId = payload != null ? extractLongSafe(payload, "conversationId") : null;
        
        if (conversationId == null) {
            return Map.of(
                "conversationType", "DIRECT",
                "isMuted", false
            );
        }
        
        ChatConversation conversation = conversationDAO.findById(conversationId).orElse(null);
        
        return Map.of(
            "conversationType", conversation != null ? conversation.getConversationType().name() : "DIRECT",
            "isMuted", false // TODO: gestire mute a livello conversazione
        );
    }

    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        // Chat events routing rules
        switch (eventType) {
            case "CHAT_MESSAGE_RECEIVED":
            case "CHAT_GROUP_MESSAGE":
            case "CHAT_RESERVATION_MESSAGE":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of("PUSH")
                );
            
            case "CHAT_TYPING_INDICATOR":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of() // No push per typing
                );
            
            case "CHAT_USER_JOINED":
            case "CHAT_USER_LEFT":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of()
                );
            
            case "CHAT_MESSAGES_READ":
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of()
                );
            
            default:
                return Map.of(
                    "mandatory", List.of("WEBSOCKET"),
                    "optional", List.of()
                );
        }
    }

    @Override
    protected ChatNotification createNotificationRecord(
            String eventId, 
            Long userId, 
            String channel, 
            Map<String, Object> message
    ) {
        Map<String, Object> payload = extractPayloadSafe(message);
        String eventType = extractEventType(message);
        
        ChatNotification notification = new ChatNotification();
        notification.setEventId(eventId);
        notification.setUserId(userId);
        notification.setChannel(channel);
        notification.setEventType(eventType);
        
        if (payload != null) {
            // Estrai dettagli dalla payload
            Long conversationId = extractLongSafe(payload, "conversationId");
            if (conversationId != null) {
                notification.setConversationId(conversationId);
            }
            Long messageId = extractLongSafe(payload, "messageId");
            if (messageId != null) {
                notification.setMessageId(messageId);
            }
            if (payload.containsKey("content")) {
                notification.setMessagePreview(truncateContent((String) payload.get("content"), 100));
            }
            if (payload.containsKey("senderName")) {
                notification.setSenderName((String) payload.get("senderName"));
            }
        }
        
        // Imposta titolo e body per notifica
        notification.setTitle(generateTitle(eventType, payload));
        notification.setBody(generateBody(eventType, payload));
        
        // WebSocket destination
        notification.setWebSocketDestination(generateWebSocketDestination(eventType, payload));
        
        return notification;
    }

    /**
     * Genera titolo della notifica basato su event type
     */
    private String generateTitle(String eventType, Map<String, Object> payload) {
        String senderName = (payload != null && payload.containsKey("senderName"))
            ? (String) payload.get("senderName") 
            : "Utente";
        
        switch (eventType) {
            case "CHAT_MESSAGE_RECEIVED":
            case "CHAT_GROUP_MESSAGE":
            case "CHAT_RESERVATION_MESSAGE":
                return "Nuovo messaggio da " + senderName;
            
            case "CHAT_TYPING_INDICATOR":
                return senderName + " sta scrivendo...";
            
            case "CHAT_USER_JOINED":
                return senderName + " si è unito alla conversazione";
            
            case "CHAT_USER_LEFT":
                return senderName + " ha lasciato la conversazione";
            
            case "CHAT_MESSAGES_READ":
                return "Messaggi letti";
            
            default:
                return "Chat update";
        }
    }

    /**
     * Genera body della notifica
     */
    private String generateBody(String eventType, Map<String, Object> payload) {
        if (payload != null && payload.containsKey("content")) {
            return truncateContent((String) payload.get("content"), 150);
        }
        return "";
    }

    /**
     * Genera destinazione WebSocket
     */
    private String generateWebSocketDestination(String eventType, Map<String, Object> payload) {
        Long conversationId = (payload != null) ? extractLongSafe(payload, "conversationId") : null;
        
        if (conversationId == null) {
            return "/topic/chat/unknown";
        }
        
        switch (eventType) {
            case "CHAT_MESSAGE_RECEIVED":
                return "/topic/chat/direct/" + conversationId;
            
            case "CHAT_GROUP_MESSAGE":
                return "/topic/chat/group/" + conversationId;
            
            case "CHAT_RESERVATION_MESSAGE":
                Long reservationId = extractLongSafe(payload, "reservationId");
                return "/topic/chat/reservation/" + (reservationId != null ? reservationId : conversationId);
            
            case "CHAT_TYPING_INDICATOR":
            case "CHAT_MESSAGES_READ":
            case "CHAT_USER_JOINED":
            case "CHAT_USER_LEFT":
                return "/topic/chat/presence/" + conversationId;
            
            default:
                return "/topic/chat/" + conversationId;
        }
    }

    /**
     * Tronca contenuto per preview
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength - 3) + "...";
    }
}
