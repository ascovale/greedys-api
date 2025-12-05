package com.application.common.service.notification.listener;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.application.common.persistence.dao.notification.SocialNotificationDAO;
import com.application.common.persistence.model.notification.SocialNotification;
import com.application.common.service.notification.dto.NotificationEventPayloadDTO;
import com.application.common.service.notification.orchestrator.NotificationOrchestrator;
import com.application.common.service.notification.orchestrator.SocialOrchestrator;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ SOCIAL NOTIFICATION LISTENER
 * 
 * Listener RabbitMQ per le code di notifica social ed eventi.
 * 
 * QUEUES:
 * - notification.social.feed: Nuovi post nel feed
 * - notification.social.events: Eventi restaurant (RSVP, reminder)
 * 
 * FLOW:
 * RabbitMQ → SocialNotificationListener → SocialOrchestrator → SocialNotificationDAO → WebSocket
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialNotificationListener extends BaseNotificationListener<SocialNotification> {

    private final SocialOrchestrator socialOrchestrator;
    private final SocialNotificationDAO socialNotificationDAO;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Listener per feed social (nuovi post, like, commenti, etc)
     */
    @RabbitListener(
        queues = "${rabbitmq.queue.notification.social.feed:notification.social.feed}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleSocialFeedNotification(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        log.info("📩 [SOCIAL-FEED] Received message: eventId={}, eventType={}", 
            payload.getEventId(), payload.getEventType());
        processNotificationMessage(payload, deliveryTag, channel);
    }

    /**
     * Listener per eventi (RSVP, reminder, cancellazioni)
     */
    @RabbitListener(
        queues = "${rabbitmq.queue.notification.social.events:notification.social.events}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleEventNotification(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        log.info("📩 [SOCIAL-EVENTS] Received message: eventId={}, eventType={}", 
            payload.getEventId(), payload.getEventType());
        processNotificationMessage(payload, deliveryTag, channel);
    }

    @Override
    protected NotificationOrchestrator<SocialNotification> getTypeSpecificOrchestrator(Map<String, Object> message) {
        return socialOrchestrator;
    }

    @Override
    protected void enrichMessageWithTypeSpecificFields(Map<String, Object> message, NotificationEventPayloadDTO payload) {
        // Social non necessita di arricchimento particolare
        if (payload.getData() != null) {
            message.put("payload", payload.getData());
        }
    }

    @Override
    protected boolean existsByEventId(String eventId) {
        return socialNotificationDAO.existsByEventId(eventId);
    }

    @Override
    protected void persistNotification(SocialNotification notification) {
        socialNotificationDAO.save(notification);
    }

    @Override
    protected void attemptWebSocketSend(SocialNotification notification) {
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
    protected boolean checkIfAllNotificationsExist(List<SocialNotification> notifications) {
        for (SocialNotification notification : notifications) {
            if (!socialNotificationDAO.existsByEventId(notification.getEventId())) {
                return false;
            }
        }
        return !notifications.isEmpty();
    }
}
