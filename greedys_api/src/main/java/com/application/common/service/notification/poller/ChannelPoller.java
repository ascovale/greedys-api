package com.application.common.service.notification.poller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.NotificationChannelSendDAO;
import com.application.common.persistence.dao.RestaurantNotificationDAO;
import com.application.common.persistence.model.notification.NotificationChannelSend;
import com.application.common.persistence.model.notification.NotificationChannelSend.ChannelType;
import com.application.restaurant.persistence.model.RestaurantNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ LIVELLO 3: CHANNEL POLLER (CHANNEL ISOLATION PATTERN)
 * 
 * ⭐⭐⭐ ARCHITETTURA CRITICA ⭐⭐⭐
 * 
 * Responsabilità:
 * 1. Per ogni notifica PENDING
 * 2. Per ogni canale (SMS, EMAIL, PUSH, WEBSOCKET, SLACK)
 * 3. Crea UNA riga di NotificationChannelSend (se non esiste)
 * 4. Invia via il canale
 * 5. UPDATE is_sent indipendentemente
 * 
 * ⭐ CHANNEL ISOLATION PATTERN:
 * 
 * Vecchio (SBAGLIATO):
 *   Listener crea 5 channel_send (SMS, EMAIL, PUSH, WS, SLACK)
 *   Se listener muore dopo SMS: EMAIL non viene mai creato
 *   Se SMS fallisce: retry blocca tutti gli altri canali
 * 
 * Nuovo (CORRETTO):
 *   Listener crea SOLO notification_outbox (no channel_send)
 *   ChannelPoller crea ONE CHANNEL AT A TIME
 *   
 *   CICLO 1:
 *     - Check exists SMS? NO → CREATE SMS entry
 *     - Send SMS
 *     - UPDATE is_sent=true/false (per SMS SOLO)
 *   
 *   CICLO 2:
 *     - Check exists EMAIL? NO → CREATE EMAIL entry
 *     - Send EMAIL
 *     - UPDATE is_sent=true/false (per EMAIL SOLO)
 *   
 *   Se SMS fallisce: CICLO 1 retry SMS, CICLO 2 prosegue EMAIL normalmente
 *   Se EMAIL fallisce: CICLO 2 retry EMAIL, altri canali non affetti
 * 
 * ✅ VANTAGGI:
 * 1. Error Isolation: SMS crash non blocca EMAIL/PUSH/etc
 * 2. Granular Retry: Solo il canale che fallisce riprova
 * 3. No Batch Overhead: Un canale alla volta
 * 4. Observability: Puoi vedere quale canale ha problemi
 * 5. Scalability: Puoi processare canali in parallelo (future work)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Channel Isolation Implementation)
 */
@Slf4j
@Service
public class ChannelPoller {

    private static final int MAX_RETRIES = 3;

    private final NotificationChannelSendDAO channelSendDAO;
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChannelPoller(NotificationChannelSendDAO channelSendDAO,
                        RestaurantNotificationDAO restaurantNotificationDAO,
                        SimpMessagingTemplate simpMessagingTemplate) {
        this.channelSendDAO = channelSendDAO;
        this.restaurantNotificationDAO = restaurantNotificationDAO;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * Polling ogni 10 secondi per inviare notifiche via canali.
     * 
     * ⭐ TIMING:
     * - fixedDelay=10000: 10 secondi tra esecuzioni (più lento di outbox poller)
     * - initialDelay=4000: Attende 4 secondi prima della prima esecuzione
     * 
     * Questo garantisce che EventOutboxPoller e NotificationOutboxPoller
     * abbiano fatto il loro lavoro prima che ChannelPoller inizi.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 4000)
    public void pollAndSendChannels() {
        try {
            long pendingCount = channelSendDAO.countPending();

            if (pendingCount == 0) {
                log.debug("No pending channels to send");
                return;
            }

            log.info("Found {} pending channel sends to process", pendingCount);

            // Trova tutte le notifiche che hanno almeno un canale pending
            Set<Long> notificationIds = channelSendDAO.findNotificationsWithPendingChannels();

            if (notificationIds.isEmpty()) {
                return;
            }

            log.debug("Processing {} notifications with pending channels", notificationIds.size());

            // ⭐ CHANNEL ISOLATION LOOP
            for (Long notificationId : notificationIds) {
                // Per OGNI canale (uno alla volta)
                for (ChannelType channelType : ChannelType.values()) {
                    // ⭐ Skip direct channels in main loop (processed separately)
                    if (isDirectChannel(channelType)) {
                        processSingleChannel(notificationId, channelType);  // Processa DIRETTAMENTE
                        continue;
                    }
                    processSingleChannel(notificationId, channelType);
                }
            }

        } catch (Exception e) {
            log.error("Error in ChannelPoller.pollAndSendChannels", e);
        }
    }

