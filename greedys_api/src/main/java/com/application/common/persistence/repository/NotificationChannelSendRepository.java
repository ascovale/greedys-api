package com.application.common.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.NotificationChannelSend;
import com.application.common.persistence.model.notification.NotificationChannelSend.ChannelType;

/**
 * Repository per NotificationChannelSend (LIVELLO 3)
 * 
 * Gestisce il tracking dell'invio delle notifiche via singoli canali (SMS, EMAIL, PUSH, etc).
 * Pattern: Channel Isolation - ogni canale è processato indipendentemente
 * 
 * ⭐ CHANNEL ISOLATION PATTERN:
 * - Una notifica ha N rows di NotificationChannelSend (una per canale)
 * - Ogni canale è inviato indipendentemente
 * - Se SMS fallisce, EMAIL/PUSH/etc continuano normalmente
 * - Retry granulare per singolo canale
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Channel Send Isolation)
 */
@Repository
public interface NotificationChannelSendRepository extends JpaRepository<NotificationChannelSend, Long> {

    /**
     * Trova tutti i canali PENDING per l'invio (is_sent IS NULL).
     * Usato dal ChannelPoller per selezionare i canali da inviare.
     * 
     * ⭐ IMPORTANTE: is_sent = NULL significa "non ancora inviato"
     * - NULL: Pending (non ancora tentato)
     * - true: Sent successfully
     * - false: Failed definitively (max retries)
     * 
     * @return Lista di NotificationChannelSend con is_sent=NULL
     */
    @Query("SELECT n FROM NotificationChannelSend n WHERE n.sent IS NULL ORDER BY n.createdAt ASC")
    List<NotificationChannelSend> findPending();

    /**
     * Trova un canale specifico per notificationId e channelType.
     * Usato per check se esiste già (per evitare duplicati).
     * 
     * @param notificationId L'ID della notifica
     * @param channelType Il tipo di canale (SMS, EMAIL, PUSH, etc)
     * @return Optional del NotificationChannelSend
     */
    Optional<NotificationChannelSend> findByNotificationIdAndChannelType(Long notificationId, ChannelType channelType);

    /**
     * Verifica se esiste un canale per una notifica e un channel type.
     * Usato da ChannelPoller per il check "CREATE IF NOT EXISTS".
     * 
     * @param notificationId L'ID della notifica
     * @param channelType Il tipo di canale
     * @return true se esiste, false altrimenti
     */
    boolean existsByNotificationIdAndChannelType(Long notificationId, ChannelType channelType);

    /**
     * Trova tutti i canali di una notifica specifica.
     * Usato per debug/analisi e per verificare se tutti i canali sono stati inviati.
     * 
     * @param notificationId L'ID della notifica
     * @return Lista di NotificationChannelSend per quella notifica
     */
    List<NotificationChannelSend> findByNotificationId(Long notificationId);

    /**
     * Verifica se TUTTI i canali di una notifica sono stati inviati.
     * Usato per marcare la NotificationOutbox come completata.
     * 
     * @param notificationId L'ID della notifica
     * @return true se tutti i canali hanno is_sent=true
     */
    @Query("SELECT COUNT(n) = 0 FROM NotificationChannelSend n WHERE n.notificationId = :notificationId AND (n.sent IS NULL OR n.sent = false)")
    boolean areAllChannelsSent(@Param("notificationId") Long notificationId);

    /**
     * Trova tutti i canali PENDING per un tipo specifico di canale.
     * Usato per batch sending (es: invia tutti gli SMS in parallelo).
     * 
     * @param channelType Il tipo di canale
     * @return Lista di NotificationChannelSend per quel tipo di canale
     */
    @Query("SELECT n FROM NotificationChannelSend n WHERE n.channelType = :channelType AND n.sent IS NULL ORDER BY n.createdAt ASC")
    List<NotificationChannelSend> findPendingByChannelType(@Param("channelType") ChannelType channelType);

    /**
     * Conta i canali PENDING.
     * Utile per monitoraggio e debug.
     * 
     * @return Numero di canali pending
     */
    @Query("SELECT COUNT(n) FROM NotificationChannelSend n WHERE n.sent IS NULL")
    long countPending();

