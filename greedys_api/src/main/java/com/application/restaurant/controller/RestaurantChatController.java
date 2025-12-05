package com.application.restaurant.controller;

import com.application.common.persistence.model.chat.ChatConversation;
import com.application.common.persistence.model.chat.ChatMessage;
import com.application.common.persistence.model.chat.ChatParticipant;
import com.application.common.persistence.model.chat.MessageType;
import com.application.common.persistence.model.chat.ParticipantRole;
import com.application.common.service.chat.ChatService;
import com.application.restaurant.persistence.model.user.RUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Restaurant Chat Controller
 * Handles chat operations for restaurant users
 */
@RestController
@RequestMapping("/restaurant/chat")
@RequiredArgsConstructor
@Slf4j
public class RestaurantChatController {

    private final ChatService chatService;

    // ==================== CONVERSATIONS ====================

    /**
     * Get all conversations for the restaurant user
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ChatConversation>> getConversations(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Restaurant user {} fetching conversations", restaurantUser.getId());
        Page<ChatConversation> conversations = chatService.getUserConversations(
                restaurantUser.getId(), page, size);
        return ResponseEntity.ok(conversations);
    }

    /**
     * Get a specific conversation
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ChatConversation> getConversation(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId) {
        return chatService.getConversation(conversationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a direct conversation with a customer
     */
    @PostMapping("/conversations/direct")
    public ResponseEntity<ChatConversation> createDirectConversation(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestBody CreateDirectConversationRequest request) {
        log.info("Restaurant user {} creating direct conversation with customer {}", 
                restaurantUser.getId(), request.customerId());
        ChatConversation conversation = chatService.createDirectConversation(
                restaurantUser.getId(), request.customerId());
        return ResponseEntity.ok(conversation);
    }

    /**
     * Create a group conversation (e.g., for staff communication)
     */
    @PostMapping("/conversations/group")
    public ResponseEntity<ChatConversation> createGroupConversation(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestBody CreateGroupConversationRequest request) {
        log.info("Restaurant user {} creating group '{}'", restaurantUser.getId(), request.name());
        ChatConversation conversation = chatService.createGroupConversation(
                restaurantUser.getId(), request.name(), request.memberIds());
        return ResponseEntity.ok(conversation);
    }

    // ==================== MESSAGES ====================

    /**
     * Get messages in a conversation
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<ChatMessage>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<ChatMessage> messages = chatService.getMessages(conversationId, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get messages after a specific message (for sync)
     */
    @GetMapping("/conversations/{conversationId}/messages/after/{messageId}")
    public ResponseEntity<List<ChatMessage>> getMessagesAfter(
            @PathVariable Long conversationId,
            @PathVariable Long messageId) {
        List<ChatMessage> messages = chatService.getMessagesAfter(conversationId, messageId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Send a message
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessage> sendMessage(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {
        log.debug("Restaurant user {} sending message to conversation {}", 
                restaurantUser.getId(), conversationId);
        MessageType messageType = request.messageType() != null ? request.messageType() : MessageType.TEXT;
        ChatMessage message = chatService.sendMessage(
                conversationId, restaurantUser.getId(), request.content(), messageType);
        return ResponseEntity.ok(message);
    }

    /**
     * Reply to a message
     */
    @PostMapping("/conversations/{conversationId}/messages/{messageId}/reply")
    public ResponseEntity<ChatMessage> replyToMessage(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @RequestBody SendMessageRequest request) {
        ChatMessage message = chatService.sendReply(
                conversationId, restaurantUser.getId(), request.content(), messageId);
        return ResponseEntity.ok(message);
    }

    /**
     * Delete a message
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long messageId) {
        log.info("Restaurant user {} deleting message {}", restaurantUser.getId(), messageId);
        chatService.deleteMessage(messageId, restaurantUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Search messages in a conversation
     */
    @GetMapping("/conversations/{conversationId}/messages/search")
    public ResponseEntity<Page<ChatMessage>> searchMessages(
            @PathVariable Long conversationId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ChatMessage> messages = chatService.searchMessages(conversationId, query, page, size);
        return ResponseEntity.ok(messages);
    }

    // ==================== READ RECEIPTS ====================

    /**
     * Mark messages as read
     */
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId,
            @RequestBody MarkReadRequest request) {
        chatService.markAsRead(conversationId, restaurantUser.getId(), request.lastReadMessageId());
        return ResponseEntity.ok().build();
    }

    /**
     * Get unread count for a conversation
     */
    @GetMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId) {
        Long count = chatService.countUnreadMessages(conversationId, restaurantUser.getId());
        return ResponseEntity.ok(count);
    }

    // ==================== PARTICIPANTS ====================

    /**
     * Get active participants in a conversation
     */
    @GetMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<List<ChatParticipant>> getParticipants(@PathVariable Long conversationId) {
        List<ChatParticipant> participants = chatService.getActiveParticipants(conversationId);
        return ResponseEntity.ok(participants);
    }

    /**
     * Add participant to a group conversation
     */
    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<ChatParticipant> addParticipant(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId,
            @RequestBody AddParticipantRequest request) {
        return chatService.getConversation(conversationId)
                .map(conversation -> {
                    log.info("Restaurant user {} adding participant {} to conversation {}", 
                            restaurantUser.getId(), request.userId(), conversationId);
                    ParticipantRole role = request.role() != null ? request.role() : ParticipantRole.MEMBER;
                    ChatParticipant participant = chatService.addParticipant(
                            conversation, request.userId(), role);
                    return ResponseEntity.ok(participant);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Remove participant from a conversation
     */
    @DeleteMapping("/conversations/{conversationId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId,
            @PathVariable Long userId) {
        log.info("Restaurant user {} removing participant {} from conversation {}", 
                restaurantUser.getId(), userId, conversationId);
        chatService.removeParticipant(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Leave a conversation
     */
    @PostMapping("/conversations/{conversationId}/leave")
    public ResponseEntity<Void> leaveConversation(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId) {
        log.info("Restaurant user {} leaving conversation {}", restaurantUser.getId(), conversationId);
        chatService.removeParticipant(conversationId, restaurantUser.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== TYPING INDICATOR ====================

    /**
     * Send typing indicator
     */
    @PostMapping("/conversations/{conversationId}/typing")
    public ResponseEntity<Void> sendTypingIndicator(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long conversationId,
            @RequestBody TypingIndicatorRequest request) {
        chatService.sendTypingIndicator(conversationId, restaurantUser.getId(), request.isTyping());
        return ResponseEntity.ok().build();
    }

    // ==================== REQUEST DTOs ====================

    public record CreateDirectConversationRequest(Long customerId) {}

    public record CreateGroupConversationRequest(
            String name,
            List<Long> memberIds
    ) {}

    public record SendMessageRequest(
            String content,
            MessageType messageType
    ) {}

    public record MarkReadRequest(Long lastReadMessageId) {}

    public record AddParticipantRequest(
            Long userId,
            ParticipantRole role
    ) {}

    public record TypingIndicatorRequest(boolean isTyping) {}
}