    /**
     * Processa un SINGOLO canale per una SINGOLA notifica.
     * 
     * ⭐ CHANNEL ISOLATION:
     * - Se questo metodo fallisce: SOLO questo canale è affetto
     * - Altri canali continuano normalmente
     * - Retry è granulare per canale
     * 
     * ⭐ DIRECT CHANNELS (WEBSOCKET):
     * - Alcuni canali (WebSocket) NON hanno bisogno di NotificationOutbox
     * - Vengono inviati DIRETTAMENTE senza persistenza
     * - Questo riduce latenza e overhead
     * 
     * @param notificationId L'ID della notifica
     * @param channelType Il tipo di canale (SMS, EMAIL, PUSH, etc)
     */
    @Transactional
    private void processSingleChannel(Long notificationId, ChannelType channelType) {
        try {
            // ⭐ DIRECT CHANNELS: Salta il secondo Outbox
            if (isDirectChannel(channelType)) {
                log.debug("Processing direct channel (no Outbox required): notif={}, channel={}", 
                         notificationId, channelType);
                // Invia DIRETTAMENTE senza persistenza
                sendViaChannelDirect(notificationId, channelType);
                log.info("Direct channel sent successfully: notif={}, channel={}", notificationId, channelType);
                return;  // ← Esci, non creare NotificationChannelSend
            }

            // ⭐ PERSISTENT CHANNELS: Usa il secondo Outbox
            // Step 1: Check se esiste una riga per questo combo
            if (!channelSendDAO.existsByNotificationIdAndChannelType(notificationId, channelType)) {
                // Step 2: Se non esiste, crea SOLO per questo canale
                NotificationChannelSend send = new NotificationChannelSend();
                send.setNotificationId(notificationId);
                send.setChannelType(channelType);
                send.setSent(null);  // NULL = pending
                send.setAttemptCount(0);
                channelSendDAO.save(send);
                
                log.debug("Created channel send entry: notif={}, channel={}", notificationId, channelType);
            }

            // Step 3: Leggi l'entry per questo canale
            NotificationChannelSend send = channelSendDAO
                    .findByNotificationIdAndChannelType(notificationId, channelType)
                    .orElse(null);

            if (send == null) {
                log.warn("Channel send not found after creation: notif={}, channel={}", notificationId, channelType);
                return;
            }

            // Step 4: Se non è pending (già inviato/fallito), salta
            if (send.getSent() != null) {
                log.debug("Channel already processed: notif={}, channel={}, sent={}", 
                         notificationId, channelType, send.getSent());
                return;
            }

            // Step 5: Invia via il SINGOLO canale
            sendViaChannel(send);

            // Step 6: UPDATE is_sent=true (inviato con successo)
            channelSendDAO.markAsSent(send.getId(), Instant.now());
            log.info("Channel sent successfully: notif={}, channel={}", notificationId, channelType);

        } catch (Exception e) {
            log.error("Failed to send notification {} via channel {}", notificationId, channelType, e);

            // Skip error handling for direct channels (WebSocket)
            if (isDirectChannel(channelType)) {
                log.warn("Direct channel send failed (transient): notif={}, channel={}", notificationId, channelType);
                return;
            }

            // Cerca l'entry e aggiorna attempt count
            NotificationChannelSend send = channelSendDAO
                    .findByNotificationIdAndChannelType(notificationId, channelType)
                    .orElse(null);

            if (send != null) {
                // Incrementa attempt count
                channelSendDAO.incrementAttempt(send.getId(), Instant.now(), e.getMessage());

                // Se hai raggiunto max retries, marca come definitivamente fallito
                if (send.getAttemptCount() >= MAX_RETRIES) {
                    channelSendDAO.markAsFailed(send.getId(), 
                            "Max retries reached: " + e.getMessage(), 
                            Instant.now());
                    log.error("Channel marked as FAILED after {} attempts: notif={}, channel={}", 
                             MAX_RETRIES, notificationId, channelType);
                } else {
                    log.warn("Channel send attempt {}/{} failed, will retry: notif={}, channel={}", 
                            send.getAttemptCount(), MAX_RETRIES, notificationId, channelType);
                }
            }
        }
    }

    /**
     * Determina se un canale è DIRECT (non ha bisogno di persistenza nel secondo Outbox).
     * 
     * ⭐ DIRECT CHANNELS:
     * - WEBSOCKET: In-memory, nessuna persistenza necessaria
     * 
     * ⭐ PERSISTENT CHANNELS:
     * - SMS, EMAIL, PUSH, SLACK: Richiedono persistenza per garantire consegna
     * 
     * @param channelType Il tipo di canale
     * @return true se il canale è DIRECT
     */
    private boolean isDirectChannel(ChannelType channelType) {
        return channelType == ChannelType.WEBSOCKET;
    }

    /**
     * Invia una notifica via un canale DIRETTO (senza Outbox).
     * 
     * @param notificationId L'ID della notifica
     * @param channelType Il tipo di canale
     * @throws Exception Se l'invio fallisce
     */
    private void sendViaChannelDirect(Long notificationId, ChannelType channelType) throws Exception {
        switch (channelType) {
            case WEBSOCKET -> sendWebSocketDirect(notificationId);
            default -> throw new IllegalArgumentException("Channel " + channelType + " is not a direct channel");
        }
    }

