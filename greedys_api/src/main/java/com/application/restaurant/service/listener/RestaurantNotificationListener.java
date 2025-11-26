package com.application.restaurant.service.listener;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.application.restaurant.persistence.dao.RestaurantUserNotificationDAO;
import com.application.restaurant.persistence.model.RestaurantUserNotification;
import com.application.common.notification.service.NotificationWebSocketSender;
import com.application.common.service.notification.listener.BaseNotificationListener;
import com.application.common.service.notification.dto.NotificationEventPayloadDTO;
import com.application.common.service.notification.orchestrator.NotificationOrchestrator;
import com.application.common.service.notification.orchestrator.NotificationOrchestratorFactory;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RABBITLISTENER PER RESTAURANT STAFF NOTIFICATIONS (REFACTORED)
 * 
 * Now extends BaseNotificationListener<T> to avoid code duplication.
 * 
 * Ascolta sulla queue: notification.restaurant
 * 
 * FLUSSO:
 * 1. RabbitMQ invia message su queue notification.restaurant
 * 2. @RabbitListener calls onNotificationMessage()
 * 3. BaseNotificationListener.processNotificationMessage() è chiamato
 *    ├─ Parse message
 *    ├─ Idempotency check
 *    ├─ Get RestaurantUserOrchestrator from factory
 *    ├─ Orchestrator disaggregates: 1 message → N notification records
 *    ├─ Listener salva N disaggregated records
 *    └─ ACK message
 * 
 * ⭐ IMPORTANTE: Disaggregation Logic
 * - BaseNotificationListener handles common flow
 * - RestaurantUserOrchestrator handles restaurant-specific disaggregation
 * - This listener just routes message to base class
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Refactored to use BaseNotificationListener)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantNotificationListener extends BaseNotificationListener<RestaurantUserNotification> {

    private final RestaurantUserNotificationDAO notificationDAO;
    private final NotificationOrchestratorFactory orchestratorFactory;
    private final NotificationWebSocketSender webSocketSender;

    /**
     * ⭐ MAIN LISTENER METHOD
     * 
     * @RabbitListener(queues="notification.restaurant", ackMode=MANUAL)
     * - Ascolta su queue notification.restaurant
     * - MANUAL ACK: BaseNotificationListener gestisce ACK/NACK
     * 
     * @Retryable(maxAttempts=3, delay=1000ms)
     * - Retry automatico su errore transiente
     * 
     * @Payload message: Map con dati evento
     *   {
     *     aggregate_type: "RESTAURANT" (or other),
     *     event_type: "RESERVATION_REQUESTED",
     *     restaurant_id: 5,
     *     event_id: "RES-REQ-12345",
     *     payload: {...}
     *   }
     * 
     * @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Per MANUAL ACK/NACK
     * @Param channel: RabbitMQ Channel per ACK/NACK
     */
    @RabbitListener(
        queues = "notification.restaurant",
        ackMode = "MANUAL"
    )
    @Retryable(
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 1000)
    )
    public void onNotificationMessage(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        // Delegate to base class for common processing
        processNotificationMessage(payload, deliveryTag, channel);
    }

    /**
     * ⭐ IMPLEMENTATION: Get type-specific orchestrator
     * 
     * Returns RestaurantUserOrchestrator which knows how to:
     * - Load restaurant staff as recipients
     * - Load staff notification preferences
     * - Load restaurant group settings
     * - Apply restaurant-specific business rules
     * 
     * @param message RabbitMQ message
     * @return RestaurantUserOrchestrator
     */
    @Override
    protected NotificationOrchestrator<RestaurantUserNotification> getTypeSpecificOrchestrator(
        Map<String, Object> message
    ) {
        return orchestratorFactory.getOrchestrator("RESTAURANT");
    }

    /**
     * ⭐ IMPLEMENTATION: Enrich message with RESTAURANT-specific fields
     * 
     * For RESTAURANT scope, adds "restaurant_id" from recipientId.
     * 
     * @param message Map to enrich
     * @param payload Original DTO
     */
    @Override
    protected void enrichMessageWithTypeSpecificFields(
        Map<String, Object> message,
        com.application.common.service.notification.dto.NotificationEventPayloadDTO payload
    ) {
        // For RESTAURANT: recipientId IS the restaurant_id
        message.put("restaurant_id", payload.getRecipientId());
    }

    /**
     * ⭐ IMPLEMENTATION: Check idempotency
     * 
     * Uses RestaurantUserNotificationDAO to check if eventId already exists.
     * 
     * @param eventId Event ID to check
     * @return true if already processed, false if new
     */
    @Override
    protected boolean existsByEventId(String eventId) {
        return notificationDAO.existsByEventId(eventId);
    }

    /**
     * ⭐ IMPLEMENTATION: Persist notification to database
     * 
     * Saves RestaurantUserNotification record.
     * 
     * @param notification Notification to persist
     */
    @Override
    protected void persistNotification(RestaurantUserNotification notification) {
        notificationDAO.save(notification);
    }

    /**
     * ⭐ IMPLEMENTATION: Attempt WebSocket send immediately after persist
     * 
     * WEBSOCKET DELIVERY STRATEGY (best-effort, no retry):
     * - If client is online → delivery succeeds, client receives immediately
     * - If client offline → send fails silently, NO RETRY
     * - If service crashes between persist and send → client doesn't receive (acceptable)
     * 
     * @param notification Notification to send via WebSocket
     */
    @Override
    protected void attemptWebSocketSend(RestaurantUserNotification notification) {
        // Only attempt send if channel is WEBSOCKET
        if (notification.getChannel() != null && 
            notification.getChannel().toString().equals("WEBSOCKET")) {
            webSocketSender.sendRestaurantNotification(notification);
        }
    }
}
