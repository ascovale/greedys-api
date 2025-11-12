package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.common.persistence.model.notification.NotificationChannelSend;
import com.application.common.persistence.model.notification.NotificationChannelSend.ChannelType;

/**
 * DAO per NotificationChannelSend (LIVELLO 3)
 * 
 * Gestisce il tracking dell'invio delle notifiche via singoli canali (SMS, EMAIL, PUSH, etc).
 * Pattern: Channel Isolation - ogni canale è processato indipendentemente
 */
public interface NotificationChannelSendDAO extends JpaRepository<NotificationChannelSend, Long> {

    /**
     * Trova tutti i canali PENDING per l'invio (is_sent IS NULL).
     * Usato dal ChannelPoller per selezionare i canali da inviare.
     */
    @Query("SELECT n FROM NotificationChannelSend n WHERE n.sent IS NULL ORDER BY n.createdAt ASC")
    List<NotificationChannelSend> findPending();

    /**
     * Trova un canale specifico per notificationId e channelType.
     */
    Optional<NotificationChannelSend> findByNotificationIdAndChannelType(Long notificationId, ChannelType channelType);

    /**
     * Verifica se esiste un canale per una notifica e un channel type.
     */
    boolean existsByNotificationIdAndChannelType(Long notificationId, ChannelType channelType);

    /**
     * Trova tutti i canali di una notifica specifica.
     */
    List<NotificationChannelSend> findByNotificationId(Long notificationId);

    /**
     * Verifica se TUTTI i canali di una notifica sono stati inviati.
     */
    @Query("SELECT COUNT(n) = 0 FROM NotificationChannelSend n WHERE n.notificationId = :notificationId AND (n.sent IS NULL OR n.sent = false)")
    boolean areAllChannelsSent(@Param("notificationId") Long notificationId);

    /**
     * Trova tutti i canali PENDING per un tipo specifico di canale.
     */
    @Query("SELECT n FROM NotificationChannelSend n WHERE n.channelType = :channelType AND n.sent IS NULL ORDER BY n.createdAt ASC")
    List<NotificationChannelSend> findPendingByChannelType(@Param("channelType") ChannelType channelType);

    /**
     * Conta i canali PENDING.
     */
    @Query("SELECT COUNT(n) FROM NotificationChannelSend n WHERE n.sent IS NULL")
    long countPending();

    /**
     * Conta i canali FAILED (is_sent=false).
     */
    @Query("SELECT COUNT(n) FROM NotificationChannelSend n WHERE n.sent = false")
    long countFailed();

    /**
     * Aggiorna un canale a SENT con timestamp.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationChannelSend n SET n.sent = true, n.sentAt = :sentAt WHERE n.id = :id")
    void markAsSent(@Param("id") Long id, @Param("sentAt") Instant sentAt);

    /**
     * Aggiorna un canale a FAILED con messaggio di errore e incrementa attempt count.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationChannelSend n SET n.sent = false, n.lastError = :error, n.lastAttemptAt = :lastAttemptAt, n.attemptCount = n.attemptCount + 1 " +
           "WHERE n.id = :id")
    void markAsFailed(@Param("id") Long id, @Param("error") String error, @Param("lastAttemptAt") Instant lastAttemptAt);

    /**
     * Incrementa l'attempt count senza marcare come failed.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationChannelSend n SET n.lastAttemptAt = :lastAttemptAt, n.lastError = :error, n.attemptCount = n.attemptCount + 1 " +
           "WHERE n.id = :id")
    void incrementAttempt(@Param("id") Long id, @Param("lastAttemptAt") Instant lastAttemptAt, @Param("error") String error);

    /**
     * Trova i canali FAILED che ancora possono essere ritentati.
     */
    @Query("SELECT n FROM NotificationChannelSend n WHERE n.sent = false AND n.attemptCount < :maxRetries ORDER BY n.lastAttemptAt ASC")
    List<NotificationChannelSend> findFailedWithRetryAvailable(@Param("maxRetries") int maxRetries);

    /**
     * Trova tutti gli ID di notifiche che hanno almeno un canale PENDING.
     */
    @Query(value = "SELECT DISTINCT n.notification_id FROM notification_channel_send n WHERE n.is_sent IS NULL", nativeQuery = true)
    Set<Long> findNotificationsWithPendingChannels();

    /**
     * Pulisce i canali più vecchi di X giorni (is_sent=true).
     */
    @Modifying
    @Query("DELETE FROM NotificationChannelSend n WHERE n.sent = true AND n.sentAt < :olderThan")
    int deleteSentBefore(@Param("olderThan") Instant olderThanInstant);

    /**
     * Pulisce i canali falliti da molto tempo.
     */
    @Modifying
    @Query("DELETE FROM NotificationChannelSend n WHERE n.sent = false AND n.lastAttemptAt < :olderThan AND n.attemptCount >= :maxRetries")
    int deleteFailedBefore(@Param("olderThan") Instant olderThanInstant, @Param("maxRetries") int maxRetries);
}
