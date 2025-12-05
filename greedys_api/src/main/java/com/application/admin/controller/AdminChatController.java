package com.application.admin.controller;

import com.application.admin.persistence.model.Admin;
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
 * Admin Chat Controller
 * Handles chat operations for admin users - full access to all conversations
 */
@RestController
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
@Slf4j
public class AdminChatController {

    private final ChatService chatService;

    // ==================== CONVERSATIONS ====================

    /**
     * Get all conversations for the admin
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ChatConversation>> getConversations(
            @AuthenticationPrincipal Admin admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Admin {} fetching conversations", admin.getId());
        Page<ChatConversation> conversations = chatService.getUserConversations(admin.getId(), page, size);
        return ResponseEntity.ok(conversations);
    }

    /**
     * Get any conversation by ID (admin access)
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ChatConversation> getConversation(@PathVariable Long conversationId) {
        return chatService.getConversation(conversationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a direct conversation with any user
     */
    @PostMapping("/conversations/direct")
    public ResponseEntity<ChatConversation> createDirectConversation(
            @AuthenticationPrincipal Admin admin,
            @RequestBody CreateDirectConversationRequest request) {
        log.info("Admin {} creating direct conversation with user {}", admin.getId(), request.userId());
        ChatConversation conversation = chatService.createDirectConversation(admin.getId(), request.userId());
        return ResponseEntity.ok(conversation);
    }

    /**
     * Create a group conversation (support team, etc.)
     */
    @PostMapping("/conversations/group")
    public ResponseEntity<ChatConversation> createGroupConversation(
            @AuthenticationPrincipal Admin admin,
            @RequestBody CreateGroupConversationRequest request) {
        log.info("Admin {} creating group '{}'", admin.getId(), request.name());
        ChatConversation conversation = chatService.createGroupConversation(
                admin.getId(), request.name(), request.memberIds());
        return ResponseEntity.ok(conversation);
    }

    // ==================== MESSAGES ====================

    /**
     * Get messages in any conversation (admin access)
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
     * Send a message
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessage> sendMessage(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {
        log.debug("Admin {} sending message to conversation {}", admin.getId(), conversationId);
        MessageType messageType = request.messageType() != null ? request.messageType() : MessageType.TEXT;
        ChatMessage message = chatService.sendMessage(
                conversationId, admin.getId(), request.content(), messageType);
        return ResponseEntity.ok(message);
    }

    /**
     * Delete any message (admin moderation)
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long messageId) {
        log.info("Admin {} deleting message {} (moderation)", admin.getId(), messageId);
        // Admin can delete any message - in real implementation would need to handle this differently
        chatService.deleteMessage(messageId, admin.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Search messages across any conversation
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
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long conversationId,
            @RequestBody MarkReadRequest request) {
        chatService.markAsRead(conversationId, admin.getId(), request.lastReadMessageId());
        return ResponseEntity.ok().build();
    }

    /**
     * Get unread count
     */
    @GetMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long conversationId) {
        Long count = chatService.countUnreadMessages(conversationId, admin.getId());
        return ResponseEntity.ok(count);
    }

    // ==================== PARTICIPANTS ====================

    /**
     * Get participants in any conversation
     */
    @GetMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<List<ChatParticipant>> getParticipants(@PathVariable Long conversationId) {
        List<ChatParticipant> participants = chatService.getActiveParticipants(conversationId);
        return ResponseEntity.ok(participants);
    }

    /**
     * Add participant to any conversation (admin privilege)
     */
    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<ChatParticipant> addParticipant(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long conversationId,
            @RequestBody AddParticipantRequest request) {
        return chatService.getConversation(conversationId)
                .map(conversation -> {
                    log.info("Admin {} adding participant {} to conversation {}", 
                            admin.getId(), request.userId(), conversationId);
                    ParticipantRole role = request.role() != null ? request.role() : ParticipantRole.MEMBER;
                    ChatParticipant participant = chatService.addParticipant(
                            conversation, request.userId(), role);
                    return ResponseEntity.ok(participant);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Remove participant from any conversation (admin moderation)
     */
    @DeleteMapping("/conversations/{conversationId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long conversationId,
            @PathVariable Long userId) {
        log.info("Admin {} removing participant {} from conversation {}", admin.getId(), userId, conversationId);
        chatService.removeParticipant(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== TYPING ====================

    @PostMapping("/conversations/{conversationId}/typing")
    public ResponseEntity<Void> sendTypingIndicator(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long conversationId,
            @RequestBody TypingIndicatorRequest request) {
        chatService.sendTypingIndicator(conversationId, admin.getId(), request.isTyping());
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
