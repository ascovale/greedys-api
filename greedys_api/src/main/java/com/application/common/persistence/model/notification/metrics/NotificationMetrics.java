package com.application.common.persistence.model.notification.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.application.common.persistence.model.notification.NotificationChannelSend.ChannelType;
import com.application.common.persistence.model.notification.messaging.model.NotificationEvent.NotificationType;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Metrics collector for notification system using Micrometer.
 * Integrates with Prometheus for monitoring and alerting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Record successful notification delivery.
     *
     * @param type Notification type (e.g., RESERVATION_CONFIRMED)
     * @param channel Delivery channel (e.g., WEBSOCKET, EMAIL, FIREBASE)
     */
    public void recordNotificationSent(NotificationType type, ChannelType channel) {
        Counter.builder("notifications.sent")
                .tag("type", type.name())
                .tag("channel", channel.name())
                .tag("status", "success")
                .description("Total notifications successfully sent")
                .register(meterRegistry)
                .increment();
        
        log.debug("📊 Metric recorded: notification sent - type={}, channel={}", type, channel);
    }

    /**
     * Record failed notification delivery.
     *
     * @param type Notification type
     * @param channel Delivery channel
     * @param reason Failure reason (e.g., "user_not_connected", "email_bounce")
     */
    public void recordNotificationFailed(NotificationType type, ChannelType channel, String reason) {
        Counter.builder("notifications.failed")
                .tag("type", type.name())
                .tag("channel", channel.name())
                .tag("reason", reason)
                .tag("status", "failed")
                .description("Total notifications that failed to send")
                .register(meterRegistry)
                .increment();
        
        log.warn("📊 Metric recorded: notification failed - type={}, channel={}, reason={}", 
                type, channel, reason);
    }

    /**
     * Record notification delivery latency.
     *
     * @param channel Delivery channel
     * @param durationMs Duration in milliseconds
     */
    public void recordNotificationLatency(ChannelType channel, long durationMs) {
        Timer.builder("notifications.latency")
                .tag("channel", channel.name())
                .description("Notification delivery latency")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        
        log.debug("📊 Metric recorded: notification latency - channel={}, duration={}ms", 
                channel, durationMs);
    }

    /**
     * Update WebSocket active sessions gauge.
     *
     * @param count Current number of active sessions
     */
    public void setWebSocketActiveSessions(int count) {
        meterRegistry.gauge("websocket.sessions.active", count);
        log.debug("📊 Metric updated: websocket active sessions - count={}", count);
    }

    /**
     * Record WebSocket connection event.
     *
     * @param userType User type (customer, restaurant, admin)
     */
    public void recordWebSocketConnection(String userType) {
        Counter.builder("websocket.connections")
                .tag("user_type", userType)
                .tag("event", "connected")
                .description("Total WebSocket connections")
                .register(meterRegistry)
                .increment();
        
        log.debug("📊 Metric recorded: websocket connection - userType={}", userType);
    }

    /**
     * Record WebSocket disconnection event.
     *
     * @param userType User type
     */
    public void recordWebSocketDisconnection(String userType) {
        Counter.builder("websocket.connections")
                .tag("user_type", userType)
                .tag("event", "disconnected")
                .description("Total WebSocket disconnections")
                .register(meterRegistry)
                .increment();
        
        log.debug("📊 Metric recorded: websocket disconnection - userType={}", userType);
    }

    /**
     * Record outbox message processing.
     *
     * @param status Status (published, failed, retrying)
     */
    public void recordOutboxProcessed(String status) {
        Counter.builder("outbox.messages.processed")
                .tag("status", status)
                .description("Total outbox messages processed")
                .register(meterRegistry)
                .increment();
        
        log.debug("📊 Metric recorded: outbox processed - status={}", status);
    }

    /**
     * Update outbox pending messages gauge.
     *
     * @param count Number of pending messages
     */
    public void setOutboxPendingMessages(int count) {
        meterRegistry.gauge("outbox.messages.pending", count);
        log.debug("📊 Metric updated: outbox pending - count={}", count);
    }

    /**
     * Record RabbitMQ message published.
     *
     * @param queue Queue name (e.g., customer, restaurant, admin)
     */
    public void recordRabbitMQPublished(String queue) {
        Counter.builder("rabbitmq.messages.published")
                .tag("queue", queue)
                .tag("status", "success")
                .description("Total RabbitMQ messages published")
                .register(meterRegistry)
                .increment();
        
        log.debug("📊 Metric recorded: rabbitmq published - queue={}", queue);
    }

    /**
     * Record RabbitMQ message consumption.
     *
     * @param queue Queue name
     */
    public void recordRabbitMQConsumed(String queue) {
        Counter.builder("rabbitmq.messages.consumed")
                .tag("queue", queue)
                .tag("status", "success")
                .description("Total RabbitMQ messages consumed")
                .register(meterRegistry)
                .increment();
        
        log.debug("📊 Metric recorded: rabbitmq consumed - queue={}", queue);
    }

    /**
     * Record RabbitMQ DLQ message (dead letter).
     *
     * @param queue Original queue name
     * @param reason Reason for DLQ (e.g., "max_retries_exceeded")
     */
    public void recordRabbitMQDLQ(String queue, String reason) {
        Counter.builder("rabbitmq.messages.dlq")
                .tag("queue", queue)
                .tag("reason", reason)
                .description("Total messages sent to DLQ")
                .register(meterRegistry)
                .increment();
        
        log.warn("📊 Metric recorded: rabbitmq DLQ - queue={}, reason={}", queue, reason);
    }
}
