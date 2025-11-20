package com.application.common.persistence.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.notification.EventOutbox.Status;

/**
 * DAO per EventOutbox (LIVELLO 1)
 * 
 * Gestisce la persistenza degli eventi di dominio prima della pubblicazione su RabbitMQ.
 * Pattern: Transactional Outbox con idempotency check basato su processedBy
 */
public interface EventOutboxDAO extends JpaRepository<EventOutbox, Long> {

    /**
     * Trova tutti gli eventi con status PENDING.
     * Usato da EventOutboxPoller per selezionare gli eventi da pubblicare.
     */
    List<EventOutbox> findByStatus(Status status);

    /**
     * Trova un evento specifico per event_id.
     */
    Optional<EventOutbox> findByEventId(String eventId);

    /**
     * Verifica se un evento è già stato processato da un listener specifico.
     * Usato per l'idempotency check.
     */
    @Query("SELECT COUNT(e) > 0 FROM EventOutbox e WHERE e.eventId = :eventId AND e.processedBy = :processedBy")
    boolean existsByEventIdAndProcessedBy(@Param("eventId") String eventId, @Param("processedBy") String processedBy);

    /**
     * Aggiorna lo status di un evento a PROCESSED e imposta processedBy.
     * Operazione atomica: una sola riga UPDATE.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EventOutbox e SET e.status = 'PROCESSED', e.processedBy = :processedBy, e.processedAt = :processedAt " +
           "WHERE e.eventId = :eventId")
    void updateProcessedBy(@Param("eventId") String eventId, @Param("processedBy") String processedBy, @Param("processedAt") Instant processedAt);

    /**
     * Aggiorna lo status di un evento a FAILED.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EventOutbox e SET e.status = 'FAILED', e.errorMessage = :error, e.retryCount = e.retryCount + 1 " +
           "WHERE e.eventId = :eventId")
    void markAsFailed(@Param("eventId") String eventId, @Param("error") String error);

    /**
     * Conta gli eventi PENDING.
     */
    @Query("SELECT COUNT(e) FROM EventOutbox e WHERE e.status = 'PENDING'")
    long countPending();

    /**
     * Seleziona gli eventi FAILED che non hanno superato il numero di retry.
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<EventOutbox> findFailedWithRetryAvailable(@Param("maxRetries") int maxRetries);

    /**
     * Pulisce gli eventi PROCESSED più vecchi di X giorni.
     */
    @Modifying
    @Query("DELETE FROM EventOutbox e WHERE e.status = 'PROCESSED' AND e.processedAt < :olderThan")
    int deleteProcessedBefore(@Param("olderThan") Instant olderThanInstant);

    /**
     * Trova gli eventi PENDING creati negli ultimi N secondi (NUOVI).
     * Usato da EventOutboxPollerFast per processare subito i nuovi eventi.
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = :status AND e.createdAt >= :createdAfter ORDER BY e.createdAt ASC")
    List<EventOutbox> findByStatusAndCreatedAfter(@Param("status") Status status, @Param("createdAfter") Instant createdAfter);

    /**
     * Trova gli eventi PENDING creati prima di N secondi (VECCHI, stuck).
     * Usato da EventOutboxPollerSlow per fare retry su eventi rimasti bloccati.
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = :status AND e.createdAt < :createdBefore ORDER BY e.createdAt ASC")
    List<EventOutbox> findByStatusAndCreatedBefore(@Param("status") Status status, @Param("createdBefore") Instant createdBefore);

    /**
     * Trova gli eventi PENDING con limite di risultati.
     * Usato da EventOutboxOrchestrator per processare un batch di eventi per ciclo.
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<EventOutbox> findByStatusWithLimit(@Param("status") Status status, org.springframework.data.domain.Pageable pageable);

    /**
     * Overload helper per trovare con limit diretto
     */
    default List<EventOutbox> findByStatusWithLimit(Status status, int limit) {
        return findByStatusWithLimit(status, org.springframework.data.domain.PageRequest.of(0, limit));
    }
}
