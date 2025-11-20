package com.application.customer.service.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.customer.persistence.dao.CustomerNotificationDAO;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.customer.persistence.model.CustomerNotification.DeliveryStatus;
import com.application.customer.persistence.model.CustomerNotification.NotificationChannel;
import com.application.customer.persistence.model.CustomerNotification.NotificationPriority;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RABBITLISTENER PER CUSTOMER NOTIFICATIONS
 * 
 * Ascolta sulla queue: notification.customer
 * 
 * FLUSSO:
 * 1. RabbitMQ invia message su queue notification.customer
 * 2. Listener riceve message (MANUAL ACK)
 * 3. Verifica idempotency: existsByEventId(eventId)
 * 4. Disaggrega per (customer × channel)
 * 5. Crea CustomerNotification rows
 * 6. Salva disaggregazioni nel DB
 * 7. ACK message (conferma a RabbitMQ)
 * 
 * ⭐ IMPORTANTE: NO SHARED READ per customer
 * - Ogni customer è isolato
 * - readByAll è sempre false
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerNotificationListener {

    private final CustomerNotificationDAO notificationDAO;

    @RabbitListener(
        queues = "notification.customer",
        ackMode = "MANUAL"
    )
    @Transactional
    @Retryable(
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 1000)
    )
    public void onNotificationMessage(
        @Payload Map<String, Object> message,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        try {
            log.info("📩 CustomerNotificationListener: received message on queue notification.customer");
            
            // ⭐ ESTRAI DATI
            String eventId = (String) message.get("event_id");
            String eventType = (String) message.get("event_type");
            String aggregateType = (String) message.get("aggregate_type");
            Long customerId = ((Number) message.get("customer_id")).longValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            
            log.info("🔍 Processing event: eventId={}, eventType={}, customerId={}", 
                eventId, eventType, customerId);
            
            // ⭐ IDEMPOTENCY CHECK
            if (notificationDAO.existsByEventId(eventId)) {
                log.warn("⚠️  Duplicate eventId detected: {}. Skipping (already processed)", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // ⭐ CARICA CHANNELS ENABLED per customer
            // TODO: Iniettare CustomerPreferencesService
            java.util.List<NotificationChannel> enabledChannels = getEnabledChannelsStub();
            
            // ⭐ DISAGGREGA: Per ogni channel, crea 1 row
            int disaggregationCount = 0;
            for (NotificationChannel channel_enum : enabledChannels) {
                // Crea unique eventId per questa disaggregazione
                String disaggregatedEventId = generateDisaggregatedEventId(eventId, customerId, channel_enum);
                
                @SuppressWarnings("unchecked")
                Map<String, String> props = (Map<String, String>) payload.getOrDefault("properties", new HashMap<>());
                
                // Crea CustomerNotification row
                CustomerNotification notification = CustomerNotification.builder()
                    .eventId(disaggregatedEventId)
                    .userId(customerId)
                    .channel(channel_enum)
                    .status(DeliveryStatus.PENDING)
                    .readByAll(false)  // NO shared read per customer
                    .priority(NotificationPriority.NORMAL)
                    .title((String) payload.get("title"))
                    .body((String) payload.get("body"))
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .properties(props)
                    .build();
                
                notificationDAO.save(notification);
                disaggregationCount++;
            }
            
            log.info("✅ Successfully created {} disaggregated notifications for customerId={}", disaggregationCount, customerId);
            
            // ⭐ MANUAL ACK
            channel.basicAck(deliveryTag, false);
            log.info("✔️  Message ACK'd successfully");
            
        } catch (Exception e) {
            log.error("❌ Error processing notification message: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
                log.info("❌ Message NACK'd and requeued");
            } catch (Exception nackError) {
                log.error("Failed to NACK message", nackError);
            }
            throw new RuntimeException("Failed to process notification: " + e.getMessage(), e);
        }
    }

    /**
     * ⭐ GENERATE DISAGGREGATED EVENT ID
     */
    private String generateDisaggregatedEventId(String eventId, Long customerId, NotificationChannel channel) {
        return String.format("%s_%d_%s_%d", 
            eventId, 
            customerId, 
            channel.name(), 
            System.currentTimeMillis()
        );
    }

    /**
     * ⭐ STUB: Get enabled channels
     * TODO: Sostituire con CustomerPreferencesService
     */
    private java.util.List<NotificationChannel> getEnabledChannelsStub() {
        // Default: WEBSOCKET + EMAIL
        return java.util.List.of(NotificationChannel.WEBSOCKET, NotificationChannel.EMAIL);
    }
}
