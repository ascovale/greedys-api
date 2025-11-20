package com.application.customer.persistence.dao;

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
import com.application.customer.persistence.model.CustomerNotification;

/**
 * ⭐ DATA ACCESS OBJECT per CustomerNotification (disaggregata per channel)
 * 
 * Gestisce persistenza di notifiche DISAGGREGATE per:
 * - OGNI customer
 * - OGNI notification channel (WEBSOCKET, EMAIL, PUSH, SMS)
 * 
 * USAGE PATTERNS:
 * 1. IDEMPOTENCY CHECK (in RabbitListener)
 *    existsByEventId(eventId) → skip se già esiste
 * 
 * 2. CREATION (in RabbitListener)
 *    save(CustomerNotification) → persisti disaggregata
 * 
 * 3. CHANNEL POLLING (in ChannelPoller)
 *    findPendingByChannel(channel, limit) → batch SELECT per channel
 *    → Per ogni notification, invia via channel
 *    → updateStatus(notificationId, status) → UPDATE status dopo invio
 * 
 * 4. DASHBOARD/QUERY (in NotificationService)
 *    findByUserIdOrderByCreatedAtDesc(customerId) → tutte notifiche per customer
 *    findUnreadByUserId(customerId) → notifiche non lette
 * 
 * ⭐ IMPORTANTE: NO SHARED READ per customer (ogni customer è isolato)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Repository
public interface CustomerNotificationDAO extends JpaRepository<CustomerNotification, Long> {

    /**
     * ⭐ IDEMPOTENCY CHECK
     * 
     * Usato in CustomerNotificationListener prima di creare nuova notifica.
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
    Optional<CustomerNotification> findByEventId(String eventId);

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
        SELECT * FROM notification 
        WHERE channel = :channel 
        AND status = 'PENDING'
        ORDER BY priority DESC, created_at ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<CustomerNotification> findPendingByChannel(
        @Param("channel") String channel,
        @Param("limit") int limit
    );

    /**
     * Find PENDING by channel (spring data style)
     */
    List<CustomerNotification> findByChannelAndStatus(
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
    @Query("UPDATE CustomerNotificationEntity c SET c.status = :status, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :notificationId")
    int updateStatus(
        @Param("notificationId") Long notificationId,
        @Param("status") DeliveryStatus status
    );

    /**
     * ⭐ DASHBOARD: Find by userId (per notification center)
     * 
     * Trova tutte le notifiche per uno specifico customer
     * Ordinate per creation_time DESC (newest first)
     * 
     * @param userId Customer ID
     * @return notifiche ordinate
     */
    List<CustomerNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * ⭐ DASHBOARD: Find UNREAD by userId
     * 
     * Notifiche non ancora lette (status != READ) per customer.
     * Usato per badge counter nella UI.
     * 
     * @param userId Customer ID
     * @return notifiche non lette
     */
    @Query("SELECT c FROM CustomerNotificationEntity c WHERE c.userId = :userId AND c.status != 'READ' ORDER BY c.createdAt DESC")
    List<CustomerNotification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Find by userId and status
     */
    List<CustomerNotification> findByUserIdAndStatus(Long userId, DeliveryStatus status);

    /**
     * Count unread per userId (per badge)
     */
    long countByUserIdAndStatus(Long userId, DeliveryStatus status);

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
        DELETE FROM notification 
        WHERE status = 'READ' 
        AND created_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL :daysOld DAY)
        """, nativeQuery = true)
    int deleteOldReadNotifications(@Param("daysOld") int daysOld);

}
