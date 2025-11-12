package com.application.common.persistence.model.notification.messaging.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.application.common.config.RabbitMQConfig;
import com.application.common.persistence.model.notification.messaging.model.NotificationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitNotificationEventPublisher implements NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(NotificationEvent event) {
        String routingKey = buildRoutingKey(event);
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NOTIFICATIONS, 
            routingKey, 
            event
        );
        
        log.debug("Published event {} to {}", event.getEventId(), routingKey);
    }

    /**
     * Costruisce routing key per RabbitMQ Topic Exchange.
     * Pattern: notification.{recipientType}.{eventName}
     * 
     * Esempi:
     * - notification.customer.reservation_confirmed
     * - notification.customer.chat_message
     * - notification.restaurant.reservation_cancelled
     * - notification.admin.system_alert
     * 
     * @param event NotificationEvent contenente aggregateType e type
     * @return routing key (es: "notification.admin.system_alert")
     */
    private String buildRoutingKey(NotificationEvent event) {
        // ✅ FIX: Mappatura esplicita per TUTTI i recipientType
        String audience = switch (event.getAggregateType().toUpperCase()) {
            case "CUSTOMER" -> "customer";
            case "RESTAURANT", "RESTAURANT_OWNER", "RESTAURANT_STAFF" -> "restaurant";
            case "ADMIN", "ADMIN_USER" -> "admin";
            case "AGENCY", "AGENCY_USER" -> "agency";
            default -> {
                log.error("❌ Unknown aggregateType: {}", event.getAggregateType());
                throw new IllegalArgumentException(
                    "Unknown aggregateType: " + event.getAggregateType()
                );
            }
        };
        
        String eventName = event.getType().name().toLowerCase();
        
        String routingKey = String.format("notification.%s.%s", audience, eventName);
        
        log.debug("🔑 Routing key: {} (aggregateType={})", routingKey, event.getAggregateType());
        
        return routingKey;
    }
}
