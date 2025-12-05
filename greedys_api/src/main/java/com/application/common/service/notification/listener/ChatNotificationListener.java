package com.application.common.service.notification.listener;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.application.common.persistence.dao.notification.ChatNotificationDAO;
import com.application.common.persistence.model.notification.ChatNotification;
import com.application.common.service.notification.dto.NotificationEventPayloadDTO;
import com.application.common.service.notification.orchestrator.ChatOrchestrator;
import com.application.common.service.notification.orchestrator.NotificationOrchestrator;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ CHAT NOTIFICATION LISTENER
 * 
 * Listener RabbitMQ per le code di notifica chat.
 * 
 * QUEUES:
 * - notification.chat.direct: Chat 1-1
 * - notification.chat.group: Chat di gruppo
 * - notification.chat.support: Chat di supporto
 * - notification.chat.reservation: Chat legate a prenotazioni
 * 
 * FLOW:
 * RabbitMQ → ChatNotificationListener → ChatOrchestrator → ChatNotificationDAO → WebSocket
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationListener extends BaseNotificationListener<ChatNotification> {

    private final ChatOrchestrator chatOrchestrator;
    private final ChatNotificationDAO chatNotificationDAO;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Listener per chat dirette (1-1)
     */
    @RabbitListener(
        queues = "${rabbitmq.queue.notification.chat.direct:notification.chat.direct}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleDirectChatNotification(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        log.info("📩 [CHAT-DIRECT] Received message: eventId={}", payload.getEventId());
        processNotificationMessage(payload, deliveryTag, channel);
    }

    /**
     * Listener per chat di gruppo
     */
    @RabbitListener(
        queues = "${rabbitmq.queue.notification.chat.group:notification.chat.group}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleGroupChatNotification(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        log.info("📩 [CHAT-GROUP] Received message: eventId={}", payload.getEventId());
        processNotificationMessage(payload, deliveryTag, channel);
    }

    /**
     * Listener per chat di supporto
     */
    @RabbitListener(
        queues = "${rabbitmq.queue.notification.chat.support:notification.chat.support}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleSupportChatNotification(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        log.info("📩 [CHAT-SUPPORT] Received message: eventId={}", payload.getEventId());
        processNotificationMessage(payload, deliveryTag, channel);
    }

    /**
     * Listener per chat di prenotazione
     */
    @RabbitListener(
        queues = "${rabbitmq.queue.notification.chat.reservation:notification.chat.reservation}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleReservationChatNotification(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        log.info("📩 [CHAT-RESERVATION] Received message: eventId={}", payload.getEventId());
        processNotificationMessage(payload, deliveryTag, channel);
    }

    @Override
    protected NotificationOrchestrator<ChatNotification> getTypeSpecificOrchestrator(Map<String, Object> message) {
        return chatOrchestrator;
    }

    @Override
    protected void enrichMessageWithTypeSpecificFields(Map<String, Object> message, NotificationEventPayloadDTO payload) {
        // Chat non necessita di arricchimento particolare
        // I dati sono già nel payload
        if (payload.getData() != null) {
            message.put("payload", payload.getData());
        }
    }

    @Override
    protected boolean existsByEventId(String eventId) {
        return chatNotificationDAO.existsByEventId(eventId);
    }

    @Override
    protected void persistNotification(ChatNotification notification) {
        chatNotificationDAO.save(notification);
    }

    @Override
    protected void attemptWebSocketSend(ChatNotification notification) {
        try {
            String destination = notification.getWebSocketDestination();
            if (destination != null && !destination.isEmpty()) {
                messagingTemplate.convertAndSend(destination, notification);
                log.debug("📡 WebSocket sent to {}: {}", destination, notification.getEventId());
            }
        } catch (Exception e) {
            // Best-effort: log but don't throw
            log.warn("⚠️ WebSocket send failed (best-effort): {}", e.getMessage());
        }
    }

    @Override
    protected boolean checkIfAllNotificationsExist(List<ChatNotification> notifications) {
        for (ChatNotification notification : notifications) {
            if (!chatNotificationDAO.existsByEventId(notification.getEventId())) {
                return false;
            }
        }
        return !notifications.isEmpty();
    }
}
