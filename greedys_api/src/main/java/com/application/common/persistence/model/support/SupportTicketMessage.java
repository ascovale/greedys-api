package com.application.common.persistence.model.support;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ SUPPORT TICKET MESSAGE ENTITY
 * 
 * Rappresenta un messaggio in un ticket di supporto.
 * 
 * FEATURES:
 * - Messaggi da cliente o agente
 * - Note interne (visibili solo ad agenti)
 * - Risposte automatiche del bot
 * - Tracking first response
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "support_ticket_message", indexes = {
    @Index(name = "idx_ticket_msg_ticket", columnList = "ticket_id"),
    @Index(name = "idx_ticket_msg_sender", columnList = "sender_id"),
    @Index(name = "idx_ticket_msg_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ticket di appartenenza
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    /**
     * ID del mittente (FK a abstract_user)
     * Null per messaggi di sistema/bot
     */
    @Column(name = "sender_id")
    private Long senderId;

    /**
     * Contenuto del messaggio
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Se è una nota interna (visibile solo ad agenti/admin)
     */
    @Column(name = "is_internal_note")
    @Builder.Default
    private Boolean isInternalNote = false;

    /**
     * Alias per compatibilità - nota interna
     */
    @Column(name = "is_internal")
    @Builder.Default
    private Boolean isInternal = false;

    /**
     * Se è una risposta del bot
     */
    @Column(name = "is_bot_response")
    @Builder.Default
    private Boolean isBotResponse = false;

    /**
     * Alias per compatibilità - from bot
     */
    @Column(name = "is_from_bot")
    @Builder.Default
    private Boolean isFromBot = false;

    /**
     * Se è un messaggio dello staff
     */
    @Column(name = "is_from_staff")
    @Builder.Default
    private Boolean isFromStaff = false;

    /**
     * Confidence del bot (se is_bot_response)
     */
    @Column(name = "bot_confidence")
    private Double botConfidence;

    /**
     * Data creazione
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Se è stato letto dal destinatario
     */
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Data lettura
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * URL allegato (se presente)
     */
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    /**
     * Nome file allegato
     */
    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (isInternalNote == null) {
            isInternalNote = false;
        }
        if (isBotResponse == null) {
            isBotResponse = false;
        }
        if (isFromBot == null) {
            isFromBot = false;
        }
        if (isFromStaff == null) {
            isFromStaff = false;
        }
        if (isInternal == null) {
            isInternal = false;
        }
        if (isRead == null) {
            isRead = false;
        }
    }

    /**
     * Segna come letto
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = Instant.now();
    }

    /**
     * Verifica se è un messaggio del cliente
     */
    public boolean isFromCustomer() {
        return senderId != null && !isBotResponse && !isInternalNote;
    }

    /**
     * Verifica se è un messaggio dell'agente
     */
    public boolean isFromAgent() {
        return senderId != null && !isBotResponse && !isFromCustomer();
    }

    /**
     * Verifica se è visibile al cliente
     */
    public boolean isVisibleToCustomer() {
        return !Boolean.TRUE.equals(isInternalNote);
    }
}
