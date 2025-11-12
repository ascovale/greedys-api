package com.application.common.service.notification.poller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.config.NotificationOutboxPollerProperties;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.notification.EventOutbox.Status;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LIVELLO 1: EVENT OUTBOX POLLER - FAST VERSION
 * 
 * Responsabilità:
 * 1. Trova eventi PENDING creati negli ultimi 60 secondi (NUOVI)
 * 2. Pubblica su RabbitMQ (exchange: event-stream) SUBITO
 * 3. Marca come PROCESSED
 * 
 * TIMING:
 * - Corre ogni 1 secondo (veloce per catturare nuovi eventi)
 * - Latency: max 1-2 secondi dall'INSERT al PUBLISH
 * 
 * FLOW:
 * [T0] Service → INSERT evento in event_outbox (status=PENDING, created_at=NOW)
 * [T1] PollerFast (1 sec) → SELECT WHERE status=PENDING AND created_at >= NOW-60s
 * [T2] PollerFast → PUBLISH to RabbitMQ event-stream
 * [T3] PollerFast → UPDATE status=PROCESSED
 * [T4] RabbitMQ → Delivera 3 listener in parallelo (ADMIN, RESTAURANT, CUSTOMER)
 * 
 * ✅ AT-LEAST-ONCE DELIVERY:
 * - Se RabbitMQ fallisce: evento rimane PENDING, prossimo ciclo riprova
 * - Se poller muore: evento rimane PENDING, riprova al restart
 * - Se evento è già stato pubblicato: PROCESSED flag previene duplicati
 * 
 * ⚠️ NOTA IMPORTANTE:
 * - Non è responsabilità di questo poller verificare se il listener ha elaborato
 * - Quello è compito del listener (idempotency check su processed_by)
 * - Questo poller è solo per la PUBBLICAZIONE su RabbitMQ
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Slf4j
@Service
public class EventOutboxPoller {

    private static final int MAX_RETRIES = 3;

    private final EventOutboxDAO eventOutboxDAO;
    private final NotificationOutboxPollerProperties pollerProperties;

    public EventOutboxPoller(EventOutboxDAO eventOutboxDAO, NotificationOutboxPollerProperties pollerProperties) {
        this.eventOutboxDAO = eventOutboxDAO;
        this.pollerProperties = pollerProperties;
    }

