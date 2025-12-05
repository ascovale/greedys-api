package com.application.common.persistence.dao.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.chat.ChatMessageAttachment;

/**
 * ⭐ CHAT MESSAGE ATTACHMENT DAO
 * 
 * Repository per la gestione degli allegati ai messaggi chat.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface ChatMessageAttachmentDAO extends JpaRepository<ChatMessageAttachment, Long> {

    /**
     * Trova allegati di un messaggio
     */
    List<ChatMessageAttachment> findByMessageId(Long messageId);

    /**
     * Trova allegati di una conversazione (per media gallery)
     */
    @Query("SELECT a FROM ChatMessageAttachment a " +
           "JOIN a.message m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.isDeleted = false " +
           "ORDER BY a.createdAt DESC")
    List<ChatMessageAttachment> findByConversationId(@Param("conversationId") Long conversationId);

    /**
     * Trova allegati per tipo (immagini/video/file)
     */
    @Query("SELECT a FROM ChatMessageAttachment a " +
           "JOIN a.message m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.isDeleted = false " +
           "AND a.fileType LIKE :fileTypePrefix% " +
           "ORDER BY a.createdAt DESC")
    List<ChatMessageAttachment> findByConversationIdAndFileTypePrefix(
        @Param("conversationId") Long conversationId, 
        @Param("fileTypePrefix") String fileTypePrefix
    );

    /**
     * Calcola spazio usato da un utente (per quotas)
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM ChatMessageAttachment a " +
           "JOIN a.message m " +
           "WHERE m.senderId = :userId")
    Long calculateTotalStorageByUser(@Param("userId") Long userId);

    /**
     * Calcola spazio usato in una conversazione
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM ChatMessageAttachment a " +
           "JOIN a.message m " +
           "WHERE m.conversation.id = :conversationId")
    Long calculateTotalStorageByConversation(@Param("conversationId") Long conversationId);

    /**
     * Conta allegati di un messaggio
     */
    Long countByMessageId(Long messageId);

    /**
     * Trova allegati recenti di un utente
     */
    @Query("SELECT a FROM ChatMessageAttachment a " +
           "JOIN a.message m " +
           "WHERE m.senderId = :userId " +
           "ORDER BY a.createdAt DESC " +
           "LIMIT :limit")
    List<ChatMessageAttachment> findRecentByUserId(
        @Param("userId") Long userId, 
        @Param("limit") int limit
    );
}
