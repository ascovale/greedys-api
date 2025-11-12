package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.common.persistence.model.notification.NotificationOutbox;
import com.application.common.persistence.model.notification.NotificationOutbox.Status;

/**
 * DAO per NotificationOutbox (LIVELLO 2)
 * 
 * Gestisce la persistenza delle notifiche prima della pubblicazione ai channel poller.
 * Pattern: Transactional Outbox
 */
public interface NotificationOutboxDAO extends JpaRepository<NotificationOutbox, Long> {

    /**
     * Trova tutte le notifiche con status PENDING.
     * Usato da NotificationOutboxPoller per selezionare le notifiche da pubblicare a RabbitMQ.
     */
    List<NotificationOutbox> findByStatus(Status status);

    /**
     * Trova una notifica specifica per id.
     */
    Optional<NotificationOutbox> findByNotificationId(Long notificationId);

    /**
     * Trova tutte le notifiche di un aggregate specifico.
     */
    List<NotificationOutbox> findByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);

    /**
     * Conta le notifiche PENDING.
     */
    @Query("SELECT COUNT(n) FROM NotificationOutbox n WHERE n.status = 'PENDING'")
    long countPending();

    /**
     * Conta le notifiche FAILED.
     */
    @Query("SELECT COUNT(n) FROM NotificationOutbox n WHERE n.status = 'FAILED'")
    long countFailed();

    /**
     * Aggiorna lo status di una notificaa PUBLISHED.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox n SET n.status = 'PUBLISHED', n.processedAt = :processedAt " +
           "WHERE n.notificationId = :notificationId")
    void updatePublished(@Param("notificationId") Long notificationId, @Param("processedAt") Instant processedAt);

    /**
     * Aggiorna lo status di una notifica a FAILED con messaggio di errore.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox n SET n.status = 'FAILED', n.errorMessage = :error, n.retryCount = n.retryCount + 1 " +
           "WHERE n.notificationId = :notificationId")
    void markAsFailed(@Param("notificationId") Long notificationId, @Param("error") String error);

    /**
     * Seleziona le notifiche FAILED che non hanno superato il numero di retry.
     */
    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries ORDER BY n.createdAt ASC")
    List<NotificationOutbox> findFailedWithRetryAvailable(@Param("maxRetries") int maxRetries);

    /**
     * Pulisce le notifiche PUBLISHED più vecchie di X giorni.
     */
    @Modifying
    @Query("DELETE FROM NotificationOutbox n WHERE n.status = 'PUBLISHED' AND n.processedAt < :olderThan")
    int deletePublishedBefore(@Param("olderThan") Instant olderThanInstant);
    
    /**
     * Trova le notifiche PENDING o FAILED che possono essere riprocessate.
     * Per il fallback scheduler in OutboxPublisher.
     */
    @Query("SELECT n FROM NotificationOutbox n WHERE " +
           "((n.status = 'PENDING' AND n.createdAt < :cutoff) OR " +
           "(n.status = 'FAILED' AND n.retryCount < :maxRetries)) " +
           "ORDER BY n.createdAt ASC")
    List<NotificationOutbox> findPendingOrFailedWithRetryAvailable(
            @Param("cutoff") Instant cutoff,
            @Param("maxRetries") int maxRetries,
            PageRequest pageRequest);
    
    /**
     * Elimina le notifiche PUBLISHED più vecchie di una data specifica.
     * Per il cleanup scheduler in OutboxPublisher.
     */
    @Modifying
    @Query("DELETE FROM NotificationOutbox n WHERE n.status = 'PUBLISHED' AND n.processedAt < :olderThan")
    int deleteOldPublishedMessages(@Param("olderThan") Instant olderThan);
}
