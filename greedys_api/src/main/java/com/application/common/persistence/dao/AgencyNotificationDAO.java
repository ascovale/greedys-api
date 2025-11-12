package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.common.persistence.model.notification.AgencyNotification;

/**
 * DAO per AgencyNotification
 */
public interface AgencyNotificationDAO extends JpaRepository<AgencyNotification, Long> {

    /**
     * Trova tutte le notifiche di un user agency.
     */
    @Query("SELECT a FROM AgencyNotification a WHERE a.userId = :userId ORDER BY a.creationTime DESC")
    List<AgencyNotification> findByUserId(@Param("userId") Long userId);

    /**
     * Trova tutte le notifiche NON LETTE di un user agency.
     */
    @Query("SELECT a FROM AgencyNotification a WHERE a.userId = :userId AND a.read = false ORDER BY a.creationTime DESC")
    List<AgencyNotification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Conta le notifiche NON LETTE di un user agency.
     */
    @Query("SELECT COUNT(a) FROM AgencyNotification a WHERE a.userId = :userId AND a.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Marca una notifica come letta.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AgencyNotification a SET a.read = true, a.readAt = :readAt WHERE a.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("readAt") Instant readAt);

    /**
     * Marca tutte le notifiche di un user agency come lette.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AgencyNotification a SET a.read = true, a.readAt = :readAt WHERE a.userId = :userId AND a.read = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    /**
     * Se sharedRead=true, marca la notifica come letta per TUTTI i recipient.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AgencyNotification a SET a.read = true, a.readByUserId = :readByUserId, a.readAt = :readAt " +
           "WHERE a.id = :notificationId AND a.sharedRead = true AND a.read = false")
    void markAsReadShared(@Param("notificationId") Long notificationId, @Param("readByUserId") Long readByUserId, @Param("readAt") Instant readAt);
}
