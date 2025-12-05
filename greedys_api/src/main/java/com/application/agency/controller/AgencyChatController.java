package com.application.agency.controller;

import com.application.agency.persistence.model.user.AgencyUser;
import com.application.common.persistence.model.chat.ChatConversation;
import com.application.common.persistence.model.chat.ChatMessage;
import com.application.common.persistence.model.chat.ChatParticipant;
import com.application.common.persistence.model.chat.MessageType;
import com.application.common.persistence.model.chat.ParticipantRole;
import com.application.common.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agency Chat Controller
 * Handles chat operations for agency users
 */
@RestController
@RequestMapping("/agency/chat")
@RequiredArgsConstructor
@Slf4j
public class AgencyChatController {

    private final ChatService chatService;

    // ==================== CONVERSATIONS ====================

    /**
     * Get all conversations for the agency user
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ChatConversation>> getConversations(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Agency user {} fetching conversations", agencyUser.getId());
        Page<ChatConversation> conversations = chatService.getUserConversations(agencyUser.getId(), page, size);
        return ResponseEntity.ok(conversations);
    }

    /**
     * Get a specific conversation
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ChatConversation> getConversation(@PathVariable Long conversationId) {
        return chatService.getConversation(conversationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a direct conversation
     */
    @PostMapping("/conversations/direct")
    public ResponseEntity<ChatConversation> createDirectConversation(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestBody CreateDirectConversationRequest request) {
        log.info("Agency user {} creating direct conversation with {}", agencyUser.getId(), request.userId());
        ChatConversation conversation = chatService.createDirectConversation(agencyUser.getId(), request.userId());
        return ResponseEntity.ok(conversation);
    }

    /**
     * Create a group conversation (for agency team)
     */
    @PostMapping("/conversations/group")
    public ResponseEntity<ChatConversation> createGroupConversation(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestBody CreateGroupConversationRequest request) {
        log.info("Agency user {} creating group '{}'", agencyUser.getId(), request.name());
        ChatConversation conversation = chatService.createGroupConversation(
                agencyUser.getId(), request.name(), request.memberIds());
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
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {
        log.debug("Agency user {} sending message to conversation {}", agencyUser.getId(), conversationId);
        MessageType messageType = request.messageType() != null ? request.messageType() : MessageType.TEXT;
        ChatMessage message = chatService.sendMessage(
                conversationId, agencyUser.getId(), request.content(), messageType);
        return ResponseEntity.ok(message);
    }

    /**
     * Reply to a message
     */
    @PostMapping("/conversations/{conversationId}/messages/{messageId}/reply")
    public ResponseEntity<ChatMessage> replyToMessage(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @RequestBody SendMessageRequest request) {
        ChatMessage message = chatService.sendReply(
                conversationId, agencyUser.getId(), request.content(), messageId);
        return ResponseEntity.ok(message);
    }

    /**
     * Delete a message
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long messageId) {
        log.info("Agency user {} deleting message {}", agencyUser.getId(), messageId);
        chatService.deleteMessage(messageId, agencyUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Search messages
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
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long conversationId,
            @RequestBody MarkReadRequest request) {
        chatService.markAsRead(conversationId, agencyUser.getId(), request.lastReadMessageId());
        return ResponseEntity.ok().build();
    }

    /**
     * Get unread count
     */
    @GetMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long conversationId) {
        Long count = chatService.countUnreadMessages(conversationId, agencyUser.getId());
        return ResponseEntity.ok(count);
    }

    // ==================== PARTICIPANTS ====================

    /**
     * Get participants
     */
    @GetMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<List<ChatParticipant>> getParticipants(@PathVariable Long conversationId) {
        List<ChatParticipant> participants = chatService.getActiveParticipants(conversationId);
        return ResponseEntity.ok(participants);
    }

    /**
     * Add participant
     */
    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<ChatParticipant> addParticipant(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long conversationId,
            @RequestBody AddParticipantRequest request) {
        return chatService.getConversation(conversationId)
                .map(conversation -> {
                    log.info("Agency user {} adding participant {} to conversation {}", 
                            agencyUser.getId(), request.userId(), conversationId);
                    ParticipantRole role = request.role() != null ? request.role() : ParticipantRole.MEMBER;
                    ChatParticipant participant = chatService.addParticipant(
                            conversation, request.userId(), role);
                    return ResponseEntity.ok(participant);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Leave conversation
     */
    @PostMapping("/conversations/{conversationId}/leave")
    public ResponseEntity<Void> leaveConversation(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long conversationId) {
        log.info("Agency user {} leaving conversation {}", agencyUser.getId(), conversationId);
        chatService.removeParticipant(conversationId, agencyUser.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== TYPING ====================

    @PostMapping("/conversations/{conversationId}/typing")
    public ResponseEntity<Void> sendTypingIndicator(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long conversationId,
            @RequestBody TypingIndicatorRequest request) {
        chatService.sendTypingIndicator(conversationId, agencyUser.getId(), request.isTyping());
        return ResponseEntity.ok().build();
    }

    // ==================== REQUEST DTOs ====================

    public record CreateDirectConversationRequest(Long userId) {}

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
