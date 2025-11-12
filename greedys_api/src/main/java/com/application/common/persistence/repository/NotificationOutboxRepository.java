package com.application.common.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.NotificationOutbox;
import com.application.common.persistence.model.notification.NotificationOutbox.Status;

/**
 * Repository per NotificationOutbox (LIVELLO 2)
 * 
 * Gestisce la persistenza delle notifiche prima della pubblicazione ai channel poller.
 * Pattern: Transactional Outbox
 * 
 * ⭐ NOTE IMPORTANTI:
 * - Un evento → N listener → N notification_outbox rows
 * - Es: ReservationRequestedEvent → AdminNotificationListener crea 3 AdminNotification + 3 notification_outbox rows
 * - ChannelPoller legge questo e crea NotificationChannelSend (uno per canale)
 */
@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    /**
     * Trova tutte le notifiche con status PENDING.
     * Usato da NotificationOutboxPoller per selezionare le notifiche da pubblicare a RabbitMQ.
     * 
     * @return Lista di NotificationOutbox con status=PENDING
     */
    List<NotificationOutbox> findByStatus(Status status);

    /**
     * Trova una notifica specifica per id.
     * 
     * @param notificationId L'ID della notifica
     * @return Optional della NotificationOutbox
     */
    Optional<NotificationOutbox> findByNotificationId(Long notificationId);

    /**
     * Trova tutte le notifiche di un aggregate specifico.
     * Utile per debug/analisi.
     * 
     * @param aggregateType Tipo di aggregato (RESERVATION, CUSTOMER, etc)
     * @param aggregateId ID dell'aggregato
     * @return Lista di NotificationOutbox per quel aggregato
     */
    List<NotificationOutbox> findByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);

    /**
     * Conta le notifiche PENDING.
     * Utile per monitoraggio e debug.
     * 
     * @return Numero di notifiche pending
     */
    @Query("SELECT COUNT(n) FROM NotificationOutbox n WHERE n.status = 'PENDING'")
    long countPending();

    /**
     * Conta le notifiche FAILED.
     * Utile per identificare problemi di consegna.
     * 
     * @return Numero di notifiche failed
     */
    @Query("SELECT COUNT(n) FROM NotificationOutbox n WHERE n.status = 'FAILED'")
    long countFailed();

    /**
     * Aggiorna lo status di una notifica a PUBLISHED.
     * 
     * @param notificationId L'ID della notifica
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox n SET n.status = 'PUBLISHED', n.processedAt = :processedAt " +
           "WHERE n.notificationId = :notificationId")
    void updatePublished(@Param("notificationId") Long notificationId, @Param("processedAt") Instant processedAt);

    /**
     * Aggiorna lo status di una notifica a FAILED con messaggio di errore.
     * 
     * @param notificationId L'ID della notifica
     * @param error Messaggio di errore
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox n SET n.status = 'FAILED', n.errorMessage = :error, n.retryCount = n.retryCount + 1 " +
           "WHERE n.notificationId = :notificationId")
    void markAsFailed(@Param("notificationId") Long notificationId, @Param("error") String error);

    /**
     * Seleziona le notifiche FAILED che non hanno superato il numero di retry.
     * Per il retry logic di NotificationOutboxPoller.
     * 
     * @param maxRetries Numero massimo di retry
     * @return Lista di NotificationOutbox failed che possono essere ritentate
     */
    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries ORDER BY n.createdAt ASC")
    List<NotificationOutbox> findFailedWithRetryAvailable(@Param("maxRetries") int maxRetries);

    /**
     * Pulisce le notifiche PUBLISHED più vecchie di X giorni.
     * Da eseguire periodicamente per evitare che la tabella diventi troppo grande.
     * 
     * @param olderThanInstant Data limite
     * @return Numero di righe eliminate
     */
    @Modifying
    @Query("DELETE FROM NotificationOutbox n WHERE n.status = 'PUBLISHED' AND n.processedAt < :olderThan")
    int deletePublishedBefore(@Param("olderThan") Instant olderThanInstant);
}
