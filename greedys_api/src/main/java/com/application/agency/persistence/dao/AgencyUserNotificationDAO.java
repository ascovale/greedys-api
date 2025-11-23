package com.application.agency.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.agency.persistence.model.AgencyUserNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;

/**
 * ⭐ DATA ACCESS OBJECT per AgencyUserNotification (disaggregata per channel)
 * 
 * Gestisce persistenza di notifiche DISAGGREGATE per:
 * - OGNI agency user (agent/manager)
 * - OGNI notification channel (WEBSOCKET, EMAIL, PUSH, SMS)
 * 
 * USAGE PATTERNS:
 * 1. IDEMPOTENCY CHECK (in RabbitListener)
 *    existsByEventId(eventId) → skip se già esiste
 * 
 * 2. CREATION (in RabbitListener)
 *    save(AgencyUserNotification) → persisti disaggregata
 * 
 * 3. BATCH READ (in ReadStatusService, quando readByAll=true)
 *    updateReadByAll(eventId, agencyId, channel)
 *    → UPDATE all rows per quel eventId + agencyId + channel SET read_at=now, status=READ
 * 
 * 4. CHANNEL POLLING (in ChannelPoller)
 *    findPendingByChannel(channel, limit) → batch SELECT per channel
 *    → Per ogni notification, invia via channel
 *    → updateStatus(notificationId, status) → UPDATE status dopo invio
 * 
 * 5. DASHBOARD/QUERY (in NotificationService)
 *    findByAgencyIdOrderByCreatedAtDesc(agencyId) → tutte notifiche per agency
 *    findUnreadByAgencyId(agencyId) → notifiche non lette
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Repository
public interface AgencyUserNotificationDAO extends JpaRepository<AgencyUserNotification, Long> {

    /**
     * ⭐ IDEMPOTENCY CHECK
     * 
     * Usato in AgencyNotificationListener prima di creare nuova notifica.
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
    Optional<AgencyUserNotification> findByEventId(String eventId);

    /**
     * ⭐ BATCH READ UPDATE (shared read logic)
     * 
     * Quando UN agency user legge notifica e readByAll=true:
     * → UPDATE tutte le rows per quel eventId + agencyId + channel
     * → Marca tutte come READ per quel broadcast event
     * 
     * Esempio:
     * - EventId: "NEW_ORDER_AGE-123_2025-01-20T10:30"
     * - AgencyId: 2
     * - Channel: WEBSOCKET
     * - N users: 5 agency agents
     * → UPDATE 5 rows SET read_at=NOW(), status=READ
     * 
     * @param eventId Unique event identifier
     * @param agencyId Per batch operations (same agency)
     * @param channel Per specifico channel
     * @return numero di rows aggiornate
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE agency_user_notification 
        SET read_at = CURRENT_TIMESTAMP, status = 'READ', updated_at = CURRENT_TIMESTAMP
        WHERE event_id = :eventId 
        AND agency_id = :agencyId 
        AND channel = :channel 
        AND read_by_all = true
        """, nativeQuery = true)
    int updateReadByAll(
        @Param("eventId") String eventId,
        @Param("agencyId") Long agencyId,
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
        SELECT * FROM agency_user_notification 
        WHERE channel = :channel 
        AND status = 'PENDING'
        ORDER BY priority DESC, created_at ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<AgencyUserNotification> findPendingByChannel(
        @Param("channel") String channel,
        @Param("limit") int limit
    );

    /**
     * Find PENDING by channel (spring data style)
     */
    List<AgencyUserNotification> findByChannelAndStatus(
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
    @Query("UPDATE AgencyUserNotification a SET a.status = :status, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :notificationId")
    int updateStatus(
        @Param("notificationId") Long notificationId,
        @Param("status") DeliveryStatus status
    );

    /**
     * ⭐ DASHBOARD: Find by agencyId (per notification center)
     * 
     * Trova tutte le notifiche per una specifica agency (tutti gli agent)
     * Ordinate per creation_time DESC (newest first)
     * 
     * @param agencyId Agency
     * @return notifiche ordinate
     */
    List<AgencyUserNotification> findByAgencyIdOrderByCreatedAtDesc(Long agencyId);

    /**
     * ⭐ DASHBOARD: Find UNREAD by agencyId
     * 
     * Notifiche non ancora lette (status != READ) per agency.
     * Usato per badge counter nella UI.
     * 
     * @param agencyId Agency
     * @return notifiche non lette
     */
    @Query("SELECT a FROM AgencyUserNotification a WHERE a.agencyId = :agencyId AND a.status != 'READ' ORDER BY a.createdAt DESC")
    List<AgencyUserNotification> findUnreadByAgencyId(@Param("agencyId") Long agencyId);

    /**
     * Find by agencyId and userId (per una specifica persona)
     */
    List<AgencyUserNotification> findByAgencyIdAndUserIdOrderByCreatedAtDesc(Long agencyId, Long userId);

    /**
     * Find by agencyId and userId and status
     */
    List<AgencyUserNotification> findByAgencyIdAndUserIdAndStatus(Long agencyId, Long userId, DeliveryStatus status);

    /**
     * Count unread per agencyId (per badge)
     */
    long countByAgencyIdAndStatus(Long agencyId, DeliveryStatus status);

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
        DELETE FROM agency_user_notification 
        WHERE status = 'READ' 
        AND created_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL :daysOld DAY)
        """, nativeQuery = true)
    int deleteOldReadNotifications(@Param("daysOld") int daysOld);

    // ========== SHARED READ SCOPE METHODS ==========

    /**
     * AGENCY scope: Mark all unread notifications for agency as read
     * 
     * Use case: Agency-wide announcement
     *   - AgencyUser#1 (agency#3) reads → all AgencyUsers in agency#3 see it as read
     * 
     * @param agencyId Agency ID
     * @param readByUserId User who triggered read
     * @param readAt Timestamp
     * @return Number of rows updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE AgencyUserNotification a SET a.status = 'READ', a.readByUserId = :readByUserId, a.readAt = :readAt " +
           "WHERE a.agencyId = :agencyId AND a.sharedRead = true AND a.status != 'READ'")
    int markAsReadAgency(@Param("agencyId") Long agencyId, 
                         @Param("readByUserId") Long readByUserId, 
                         @Param("readAt") java.time.Instant readAt);

    /**
     * AGENCY_HUB scope: Mark all notifications for hub as read
     * 
     * Use case: Hub manager notification sent to all staff across multiple agencies
     *   - Hub#20 manages agency#1, agency#2
     *   - Hub staff reads → ALL staff in hub#20 see it as read
     * 
     * @param hubId Agency User Hub ID
     * @param readByUserId User who triggered read
     * @param readAt Timestamp
     * @return Number of rows updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE AgencyUserNotification a SET a.status = 'READ', a.readByUserId = :readByUserId, a.readAt = :readAt " +
           "WHERE a.agencyUser.agencyUserHub.id = :hubId AND a.sharedRead = true AND a.status != 'READ'")
    int markAsReadAgencyHub(@Param("hubId") Long hubId, 
                            @Param("readByUserId") Long readByUserId, 
                            @Param("readAt") java.time.Instant readAt);

    /**
     * AGENCY_HUB_ALL scope: Admin broadcast - mark ALL as read immediately
     * 
     * Use case: Critical system announcement to entire hub
     *   - Admin marks hub#20 as read
     *   - ALL users in hub#20 see notification as "read"
     * 
     * @param hubId Agency User Hub ID
     * @param readByUserId User who triggered read
     * @param readAt Timestamp
     * @return Number of rows updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE AgencyUserNotification a SET a.status = 'READ', a.readByUserId = :readByUserId, a.readAt = :readAt " +
           "WHERE a.agencyUser.agencyUserHub.id = :hubId")
    int markAsReadAgencyHubAll(@Param("hubId") Long hubId, 
                               @Param("readByUserId") Long readByUserId, 
                               @Param("readAt") java.time.Instant readAt);

}
