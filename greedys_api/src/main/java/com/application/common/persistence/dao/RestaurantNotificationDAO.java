package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.common.persistence.model.notification.RestaurantNotification;

/**
 * DAO per RestaurantNotification
 */
public interface RestaurantNotificationDAO extends JpaRepository<RestaurantNotification, Long> {

    /**
     * Trova tutte le notifiche di un user di ristorante.
     */
    @Query("SELECT r FROM RestaurantNotification r WHERE r.userId = :userId ORDER BY r.creationTime DESC")
    List<RestaurantNotification> findByUserId(@Param("userId") Long userId);

    /**
     * Trova tutte le notifiche di un ristorante specifico.
     */
    @Query("SELECT r FROM RestaurantNotification r WHERE r.restaurantId = :restaurantId ORDER BY r.creationTime DESC")
    List<RestaurantNotification> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Trova tutte le notifiche NON LETTE di un user per un ristorante.
     */
    @Query("SELECT r FROM RestaurantNotification r WHERE r.userId = :userId AND r.restaurantId = :restaurantId AND r.read = false ORDER BY r.creationTime DESC")
    List<RestaurantNotification> findUnreadByUserAndRestaurant(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);

    /**
     * Conta le notifiche NON LETTE di un user.
     */
    @Query("SELECT COUNT(r) FROM RestaurantNotification r WHERE r.userId = :userId AND r.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Marca una notifica come letta.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotification r SET r.read = true, r.readAt = :readAt WHERE r.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("readAt") Instant readAt);

    /**
     * Marca tutte le notifiche di un user come lette.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotification r SET r.read = true, r.readAt = :readAt WHERE r.userId = :userId AND r.read = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    /**
     * Se sharedRead=true, marca la notifica come letta per TUTTI i recipient.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotification r SET r.read = true, r.readByUserId = :readByUserId, r.readAt = :readAt " +
           "WHERE r.id = :notificationId AND r.sharedRead = true AND r.read = false")
    void markAsReadShared(@Param("notificationId") Long notificationId, @Param("readByUserId") Long readByUserId, @Param("readAt") Instant readAt);
}
