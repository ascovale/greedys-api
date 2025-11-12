package com.application.common.persistence.model.notification.channel;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.application.common.persistence.model.notification.websocket.WebSocketSessionManager;
import com.application.common.service.notification.model.NotificationMessage;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.restaurant.persistence.dao.RUserDAO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Canale di notifica via WebSocket per invio real-time.
 * Supporta routing multi-tipo utente: CUSTOMER, RESTAURANT, ADMIN
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationChannel implements NotificationChannel {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final CustomerDAO customerDAO;
    private final RUserDAO rUserDAO;

    @Override
    public void send(NotificationMessage message) {
        // 1. Recupera username (email) dal database in base al recipientType
        String username = getUsernameByRecipientTypeAndId(
                message.getRecipientType(),
                message.getRecipientId());

        if (username == null) {
            log.warn("⚠️ Cannot find username for recipientId={}, recipientType={}",
                    message.getRecipientId(), message.getRecipientType());
            return;
        }

        // 2. Verifica se l'utente è connesso
        if (!sessionManager.isUserConnected(username)) {
            log.debug("📡 User {} not connected via WebSocket, skipping", username);
            return;
        }

        // 3. Costruisci destination routing per tipo utente
        String destination = buildDestination(message);

        // 4. Invia messaggio via WebSocket all'utente specifico
        messagingTemplate.convertAndSendToUser(
                username, // ← EMAIL (da JWT)
                destination,
                message);

        log.info("✅ Sent WebSocket notification: username={}, type={}, destination={}",
                username, message.getType(), destination);
    }

    /**
     * Recupera username (email) in base al tipo di destinatario
     * 
     * @param recipientType CUSTOMER, RESTAURANT, ADMIN
     * @param recipientId   ID dell'entità
     * @return Email dell'utente o null se non trovato
     */
    private String getUsernameByRecipientTypeAndId(String recipientType, Long recipientId) {
        return switch (recipientType.toUpperCase()) {
            case "CUSTOMER" -> customerDAO.findById(recipientId)
                    .map(customer -> customer.getEmail())
                    .orElse(null);

            case "RESTAURANT" -> rUserDAO.findById(recipientId)
                    .map(rUser -> rUser.getEmail())
                    .orElse(null);

            case "ADMIN" -> {
                // TODO: Implementare AdminDAO quando disponibile
                log.warn("⚠️ Admin notifications not yet implemented");
                yield null;
            }

            default -> {
                log.error("❌ Unknown recipientType: {}", recipientType);
                yield null;
            }
        };
    }

    /**
     * Costruisce il destination path per il routing WebSocket
     * Formato: /queue/{recipientType}/notifications
     * Esempi:
     * - /queue/customer/notifications
     * - /queue/restaurant/notifications
     * - /queue/admin/notifications
     */
    private String buildDestination(NotificationMessage message) {
        String recipientTypePrefix = message.getRecipientType().toLowerCase();
        return String.format("/queue/%s/notifications", recipientTypePrefix);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WEBSOCKET;
    }

    @Override
    public boolean isAvailable() {
        return messagingTemplate != null;
    }

    @Override
    public int getPriority() {
        return 1; // Highest priority for real-time delivery
    }
}