    /**
     * Invia una notifica via WebSocket DIRETTAMENTE (senza persistenza).
     * 
     * ⭐ VANTAGGI:
     * - In-memory, latenza quasi zero
     * - Nessuna persistenza necessaria (browser è volatile)
     * - Client riceve immediatamente
     * 
     * ⭐ FLOW:
     * 1. Leggi RestaurantNotification dal DB
     * 2. Estrai userId (è il staff che riceve)
     * 3. Prepara payload JSON
     * 4. Invia via SimpMessagingTemplate a /user/{userId}/queue/notifications
     * 5. Browser riceve in tempo reale
     * 
     * @param notificationId L'ID della notifica
     * @throws Exception Se l'invio fallisce
     */
    private void sendWebSocketDirect(Long notificationId) throws Exception {
        try {
            // Step 1: Leggi la notifica dal DB
            Optional<RestaurantNotification> notifOpt = restaurantNotificationDAO.findById(notificationId);
            
            if (notifOpt.isEmpty()) {
                log.warn("RestaurantNotification not found: {}", notificationId);
                return;
            }
            
            RestaurantNotification notification = notifOpt.get();
            Long userId = notification.getUserId();
            
            if (userId == null) {
                log.warn("Notification {} has null userId, skipping WebSocket send", notificationId);
                return;
            }
            
            // Step 2: Prepara il payload per il client
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", notification.getId());
            payload.put("title", notification.getTitle());
            payload.put("body", notification.getBody());
            payload.put("timestamp", Instant.now().toString());
            payload.put("properties", notification.getProperties());
            
            log.debug("WebSocket payload prepared: notif={}, user={}, payload={}", 
                     notificationId, userId, payload);
            
            // Step 3: Invia via WebSocket a /user/{userId}/queue/notifications
            simpMessagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                payload
            );
            
            log.info("WebSocket message sent successfully: notif={}, user={}", notificationId, userId);
            
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification {}: {}", notificationId, e.getMessage(), e);
            // ⭐ NO RETRY for WebSocket (best-effort only)
            // Se il staff è offline: messaggio perso
            // Se il browser non è connesso: no consegna
            throw e;
        }
    }

    /**
     * Invia una notifica via un canale specifico (con Outbox).
     * 
     * Implementazione placeholder - verrà specializzata per ogni canale
     * 
     * @param send L'entry di NotificationChannelSend con i dettagli dell'invio
     * @throws Exception Se l'invio fallisce
     */
    private void sendViaChannel(NotificationChannelSend send) throws Exception {
        switch (send.getChannelType()) {
            case SMS -> sendSMS(send);
            case EMAIL -> sendEmail(send);
            case PUSH -> sendPush(send);
            case WEBSOCKET -> sendWebSocket(send);
            case SLACK -> sendSlack(send);
        }
    }

    private void sendSMS(NotificationChannelSend send) throws Exception {
        // TODO: Implementare SMS send logic
        // - Leggi NotificationChannelSend.notificationId
        // - Leggi notifica (AdminNotification, RestaurantNotification, etc)
        // - Leggi user phone number (da userId/userType)
        // - Chiama SMS gateway
        log.debug("TODO: Send SMS for notification {}", send.getNotificationId());
    }

    private void sendEmail(NotificationChannelSend send) throws Exception {
        // TODO: Implementare Email send logic
        // - Leggi NotificationChannelSend.notificationId
        // - Leggi notifica
        // - Leggi user email (da userId/userType)
        // - Chiama Email service
        log.debug("TODO: Send Email for notification {}", send.getNotificationId());
    }

    private void sendPush(NotificationChannelSend send) throws Exception {
        // TODO: Implementare Push send logic
        // - Leggi NotificationChannelSend.notificationId
        // - Leggi notifica
        // - Leggi user device tokens (da userId/userType)
        // - Chiama FCM/APNs
        log.debug("TODO: Send Push for notification {}", send.getNotificationId());
    }

    private void sendWebSocket(NotificationChannelSend send) throws Exception {
        // TODO: Implementare WebSocket send logic
        // - Leggi NotificationChannelSend.notificationId
        // - Leggi notifica
        // - Broadcast via WebSocket a userId
        log.debug("TODO: Send WebSocket for notification {}", send.getNotificationId());
    }

    private void sendSlack(NotificationChannelSend send) throws Exception {
        // TODO: Implementare Slack send logic
        // - Leggi NotificationChannelSend.notificationId
        // - Leggi notifica
        // - Invia a Slack channel
        log.debug("TODO: Send Slack for notification {}", send.getNotificationId());
    }

    /**
     * Metodo per il monitoring: conta i canali pending.
     */
    public long getPendingChannelCount() {
        return channelSendDAO.countPending();
    }

    /**
     * Metodo per il monitoring: conta i canali falliti.
     */
    public long getFailedChannelCount() {
        return channelSendDAO.countFailed();
    }
}
