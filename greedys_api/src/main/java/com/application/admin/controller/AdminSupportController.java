package com.application.admin.controller;

import com.application.admin.persistence.model.Admin;
import com.application.common.persistence.model.support.SupportTicket;
import com.application.common.persistence.model.support.SupportTicketMessage;
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
 * Admin Support Controller
 * Handles support ticket operations for admin/staff - full management access
 */
@RestController
@RequestMapping("/admin/support")
@RequiredArgsConstructor
@Slf4j
public class AdminSupportController {

    private final SupportTicketService supportTicketService;

    // ==================== TICKET RETRIEVAL (FULL ACCESS) ====================

    /**
     * Get all open tickets (for staff dashboard)
     */
    @GetMapping("/tickets/open")
    public ResponseEntity<Page<SupportTicket>> getOpenTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SupportTicket> tickets = supportTicketService.getOpenTickets(page, size);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get tickets assigned to me
     */
    @GetMapping("/tickets/assigned")
    public ResponseEntity<Page<SupportTicket>> getAssignedTickets(
            @AuthenticationPrincipal Admin admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SupportTicket> tickets = supportTicketService.getTicketsAssignedTo(admin.getId(), page, size);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a specific ticket by ID (full access)
     */
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<SupportTicket> getTicket(@PathVariable Long ticketId) {
        return supportTicketService.getTicket(ticketId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get ticket by number
     */
    @GetMapping("/tickets/number/{ticketNumber}")
    public ResponseEntity<SupportTicket> getTicketByNumber(@PathVariable String ticketNumber) {
        return supportTicketService.getTicketByNumber(ticketNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tickets by requester ID
     */
    @GetMapping("/tickets/requester/{requesterId}")
    public ResponseEntity<Page<SupportTicket>> getTicketsByRequester(
            @PathVariable Long requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SupportTicket> tickets = supportTicketService.getTicketsByRequester(requesterId, page, size);
        return ResponseEntity.ok(tickets);
    }

    // ==================== TICKET MESSAGES ====================

    /**
     * Get all messages for a ticket (including internal notes)
     */
    @GetMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<List<SupportTicketMessage>> getAllMessages(@PathVariable Long ticketId) {
        List<SupportTicketMessage> messages = supportTicketService.getMessages(ticketId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get public messages only
     */
    @GetMapping("/tickets/{ticketId}/messages/public")
    public ResponseEntity<List<SupportTicketMessage>> getPublicMessages(@PathVariable Long ticketId) {
        List<SupportTicketMessage> messages = supportTicketService.getPublicMessages(ticketId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Add a staff message to a ticket
     */
    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<SupportTicketMessage> addMessage(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId,
            @RequestBody AddMessageRequest request) {
        log.info("Admin {} adding message to ticket {}", admin.getId(), ticketId);
        SupportTicketMessage message = supportTicketService.addMessage(
                ticketId, 
                admin.getId(), 
                request.content(),
                true,  // isFromStaff
                false, // isFromBot
                request.isInternal() != null && request.isInternal()  // isInternal
        );
        return ResponseEntity.ok(message);
    }

    // ==================== TICKET ASSIGNMENT ====================

    /**
     * Assign a ticket to a staff member
     */
    @PostMapping("/tickets/{ticketId}/assign")
    public ResponseEntity<SupportTicket> assignTicket(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId,
            @RequestBody(required = false) AssignTicketRequest request) {
        Long assigneeId = (request != null && request.staffId() != null) ? request.staffId() : admin.getId();
        log.info("Admin {} assigning ticket {} to {}", admin.getId(), ticketId, assigneeId);
        SupportTicket ticket = supportTicketService.assignTicket(ticketId, assigneeId);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Self-assign a ticket
     */
    @PostMapping("/tickets/{ticketId}/assign/me")
    public ResponseEntity<SupportTicket> selfAssignTicket(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId) {
        log.info("Admin {} self-assigning ticket {}", admin.getId(), ticketId);
        SupportTicket ticket = supportTicketService.assignTicket(ticketId, admin.getId());
        return ResponseEntity.ok(ticket);
    }

    // ==================== TICKET STATUS ====================

    /**
     * Update ticket status
     */
    @PutMapping("/tickets/{ticketId}/status")
    public ResponseEntity<SupportTicket> updateStatus(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId,
            @RequestBody UpdateStatusRequest request) {
        log.info("Admin {} updating ticket {} status to {}", admin.getId(), ticketId, request.status());
        SupportTicket ticket = supportTicketService.updateStatus(ticketId, request.status(), admin.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Escalate a ticket
     */
    @PostMapping("/tickets/{ticketId}/escalate")
    public ResponseEntity<SupportTicket> escalateTicket(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId,
            @RequestBody EscalateRequest request) {
        log.warn("Admin {} escalating ticket {} - Reason: {}", admin.getId(), ticketId, request.reason());
        SupportTicket ticket = supportTicketService.escalateTicket(ticketId, request.reason());
        return ResponseEntity.ok(ticket);
    }

    // ==================== QUICK ACTIONS ====================

    /**
     * Resolve a ticket
     */
    @PostMapping("/tickets/{ticketId}/resolve")
    public ResponseEntity<SupportTicket> resolveTicket(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId) {
        log.info("Admin {} resolving ticket {}", admin.getId(), ticketId);
        SupportTicket ticket = supportTicketService.updateStatus(ticketId, TicketStatus.RESOLVED, admin.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Close a ticket
     */
    @PostMapping("/tickets/{ticketId}/close")
    public ResponseEntity<SupportTicket> closeTicket(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId) {
        log.info("Admin {} closing ticket {}", admin.getId(), ticketId);
        SupportTicket ticket = supportTicketService.updateStatus(ticketId, TicketStatus.CLOSED, admin.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Reopen a ticket
     */
    @PostMapping("/tickets/{ticketId}/reopen")
    public ResponseEntity<SupportTicket> reopenTicket(
            @AuthenticationPrincipal Admin admin,
            @PathVariable Long ticketId) {
        log.info("Admin {} reopening ticket {}", admin.getId(), ticketId);
        SupportTicket ticket = supportTicketService.updateStatus(ticketId, TicketStatus.OPEN, admin.getId());
        return ResponseEntity.ok(ticket);
    }

    // ==================== REQUEST DTOs ====================

    public record AddMessageRequest(
            String content,
            Boolean isInternal
    ) {}

    public record AssignTicketRequest(Long staffId) {}

    public record UpdateStatusRequest(TicketStatus status) {}

    public record EscalateRequest(String reason) {}
}
