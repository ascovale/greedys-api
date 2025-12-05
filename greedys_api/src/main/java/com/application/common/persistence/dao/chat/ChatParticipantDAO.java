package com.application.common.persistence.dao.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.chat.ChatParticipant;
import com.application.common.persistence.model.chat.ParticipantRole;

/**
 * ⭐ CHAT PARTICIPANT DAO
 * 
 * Repository per la gestione dei partecipanti alle conversazioni.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface ChatParticipantDAO extends JpaRepository<ChatParticipant, Long> {

    /**
     * Trova tutti i partecipanti attivi di una conversazione
     */
    @Query("SELECT p FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.leftAt IS NULL")
    List<ChatParticipant> findActiveByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Trova tutti i partecipanti (inclusi quelli usciti)
     */
    List<ChatParticipant> findByConversationId(Long conversationId);

    /**
     * Trova partecipante specifico
     */
    Optional<ChatParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * Trova partecipante attivo specifico
     */
    @Query("SELECT p FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.userId = :userId " +
           "AND p.leftAt IS NULL")
    Optional<ChatParticipant> findActiveByConversationIdAndUserId(
        @Param("conversationId") Long conversationId, 
        @Param("userId") Long userId
    );

    /**
     * Verifica se un utente è partecipante attivo
     */
    @Query("SELECT COUNT(p) > 0 FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.userId = :userId " +
           "AND p.leftAt IS NULL")
    boolean existsActiveByConversationIdAndUserId(
        @Param("conversationId") Long conversationId, 
        @Param("userId") Long userId
    );

    /**
     * Trova tutte le conversazioni di un utente
     */
    @Query("SELECT p.conversation.id FROM ChatParticipant p " +
           "WHERE p.userId = :userId " +
           "AND p.leftAt IS NULL")
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);

    /**
     * Conta partecipanti attivi di una conversazione
     */
    @Query("SELECT COUNT(p) FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.leftAt IS NULL")
    Long countActiveByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Trova ID degli utenti partecipanti attivi (per notifiche)
     */
    @Query("SELECT p.userId FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.leftAt IS NULL")
    List<Long> findActiveUserIdsByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Trova ID degli utenti partecipanti attivi eccetto uno (per notifiche - escludi mittente)
     */
    @Query("SELECT p.userId FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.userId != :excludeUserId " +
           "AND p.leftAt IS NULL " +
           "AND p.isMuted = false")
    List<Long> findActiveUserIdsExcept(
        @Param("conversationId") Long conversationId, 
        @Param("excludeUserId") Long excludeUserId
    );

    /**
     * Trova partecipanti per ruolo
     */
    @Query("SELECT p FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.role = :role " +
           "AND p.leftAt IS NULL")
    List<ChatParticipant> findByConversationIdAndRole(
        @Param("conversationId") Long conversationId, 
        @Param("role") ParticipantRole role
    );

    /**
     * Aggiorna l'ultimo messaggio letto
     */
    @Modifying
    @Query("UPDATE ChatParticipant p " +
           "SET p.lastReadAt = :now, p.lastReadMessageId = :messageId " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.userId = :userId")
    void updateLastRead(
        @Param("conversationId") Long conversationId, 
        @Param("userId") Long userId, 
        @Param("messageId") Long messageId,
        @Param("now") Instant now
    );
}
