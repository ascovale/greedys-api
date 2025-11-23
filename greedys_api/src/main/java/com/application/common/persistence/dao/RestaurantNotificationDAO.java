package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.restaurant.persistence.model.RestaurantNotification;

/**
 * DAO per RestaurantNotification
 */
public interface RestaurantNotificationDAO extends JpaRepository<RestaurantNotification, Long> {

    /**
     * Trova tutte le notifiche di un user di ristorante.
     */
    @Query("SELECT r FROM RestaurantNotificationEntity r WHERE r.RUser.id = :userId ORDER BY r.creationTime DESC")
    List<RestaurantNotification> findByUserId(@Param("userId") Long userId);

    /**
     * Trova tutte le notifiche di un ristorante specifico.
     */
    @Query("SELECT r FROM RestaurantNotificationEntity r WHERE r.RUser.restaurant.id = :restaurantId ORDER BY r.creationTime DESC")
    List<RestaurantNotification> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Trova tutte le notifiche NON LETTE di un user per un ristorante.
     */
    @Query("SELECT r FROM RestaurantNotificationEntity r WHERE r.RUser.id = :userId AND r.RUser.restaurant.id = :restaurantId AND r.read = false ORDER BY r.creationTime DESC")
    List<RestaurantNotification> findUnreadByUserAndRestaurant(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);

    /**
     * Conta le notifiche NON LETTE di un user.
     */
    @Query("SELECT COUNT(r) FROM RestaurantNotificationEntity r WHERE r.RUser.id = :userId AND r.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Marca una notifica come letta.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotificationEntity r SET r.read = true, r.readAt = :readAt WHERE r.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("readAt") Instant readAt);

    /**
     * Marca tutte le notifiche di un user come lette.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotificationEntity r SET r.read = true, r.readAt = :readAt WHERE r.RUser.id = :userId AND r.read = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    /**
     * Se sharedRead=true, marca la notifica come letta per TUTTI i recipient.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotificationEntity r SET r.read = true, r.readByUserId = :readByUserId, r.readAt = :readAt " +
           "WHERE r.id = :notificationId AND r.sharedRead = true AND r.read = false")
    void markAsReadShared(@Param("notificationId") Long notificationId, @Param("readByUserId") Long readByUserId, @Param("readAt") Instant readAt);

    /**
     * ⭐ Conta notifiche di un user (per badge "new since menu opened")
     */
    @Query("SELECT COUNT(r) FROM RestaurantNotificationEntity r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * ⭐ Conta notifiche create DOPO il timestamp di menu-open (for badge)
     * 
     * Definizione di "new":
     * - Notifiche create DOPO lastMenuOpenedAt
     * - Se lastMenuOpenedAt è NULL, tutti le notifiche sono "new"
     */
    @Query("SELECT COUNT(r) FROM RestaurantNotificationEntity r WHERE r.userId = :userId AND r.creationTime > :lastMenuOpenedAt")
    long countByUserIdAndCreatedAfter(@Param("userId") Long userId, @Param("lastMenuOpenedAt") Instant lastMenuOpenedAt);

    // ========== SHARED READ SCOPE METHODS ==========

    /**
     * RESTAURANT scope: Mark all unread notifications for restaurant as read
     * 
     * Use case: Reservation alert sent to all staff in restaurant#5
     *   - Staff#1 reads → all staff in restaurant#5 see it as read
     * 
     * @param restaurantId Restaurant ID
     * @param readByUserId User who triggered read
     * @param readAt Timestamp
     * @return Number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotificationEntity r SET r.read = true, r.readByUserId = :readByUserId, r.readAt = :readAt " +
           "WHERE r.RUser.restaurant.id = :restaurantId AND r.sharedRead = true AND r.read = false")
    int markAsReadRestaurant(@Param("restaurantId") Long restaurantId, 
                             @Param("readByUserId") Long readByUserId, 
                             @Param("readAt") Instant readAt);

    /**
     * RESTAURANT_HUB scope: Mark all notifications for hub as read
     * 
     * Use case: Hub manager notification sent to all staff across hub's restaurants
     *   - Hub#10 manages restaurant#1, restaurant#2
     *   - Hub staff reads → ALL staff in hub#10 see it as read
     * 
     * @param hubId Restaurant User Hub ID
     * @param readByUserId User who triggered read
     * @param readAt Timestamp
     * @return Number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotificationEntity r SET r.read = true, r.readByUserId = :readByUserId, r.readAt = :readAt " +
           "WHERE r.RUser.RUserHub.id = :hubId AND r.sharedRead = true AND r.read = false")
    int markAsReadRestaurantHub(@Param("hubId") Long hubId, 
                                @Param("readByUserId") Long readByUserId, 
                                @Param("readAt") Instant readAt);

    /**
     * RESTAURANT_HUB_ALL scope: Admin broadcast - mark ALL as read immediately
     * 
     * Use case: Critical system announcement to entire hub
     *   - Admin marks hub#10 as read
     *   - ALL users in hub#10 see notification as "read"
     * 
     * @param hubId Restaurant User Hub ID
     * @param readByUserId User who triggered read
     * @param readAt Timestamp
     * @return Number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestaurantNotificationEntity r SET r.read = true, r.readByUserId = :readByUserId, r.readAt = :readAt " +
           "WHERE r.RUser.RUserHub.id = :hubId")
    int markAsReadRestaurantHubAll(@Param("hubId") Long hubId, 
                                    @Param("readByUserId") Long readByUserId, 
                                    @Param("readAt") Instant readAt);
}
