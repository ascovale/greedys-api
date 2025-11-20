package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.common.persistence.model.notification.NotificationOutbox;
import com.application.common.persistence.model.notification.NotificationOutbox.Status;

/**
 * DAO per NotificationOutbox
 * 
 * Gestisce il tracciamento dell'invio di notifiche via vari canali
 * (WebSocket, Push, Email, SMS)
 */
public interface NotificationOutboxDAO extends JpaRepository<NotificationOutbox, Long> {

    /**
     * Trova tutte le notifiche PENDING da inviare
     * 
     * Usato da NotificationChannelPoller per trovare lavoro
     */
    @Query("SELECT no FROM NotificationOutbox no WHERE no.status = :status ORDER BY no.createdAt ASC")
    List<NotificationOutbox> findPending(@Param("status") Status status);

    /**
     * Trova le N notifiche PENDING più vecchie
     * 
     * Usato dal poller per limitare batch processing
     */
    @Query(value = "SELECT * FROM notification_outbox WHERE status = :status ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<NotificationOutbox> findPendingWithLimit(@Param("status") String status, @Param("limit") int limit);

    /**
     * Conta le notifiche PENDING
     */
    @Query("SELECT COUNT(no) FROM NotificationOutbox no WHERE no.status = :status")
    long countByStatus(@Param("status") Status status);

    /**
     * Trova le notifiche di una specifica notification
     */
    @Query("SELECT no FROM NotificationOutbox no WHERE no.notificationId = :notificationId ORDER BY no.createdAt DESC")
    List<NotificationOutbox> findByNotificationId(@Param("notificationId") Long notificationId);

    /**
     * Trova le notifiche di un evento specifico
     */
    @Query("SELECT no FROM NotificationOutbox no WHERE no.eventType = :eventType AND no.aggregateId = :aggregateId ORDER BY no.createdAt DESC")
    List<NotificationOutbox> findByEventTypeAndAggregateId(@Param("eventType") String eventType, @Param("aggregateId") Long aggregateId);

    /**
     * Marca una notifica come inviata
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox no SET no.status = :status, no.sentAt = :sentAt WHERE no.id = :id")
    void markAsSent(@Param("id") Long id, @Param("status") Status status, @Param("sentAt") Instant sentAt);

    /**
     * Incrementa il retry count e aggiorna error message
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox no SET no.retryCount = no.retryCount + 1, no.errorMessage = :errorMessage WHERE no.id = :id")
    void incrementRetryCount(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    /**
     * Marca una notifica come FAILED dopo troppi retry
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox no SET no.status = :status, no.errorMessage = :errorMessage WHERE no.id = :id")
    void markAsFailed(@Param("id") Long id, @Param("status") Status status, @Param("errorMessage") String errorMessage);

    /**
     * Trova tutte le notifiche create DOPO un timestamp
     * 
     * Usato per analytics
     */
    @Query("SELECT no FROM NotificationOutbox no WHERE no.createdAt > :timestamp ORDER BY no.createdAt DESC")
    List<NotificationOutbox> findCreatedAfter(@Param("timestamp") Instant timestamp);

    /**
     * Conta i fallimenti per aggregateId (per debug)
     */
    @Query("SELECT COUNT(no) FROM NotificationOutbox no WHERE no.status = :status AND no.aggregateId = :aggregateId")
    long countFailedByAggregateId(@Param("status") Status status, @Param("aggregateId") Long aggregateId);

}
