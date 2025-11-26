package com.application.customer.service.listener;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.application.common.notification.service.NotificationWebSocketSender;
import com.application.common.service.notification.listener.BaseNotificationListener;
import com.application.common.service.notification.dto.NotificationEventPayloadDTO;
import com.application.common.service.notification.orchestrator.NotificationOrchestrator;
import com.application.common.service.notification.orchestrator.NotificationOrchestratorFactory;
import com.application.customer.persistence.dao.CustomerNotificationDAO;
import com.application.customer.persistence.model.CustomerNotification;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;

/**
 * ⭐ REFACTORED CUSTOMER NOTIFICATION LISTENER
 * 
 * Ascolta sulla queue: notification.customer
 * 
 * FLUSSO (Two-Layer Pattern):
 * Layer 1 (Producer): EventOutboxOrchestrator publishes 1 generic message per customer
 * Layer 2 (Stream Processor - THIS CLASS):
 *   1. RabbitMQ invia message su queue notification.customer
 *   2. Listener riceve message (MANUAL ACK via BaseNotificationListener)
 *   3. Verifica idempotency: existsByEventId(eventId)
 *   4. Delega a CustomerOrchestrator per disaggregazione
 *   5. CustomerOrchestrator carica customer + preferenze + event rules
 *   6. Disaggrega per (customer × channel) calcolando Group ∩ User ∩ Event
 *   7. Listener salva disaggregazioni nel DB
 *   8. ACK message (conferma a RabbitMQ)
 * 
 * ⭐ IMPORTANTE: Disaggregazione avviene IN-MEMORY (NOT su RabbitMQ)
 * - 1 RabbitMQ message → N DB records (solo nel listener)
 * - RabbitMQ message volume ottimizzato (95% reduction)
 * - Event-type rules applicate per customer (RESERVATION_CONFIRMED=[EMAIL mandatory])
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Two-Layer Orchestration Pattern)
 */
@Service
@RequiredArgsConstructor
public class CustomerNotificationListener extends BaseNotificationListener<CustomerNotification> {

    private final CustomerNotificationDAO notificationDAO;
    private final NotificationOrchestratorFactory orchestratorFactory;
    private final NotificationWebSocketSender webSocketSender;

    @RabbitListener(
        queues = "notification.customer",
        ackMode = "MANUAL"
    )
    public void onNotificationMessage(
        @Payload NotificationEventPayloadDTO payload,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        Channel channel
    ) {
        processNotificationMessage(payload, deliveryTag, channel);
    }

    /**
     * ⭐ IMPLEMENT ABSTRACT METHOD
     * Ritorna CustomerOrchestrator per disaggregazione evento
     */
    @Override
    protected NotificationOrchestrator<CustomerNotification> getTypeSpecificOrchestrator(Map<String, Object> message) {
        return orchestratorFactory.getOrchestrator("CUSTOMER");
    }

    /**
     * ⭐ IMPLEMENTATION: Enrich message with CUSTOMER-specific fields
     * 
     * For CUSTOMER scope, adds "customer_id" from recipientId.
     * 
     * @param message Map to enrich
     * @param payload Original DTO
     */
    @Override
    protected void enrichMessageWithTypeSpecificFields(
        Map<String, Object> message,
        com.application.common.service.notification.dto.NotificationEventPayloadDTO payload
    ) {
        // For CUSTOMER: recipientId IS the customer_id
        message.put("customer_id", payload.getRecipientId());
    }

    /**
     * ⭐ IMPLEMENT ABSTRACT METHOD
     * Verifica idempotency: se eventId già processato
     */
    @Override
    protected boolean existsByEventId(String eventId) {
        return notificationDAO.existsByEventId(eventId);
    }

    /**
     * ⭐ IMPLEMENT ABSTRACT METHOD
     * Persiste lista di disaggregazioni nel DB
     */
    @Override
    protected void persistNotification(CustomerNotification notification) {
        notificationDAO.save(notification);
    }

    /**
     * ⭐ IMPLEMENT ABSTRACT METHOD
     * Attempt WebSocket send immediately after persist
     */
    @Override
    protected void attemptWebSocketSend(CustomerNotification notification) {
        if (notification.getChannel() != null && 
            notification.getChannel().toString().equals("WEBSOCKET")) {
            webSocketSender.sendCustomerNotification(notification);
        }
    }
}
