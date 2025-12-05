package com.application.common.persistence.dao.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.ChatNotification;

/**
 * ⭐ CHAT NOTIFICATION DAO
 * 
 * Repository per le notifiche di chat.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface ChatNotificationDAO extends JpaRepository<ChatNotification, Long> {

    /**
     * Verifica esistenza per event ID (idempotenza)
     */
    boolean existsByEventId(String eventId);

    /**
     * Trova notifiche per utente
     */
    @Query("SELECT n FROM ChatNotification n " +
           "WHERE n.userId = :userId " +
           "ORDER BY n.creationTime DESC")
    Page<ChatNotification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Trova notifiche non lette per utente
     */
    @Query("SELECT n FROM ChatNotification n " +
           "WHERE n.userId = :userId " +
           "AND n.read = false " +
           "ORDER BY n.creationTime DESC")
    List<ChatNotification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Trova notifiche per conversazione
     */
    @Query("SELECT n FROM ChatNotification n " +
           "WHERE n.conversationId = :conversationId " +
           "ORDER BY n.creationTime DESC")
    Page<ChatNotification> findByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * Conta non lette per utente
     */
    @Query("SELECT COUNT(n) FROM ChatNotification n " +
           "WHERE n.userId = :userId AND n.read = false")
    Long countUnreadByUserId(@Param("userId") Long userId);
}
