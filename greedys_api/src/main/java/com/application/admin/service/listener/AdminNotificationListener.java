package com.application.admin.service.listener;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.application.admin.persistence.dao.AdminNotificationDAO;
import com.application.admin.persistence.model.AdminNotification;
import com.application.common.notification.service.NotificationWebSocketSender;
import com.application.common.service.notification.listener.BaseNotificationListener;
import com.application.common.service.notification.orchestrator.NotificationOrchestrator;
import com.application.common.service.notification.orchestrator.NotificationOrchestratorFactory;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;

/**
 * ⭐ REFACTORED ADMIN NOTIFICATION LISTENER
 * 
 * Ascolta sulla queue: notification.admin
 * 
 * FLUSSO (Two-Layer Pattern):
 * Layer 1 (Producer): EventOutboxOrchestrator publishes 1 generic message per admin
 * Layer 2 (Stream Processor - THIS CLASS):
 *   1. RabbitMQ invia message su queue notification.admin
 *   2. Listener riceve message (MANUAL ACK via BaseNotificationListener)
 *   3. Verifica idempotency: existsByEventId(eventId)
 *   4. Delega a AdminOrchestrator per disaggregazione
 *   5. AdminOrchestrator carica admin + preferenze + event rules
 *   6. Disaggrega per (admin × channel) calcolando Group ∩ User ∩ Event
 *   7. Applica incident tracking e audit trail
 *   8. Listener salva disaggregazioni nel DB
 *   9. ACK message (conferma a RabbitMQ)
 * 
 * ⭐ IMPORTANTE: Disaggregazione avviene IN-MEMORY (NOT su RabbitMQ)
 * - 1 RabbitMQ message → N DB records (solo nel listener)
 * - RabbitMQ message volume ottimizzato (95% reduction)
 * - Event-type rules applicate per admin (SYSTEM_ERROR=[EMAIL+SMS mandatory])
 * 
 * ⭐ ADMIN NOTIFICATIONS CHARACTERISTICS:
 * - Sempre individuali (readByAll=false)
 * - Alta priorità per ALERT, FRAUD, CRITICAL events
 * - Incident tracking enabled per system events
 * - SMS per critical security incidents
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Two-Layer Orchestration Pattern)
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationListener extends BaseNotificationListener<AdminNotification> {

	private final AdminNotificationDAO notificationDAO;
	private final NotificationOrchestratorFactory orchestratorFactory;
	private final NotificationWebSocketSender webSocketSender;

	@RabbitListener(
		queues = "notification.admin",
		ackMode = "MANUAL"
	)
	public void onNotificationMessage(
		@Payload Map<String, Object> message,
		@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
		Channel channel
	) {
		processNotificationMessage(message, deliveryTag, channel);
	}

	/**
	 * ⭐ IMPLEMENT ABSTRACT METHOD
	 * Ritorna AdminOrchestrator per disaggregazione evento
	 */
	@Override
	protected NotificationOrchestrator<AdminNotification> getTypeSpecificOrchestrator(Map<String, Object> message) {
		return orchestratorFactory.getOrchestrator("ADMIN");
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
	protected void persistNotification(AdminNotification notification) {
		notificationDAO.save(notification);
	}

	/**
	 * ⭐ IMPLEMENT ABSTRACT METHOD
	 * Attempt WebSocket send immediately after persist
	 */
	@Override
	protected void attemptWebSocketSend(AdminNotification notification) {
		if (notification.getChannel() != null && 
			notification.getChannel().toString().equals("WEBSOCKET")) {
			webSocketSender.sendAdminNotification(notification);
		}
	}
}
