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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ CHAT MESSAGE ENTITY
 * 
 * Rappresenta un singolo messaggio in una conversazione.
 * 
 * FEATURES:
 * - Tipi di messaggio: TEXT, IMAGE, FILE, SYSTEM, BOT
 * - Supporto reply thread (reply_to_message_id)
 * - Edit tracking
 * - Soft delete
 * - Metadata JSON per reactions, mentions, etc.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "chat_message", indexes = {
    @Index(name = "idx_msg_conversation", columnList = "conversation_id"),
    @Index(name = "idx_msg_sender", columnList = "sender_id"),
    @Index(name = "idx_msg_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Conversazione di appartenenza
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    /**
     * ID del mittente (FK a abstract_user)
     */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /**
     * Tipo di messaggio
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    /**
     * Contenuto del messaggio
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Messaggio a cui si risponde (per reply thread)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private ChatMessage replyToMessage;

    /**
     * Se il messaggio è stato modificato
     */
    @Column(name = "is_edited")
    @Builder.Default
    private Boolean isEdited = false;

    /**
     * Data modifica
     */
    @Column(name = "edited_at")
    private Instant editedAt;

    /**
     * Se il messaggio è eliminato (soft delete)
     */
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Data eliminazione
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Data creazione
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Metadata aggiuntivi (reactions, mentions, etc.)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Allegati al messaggio
     */
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatMessageAttachment> attachments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
        if (isEdited == null) {
            isEdited = false;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Track edits
        if (!isDeleted && isContentChanged()) {
            this.isEdited = true;
            this.editedAt = Instant.now();
        }
    }

    private boolean isContentChanged() {
        // Simplified check - in real impl would compare with loaded value
        return true;
    }

    /**
     * Modifica il contenuto del messaggio
     */
    public void edit(String newContent) {
        this.content = newContent;
        this.isEdited = true;
        this.editedAt = Instant.now();
    }

    /**
     * Elimina il messaggio (soft delete)
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        this.content = "[Messaggio eliminato]";
    }
    
    /**
     * Alias per delete() - soft delete
     */
    public void softDelete() {
        delete();
    }

    /**
     * Aggiunge una reaction al messaggio
     */
    @SuppressWarnings("unchecked")
    public void addReaction(Long userId, String reaction) {
        Map<String, List<Long>> reactions = (Map<String, List<Long>>) metadata.getOrDefault("reactions", new HashMap<>());
        reactions.computeIfAbsent(reaction, k -> new ArrayList<>()).add(userId);
        metadata.put("reactions", reactions);
    }

    /**
     * Rimuove una reaction dal messaggio
     */
    @SuppressWarnings("unchecked")
    public void removeReaction(Long userId, String reaction) {
        Map<String, List<Long>> reactions = (Map<String, List<Long>>) metadata.get("reactions");
        if (reactions != null && reactions.containsKey(reaction)) {
            reactions.get(reaction).remove(userId);
            if (reactions.get(reaction).isEmpty()) {
                reactions.remove(reaction);
            }
        }
    }

    /**
     * Verifica se il messaggio è visibile (non eliminato)
     */
    public boolean isVisible() {
        return !Boolean.TRUE.equals(isDeleted);
    }
}