    /**
     * Conta i canali FAILED (is_sent=false).
     * Utile per identificare problemi di consegna.
     * 
     * @return Numero di canali falliti
     */
    @Query("SELECT COUNT(n) FROM NotificationChannelSend n WHERE n.sent = false")
    long countFailed();

    /**
     * Aggiorna un canale a SENT con timestamp.
     * Usato da ChannelPoller dopo invio riuscito.
     * 
     * @param id L'ID del NotificationChannelSend
     * @param sentAt Timestamp dell'invio
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationChannelSend n SET n.sent = true, n.sentAt = :sentAt WHERE n.id = :id")
    void markAsSent(@Param("id") Long id, @Param("sentAt") Instant sentAt);

    /**
     * Aggiorna un canale a FAILED con messaggio di errore e incrementa attempt count.
     * Usato da ChannelPoller dopo invio fallito.
     * 
     * @param id L'ID del NotificationChannelSend
     * @param error Messaggio di errore
     * @param lastAttemptAt Timestamp dell'ultimo tentativo
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationChannelSend n SET n.sent = false, n.lastError = :error, n.lastAttemptAt = :lastAttemptAt, n.attemptCount = n.attemptCount + 1 " +
           "WHERE n.id = :id")
    void markAsFailed(@Param("id") Long id, @Param("error") String error, @Param("lastAttemptAt") Instant lastAttemptAt);

    /**
     * Incrementa l'attempt count senza marcare come failed.
     * Usato quando vogliamo ritentare senza salvare lo stato di fallimento (per evitare race conditions).
     * 
     * @param id L'ID del NotificationChannelSend
     * @param lastAttemptAt Timestamp dell'ultimo tentativo
     * @param error Messaggio di errore (opzionale, per logging)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationChannelSend n SET n.lastAttemptAt = :lastAttemptAt, n.lastError = :error, n.attemptCount = n.attemptCount + 1 " +
           "WHERE n.id = :id")
    void incrementAttempt(@Param("id") Long id, @Param("lastAttemptAt") Instant lastAttemptAt, @Param("error") String error);

    /**
     * Trova i canali FAILED che ancora possono essere ritentati.
     * Usato da ChannelPoller per il retry logic.
     * 
     * @param maxRetries Numero massimo di retry
     * @return Lista di NotificationChannelSend failed con attempt_count < maxRetries
     */
    @Query("SELECT n FROM NotificationChannelSend n WHERE n.sent = false AND n.attemptCount < :maxRetries ORDER BY n.lastAttemptAt ASC")
    List<NotificationChannelSend> findFailedWithRetryAvailable(@Param("maxRetries") int maxRetries);

    /**
     * Trova tutti gli ID di notifiche che hanno almeno un canale PENDING.
     * Usato da ChannelPoller per selezionare le notifiche da processare.
     * 
     * @return Set di notification IDs con canali pending
     */
    @Query(value = "SELECT DISTINCT n.notification_id FROM notification_channel_send n WHERE n.is_sent IS NULL", nativeQuery = true)
    Set<Long> findNotificationsWithPendingChannels();

    /**
     * Pulisce i canali più vecchi di X giorni (è_sent=true).
     * Da eseguire periodicamente per evitare che la tabella diventi troppo grande.
     * 
     * @param olderThanInstant Data limite
     * @return Numero di righe eliminate
     */
    @Modifying
    @Query("DELETE FROM NotificationChannelSend n WHERE n.sent = true AND n.sentAt < :olderThan")
    int deleteSentBefore(@Param("olderThan") Instant olderThanInstant);

    /**
     * Pulisce i canali falliti da molto tempo (is_sent=false e tentati N volte).
     * Da eseguire periodicamente per evitare accumuli di errori vecchi.
     * 
     * @param olderThanInstant Data limite
     * @return Numero di righe eliminate
     */
    @Modifying
    @Query("DELETE FROM NotificationChannelSend n WHERE n.sent = false AND n.lastAttemptAt < :olderThan AND n.attemptCount >= :maxRetries")
    int deleteFailedBefore(@Param("olderThan") Instant olderThanInstant, @Param("maxRetries") int maxRetries);
}
