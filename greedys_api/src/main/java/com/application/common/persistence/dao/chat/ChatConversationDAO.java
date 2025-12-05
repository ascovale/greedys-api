package com.application.common.persistence.dao.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.chat.ChatConversation;
import com.application.common.persistence.model.chat.ConversationType;

/**
 * ⭐ CHAT CONVERSATION DAO
 * 
 * Repository per la gestione delle conversazioni chat.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface ChatConversationDAO extends JpaRepository<ChatConversation, Long> {

    /**
     * Trova tutte le conversazioni di un utente (come partecipante)
     */
    @Query("SELECT DISTINCT c FROM ChatConversation c " +
           "JOIN c.participants p " +
           "WHERE p.userId = :userId " +
           "AND p.leftAt IS NULL " +
           "AND c.isArchived = false " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    Page<ChatConversation> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Alias per findByUserId - cerca per partecipante
     */
    @Query("SELECT DISTINCT c FROM ChatConversation c " +
           "JOIN c.participants p " +
           "WHERE p.userId = :userId " +
           "AND p.leftAt IS NULL " +
           "AND c.isArchived = false " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    Page<ChatConversation> findByParticipantUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Trova conversazioni per tipo
     */
    @Query("SELECT DISTINCT c FROM ChatConversation c " +
           "JOIN c.participants p " +
           "WHERE p.userId = :userId " +
           "AND p.leftAt IS NULL " +
           "AND c.conversationType = :type " +
           "AND c.isArchived = false")
    Page<ChatConversation> findByUserIdAndType(
        @Param("userId") Long userId, 
        @Param("type") ConversationType type, 
        Pageable pageable
    );

    /**
     * Trova conversazione DIRECT tra due utenti specifici
     */
    @Query("SELECT c FROM ChatConversation c " +
           "WHERE c.conversationType = 'DIRECT' " +
           "AND c.isArchived = false " +
           "AND EXISTS (SELECT p1 FROM ChatParticipant p1 WHERE p1.conversation = c AND p1.userId = :userId1 AND p1.leftAt IS NULL) " +
           "AND EXISTS (SELECT p2 FROM ChatParticipant p2 WHERE p2.conversation = c AND p2.userId = :userId2 AND p2.leftAt IS NULL)")
    Optional<ChatConversation> findDirectConversation(
        @Param("userId1") Long userId1, 
        @Param("userId2") Long userId2
    );

    /**
     * Trova conversazione legata a una prenotazione
     */
    Optional<ChatConversation> findByReservationIdAndConversationType(
        Long reservationId, 
        ConversationType conversationType
    );

    /**
     * Trova tutte le conversazioni per una prenotazione
     */
    List<ChatConversation> findByReservationId(Long reservationId);

    /**
     * Trova conversazioni di un ristorante
     */
    Page<ChatConversation> findByRestaurantIdAndIsArchivedFalse(Long restaurantId, Pageable pageable);

    /**
     * Conta conversazioni non lette per un utente
     */
    @Query("SELECT COUNT(DISTINCT c) FROM ChatConversation c " +
           "JOIN c.participants p " +
           "JOIN c.messages m " +
           "WHERE p.userId = :userId " +
           "AND p.leftAt IS NULL " +
           "AND c.isArchived = false " +
           "AND (p.lastReadMessageId IS NULL OR m.id > p.lastReadMessageId) " +
           "AND m.senderId != :userId")
    Long countUnreadConversations(@Param("userId") Long userId);

    /**
     * Trova conversazioni archiviate
     */
    @Query("SELECT DISTINCT c FROM ChatConversation c " +
           "JOIN c.participants p " +
           "WHERE p.userId = :userId " +
           "AND p.leftAt IS NULL " +
           "AND c.isArchived = true")
    Page<ChatConversation> findArchivedByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Verifica se un utente è partecipante di una conversazione
     */
    @Query("SELECT COUNT(p) > 0 FROM ChatParticipant p " +
           "WHERE p.conversation.id = :conversationId " +
           "AND p.userId = :userId " +
           "AND p.leftAt IS NULL")
    boolean isUserParticipant(
        @Param("conversationId") Long conversationId, 
        @Param("userId") Long userId
    );

    /**
     * Trova conversazioni per ristorante e tipo
     */
    List<ChatConversation> findByRestaurantIdAndConversationTypeAndIsArchivedFalse(
        Long restaurantId, 
        ConversationType conversationType
    );
}
