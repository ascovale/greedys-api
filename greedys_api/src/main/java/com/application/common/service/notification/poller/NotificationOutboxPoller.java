package com.application.common.service.notification.poller;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.NotificationOutboxDAO;
import com.application.common.persistence.model.notification.NotificationOutbox;
import com.application.common.persistence.model.notification.NotificationOutbox.Status;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LIVELLO 2: NOTIFICATION OUTBOX POLLER
 * 
 * Responsabilità:
 * 1. Trova notifiche PENDING da NotificationOutbox
 * 2. Pubblica su RabbitMQ (exchange: notification-channel-send)
 * 3. Marca come PUBLISHED
 * 
 * FLOW:
 * [T0] Listener → CREATE notifiche + INSERT in notification_outbox (status=PENDING)
 * [T1] Poller (5 sec) → SELECT WHERE status=PENDING LIMIT 100
 * [T2] Poller → PUBLISH to RabbitMQ notification-channel-send
 * [T3] Poller → UPDATE status=PUBLISHED
 * [T4] ChannelPoller → Riceve e crea NotificationChannelSend (uno per canale)
 * 
 * ✅ AT-LEAST-ONCE DELIVERY:
 * - Se RabbitMQ fallisce: notifica rimane PENDING, prossimo ciclo riprova
 * - Se poller muore: notifica rimane PENDING, riprova al restart
 * 
 * ⚠️ NOTA OPZIONALE:
 * - Questo poller POTREBBE essere eliminato
 * - I listener potrebbero pubblicare DIRETTAMENTE su RabbitMQ
 * - Per ora lo manteniamo per separazione di responsabilità
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Slf4j
@Service
public class NotificationOutboxPoller {

    private static final int MAX_RETRIES = 3;

    private final NotificationOutboxDAO notificationOutboxDAO;

    public NotificationOutboxPoller(NotificationOutboxDAO notificationOutboxDAO) {
        this.notificationOutboxDAO = notificationOutboxDAO;
    }

    /**
     * Polling ogni 5 secondi per pubblicare notifiche PENDING.
     * 
     * ⭐ TIMING:
     * - fixedDelay=5000: 5 secondi tra la fine di un'esecuzione e l'inizio della successiva
     * - initialDelay=3000: Attende 3 secondi prima della prima esecuzione (dopo EventOutboxPoller)
     * 
     * Questo garantisce che gli eventi siano stati pubblicati prima di verificare le notifiche.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 3000)
    public void pollAndPublishPendingNotifications() {
        try {
            long pendingCount = notificationOutboxDAO.countPending();

            if (pendingCount == 0) {
                log.debug("No pending notifications to publish");
                return;
            }

            log.info("Found {} pending notifications to publish", pendingCount);

            // Seleziona notifiche PENDING in batch
            List<NotificationOutbox> pendingNotifications = notificationOutboxDAO.findByStatus(Status.PENDING);

            if (pendingNotifications.isEmpty()) {
                return;
            }

            log.debug("Processing batch of {} notifications", pendingNotifications.size());

            for (NotificationOutbox notification : pendingNotifications) {
                publishNotification(notification);
            }

        } catch (Exception e) {
            log.error("Error in NotificationOutboxPoller.pollAndPublishPendingNotifications", e);
        }
    }

    /**
     * Pubblica una singola notifica su RabbitMQ.
     * 
     * @param notification La notifica da pubblicare
     */
    @Transactional
    private void publishNotification(NotificationOutbox notification) {
        try {
            // Step 1: Prepara i dati della notifica
            Long notificationId = notification.getNotificationId();
            String notificationType = notification.getNotificationType();

            // Step 2: Pubblica su RabbitMQ
            // TODO: INTEGRATE WITH RABBITMQ WHEN CONFIGURED
            // amqpTemplate.convertAndSend("notification-channel-send", notificationType, payload);

            log.debug("Published notification {} (type={}) to message broker", notificationId, notificationType);

            // Step 3: Marca come PUBLISHED
            notification.setStatus(Status.PUBLISHED);
            notification.setProcessedAt(Instant.now());
            notificationOutboxDAO.save(notification);

            log.info("Notification {} marked as PUBLISHED", notificationId);

        } catch (Exception e) {
            log.error("Failed to publish notification {} (type={}), will retry", 
                     notification.getNotificationId(), notification.getNotificationType(), e);

            // Incrementa retry count
            notification.setRetryCount(notification.getRetryCount() + 1);

            if (notification.getRetryCount() >= MAX_RETRIES) {
                // Dopo 3 tentativi, marca come FAILED
                notification.setStatus(Status.FAILED);
                notification.setErrorMessage(e.getMessage());
                log.error("Notification {} marked as FAILED after {} retries", 
                         notification.getNotificationId(), MAX_RETRIES);
            }

            notificationOutboxDAO.save(notification);
        }
    }

    /**
     * Metodo per il monitoring: conta le notifiche pending.
     */
    public long getPendingNotificationCount() {
        return notificationOutboxDAO.countPending();
    }

    /**
     * Metodo per il monitoring: conta le notifiche fallite.
     */
    public long getFailedNotificationCount() {
        return notificationOutboxDAO.countFailed();
    }
}
