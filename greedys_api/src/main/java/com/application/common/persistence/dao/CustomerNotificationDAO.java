package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.customer.persistence.model.CustomerNotification;

/**
 * DAO per CustomerNotification
 */
public interface CustomerNotificationDAO extends JpaRepository<CustomerNotification, Long> {

    /**
     * Trova tutte le notifiche di un customer.
     */
    @Query("SELECT c FROM CustomerNotificationEntity c WHERE c.userId = :userId ORDER BY c.creationTime DESC")
    List<CustomerNotification> findByUserId(@Param("userId") Long userId);

    /**
     * Trova tutte le notifiche NON LETTE di un customer.
     */
    @Query("SELECT c FROM CustomerNotificationEntity c WHERE c.userId = :userId AND c.read = false ORDER BY c.creationTime DESC")
    List<CustomerNotification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Conta le notifiche NON LETTE di un customer.
     */
    @Query("SELECT COUNT(c) FROM CustomerNotificationEntity c WHERE c.userId = :userId AND c.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Marca una notifica come letta.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CustomerNotificationEntity c SET c.read = true, c.readAt = :readAt WHERE c.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("readAt") Instant readAt);

    /**
     * Marca tutte le notifiche di un customer come lette.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CustomerNotificationEntity c SET c.read = true, c.readAt = :readAt WHERE c.userId = :userId AND c.read = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    /**
     * Se sharedRead=true, marca la notifica come letta per TUTTI i recipient.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CustomerNotificationEntity c SET c.read = true, c.readByUserId = :readByUserId, c.readAt = :readAt " +
           "WHERE c.id = :notificationId AND c.sharedRead = true AND c.read = false")
    void markAsReadShared(@Param("notificationId") Long notificationId, @Param("readByUserId") Long readByUserId, @Param("readAt") Instant readAt);
}
