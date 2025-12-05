package com.application.common.persistence.model.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ SUPPORT TICKET ENTITY
 * 
 * Rappresenta un ticket di supporto/assistenza.
 * 
 * WORKFLOW:
 * OPEN → IN_PROGRESS → (WAITING_CUSTOMER ↔ IN_PROGRESS) → RESOLVED → CLOSED
 *                    ↓
 *               ESCALATED → IN_PROGRESS → ...
 * 
 * FEATURES:
 * - Numero ticket univoco (TKT-XXXX)
 * - Categorizzazione e priorità
 * - Assegnazione agenti
 * - Escalation
 * - Rating alla chiusura
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "support_ticket", indexes = {
    @Index(name = "idx_ticket_number", columnList = "ticket_number"),
    @Index(name = "idx_ticket_requester", columnList = "requester_id"),
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_assigned", columnList = "assigned_agent_id"),
    @Index(name = "idx_ticket_category", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Numero ticket univoco (TKT-XXXX)
     */
    @Column(name = "ticket_number", nullable = false, unique = true, length = 20)
    private String ticketNumber;

    /**
     * ID dell'utente che ha aperto il ticket (FK a abstract_user)
     */
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    /**
     * Tipo di richiedente
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "requester_type", nullable = false, length = 20)
    private RequesterType requesterType;

    /**
     * Oggetto del ticket
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /**
     * Descrizione iniziale del problema
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Categoria del ticket
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    @Builder.Default
    private TicketCategory category = TicketCategory.OTHER;

    /**
     * Priorità
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TicketPriority priority = TicketPriority.NORMAL;

    /**
     * Stato attuale
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    /**
     * ID dell'agente assegnato (FK a abstract_user)
     */
    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;

    /**
     * ID alias per compatibilità con DAO (alias per assignedAgentId)
     */
    @Column(name = "assigned_to_id")
    private Long assignedToId;

    /**
     * FK al ristorante (se il ticket è relativo a un ristorante specifico)
     */
    @Column(name = "restaurant_id")
    private Long restaurantId;

    /**
     * FK alla prenotazione (se il ticket è relativo a una prenotazione)
     */
    @Column(name = "reservation_id")
    private Long reservationId;

    /**
     * Data creazione
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Data ultimo aggiornamento
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Data assegnazione agente
     */
    @Column(name = "assigned_at")
    private Instant assignedAt;

    /**
     * Data risoluzione
     */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Data chiusura
     */
    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Data escalation
     */
    @Column(name = "escalated_at")
    private Instant escalatedAt;

    /**
     * Motivo dell'escalation
     */
    @Column(name = "escalation_reason", length = 500)
    private String escalationReason;

    /**
     * Livello di escalation (0 = nessuna, 1+ = escalato)
     */
    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    /**
     * Prima risposta (per SLA tracking)
     */
    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    /**
     * Rating finale del cliente (1-5)
     */
    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    /**
     * Commento sul rating
     */
    @Column(name = "satisfaction_comment", length = 500)
    private String satisfactionComment;

    /**
     * Risoluzione del ticket (descrizione soluzione)
     */
    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    /**
     * ID di chi ha risolto il ticket
     */
    @Column(name = "resolved_by_id")
    private Long resolvedById;

    /**
     * Se è stato risolto automaticamente dal BOT
     */
    @Column(name = "resolved_by_bot")
    @Builder.Default
    private Boolean resolvedByBot = false;

    /**
     * Se il ticket è stato gestito dal BOT
     */
    @Column(name = "bot_handled")
    @Builder.Default
    private Boolean botHandled = false;

    /**
     * Metadata aggiuntivi
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Messaggi del ticket
     */
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SupportTicketMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (ticketNumber == null) {
            ticketNumber = generateTicketNumber();
        }
        if (status == null) {
            status = TicketStatus.OPEN;
        }
        if (priority == null) {
            priority = TicketPriority.NORMAL;
        }
        if (category == null) {
            category = TicketCategory.OTHER;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (escalationLevel == null) {
            escalationLevel = 0;
        }
        if (resolvedByBot == null) {
            resolvedByBot = false;
        }
        if (botHandled == null) {
            botHandled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Genera un numero ticket univoco
     */
    private String generateTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Assegna il ticket a un agente
     */
    public void assignTo(Long agentId) {
        this.assignedAgentId = agentId;
        this.assignedAt = Instant.now();
        this.status = TicketStatus.IN_PROGRESS;
    }

    /**
     * Escala il ticket
     */
    public void escalate() {
        this.status = TicketStatus.ESCALATED;
        // L'agente precedente viene rimosso, sarà riassegnato
        this.assignedAgentId = null;
    }

    /**
     * Segna come in attesa di risposta cliente
     */
    public void waitingCustomer() {
        this.status = TicketStatus.WAITING_CUSTOMER;
    }

    /**
     * Risolvi il ticket
     */
    public void resolve() {
        this.status = TicketStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    /**
     * Chiudi il ticket
     */
    public void close() {
        this.status = TicketStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    /**
     * Riapri il ticket
     */
    public void reopen() {
        this.status = TicketStatus.OPEN;
        this.resolvedAt = null;
        this.closedAt = null;
    }

    /**
     * Registra la prima risposta
     */
    public void recordFirstResponse() {
        if (this.firstResponseAt == null) {
            this.firstResponseAt = Instant.now();
        }
    }

    /**
     * Imposta il rating di soddisfazione
     */
    public void rate(int rating, String comment) {
        this.satisfactionRating = Math.max(1, Math.min(5, rating));
        this.satisfactionComment = comment;
    }

    /**
     * Verifica se il ticket è aperto
     */
    public boolean isOpen() {
        return status != TicketStatus.CLOSED && status != TicketStatus.RESOLVED;
    }
}
