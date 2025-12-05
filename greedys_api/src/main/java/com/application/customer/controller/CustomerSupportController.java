package com.application.customer.controller;

import com.application.common.persistence.model.support.RequesterType;
import com.application.common.persistence.model.support.SupportTicket;
import com.application.common.persistence.model.support.SupportTicketMessage;
import com.application.common.persistence.model.support.TicketCategory;
import com.application.common.persistence.model.support.TicketPriority;
import com.application.common.persistence.model.support.TicketStatus;
import com.application.common.service.support.SupportTicketService;
import com.application.customer.persistence.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer Support Controller
 * Handles support ticket operations for customers
 */
@RestController
@RequestMapping("/customer/support")
@RequiredArgsConstructor
@Slf4j
public class CustomerSupportController {

    private final SupportTicketService supportTicketService;

    // ==================== TICKET CREATION ====================

    /**
     * Create a new support ticket
     */
    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> createTicket(
            @AuthenticationPrincipal Customer customer,
            @RequestBody CreateTicketRequest request) {
        log.info("Customer {} creating support ticket: {}", customer.getId(), request.subject());
        SupportTicket ticket = supportTicketService.createTicket(
                customer.getId(),
                RequesterType.CUSTOMER,
                request.subject(),
                request.description(),
                request.category() != null ? request.category() : TicketCategory.OTHER,
                request.priority() != null ? request.priority() : TicketPriority.NORMAL,
                request.restaurantId(),
                request.reservationId()
        );
        return ResponseEntity.ok(ticket);
    }

    // ==================== TICKET RETRIEVAL ====================

    /**
     * Get all tickets for the authenticated customer
     */
    @GetMapping("/tickets")
    public ResponseEntity<Page<SupportTicket>> getMyTickets(
            @AuthenticationPrincipal Customer customer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Customer {} fetching tickets", customer.getId());
        Page<SupportTicket> tickets = supportTicketService.getTicketsByRequester(
                customer.getId(), page, size);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a specific ticket by ID
     */
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<SupportTicket> getTicket(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(customer.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Get ticket by number
     */
    @GetMapping("/tickets/number/{ticketNumber}")
    public ResponseEntity<SupportTicket> getTicketByNumber(
            @AuthenticationPrincipal Customer customer,
            @PathVariable String ticketNumber) {
        return supportTicketService.getTicketByNumber(ticketNumber)
                .filter(ticket -> ticket.getRequesterId().equals(customer.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Get messages for a ticket
     */
    @GetMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<List<SupportTicketMessage>> getTicketMessages(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long ticketId) {
        // Verify ownership first
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(customer.getId()))
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
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long ticketId,
            @RequestBody AddMessageRequest request) {
        // Verify ownership first
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(customer.getId()))
                .map(ticket -> {
                    log.info("Customer {} adding message to ticket {}", customer.getId(), ticketId);
                    SupportTicketMessage message = supportTicketService.addMessage(
                            ticketId, customer.getId(), request.content(), 
                            false, false, false);  // isFromStaff, isFromBot, isInternal all false for customer
                    return ResponseEntity.ok(message);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== TICKET STATUS ====================

    /**
     * Update ticket status (customer can only update to specific states)
     */
    @PutMapping("/tickets/{ticketId}/status")
    public ResponseEntity<SupportTicket> updateTicketStatus(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long ticketId,
            @RequestBody UpdateStatusRequest request) {
        // Verify ownership
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(customer.getId()))
                .filter(ticket -> isCustomerAllowedStatus(request.status()))
                .map(ticket -> {
                    log.info("Customer {} updating ticket {} status to {}", 
                            customer.getId(), ticketId, request.status());
                    SupportTicket updated = supportTicketService.updateStatus(
                            ticketId, request.status(), customer.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Customer marks BOT response as helpful (auto-resolves ticket)
     */
    @PostMapping("/tickets/{ticketId}/bot-helpful")
    public ResponseEntity<Void> markBotHelpful(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long ticketId,
            @RequestBody(required = false) BotHelpfulRequest request) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(customer.getId()))
                .map(ticket -> {
                    log.info("Customer {} marked BOT response helpful for ticket {}", customer.getId(), ticketId);
                    supportTicketService.markBotResponseHelpful(ticketId, request != null ? request.faqId() : null);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Customer requests human support (if BOT wasn't helpful)
     */
    @PostMapping("/tickets/{ticketId}/request-human")
    public ResponseEntity<Void> requestHumanSupport(
            @AuthenticationPrincipal Customer customer,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(customer.getId()))
                .map(ticket -> {
                    log.info("Customer {} requesting human support for ticket {}", customer.getId(), ticketId);
                    supportTicketService.requestHumanSupport(ticketId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Check if customer is allowed to set this status
     */
    private boolean isCustomerAllowedStatus(TicketStatus status) {
        // Customers can only close/reopen their tickets
        return status == TicketStatus.CLOSED || status == TicketStatus.OPEN;
    }

    // ==================== REQUEST DTOs ====================

    public record CreateTicketRequest(
            String subject,
            String description,
            TicketPriority priority,
            TicketCategory category,
            Long restaurantId,
            Long reservationId
    ) {}

    public record AddMessageRequest(
            String content
    ) {}

    public record UpdateStatusRequest(
            TicketStatus status
    ) {}

    public record BotHelpfulRequest(
            Long faqId
    ) {}
}
