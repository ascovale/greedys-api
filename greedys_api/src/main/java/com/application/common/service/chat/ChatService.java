package com.application.common.service.chat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.domain.event.EventType;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.chat.ChatConversationDAO;
import com.application.common.persistence.dao.chat.ChatMessageDAO;
import com.application.common.persistence.dao.chat.ChatParticipantDAO;
import com.application.common.persistence.model.chat.ChatConversation;
import com.application.common.persistence.model.chat.ChatMessage;
import com.application.common.persistence.model.chat.ChatParticipant;
import com.application.common.persistence.model.chat.ConversationType;
import com.application.common.persistence.model.chat.MessageType;
import com.application.common.persistence.model.chat.ParticipantRole;
import com.application.common.persistence.model.notification.EventOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ CHAT SERVICE
 * 
 * Service per la gestione delle conversazioni e messaggi chat.
 * Supporta chat dirette, di gruppo, supporto e prenotazione.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatConversationDAO conversationDAO;
    private final ChatMessageDAO messageDAO;
    private final ChatParticipantDAO participantDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final ObjectMapper objectMapper;

    // ==================== CONVERSATIONS ====================

    /**
     * Crea una nuova conversazione diretta tra due utenti
     */
    public ChatConversation createDirectConversation(Long user1Id, Long user2Id) {
        // Verifica se esiste già
        Optional<ChatConversation> existing = conversationDAO.findDirectConversation(user1Id, user2Id);
        if (existing.isPresent()) {
            log.info("Conversazione diretta già esistente tra {} e {}", user1Id, user2Id);
            return existing.get();
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(ConversationType.DIRECT);
        conversation.setCreatedById(user1Id);
        conversation = conversationDAO.save(conversation);

        // Aggiungi partecipanti
        addParticipant(conversation, user1Id, ParticipantRole.MEMBER);
        addParticipant(conversation, user2Id, ParticipantRole.MEMBER);

        log.info("✅ Creata conversazione diretta {} tra {} e {}", conversation.getId(), user1Id, user2Id);
        return conversation;
    }

    /**
     * Crea una nuova conversazione di gruppo
     */
    public ChatConversation createGroupConversation(Long creatorId, String name, List<Long> memberIds) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(ConversationType.GROUP);
        conversation.setName(name);
        conversation.setCreatedById(creatorId);
        conversation = conversationDAO.save(conversation);

        // Aggiungi creatore come owner
        addParticipant(conversation, creatorId, ParticipantRole.OWNER);

        // Aggiungi membri
        for (Long memberId : memberIds) {
            if (!memberId.equals(creatorId)) {
                addParticipant(conversation, memberId, ParticipantRole.MEMBER);
            }
        }

        log.info("✅ Creato gruppo '{}' con {} membri", name, memberIds.size() + 1);
        return conversation;
    }

    /**
     * Crea conversazione per una prenotazione
     */
    public ChatConversation createReservationConversation(Long reservationId, Long customerId, Long restaurantId) {
        // Verifica se esiste già
        Optional<ChatConversation> existing = conversationDAO.findByReservationId(reservationId)
            .stream().findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(ConversationType.RESERVATION);
        conversation.setReservationId(reservationId);
        conversation.setRestaurantId(restaurantId);
        conversation.setCreatedById(customerId);
        conversation = conversationDAO.save(conversation);

        addParticipant(conversation, customerId, ParticipantRole.MEMBER);
        // Il ristorante verrà aggiunto come partecipante speciale

        log.info("✅ Creata conversazione prenotazione {} per reservation {}", conversation.getId(), reservationId);
        return conversation;
    }

    /**
     * Ottieni conversazioni di un utente
     */
    @Transactional(readOnly = true)
    public Page<ChatConversation> getUserConversations(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return conversationDAO.findByParticipantUserId(userId, pageable);
    }

    /**
     * Ottieni conversazione per ID
     */
    @Transactional(readOnly = true)
    public Optional<ChatConversation> getConversation(Long conversationId) {
        return conversationDAO.findById(conversationId);
    }

    // ==================== PARTICIPANTS ====================

    /**
     * Aggiungi partecipante a una conversazione
     */
    public ChatParticipant addParticipant(ChatConversation conversation, Long userId, ParticipantRole role) {
        // Verifica se già presente
        Optional<ChatParticipant> existing = participantDAO.findByConversationIdAndUserId(conversation.getId(), userId);
        if (existing.isPresent()) {
            ChatParticipant participant = existing.get();
            if (participant.getLeftAt() != null) {
                // Rientra nella conversazione
                participant.setLeftAt(null);
                participant.setRole(role);
                return participantDAO.save(participant);
            }
            return participant;
        }

        ChatParticipant participant = new ChatParticipant();
        participant.setConversation(conversation);
        participant.setUserId(userId);
        participant.setRole(role);
        return participantDAO.save(participant);
    }

    /**
     * Rimuovi partecipante da una conversazione
     */
    public void removeParticipant(Long conversationId, Long userId) {
        participantDAO.findByConversationIdAndUserId(conversationId, userId)
            .ifPresent(participant -> {
                participant.leave();
                participantDAO.save(participant);
                log.info("Utente {} ha lasciato la conversazione {}", userId, conversationId);
            });
    }

    /**
     * Ottieni partecipanti attivi
     */
    @Transactional(readOnly = true)
    public List<ChatParticipant> getActiveParticipants(Long conversationId) {
        return participantDAO.findActiveByConversationId(conversationId);
    }

    // ==================== MESSAGES ====================

    /**
     * Invia un messaggio
     */
    public ChatMessage sendMessage(Long conversationId, Long senderId, String content, MessageType messageType) {
        ChatConversation conversation = conversationDAO.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversazione non trovata: " + conversationId));

        // Verifica che il sender sia un partecipante attivo
        participantDAO.findByConversationIdAndUserId(conversationId, senderId)
            .filter(p -> p.getLeftAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("Utente non è un partecipante attivo"));

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMessageType(messageType);
        message = messageDAO.save(message);

        // Aggiorna last_message_at
        conversation.setLastMessageAt(Instant.now());
        conversationDAO.save(conversation);

        // Crea evento per notifiche
        triggerMessageNotification(conversation, message);

        log.debug("📨 Messaggio {} inviato in conversazione {} da utente {}", 
            message.getId(), conversationId, senderId);
        
        return message;
    }

    /**
     * Invia messaggio di testo (shortcut)
     */
    public ChatMessage sendTextMessage(Long conversationId, Long senderId, String content) {
        return sendMessage(conversationId, senderId, content, MessageType.TEXT);
    }

    /**
     * Invia risposta a un messaggio
     */
    public ChatMessage sendReply(Long conversationId, Long senderId, String content, Long replyToMessageId) {
        ChatMessage replyTo = messageDAO.findById(replyToMessageId)
            .orElseThrow(() -> new IllegalArgumentException("Messaggio originale non trovato"));

        ChatMessage message = sendMessage(conversationId, senderId, content, MessageType.TEXT);
        message.setReplyToMessage(replyTo);
        return messageDAO.save(message);
    }

    /**
     * Ottieni messaggi di una conversazione
     */
    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessages(Long conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageDAO.findByConversationId(conversationId, pageable);
    }

    /**
     * Ottieni messaggi dopo un certo messaggio (per realtime sync)
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesAfter(Long conversationId, Long afterMessageId) {
        return messageDAO.findByConversationIdAfterMessage(conversationId, afterMessageId);
    }

    /**
     * Marca messaggi come letti
     */
    public void markAsRead(Long conversationId, Long userId, Long lastReadMessageId) {
        participantDAO.findByConversationIdAndUserId(conversationId, userId)
            .ifPresent(participant -> {
                participant.setLastReadMessageId(lastReadMessageId);
                participant.setLastReadAt(Instant.now());
                participantDAO.save(participant);
            });
    }

    /**
     * Conta messaggi non letti per un utente
     */
    @Transactional(readOnly = true)
    public Long countUnreadMessages(Long conversationId, Long userId) {
        return messageDAO.countUnreadMessages(conversationId, userId);
    }

    /**
     * Elimina messaggio (soft delete)
     */
    public void deleteMessage(Long messageId, Long userId) {
        messageDAO.findById(messageId)
            .filter(m -> m.getSenderId().equals(userId))
            .ifPresent(message -> {
                message.softDelete();
                messageDAO.save(message);
                log.info("Messaggio {} eliminato da utente {}", messageId, userId);
            });
    }

    /**
     * Cerca messaggi in una conversazione
     */
    @Transactional(readOnly = true)
    public Page<ChatMessage> searchMessages(Long conversationId, String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageDAO.searchInConversation(conversationId, searchTerm, pageable);
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Triggera notifica per nuovo messaggio
     */
    private void triggerMessageNotification(ChatConversation conversation, ChatMessage message) {
        EventType eventType = switch (conversation.getConversationType()) {
            case DIRECT -> EventType.CHAT_MESSAGE_RECEIVED;
            case GROUP -> EventType.CHAT_GROUP_MESSAGE;
            case SUPPORT -> EventType.SUPPORT_TICKET_MESSAGE;
            case RESERVATION -> EventType.CHAT_RESERVATION_MESSAGE;
        };

        // Notifica tutti i partecipanti tranne il mittente
        List<ChatParticipant> participants = participantDAO.findActiveByConversationId(conversation.getId());
        for (ChatParticipant participant : participants) {
            if (!participant.getUserId().equals(message.getSenderId()) && participant.getNotificationsEnabled()) {
                try {
                    String payload = objectMapper.writeValueAsString(Map.of(
                        "conversationId", conversation.getId(),
                        "messageId", message.getId(),
                        "senderId", message.getSenderId(),
                        "content", message.getContent(),
                        "messageType", message.getMessageType().name()
                    ));
                    
                    EventOutbox outbox = EventOutbox.builder()
                        .eventId("chat_" + message.getId() + "_" + participant.getUserId())
                        .eventType(eventType.name())
                        .aggregateType("ChatMessage")
                        .aggregateId(message.getId())
                        .payload(payload)
                        .build();
                    
                    eventOutboxDAO.save(outbox);
                } catch (Exception e) {
                    log.error("Errore creazione evento notifica chat: {}", e.getMessage());
                }
            }
        }
    }

    // ==================== TYPING INDICATOR ====================

    /**
     * Notifica che un utente sta scrivendo
     */
    public void sendTypingIndicator(Long conversationId, Long userId, boolean isTyping) {
        // Questo sarà gestito via WebSocket direttamente, non persiste
        log.trace("Typing indicator: user {} is typing={} in conversation {}", userId, isTyping, conversationId);
    }
}
