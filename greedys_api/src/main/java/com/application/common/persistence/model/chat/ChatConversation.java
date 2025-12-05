package com.application.common.persistence.model.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * ⭐ CHAT CONVERSATION ENTITY
 * 
 * Rappresenta una conversazione chat che può essere:
 * - DIRECT: 1:1 tra due utenti
 * - GROUP: Gruppo di N utenti
 * - SUPPORT: Ticket di supporto
 * - RESERVATION: Chat legata a una prenotazione
 * 
 * ARCHITETTURA:
 * - Una conversazione ha N partecipanti (ChatParticipant)
 * - Una conversazione ha N messaggi (ChatMessage)
 * - Le conversazioni RESERVATION sono legate a una prenotazione specifica
 * - Le conversazioni SUPPORT sono legate a un ticket
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "chat_conversation", indexes = {
    @Index(name = "idx_conv_type", columnList = "conversation_type"),
    @Index(name = "idx_conv_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_conv_reservation", columnList = "reservation_id"),
    @Index(name = "idx_conv_created_by", columnList = "created_by_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo di conversazione
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false, length = 20)
    private ConversationType conversationType;

    /**
     * Titolo della conversazione (opzionale per DIRECT, obbligatorio per GROUP)
     */
    @Column(name = "title", length = 255)
    private String title;
    
    /**
     * Nome della conversazione (alias per title, usato nei gruppi)
     */
    @Column(name = "name", length = 255)
    private String name;

    /**
     * Timestamp ultimo messaggio (per ordinamento)
     */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    /**
     * FK alla prenotazione (solo per RESERVATION type)
     */
    @Column(name = "reservation_id")
    private Long reservationId;

    /**
     * FK al ristorante (contesto)
     */
    @Column(name = "restaurant_id")
    private Long restaurantId;

    /**
     * FK all'agenzia (contesto, se applicabile)
     */
    @Column(name = "agency_id")
    private Long agencyId;

    /**
     * ID dell'utente che ha creato la conversazione (FK a abstract_user)
     */
    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    /**
     * Data di creazione
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Data ultimo aggiornamento
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Se la conversazione è archiviata
     */
    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    /**
     * Data archiviazione
     */
    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * Metadata aggiuntivi (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Partecipanti alla conversazione
     */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    /**
     * Messaggi della conversazione
     */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (isArchived == null) {
            isArchived = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Archivia la conversazione
     */
    public void archive() {
        this.isArchived = true;
        this.archivedAt = Instant.now();
    }

    /**
     * Ripristina la conversazione dall'archivio
     */
    public void unarchive() {
        this.isArchived = false;
        this.archivedAt = null;
    }
}
