package com.application.common.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.notification.EventOutbox.Status;

/**
 * Repository per EventOutbox (LIVELLO 1)
 * 
 * Gestisce la persistenza degli eventi di dominio prima della pubblicazione su RabbitMQ.
 * Pattern: Transactional Outbox con idempotency check basato su processedBy
 */
@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {

    /**
     * Trova tutti gli eventi con status PENDING.
     * Usato da EventOutboxPoller per selezionare gli eventi da pubblicare.
     * 
     * @return Lista di EventOutbox con status=PENDING
     */
    List<EventOutbox> findByStatus(Status status);

    /**
     * Trova un evento specifico per event_id.
     * 
     * @param eventId L'ID univoco dell'evento
     * @return Optional dell'EventOutbox
     */
    Optional<EventOutbox> findByEventId(String eventId);

    /**
     * Verifica se un evento è già stato processato da un listener specifico.
     * Usato per l'idempotency check: se processedBy è già set, l'evento è stato già processato.
     * 
     * @param eventId L'ID univoco dell'evento
     * @param processedBy Il nome del listener (es. 'ADMIN_LISTENER')
     * @return true se l'evento è stato già processato
     */
    @Query("SELECT COUNT(e) > 0 FROM EventOutbox e WHERE e.eventId = :eventId AND e.processedBy = :processedBy")
    boolean existsByEventIdAndProcessedBy(@Param("eventId") String eventId, @Param("processedBy") String processedBy);

    /**
     * Aggiorna lo status di un evento a PROCESSED e imposta processedBy.
     * Operazione atomica: una sola riga UPDATE.
     * 
     * @param eventId L'ID univoco dell'evento
     * @param processedBy Il nome del listener
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EventOutbox e SET e.status = 'PROCESSED', e.processedBy = :processedBy, e.processedAt = :processedAt " +
           "WHERE e.eventId = :eventId")
    void updateProcessedBy(@Param("eventId") String eventId, @Param("processedBy") String processedBy, @Param("processedAt") Instant processedAt);

    /**
     * Aggiorna lo status di un evento a FAILED.
     * Usato da EventOutboxPoller se la pubblicazione a RabbitMQ fallisce.
     * 
     * @param eventId L'ID univoco dell'evento
     * @param error Il messaggio di errore
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EventOutbox e SET e.status = 'FAILED', e.lastError = :error, e.failedAt = :failedAt, e.retryCount = e.retryCount + 1 " +
           "WHERE e.eventId = :eventId")
    void markAsFailed(@Param("eventId") String eventId, @Param("error") String error, @Param("failedAt") Instant failedAt);

    /**
     * Conta gli eventi PENDING.
     * Utile per il debug e monitoraggio.
     * 
     * @return Numero di eventi pending
     */
    @Query("SELECT COUNT(e) FROM EventOutbox e WHERE e.status = 'PENDING'")
    long countPending();

    /**
     * Conta gli eventi FAILED.
     * Utile per identificare problemi di consegna.
     * 
     * @return Numero di eventi failed
     */
    @Query("SELECT COUNT(e) FROM EventOutbox e WHERE e.status = 'FAILED'")
    long countFailed();

    /**
     * Seleziona gli eventi FAILED che non hanno superato il numero di retry.
     * Per il retry logic di EventOutboxPoller.
     * 
     * @param maxRetries Numero massimo di retry
     * @return Lista di EventOutbox failed che possono essere ritentati
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<EventOutbox> findFailedWithRetryAvailable(@Param("maxRetries") int maxRetries);

    /**
     * Pulisce gli eventi PROCESSED più vecchi di X giorni.
     * Da eseguire periodicamente per evitare che la tabella diventi troppo grande.
     * 
     * @param olderThanInstant Data limite
     * @return Numero di righe eliminate
     */
    @Modifying
    @Query("DELETE FROM EventOutbox e WHERE e.status = 'PROCESSED' AND e.processedAt < :olderThan")
    int deleteProcessedBefore(@Param("olderThan") Instant olderThanInstant);
}
