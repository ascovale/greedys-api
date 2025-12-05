package com.application.customer.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.chat.ChatConversation;
import com.application.common.persistence.model.chat.ChatMessage;
import com.application.common.persistence.model.chat.ChatParticipant;
import com.application.common.persistence.model.chat.MessageType;
import com.application.common.service.chat.ChatService;
import com.application.customer.persistence.model.Customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 💬 CUSTOMER CHAT CONTROLLER
 * 
 * REST API per la gestione chat lato Customer.
 * - Chat dirette con altri utenti
 * - Chat legate a prenotazioni
 * - Chat di gruppo
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@RestController
@RequestMapping("/customer/chat")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer Chat", description = "APIs per chat lato customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerChatController extends BaseController {

    private final ChatService chatService;

    // ==================== CONVERSATIONS ====================

    @Operation(summary = "Crea chat prenotazione", description = "Crea una chat collegata alla prenotazione")
    @CreateApiResponses
    @PostMapping("/conversations/reservation")
    public ResponseEntity<ChatConversation> createReservationChat(
            @RequestBody CreateReservationChatDTO dto,
            @AuthenticationPrincipal Customer customer) {
        return executeCreate("createReservationChat", () -> 
            chatService.createReservationConversation(
                dto.getReservationId(), 
                customer.getId(), 
                dto.getRestaurantId()
            )
        );
    }

    @Operation(summary = "Lista conversazioni", description = "Lista delle conversazioni del customer")
    @ReadApiResponses
    @GetMapping("/conversations")
    public ResponseEntity<Page<ChatConversation>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Customer customer) {
        return executePaginated("getConversations", () -> 
            chatService.getUserConversations(customer.getId(), page, size)
        );
    }

    @Operation(summary = "Dettaglio conversazione", description = "Dettaglio di una conversazione")
    @ReadApiResponses
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ChatConversation> getConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Customer customer) {
        // TODO: Verificare che il customer sia partecipante
        return execute("getConversation", () -> 
            chatService.getConversation(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversazione non trovata"))
        );
    }

    // ==================== MESSAGES ====================

    @Operation(summary = "Lista messaggi", description = "Messaggi di una conversazione")
    @ReadApiResponses
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<ChatMessage>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Customer customer) {
        return executePaginated("getMessages", () -> 
            chatService.getMessages(conversationId, page, size)
        );
    }

    @Operation(summary = "Invia messaggio", description = "Invia un messaggio nella conversazione")
    @CreateApiResponses
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageDTO dto,
            @AuthenticationPrincipal Customer customer) {
        return executeCreate("sendMessage", () -> 
            chatService.sendMessage(
                conversationId, 
                customer.getId(), 
                dto.getContent(), 
                dto.getMessageType() != null ? dto.getMessageType() : MessageType.TEXT
            )
        );
    }

    @Operation(summary = "Elimina messaggio", description = "Elimina un proprio messaggio")
    @ReadApiResponses
    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal Customer customer) {
        return executeVoid("deleteMessage", () -> 
            chatService.deleteMessage(messageId, customer.getId())
        );
    }

    // ==================== READ STATUS ====================

    @Operation(summary = "Marca come letto", description = "Marca i messaggi come letti")
    @ReadApiResponses
    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long conversationId,
            @RequestBody MarkReadDTO dto,
            @AuthenticationPrincipal Customer customer) {
        return executeVoid("markAsRead", () -> 
            chatService.markAsRead(conversationId, customer.getId(), dto.getLastReadMessageId())
        );
    }

    @Operation(summary = "Conta non letti", description = "Conta messaggi non letti")
    @ReadApiResponses
    @GetMapping("/conversations/{conversationId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Customer customer) {
        return execute("getUnreadCount", () -> 
            chatService.countUnreadMessages(conversationId, customer.getId())
        );
    }

    // ==================== PARTICIPANTS ====================

    @Operation(summary = "Lista partecipanti", description = "Partecipanti attivi della conversazione")
    @ReadApiResponses
    @GetMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<List<ChatParticipant>> getParticipants(@PathVariable Long conversationId) {
        return executeList("getParticipants", () -> 
            chatService.getActiveParticipants(conversationId)
        );
    }

    @Operation(summary = "Lascia conversazione", description = "Esci dalla conversazione")
    @ReadApiResponses
    @DeleteMapping("/conversations/{conversationId}/leave")
    public ResponseEntity<Void> leaveConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Customer customer) {
        return executeVoid("leaveConversation", () -> 
            chatService.removeParticipant(conversationId, customer.getId())
        );
    }

    // ==================== DTOs ====================

    @Data
    public static class CreateReservationChatDTO {
        private Long reservationId;
        private Long restaurantId;
    }

    @Data
    public static class SendMessageDTO {
        private String content;
        private MessageType messageType;
    }

    @Data
    public static class MarkReadDTO {
        private Long lastReadMessageId;
    }
}
