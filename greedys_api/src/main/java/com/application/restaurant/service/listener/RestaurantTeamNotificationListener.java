package com.application.restaurant.service.listener;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
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
 * ⭐ RABBITLISTENER PER RESTAURANT TEAM NOTIFICATIONS
 * 
 * Now extends BaseNotificationListener<T> to avoid code duplication.
 * 
 * KEY DIFFERENCE FROM RestaurantNotificationListener:
 * - Listens to: "notification.restaurant.reservations" (TEAM queue)
 * - Scope: TEAM notifications (all staff see same notification)
 * - Orchestrator: "RESTAURANT_TEAM" (sets read_by_all=true)
 * 
 * MESSAGE FLOW:
 * 1. EventOutboxOrchestrator routes RESERVATION_NEW/MODIFY/CANCEL initiated by CUSTOMER
 *    → to "notification.restaurant.reservations" queue (TEAM scope)
 * 2. RestaurantTeamNotificationListener receives message from queue
 * 3. BaseNotificationListener.processNotificationMessage() è chiamato
 *    ├─ Parse message
 *    ├─ Idempotency check
 *    ├─ Get RestaurantTeamOrchestrator from factory
 *    ├─ Orchestrator disaggregates: 1 message → N notification records (all read_by_all=true)
 *    ├─ Listener salva N disaggregated records
 *    ├─ Attempts WebSocket delivery to team channel: /topic/restaurant/{id}/reservations
 *    └─ ACK message
 * 
 * EXAMPLE:
 * Input: RESERVATION_NEW from notification.restaurant.reservations queue
 * Processing:
 *   - Load ALL 10 restaurant staff (no preference filtering)
 *   - Create 20 notifications (10 staff × 2 channels) with read_by_all=true
 *   - Send WebSocket to /topic/restaurant/{restaurantId}/reservations (team channel)
 * 
 * @author Greedy's System
 * @since 2025-01-21 (Created for team-scoped RESERVATION notifications)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantTeamNotificationListener extends BaseNotificationListener<RestaurantUserNotification> {

    private final RestaurantUserNotificationDAO notificationDAO;
    private final NotificationOrchestratorFactory orchestratorFactory;
    private final NotificationWebSocketSender webSocketSender;

    /**
     * ⭐ MAIN LISTENER METHOD FOR TEAM NOTIFICATIONS
     * 
     * @RabbitListener(queues="notification.restaurant.reservations", ackMode=MANUAL)
     * - Ascolta su queue notification.restaurant.reservations (TEAM scope)
     * - MANUAL ACK: BaseNotificationListener gestisce ACK/NACK
     * 
     * @Retryable(maxAttempts=3, delay=1000ms)
     * - Retry automatico su errore transiente
     * 
     * @param message Map con dati evento (TEAM-scoped RESERVATION notification)
     * @param deliveryTag Per MANUAL ACK/NACK
     * @param channel RabbitMQ Channel per ACK/NACK
     */
    @RabbitListener(
        queues = "notification.restaurant.reservations",
        ackMode = "MANUAL"
    )
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public void onTeamNotificationMessage(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        log.info("🏢👥 RestaurantTeamNotificationListener: Received TEAM notification from queue");
        
        // Delegate to base class for common processing
        processNotificationMessage(payload, deliveryTag, channel);
    }

    /**
     * ⭐ IMPLEMENTATION: Get type-specific orchestrator
     * 
     * Returns RestaurantTeamOrchestrator which knows how to:
     * - Load ALL restaurant staff (no filtering for team scope)
     * - Create notifications with read_by_all=true (team shared)
     * - Set team WebSocket destination: /topic/restaurant/{id}/reservations
     * 
     * @param message RabbitMQ message
     * @return RestaurantTeamOrchestrator
     */
    @Override
    protected NotificationOrchestrator<RestaurantUserNotification> getTypeSpecificOrchestrator(
        Map<String, Object> message
    ) {
        return orchestratorFactory.getOrchestrator("RESTAURANT_TEAM");
    }

    /**
     * ⭐ IMPLEMENTATION: Enrich message with TEAM-specific fields
     * 
     * For TEAM scope, adds "restaurant_id" from recipientId.
     * TEAM notifications target a specific restaurant, all staff see them.
     * 
     * @param message Map to enrich
     * @param payload Original DTO
     */
    @Override
    protected void enrichMessageWithTypeSpecificFields(
        Map<String, Object> message,
        com.application.common.service.notification.dto.NotificationEventPayloadDTO payload
    ) {
        // For TEAM: recipientId IS the restaurant_id
        message.put("restaurant_id", payload.getRecipientId());
    }

    /**
     * ⭐ IMPLEMENTATION: Check idempotency
     * 
     * Uses RestaurantUserNotificationDAO to check if eventId already exists.
     * Idempotency check prevents duplicate notifications if event is reprocessed.
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
     * Saves RestaurantUserNotification record with read_by_all=true.
     * Uses notification_restaurant_user table with UNIQUE(eventId) constraint for idempotency.
     * 
     * @param notification Notification to persist (from TEAM orchestrator)
     */
    @Override
    protected void persistNotification(RestaurantUserNotification notification) {
        notificationDAO.save(notification);
    }

    /**
     * ⭐ IMPLEMENTATION: Attempt WebSocket send immediately after persist
     * 
     * TEAM WEBSOCKET DELIVERY:
     * - Sends to TEAM channel: /topic/restaurant/{restaurantId}/reservations
     * - All staff subscribed to team channel receive the same notification
     * - Best-effort delivery (no retry if client offline)
     * 
     * @param notification Notification to send via WebSocket (has destination set)
     */
    @Override
    protected void attemptWebSocketSend(RestaurantUserNotification notification) {
        // Only attempt send if channel is WEBSOCKET
        if (notification.getChannel() != null && 
            notification.getChannel().toString().equals("WEBSOCKET")) {
            log.debug("📤 Sending TEAM notification to WebSocket: destination={}", 
                notification.getProperties().get("destination"));
            webSocketSender.sendRestaurantNotification(notification);
        }
    }
}
