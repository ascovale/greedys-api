package com.application.restaurant.controller;

import com.application.common.persistence.model.support.RequesterType;
import com.application.common.persistence.model.support.SupportTicket;
import com.application.common.persistence.model.support.SupportTicketMessage;
import com.application.common.persistence.model.support.TicketCategory;
import com.application.common.persistence.model.support.TicketPriority;
import com.application.common.persistence.model.support.TicketStatus;
import com.application.common.service.support.SupportTicketService;
import com.application.restaurant.persistence.model.user.RUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Restaurant Support Controller
 * Handles support ticket operations for restaurant users
 */
@RestController
@RequestMapping("/restaurant/support")
@RequiredArgsConstructor
@Slf4j
public class RestaurantSupportController {

    private final SupportTicketService supportTicketService;

    // ==================== TICKET CREATION ====================

    /**
     * Create a new support ticket
     */
    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> createTicket(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestBody CreateTicketRequest request) {
        log.info("Restaurant user {} creating support ticket: {}", restaurantUser.getId(), request.subject());
        SupportTicket ticket = supportTicketService.createTicket(
                restaurantUser.getId(),
                RequesterType.RESTAURANT,
                request.subject(),
                request.description(),
                request.category() != null ? request.category() : TicketCategory.OTHER,
                request.priority() != null ? request.priority() : TicketPriority.NORMAL,
                restaurantUser.getRestaurant() != null ? restaurantUser.getRestaurant().getId() : null,
                request.reservationId()
        );
        return ResponseEntity.ok(ticket);
    }

    // ==================== TICKET RETRIEVAL ====================

    /**
     * Get all tickets for the restaurant user
     */
    @GetMapping("/tickets")
    public ResponseEntity<Page<SupportTicket>> getMyTickets(
            @AuthenticationPrincipal RUser restaurantUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Restaurant user {} fetching tickets", restaurantUser.getId());
        Page<SupportTicket> tickets = supportTicketService.getTicketsByRequester(
                restaurantUser.getId(), page, size);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a specific ticket by ID
     */
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<SupportTicket> getTicket(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(restaurantUser.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Get ticket by number
     */
    @GetMapping("/tickets/number/{ticketNumber}")
    public ResponseEntity<SupportTicket> getTicketByNumber(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable String ticketNumber) {
        return supportTicketService.getTicketByNumber(ticketNumber)
                .filter(ticket -> ticket.getRequesterId().equals(restaurantUser.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Get messages for a ticket
     */
    @GetMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<List<SupportTicketMessage>> getTicketMessages(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(restaurantUser.getId()))
                .map(ticket -> {
                    List<SupportTicketMessage> messages = supportTicketService.getPublicMessages(ticketId);
                    return ResponseEntity.ok(messages);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== TICKET MESSAGING ====================

    /**
     * Add a message to a ticket
     */
    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<SupportTicketMessage> addMessage(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long ticketId,
            @RequestBody AddMessageRequest request) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(restaurantUser.getId()))
                .map(ticket -> {
                    log.info("Restaurant user {} adding message to ticket {}", restaurantUser.getId(), ticketId);
                    SupportTicketMessage message = supportTicketService.addMessage(
                            ticketId, restaurantUser.getId(), request.content(), 
                            false, false, false);
                    return ResponseEntity.ok(message);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== TICKET STATUS ====================

    /**
     * Update ticket status
     */
    @PutMapping("/tickets/{ticketId}/status")
    public ResponseEntity<SupportTicket> updateTicketStatus(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long ticketId,
            @RequestBody UpdateStatusRequest request) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(restaurantUser.getId()))
                .filter(ticket -> isRestaurantAllowedStatus(request.status()))
                .map(ticket -> {
                    log.info("Restaurant user {} updating ticket {} status to {}", 
                            restaurantUser.getId(), ticketId, request.status());
                    SupportTicket updated = supportTicketService.updateStatus(
                            ticketId, request.status(), restaurantUser.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Mark BOT response as helpful
     */
    @PostMapping("/tickets/{ticketId}/bot-helpful")
    public ResponseEntity<Void> markBotHelpful(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long ticketId,
            @RequestBody(required = false) BotHelpfulRequest request) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(restaurantUser.getId()))
                .map(ticket -> {
                    log.info("Restaurant user {} marked BOT response helpful for ticket {}", 
                            restaurantUser.getId(), ticketId);
                    supportTicketService.markBotResponseHelpful(ticketId, request != null ? request.faqId() : null);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Request human support
     */
    @PostMapping("/tickets/{ticketId}/request-human")
    public ResponseEntity<Void> requestHumanSupport(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(restaurantUser.getId()))
                .map(ticket -> {
                    log.info("Restaurant user {} requesting human support for ticket {}", 
                            restaurantUser.getId(), ticketId);
                    supportTicketService.requestHumanSupport(ticketId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== HELPER METHODS ====================

    private boolean isRestaurantAllowedStatus(TicketStatus status) {
        return status == TicketStatus.CLOSED || status == TicketStatus.OPEN;
    }

    // ==================== REQUEST DTOs ====================

    public record CreateTicketRequest(
            String subject,
            String description,
            TicketPriority priority,
            TicketCategory category,
            Long reservationId
    ) {}

    public record AddMessageRequest(String content) {}

    public record UpdateStatusRequest(TicketStatus status) {}

    public record BotHelpfulRequest(Long faqId) {}
}
