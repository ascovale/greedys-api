package com.application.agency.service.listener;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.application.agency.persistence.dao.AgencyUserNotificationDAO;
import com.application.agency.persistence.model.AgencyUserNotification;
import com.application.common.notification.service.NotificationWebSocketSender;
import com.application.common.service.notification.listener.BaseNotificationListener;
import com.application.common.service.notification.dto.NotificationEventPayloadDTO;
import com.application.common.service.notification.orchestrator.NotificationOrchestrator;
import com.application.common.service.notification.orchestrator.NotificationOrchestratorFactory;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;

/**
 * ⭐ REFACTORED AGENCY USER NOTIFICATION LISTENER
 * 
 * Ascolta sulla queue: notification.agency
 * 
 * FLUSSO (Two-Layer Pattern):
 * Layer 1 (Producer): EventOutboxOrchestrator publishes 1 generic message per agency
 * Layer 2 (Stream Processor - THIS CLASS):
 *   1. RabbitMQ invia message su queue notification.agency
 *   2. Listener riceve message (MANUAL ACK via BaseNotificationListener)
 *   3. Verifica idempotency: existsByEventId(eventId)
 *   4. Delega a AgencyUserOrchestrator per disaggregazione
 *   5. AgencyUserOrchestrator carica staff + preferenze + event rules
 *   6. Disaggrega per (staff × channel) calcolando Group ∩ User ∩ Event
 *   7. Applica priority-based routing (HIGH → managers only, NORMAL → all agents)
 *   8. Listener salva disaggregazioni nel DB
 *   9. ACK message (conferma a RabbitMQ)
 * 
 * ⭐ IMPORTANTE: Disaggregazione avviene IN-MEMORY (NOT su RabbitMQ)
 * - 1 RabbitMQ message → N DB records (solo nel listener)
 * - RabbitMQ message volume ottimizzato (95% reduction)
 * - Event-type rules applicate per staff (BOOKING_REQUEST=[WS mandatory])
 * 
 * ⭐ PRIORITY-BASED ROUTING:
 * - HIGH priority events: notifiche solo ai managers dell'agenzia
 * - NORMAL priority: notifiche a tutti gli agenti attivi
 * - Implementato in AgencyUserOrchestrator
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Two-Layer Orchestration Pattern)
 */
@Service
@RequiredArgsConstructor
public class AgencyUserNotificationListener extends BaseNotificationListener<AgencyUserNotification> {

	private final AgencyUserNotificationDAO notificationDAO;
	private final NotificationOrchestratorFactory orchestratorFactory;
	private final NotificationWebSocketSender webSocketSender;

	@RabbitListener(
		queues = "notification.agency",
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
	 * Ritorna AgencyUserOrchestrator per disaggregazione evento
	 */
	@Override
	protected NotificationOrchestrator<AgencyUserNotification> getTypeSpecificOrchestrator(Map<String, Object> message) {
		return orchestratorFactory.getOrchestrator("AGENCY");
	}

	/**
	 * ⭐ IMPLEMENTATION: Enrich message with AGENCY-specific fields
	 * 
	 * For AGENCY scope, uses recipientId if available, otherwise uses agencyId from DTO.
	 * 
	 * @param message Map to enrich
	 * @param payload Original DTO
	 */
	@Override
	protected void enrichMessageWithTypeSpecificFields(
		Map<String, Object> message,
		com.application.common.service.notification.dto.NotificationEventPayloadDTO payload
	) {
		// For AGENCY: prefer recipientId if available, otherwise use agencyId from DTO
		if (payload.getRecipientId() != null) {
			message.put("agency_id", payload.getRecipientId());
		}
		// Note: If recipientId is null, agency_id was already added by BaseNotificationListener
		// from DTO.agencyId, so we don't overwrite it here
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
	protected void persistNotification(AgencyUserNotification notification) {
		notificationDAO.save(notification);
	}

	/**
	 * ⭐ IMPLEMENT ABSTRACT METHOD
	 * Attempt WebSocket send immediately after persist
	 */
	@Override
	protected void attemptWebSocketSend(AgencyUserNotification notification) {
		if (notification.getChannel() != null && 
			notification.getChannel().toString().equals("WEBSOCKET")) {
			webSocketSender.sendAgencyNotification(notification);
		}
	}
}
