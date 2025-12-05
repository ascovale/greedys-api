package com.application.common.persistence.model.chat;

import java.time.Instant;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ CHAT PARTICIPANT ENTITY
 * 
 * Rappresenta un partecipante a una conversazione.
 * 
 * REGOLE:
 * - Un utente può essere in multiple conversazioni
 * - Una conversazione può avere multiple partecipanti
 * - UNIQUE constraint su (conversation_id, user_id)
 * - left_at = null significa che il partecipante è ancora attivo
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "chat_participant", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_conv_user", columnNames = {"conversation_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_participant_user", columnList = "user_id"),
        @Index(name = "idx_participant_conv", columnList = "conversation_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipant {

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
     * ID dell'utente partecipante (FK a abstract_user)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Ruolo nella conversazione
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.MEMBER;

    /**
     * Data ingresso nella conversazione
     */
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    /**
     * Data uscita (null se ancora presente)
     */
    @Column(name = "left_at")
    private Instant leftAt;

    /**
     * Se le notifiche sono silenziate
     */
    @Column(name = "is_muted")
    @Builder.Default
    private Boolean isMuted = false;

    /**
     * Se le notifiche sono abilitate (diverso da muted: muted = temp, enabled = pref)
     */
    @Column(name = "notifications_enabled")
    @Builder.Default
    private Boolean notificationsEnabled = true;

    /**
     * Ultima volta che l'utente ha letto i messaggi
     */
    @Column(name = "last_read_at")
    private Instant lastReadAt;

    /**
     * ID dell'ultimo messaggio letto
     */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
        if (role == null) {
            role = ParticipantRole.MEMBER;
        }
        if (isMuted == null) {
            isMuted = false;
        }
    }

    /**
     * Segna il partecipante come uscito dalla conversazione
     */
    public void leave() {
        this.leftAt = Instant.now();
    }

    /**
     * Verifica se il partecipante è attivo
     */
    public boolean isActive() {
        return this.leftAt == null;
    }

    /**
     * Aggiorna l'ultimo messaggio letto
     */
    public void markAsRead(Long messageId) {
        this.lastReadAt = Instant.now();
        this.lastReadMessageId = messageId;
    }

    /**
     * Silenzia le notifiche
     */
    public void mute() {
        this.isMuted = true;
    }

    /**
     * Riattiva le notifiche
     */
    public void unmute() {
        this.isMuted = false;
    }
}
