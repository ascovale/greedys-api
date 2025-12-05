package com.application.common.service.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.domain.event.EventType;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.dao.support.SupportFAQDAO;
import com.application.common.persistence.dao.support.SupportTicketDAO;
import com.application.common.persistence.dao.support.SupportTicketMessageDAO;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.support.RequesterType;
import com.application.common.persistence.model.support.SupportFAQ;
import com.application.common.persistence.model.support.SupportTicket;
import com.application.common.persistence.model.support.SupportTicketMessage;
import com.application.common.persistence.model.support.TicketCategory;
import com.application.common.persistence.model.support.TicketPriority;
import com.application.common.persistence.model.support.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ SUPPORT TICKET SERVICE
 * 
 * Service per la gestione dei ticket di supporto con integrazione BOT.
 * 
 * FLOW:
 * 1. Customer/Restaurant crea ticket
 * 2. BOT cerca FAQ corrispondenti
 * 3. Se BOT risolve → chiusura automatica
 * 4. Se BOT non risolve → escalation a staff
 * 5. Staff risponde e risolve
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupportTicketService {

    private final SupportTicketDAO ticketDAO;
    private final SupportTicketMessageDAO messageDAO;
    private final SupportFAQDAO faqDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final ObjectMapper objectMapper;

    // ==================== TICKET MANAGEMENT ====================

    /**
     * Crea un nuovo ticket di supporto
     */
    public SupportTicket createTicket(
            Long requesterId,
            RequesterType requesterType,
            String subject,
            String description,
            TicketCategory category,
            TicketPriority priority,
            Long restaurantId,
            Long reservationId
    ) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setRequesterId(requesterId);
        ticket.setRequesterType(requesterType);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setPriority(priority != null ? priority : TicketPriority.NORMAL);
        ticket.setRestaurantId(restaurantId);
        ticket.setReservationId(reservationId);
        ticket.setStatus(TicketStatus.OPEN);
        
        ticket = ticketDAO.save(ticket);
        
        // Aggiungi il primo messaggio (la descrizione)
        addMessage(ticket.getId(), requesterId, description, false, false, false);
        
        // Cerca FAQ per risposta automatica del BOT
        tryBotResponse(ticket);
        
        // Crea evento per notifica
        triggerTicketEvent(ticket, EventType.SUPPORT_TICKET_CREATED);
        
        log.info("✅ Creato ticket {} - {} da {} ({})", 
            ticket.getTicketNumber(), subject, requesterId, requesterType);
        
        return ticket;
    }

    /**
     * Genera numero ticket univoco
     */
    private String generateTicketNumber() {
        return "TKT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /**
     * Ottieni ticket per ID
     */
    @Transactional(readOnly = true)
    public Optional<SupportTicket> getTicket(Long ticketId) {
        return ticketDAO.findById(ticketId);
    }

    /**
     * Ottieni ticket per numero
     */
    @Transactional(readOnly = true)
    public Optional<SupportTicket> getTicketByNumber(String ticketNumber) {
        return ticketDAO.findByTicketNumber(ticketNumber);
    }

    /**
     * Ottieni ticket di un utente
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getTicketsByRequester(Long requesterId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ticketDAO.findByRequesterId(requesterId, pageable);
    }

    /**
     * Ottieni ticket aperti per staff
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getOpenTickets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ticketDAO.findOpenTickets(pageable);
    }

    /**
     * Ottieni ticket assegnati a uno staff member
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getTicketsAssignedTo(Long staffId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ticketDAO.findByAssignedToId(staffId, pageable);
    }

    // ==================== STATUS MANAGEMENT ====================

    /**
     * Aggiorna lo status del ticket
     */
    public SupportTicket updateStatus(Long ticketId, TicketStatus newStatus, Long changedById) {
        SupportTicket ticket = ticketDAO.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket non trovato: " + ticketId));
        
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now());
            ticket.setResolvedById(changedById);
        }
        
        ticket = ticketDAO.save(ticket);
        
        // Evento status change
        triggerStatusChangeEvent(ticket, oldStatus, newStatus, changedById);
        
        log.info("Ticket {} status: {} → {}", ticket.getTicketNumber(), oldStatus, newStatus);
        
        return ticket;
    }

    /**
     * Assegna ticket a staff member
     */
    public SupportTicket assignTicket(Long ticketId, Long staffId) {
        SupportTicket ticket = ticketDAO.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket non trovato: " + ticketId));
        
        ticket.setAssignedToId(staffId);
        ticket.setAssignedAt(Instant.now());
        
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        
        ticket = ticketDAO.save(ticket);
        
        log.info("Ticket {} assegnato a staff {}", ticket.getTicketNumber(), staffId);
        
        return ticket;
    }

    /**
     * Escalation del ticket
     */
    public SupportTicket escalateTicket(Long ticketId, String reason) {
        SupportTicket ticket = ticketDAO.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket non trovato: " + ticketId));
        
        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setEscalatedAt(Instant.now());
        ticket.setEscalationReason(reason);
        ticket.setEscalationLevel(ticket.getEscalationLevel() + 1);
        
        ticket = ticketDAO.save(ticket);
        
        log.warn("⚠️ Ticket {} escalated: {}", ticket.getTicketNumber(), reason);
        
        return ticket;
    }

    // ==================== MESSAGES ====================

    /**
     * Aggiungi messaggio a un ticket
     */
    public SupportTicketMessage addMessage(
            Long ticketId,
            Long senderId,
            String content,
            boolean isFromStaff,
            boolean isFromBot,
            boolean isInternal
    ) {
        SupportTicket ticket = ticketDAO.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket non trovato: " + ticketId));
        
        SupportTicketMessage message = new SupportTicketMessage();
        message.setTicket(ticket);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setIsFromStaff(isFromStaff);
        message.setIsFromBot(isFromBot);
        message.setIsInternal(isInternal);
        
        message = messageDAO.save(message);
        
        // Aggiorna stato ticket se necessario
        if (!isInternal && !isFromBot) {
            if (isFromStaff && ticket.getStatus() == TicketStatus.WAITING_CUSTOMER) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            } else if (!isFromStaff && ticket.getStatus() == TicketStatus.IN_PROGRESS) {
                ticket.setStatus(TicketStatus.WAITING_CUSTOMER);
            }
            ticketDAO.save(ticket);
        }
        
        // Notifica se non è messaggio interno
        if (!isInternal) {
            triggerMessageEvent(ticket, message);
        }
        
        return message;
    }

    /**
     * Ottieni messaggi di un ticket
     */
    @Transactional(readOnly = true)
    public List<SupportTicketMessage> getMessages(Long ticketId) {
        return messageDAO.findByTicketId(ticketId);
    }

    /**
     * Ottieni messaggi pubblici (non interni) di un ticket
     */
    @Transactional(readOnly = true)
    public List<SupportTicketMessage> getPublicMessages(Long ticketId) {
        return messageDAO.findPublicMessages(ticketId);
    }

    // ==================== BOT INTEGRATION ====================

    /**
     * Prova risposta automatica del BOT
     */
    private void tryBotResponse(SupportTicket ticket) {
        // Cerca FAQ rilevanti
        List<SupportFAQ> faqs = faqDAO.searchByKeywordsAndRequesterType(
            ticket.getSubject() + " " + ticket.getDescription(),
            ticket.getRequesterType()
        );
        
        if (!faqs.isEmpty()) {
            SupportFAQ bestMatch = faqs.get(0);
            
            // Aggiungi risposta BOT
            String botResponse = "🤖 **Risposta automatica**\n\n" +
                "Ho trovato questa informazione che potrebbe aiutarti:\n\n" +
                "**" + bestMatch.getQuestion() + "**\n\n" +
                bestMatch.getAnswer() + "\n\n" +
                "_Questa risposta è stata utile? Se no, un operatore ti risponderà a breve._";
            
            addMessage(ticket.getId(), null, botResponse, false, true, false);
            
            // Incrementa contatore utilizzo FAQ
            faqDAO.incrementUsageCount(bestMatch.getId());
            
            // Aggiorna stato a WAITING_BOT
            ticket.setStatus(TicketStatus.WAITING_BOT);
            ticketDAO.save(ticket);
            
            log.info("🤖 BOT risposta automatica per ticket {} usando FAQ {}", 
                ticket.getTicketNumber(), bestMatch.getId());
        }
    }

    /**
     * Customer conferma che risposta BOT è stata utile
     */
    public void markBotResponseHelpful(Long ticketId, Long faqId) {
        SupportTicket ticket = ticketDAO.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket non trovato"));
        
        if (faqId != null) {
            faqDAO.incrementHelpfulCount(faqId);
        }
        
        // Chiudi ticket automaticamente
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(Instant.now());
        ticket.setResolution("Risolto automaticamente tramite FAQ");
        ticket.setResolvedByBot(true);
        ticketDAO.save(ticket);
        
        log.info("✅ Ticket {} risolto da BOT", ticket.getTicketNumber());
    }

    /**
     * Customer richiede supporto umano
     */
    public void requestHumanSupport(Long ticketId) {
        SupportTicket ticket = ticketDAO.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket non trovato"));
        
        ticket.setStatus(TicketStatus.OPEN);
        ticketDAO.save(ticket);
        
        // Aggiungi nota sistema
        addMessage(ticketId, null, "👤 Il cliente ha richiesto supporto da un operatore.", false, true, false);
        
        log.info("Ticket {} richiesto supporto umano", ticket.getTicketNumber());
    }

    // ==================== NOTIFICATIONS ====================

    private void triggerTicketEvent(SupportTicket ticket, EventType eventType) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "ticketId", ticket.getId(),
                "ticketNumber", ticket.getTicketNumber(),
                "requesterId", ticket.getRequesterId(),
                "requesterType", ticket.getRequesterType().name(),
                "subject", ticket.getSubject(),
                "category", ticket.getCategory().name(),
                "priority", ticket.getPriority().name()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("support_ticket_" + ticket.getId() + "_" + System.currentTimeMillis())
                .eventType(eventType.name())
                .aggregateType("SupportTicket")
                .aggregateId(ticket.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento ticket: {}", e.getMessage());
        }
    }

    private void triggerStatusChangeEvent(SupportTicket ticket, TicketStatus oldStatus, TicketStatus newStatus, Long changedById) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "ticketId", ticket.getId(),
                "oldStatus", oldStatus.name(),
                "newStatus", newStatus.name(),
                "changedById", changedById != null ? changedById : 0
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("support_status_" + ticket.getId() + "_" + System.currentTimeMillis())
                .eventType(EventType.SUPPORT_TICKET_STATUS_CHANGED.name())
                .aggregateType("SupportTicket")
                .aggregateId(ticket.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento status change: {}", e.getMessage());
        }
    }

    private void triggerMessageEvent(SupportTicket ticket, SupportTicketMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "ticketId", ticket.getId(),
                "messageId", message.getId(),
                "senderId", message.getSenderId() != null ? message.getSenderId() : 0,
                "isFromBot", message.getIsFromBot(),
                "isFromStaff", message.getIsFromStaff()
            ));
            
            EventOutbox outbox = EventOutbox.builder()
                .eventId("support_msg_" + message.getId())
                .eventType(EventType.SUPPORT_TICKET_MESSAGE.name())
                .aggregateType("SupportTicketMessage")
                .aggregateId(message.getId())
                .payload(payload)
                .build();
            
            eventOutboxDAO.save(outbox);
        } catch (Exception e) {
            log.error("Errore creazione evento messaggio: {}", e.getMessage());
        }
    }
}
