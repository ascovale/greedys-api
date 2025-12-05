package com.application.agency.controller;

import com.application.agency.persistence.model.user.AgencyUser;
import com.application.common.persistence.model.support.RequesterType;
import com.application.common.persistence.model.support.SupportTicket;
import com.application.common.persistence.model.support.SupportTicketMessage;
import com.application.common.persistence.model.support.TicketCategory;
import com.application.common.persistence.model.support.TicketPriority;
import com.application.common.persistence.model.support.TicketStatus;
import com.application.common.service.support.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agency Support Controller
 * Handles support ticket operations for agency users
 */
@RestController
@RequestMapping("/agency/support")
@RequiredArgsConstructor
@Slf4j
public class AgencySupportController {

    private final SupportTicketService supportTicketService;

    // ==================== TICKET CREATION ====================

    /**
     * Create a new support ticket
     */
    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> createTicket(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestBody CreateTicketRequest request) {
        log.info("Agency user {} creating support ticket: {}", agencyUser.getId(), request.subject());
        SupportTicket ticket = supportTicketService.createTicket(
                agencyUser.getId(),
                RequesterType.AGENCY,
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
     * Get all tickets for the agency user
     */
    @GetMapping("/tickets")
    public ResponseEntity<Page<SupportTicket>> getMyTickets(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Agency user {} fetching tickets", agencyUser.getId());
        Page<SupportTicket> tickets = supportTicketService.getTicketsByRequester(
                agencyUser.getId(), page, size);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a specific ticket by ID
     */
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<SupportTicket> getTicket(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(agencyUser.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Get ticket by number
     */
    @GetMapping("/tickets/number/{ticketNumber}")
    public ResponseEntity<SupportTicket> getTicketByNumber(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable String ticketNumber) {
        return supportTicketService.getTicketByNumber(ticketNumber)
                .filter(ticket -> ticket.getRequesterId().equals(agencyUser.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Get messages for a ticket
     */
    @GetMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<List<SupportTicketMessage>> getTicketMessages(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(agencyUser.getId()))
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
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long ticketId,
            @RequestBody AddMessageRequest request) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(agencyUser.getId()))
                .map(ticket -> {
                    log.info("Agency user {} adding message to ticket {}", agencyUser.getId(), ticketId);
                    SupportTicketMessage message = supportTicketService.addMessage(
                            ticketId, agencyUser.getId(), request.content(), 
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
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long ticketId,
            @RequestBody UpdateStatusRequest request) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(agencyUser.getId()))
                .filter(ticket -> isAgencyAllowedStatus(request.status()))
                .map(ticket -> {
                    log.info("Agency user {} updating ticket {} status to {}", 
                            agencyUser.getId(), ticketId, request.status());
                    SupportTicket updated = supportTicketService.updateStatus(
                            ticketId, request.status(), agencyUser.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.status(403).build());
    }

    /**
     * Mark BOT response as helpful
     */
    @PostMapping("/tickets/{ticketId}/bot-helpful")
    public ResponseEntity<Void> markBotHelpful(
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long ticketId,
            @RequestBody(required = false) BotHelpfulRequest request) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(agencyUser.getId()))
                .map(ticket -> {
                    log.info("Agency user {} marked BOT response helpful for ticket {}", 
                            agencyUser.getId(), ticketId);
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
            @AuthenticationPrincipal AgencyUser agencyUser,
            @PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .filter(ticket -> ticket.getRequesterId().equals(agencyUser.getId()))
                .map(ticket -> {
                    log.info("Agency user {} requesting human support for ticket {}", 
                            agencyUser.getId(), ticketId);
                    supportTicketService.requestHumanSupport(ticketId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(403).build());
    }

    // ==================== HELPER METHODS ====================

    private boolean isAgencyAllowedStatus(TicketStatus status) {
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

    public record AddMessageRequest(String content) {}

    public record UpdateStatusRequest(TicketStatus status) {}

    public record BotHelpfulRequest(Long faqId) {}
}