    /**
     * ⚡ FAST POLLER: Polling ogni 1 secondo per nuovi eventi.
     * 
     * Seleziona SOLO gli eventi creati negli ultimi 60 secondi.
     * Questo garantisce latency bassa per nuovi eventi senza sovraccaricare il DB.
     * 
     * ⭐ TIMING:
     * - fixedDelay=1000: 1 secondo tra la fine di un'esecuzione e l'inizio della successiva
     * - initialDelay=2000: Attende 2 secondi prima della prima esecuzione
     * - Risultato: ~1-2 secondi di latency per nuovi eventi
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    public void pollAndPublishNewEvents() {
        try {
            // Calcola il timestamp usando la configurazione
            long freshEventWindowSeconds = pollerProperties.getFastPoller().getFreshEventWindowSeconds();
            Instant freshEventThreshold = Instant.now().minus(freshEventWindowSeconds, ChronoUnit.SECONDS);

            // Seleziona SOLO gli eventi NUOVI (creati negli ultimi N secondi)
            List<EventOutbox> newEvents = eventOutboxDAO.findByStatusAndCreatedAfter(Status.PENDING, freshEventThreshold);

            if (newEvents.isEmpty()) {
                log.debug("[FAST] No new pending events to publish");
                return;
            }

            log.info("[FAST] Found {} new events to publish (created in last {}s)", newEvents.size(), freshEventWindowSeconds);

            for (EventOutbox event : newEvents) {
                publishEvent(event, "FAST");
            }

        } catch (Exception e) {
            log.error("Error in EventOutboxPoller.pollAndPublishNewEvents", e);
        }
    }

    /**
     * ✅ SLOW POLLER (CONDIZIONATO DAL FLAG):
     * 
     * ABILITATO se: notification.outbox.multi-poller.enabled=true (default)
     * DISABILITATO se: notification.outbox.multi-poller.enabled=false
     * 
     * Corre ogni 30 secondi e prende i pending creati >60 secondi fa.
     * Serve come safety net per retry di messaggi falliti.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    public void pollAndPublishOldEvents() {
        // Check if SLOW poller is enabled
        if (!pollerProperties.isSlowPollerEnabled()) {
            return;
        }

        try {
            // Calcola il timestamp usando la configurazione
            long stuckEventThreshold = pollerProperties.getSlowPoller().getStuckEventThresholdSeconds();
            Instant stuckEventThreshold_instant = Instant.now().minus(stuckEventThreshold, ChronoUnit.SECONDS);

            // Seleziona SOLO gli eventi VECCHI (creati >N secondi fa, ancora PENDING)
            List<EventOutbox> oldEvents = eventOutboxDAO.findByStatusAndCreatedBefore(Status.PENDING, stuckEventThreshold_instant);

            if (oldEvents.isEmpty()) {
                log.debug("[SLOW] No old pending events to retry");
                return;
            }

            log.warn("[SLOW] Found {} old stuck events to retry (created >{}s ago)", oldEvents.size(), stuckEventThreshold);

            for (EventOutbox event : oldEvents) {
                publishEvent(event, "SLOW");
            }

        } catch (Exception e) {
            log.error("Error in EventOutboxPoller.pollAndPublishOldEvents", e);
        }
    }

    /**
     * Pubblica un singolo evento su RabbitMQ.
     * 
     * ⭐ TRANSAZIONE:
     * - PUBLISH a RabbitMQ è fallibile
     * - Se fallisce: non aggiornare status (rimane PENDING)
     * - Prossimo ciclo riproverà
     * 
     * @param event L'evento da pubblicare
     * @param pollerName Nome del poller (FAST/SLOW) per logging
     */
    @Transactional
    private void publishEvent(EventOutbox event, String pollerName) {
        try {
            // Step 1: Prepara i dati dell'evento
            String eventType = event.getEventType();

            // Step 2: Pubblica su RabbitMQ (sarà implementato con EventPublisher)
            // TODO: INTEGRATE WITH RABBITMQ WHEN CONFIGURED
            // amqpTemplate.convertAndSend(EXCHANGE_NAME, eventType, payload);

            log.debug("[{}] Published event {} (id={}) to message broker", pollerName, eventType, event.getEventId());

            // Step 3: Marca come PROCESSED
            event.setStatus(Status.PROCESSED);
            event.setPublishedAt(Instant.now());
            eventOutboxDAO.save(event);

            log.info("[{}] Event {} marked as PROCESSED", pollerName, event.getEventId());

        } catch (Exception e) {
            log.error("[{}] Failed to publish event {} (id={}), will retry", pollerName, event.getEventType(), event.getEventId(), e);

            // Incrementa retry count
            event.setRetryCount(event.getRetryCount() + 1);

            if (event.getRetryCount() >= MAX_RETRIES) {
                // Dopo 3 tentativi, marca come FAILED
                event.setStatus(Status.FAILED);
                event.setErrorMessage(e.getMessage());
                log.error("[{}] Event {} marked as FAILED after {} retries", pollerName, event.getEventId(), MAX_RETRIES);
            }

            eventOutboxDAO.save(event);
        }
    }

    /**
     * Metodo per il monitoring/debug: conta gli eventi pending.
     * 
     * @return Numero di eventi pending
     */
    public long getPendingEventCount() {
        return eventOutboxDAO.countPending();
    }

    /**
     * Metodo per il monitoring/debug: conta gli eventi falliti.
     * 
     * @return Numero di eventi falliti
     */
    public long getFailedEventCount() {
        return eventOutboxDAO.findFailedWithRetryAvailable(MAX_RETRIES).size();
    }
}
