package com.application.common.persistence.dao.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.SocialNotification;

/**
 * ⭐ SOCIAL NOTIFICATION DAO
 * 
 * Repository per le notifiche social.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SocialNotificationDAO extends JpaRepository<SocialNotification, Long> {

    /**
     * Verifica esistenza per event ID (idempotenza)
     */
    boolean existsByEventId(String eventId);

    /**
     * Trova notifiche per utente
     */
    @Query("SELECT n FROM SocialNotification n " +
           "WHERE n.userId = :userId " +
           "ORDER BY n.creationTime DESC")
    Page<SocialNotification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Trova notifiche non lette per utente
     */
    @Query("SELECT n FROM SocialNotification n " +
           "WHERE n.userId = :userId " +
           "AND n.read = false " +
           "ORDER BY n.creationTime DESC")
    List<SocialNotification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Trova notifiche per post
     */
    @Query("SELECT n FROM SocialNotification n " +
           "WHERE n.postId = :postId " +
           "ORDER BY n.creationTime DESC")
    Page<SocialNotification> findByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * Conta non lette per utente
     */
    @Query("SELECT COUNT(n) FROM SocialNotification n " +
           "WHERE n.userId = :userId AND n.read = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Trova notifiche per tipo evento
     */
    @Query("SELECT n FROM SocialNotification n " +
           "WHERE n.userId = :userId " +
           "AND n.eventType = :eventType " +
           "ORDER BY n.creationTime DESC")
    Page<SocialNotification> findByUserIdAndEventType(
        @Param("userId") Long userId, 
        @Param("eventType") String eventType,
        Pageable pageable
    );
}
