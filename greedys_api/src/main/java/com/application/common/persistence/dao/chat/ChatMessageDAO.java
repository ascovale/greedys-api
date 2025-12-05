package com.application.common.persistence.dao.chat;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.chat.ChatMessage;
import com.application.common.persistence.model.chat.MessageType;

/**
 * ⭐ CHAT MESSAGE DAO
 * 
 * Repository per la gestione dei messaggi chat.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface ChatMessageDAO extends JpaRepository<ChatMessage, Long> {

    /**
     * Trova messaggi di una conversazione (paginati, ordine cronologico DESC)
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * Trova messaggi dopo un certo ID (per load more)
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.id > :afterMessageId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findByConversationIdAfterMessage(
        @Param("conversationId") Long conversationId, 
        @Param("afterMessageId") Long afterMessageId
    );

    /**
     * Trova messaggi prima di un certo ID (per scroll back)
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.id < :beforeMessageId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationIdBeforeMessage(
        @Param("conversationId") Long conversationId, 
        @Param("beforeMessageId") Long beforeMessageId,
        Pageable pageable
    );

    /**
     * Trova ultimo messaggio di una conversazione
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC " +
           "LIMIT 1")
    ChatMessage findLastMessageByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Conta messaggi non letti per un utente in una conversazione
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "JOIN ChatParticipant p ON p.conversation.id = m.conversation.id " +
           "WHERE m.conversation.id = :conversationId " +
           "AND p.userId = :userId " +
           "AND m.senderId != :userId " +
           "AND m.isDeleted = false " +
           "AND (p.lastReadMessageId IS NULL OR m.id > p.lastReadMessageId)")
    Long countUnreadMessages(
        @Param("conversationId") Long conversationId, 
        @Param("userId") Long userId
    );

    /**
     * Trova messaggi di un utente
     */
    Page<ChatMessage> findBySenderIdAndIsDeletedFalse(Long senderId, Pageable pageable);

    /**
     * Cerca messaggi per contenuto
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.isDeleted = false " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchInConversation(
        @Param("conversationId") Long conversationId, 
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );

    /**
     * Trova messaggi per tipo
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.messageType = :messageType " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationIdAndMessageType(
        @Param("conversationId") Long conversationId, 
        @Param("messageType") MessageType messageType,
        Pageable pageable
    );

    /**
     * Trova risposte a un messaggio (thread)
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.replyToMessage.id = :messageId " +
           "AND m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findRepliesTo(@Param("messageId") Long messageId);

    /**
     * Conta messaggi in una conversazione
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.isDeleted = false")
    Long countByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Soft delete messaggi vecchi (data retention)
     */
    @Modifying
    @Query("UPDATE ChatMessage m " +
           "SET m.isDeleted = true, m.deletedAt = :now, m.content = '[Messaggio eliminato]' " +
           "WHERE m.createdAt < :before " +
           "AND m.isDeleted = false")
    int softDeleteMessagesBefore(@Param("before") Instant before, @Param("now") Instant now);

    /**
     * Trova messaggi recenti di tutte le conversazioni di un utente
     * (per la home/lista chat)
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.conversation.id IN :conversationIds " +
           "AND m.isDeleted = false " +
           "AND m.id = (SELECT MAX(m2.id) FROM ChatMessage m2 WHERE m2.conversation.id = m.conversation.id AND m2.isDeleted = false)")
    List<ChatMessage> findLastMessagesForConversations(@Param("conversationIds") List<Long> conversationIds);
}
