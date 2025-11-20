package com.application.admin.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.model.AdminNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;

/**
 * ⭐ DATA ACCESS OBJECT per AdminNotification (disaggregata per channel)
 * 
 * Gestisce persistenza di notifiche DISAGGREGATE per:
 * - OGNI admin
 * - OGNI notification channel (WEBSOCKET, EMAIL, PUSH, SMS)
 * 
 * USAGE PATTERNS:
 * 1. IDEMPOTENCY CHECK (in RabbitListener)
 *    existsByEventId(eventId) → skip se già esiste
 * 
 * 2. CREATION (in RabbitListener)
 *    save(AdminNotification) → persisti disaggregata
 * 
 * 3. CHANNEL POLLING (in ChannelPoller)
 *    findPendingByChannel(channel, limit) → batch SELECT per channel
 *    → Per ogni notification, invia via channel
 *    → updateStatus(notificationId, status) → UPDATE status dopo invio
 * 
 * 4. DASHBOARD/QUERY (in NotificationService)
 *    findByUserIdOrderByCreatedAtDesc(adminId) → tutte notifiche per admin
 *    findUnreadByUserId(adminId) → notifiche non lette
 * 
 * ⭐ IMPORTANTE: NO SHARED READ per admin (ogni admin ha read status INDIVIDUALE)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Repository
public interface AdminNotificationDAO extends JpaRepository<AdminNotification, Long> {

    /**
     * ⭐ IDEMPOTENCY CHECK
     * 
     * Usato in AdminNotificationListener prima di creare nuova notifica.
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
    Optional<AdminNotification> findByEventId(String eventId);

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
        SELECT * FROM admin_notification 
        WHERE channel = :channel 
        AND status = 'PENDING'
        ORDER BY priority DESC, created_at ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<AdminNotification> findPendingByChannel(
        @Param("channel") String channel,
        @Param("limit") int limit
    );

    /**
     * Find PENDING by channel (spring data style)
     */
    List<AdminNotification> findByChannelAndStatus(
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
    @Query("UPDATE AdminNotification a SET a.status = :status, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :notificationId")
    int updateStatus(
        @Param("notificationId") Long notificationId,
        @Param("status") DeliveryStatus status
    );

    /**
     * ⭐ DASHBOARD: Find by userId (per notification center)
     * 
     * Trova tutte le notifiche per uno specifico admin
     * Ordinate per creation_time DESC (newest first)
     * 
     * @param userId Admin ID
     * @return notifiche ordinate
     */
    List<AdminNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * ⭐ DASHBOARD: Find UNREAD by userId
     * 
     * Notifiche non ancora lette (status != READ) per admin.
     * Usato per badge counter nella UI.
     * 
     * @param userId Admin ID
     * @return notifiche non lette
     */
    @Query("SELECT a FROM AdminNotification a WHERE a.userId = :userId AND a.status != 'READ' ORDER BY a.createdAt DESC")
    List<AdminNotification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Find by userId and status
     */
    List<AdminNotification> findByUserIdAndStatus(Long userId, DeliveryStatus status);

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
        DELETE FROM admin_notification 
        WHERE status = 'READ' 
        AND created_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL :daysOld DAY)
        """, nativeQuery = true)
    int deleteOldReadNotifications(@Param("daysOld") int daysOld);

}
