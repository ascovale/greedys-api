package com.application.restaurant.persistence.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.restaurant.persistence.model.RestaurantUserNotification;

/**
 * ⭐ DATA ACCESS OBJECT per RestaurantUserNotification (disaggregata per channel)
 * 
 * Gestisce persistenza di notifiche DISAGGREGATE per:
 * - OGNI restaurant user (staff)
 * - OGNI notification channel (WEBSOCKET, EMAIL, PUSH, SMS)
 * 
 * USAGE PATTERNS:
 * 1. IDEMPOTENCY CHECK (in RabbitListener)
 *    existsByEventId(eventId) → skip se già esiste
 * 
 * 2. CREATION (in RabbitListener)
 *    save(RestaurantUserNotification) → persisti disaggregata
 * 
 * 3. BATCH READ (in ReadStatusService, quando readByAll=true)
 *    updateReadByAll(eventId, restaurantId, channel)
 *    → UPDATE all rows per quel eventId + restaurantId + channel SET read_at=now, status=READ
 * 
 * 4. CHANNEL POLLING (in ChannelPoller)
 *    findPendingByChannel(channel, limit) → batch SELECT per channel
 *    → Per ogni notification, invia via channel
 *    → updateStatus(notificationId, status) → UPDATE status dopo invio
 * 
 * 5. DASHBOARD/QUERY (in NotificationService)
 *    findByRestaurantIdOrderByCreatedAtDesc(restaurantId) → tutte notifiche per ristorante
 *    findUnreadByRestaurantId(restaurantId) → notifiche non lette
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Repository
public interface RestaurantUserNotificationDAO extends JpaRepository<RestaurantUserNotification, Long> {

    /**
     * ⭐ IDEMPOTENCY CHECK
     * 
     * Usato in RestaurantNotificationListener prima di creare nuova notifica.
     * 
     * Se eventId esiste già → è un retry duplicato, skip
     * Se eventId non esiste → è nuovo messaggio, crea row
     * 
     * @param eventId Unique identifier per disaggregated message
     * @return true se esiste già, false altrimenti
     */
    boolean existsByEventId(String eventId);

    /**
     * Find by eventId (dovrebbe essere unica)
     */
    Optional<RestaurantUserNotification> findByEventId(String eventId);

    /**
     * ⭐ BATCH READ UPDATE (shared read logic)
     * 
     * Quando UN restaurant user legge notifica e readByAll=true:
     * → UPDATE tutte le rows per quel eventId + restaurantId + channel
     * → Marca tutte come READ per quel broadcast event
     * 
     * Esempio:
     * - EventId: "NEW_ORDER_RES-123_2025-01-20T10:30"
     * - RestaurantId: 5
     * - Channel: WEBSOCKET
     * - N users: 10 staff
     * → UPDATE 10 rows SET read_at=NOW(), status=READ
     * 
     * @param eventId Unique event identifier
     * @param restaurantId Per batch operations (same restaurant)
     * @param channel Per specifico channel
     * @return numero di rows aggiornate
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE restaurant_user_notification 
        SET read_at = CURRENT_TIMESTAMP, status = 'READ', updated_at = CURRENT_TIMESTAMP
        WHERE event_id = :eventId 
        AND restaurant_id = :restaurantId 
        AND channel = :channel 
        AND read_by_all = true
        """, nativeQuery = true)
    int updateReadByAll(
        @Param("eventId") String eventId,
        @Param("restaurantId") Long restaurantId,
        @Param("channel") String channel
    );

    /**
     * ⭐ CHANNEL POLLING (per ChannelPoller batch processing)
     * 
     * Trova tutte le notifiche PENDING per uno specifico channel
     * (es: WEBSOCKET, EMAIL, PUSH, SMS)
     * 
     * Ordinamento:
     * 1. Priority DESC (HIGH first)
     * 2. created_at ASC (FIFO per same priority)
     * 
     * Limit: evita query troppo grandi
     * 
     * @param channel NotificationChannel enum (WEBSOCKET, EMAIL, PUSH, SMS)
     * @param limit Numero max di risultati (es: 100 per batch)
     * @return liste di notifiche da elaborare
     */
    @Query(value = """
        SELECT * FROM restaurant_user_notification 
        WHERE channel = :channel 
        AND status = 'PENDING'
        ORDER BY priority DESC, created_at ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<RestaurantUserNotification> findPendingByChannel(
        @Param("channel") String channel,
        @Param("limit") int limit
    );

    /**
     * Find PENDING by channel (spring data style)
     */
    List<RestaurantUserNotification> findByChannelAndStatus(
        NotificationChannel channel,
        DeliveryStatus status
    );

    /**
     * ⭐ UPDATE STATUS (dopo tentativo invio)
     * 
     * Aggiorna lo status di una notifica dopo tentativo delivery:
     * - PENDING → DELIVERED (invio riuscito via channel)
     * - PENDING → FAILED (errore durante invio)
     * - DELIVERED/PENDING → READ (user ha letto)
     * 
     * @param notificationId ID della notifica
     * @param status Nuovo status
     * @return numero di rows aggiornate (dovrebbe essere 1)
     */
    @Modifying
    @Transactional
    @Query("UPDATE RestaurantUserNotification r SET r.status = :status, r.updatedAt = :now WHERE r.id = :notificationId")
    int updateStatus(
        @Param("notificationId") Long notificationId,
        @Param("status") DeliveryStatus status,
        @Param("now") Instant now
    );

    /**
     * ⭐ DASHBOARD: Find by restaurantId (per notification center)
     * 
     * Trova tutte le notifiche per uno specifico restaurant (tutti i staff)
     * Ordinate per creation_time DESC (newest first)
     * 
     * @param restaurantId Ristorante
     * @return notifiche ordinate
     */
    List<RestaurantUserNotification> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    /**
     * ⭐ DASHBOARD: Find UNREAD by restaurantId
     * 
     * Notifiche non ancora lette (status != READ) per ristorante.
     * Usato per badge counter nella UI.
     * 
     * @param restaurantId Ristorante
     * @return notifiche non lette
     */
    @Query("SELECT r FROM RestaurantUserNotification r WHERE r.restaurantId = :restaurantId AND r.status != 'READ' ORDER BY r.createdAt DESC")
    List<RestaurantUserNotification> findUnreadByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Find by restaurantId and userId (per una specifica persona)
     */
    List<RestaurantUserNotification> findByRestaurantIdAndUserIdOrderByCreatedAtDesc(Long restaurantId, Long userId);

    /**
     * Find by restaurantId and userId and status
     */
    List<RestaurantUserNotification> findByRestaurantIdAndUserIdAndStatus(Long restaurantId, Long userId, DeliveryStatus status);

    /**
     * Count unread per restaurantId (per badge)
     */
    long countByRestaurantIdAndStatus(Long restaurantId, DeliveryStatus status);

    /**
     * ⭐ CLEANUP: Delete old read notifications (older than X days)
     * 
     * Purge per mantenere tabella small:
     * - Leggi notifiche con status=READ e creation_time > 30 days
     * - Delete
     * 
     * Usato da ScheduledTask
     * 
     * @param daysOld Giorni di retention
     * @return numero di rows deletate
     */
    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM restaurant_user_notification 
        WHERE status = 'READ' 
        AND created_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL :daysOld DAY)
        """, nativeQuery = true)
    int deleteOldReadNotifications(@Param("daysOld") int daysOld);

}
