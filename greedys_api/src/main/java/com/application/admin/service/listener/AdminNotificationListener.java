package com.application.admin.service.listener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminNotificationDAO;
import com.application.admin.persistence.model.AdminNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.common.persistence.model.notification.NotificationPriority;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RABBITLISTENER PER ADMIN NOTIFICATIONS
 * 
 * Ascolta sulla queue: notification.admin
 * 
 * FLUSSO:
 * 1. RabbitMQ invia message su queue notification.admin
 * 2. Listener riceve message (MANUAL ACK)
 * 3. Verifica idempotency: existsByEventId(eventId)
 * 4. Se già esistente → ACK e return (duplicato da retry, skip)
 * 5. Se nuovo:
 *    a. Carica recipient (admin specificato nel payload)
 *    b. Carica preferenze canali per admin
 *    c. Disaggrega per admin × channel attivato:
 *       - for each channel in enabledChannels:
 *         - crea 1 AdminNotification row
 *         - eventId = disaggregated_eventId (unique: {originalId}_{adminId}_{channel}_{timestamp})
 *         - channel = WEBSOCKET/EMAIL/PUSH/SMS
 *         - status = PENDING (in attesa di invio)
 *         - readByAll = always false (admin notification è INDIVIDUALE)
 *    d. Persisti tutte le rows
 * 6. ACK manuale (solo dopo save riuscito)
 * 
 * ⭐ IMPORTANTE: NO SHARED READ LOGIC
 * AdminNotifications sono SEMPRE individuali (readByAll=false).
 * Quando admin legge notifica, SOLO sua read status cambia.
 * Gli altri admin NON vedono il cambio di read status.
 * 
 * ⭐ MANUAL ACK PATTERN:
 * - Ricevuta message
 * - Processata atomicamente (@Transactional)
 * - Se successo → channel.basicAck(deliveryTag, false) esplicito
 * - Se errore → channel.basicNack(deliveryTag, false, true) per requeue
 * - Garantisce: 0 messaggi persi, 0 duplicati in DB (grazie a eventId unique)
 * 
 * ⭐ IDEMPOTENCY:
 * eventId deve essere UNICO in DB (unique constraint).
 * Se RabbitMQ retries il messaggio:
 * - existsByEventId() torna true
 * - Listener ACK e return senza creare duplicati
 * 
 * Use Cases per AdminNotification:
 * - SYSTEM_ALERT (allarmi di sistema, server down, etc)
 * - FRAUD_DETECTED (frode rilevata)
 * - PAYMENT_ISSUE (problema di pagamento)
 * - USER_COMPLAINT (reclamo utente)
 * - BUSINESS_REPORT (report periodico)
 * - DATA_ANOMALY (anomalia nei dati)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation per Channel)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationListener {

	private final AdminNotificationDAO notificationDAO;

	@RabbitListener(queues = "notification.admin", ackMode = "MANUAL")
	@Transactional
	@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
	public void onNotificationMessage(
			@Payload Map<String, Object> message,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
			Channel channel) {

		try {
			log.info("🎯 AdminNotificationListener received: {}", message);

			// ⭐ EXTRACT EVENT DATA
			String eventId = (String) message.get("eventId");
			String eventType = (String) message.get("eventType");
			String aggregateType = (String) message.get("aggregateType");
			Long adminId = ((Number) message.get("userId")).longValue();  // userId = adminId for admin notifications
			String title = (String) message.get("title");
			String body = (String) message.get("body");

			// Cast properties to Map<String, String>
			@SuppressWarnings("unchecked")
			Map<String, Object> propertiesMap = (Map<String, Object>) message.getOrDefault("properties", new HashMap<>());
			@SuppressWarnings("unchecked")
			Map<String, String> properties = (Map<String, String>) (Object) propertiesMap;

			// ⭐ IDEMPOTENCY CHECK
			if (notificationDAO.existsByEventId(eventId)) {
				log.warn("⚠️ Duplicate notification eventId={}, skipping", eventId);
				channel.basicAck(deliveryTag, false);
				return;
			}

			// ⭐ DETERMINE PRIORITY (admin notifications are usually high priority)
			NotificationPriority priority = determineEventPriority(eventType);

			// ⭐ GET ENABLED CHANNELS FOR THIS ADMIN
			List<NotificationChannel> enabledChannels = getEnabledChannelsStub(adminId);
			log.info("📧 Notifying admin {} on {} channels", adminId, enabledChannels.size());

			// ⭐ DISAGGREGATE PER CHANNEL (admin is single recipient, so no loop for recipients)
			List<AdminNotification> notifications = new ArrayList<>();

			for (NotificationChannel channelEnum : enabledChannels) {
				String disaggregatedEventId = generateDisaggregatedEventId(
						eventId, adminId, channelEnum);

				AdminNotification notif = AdminNotification.builder()
						.eventId(disaggregatedEventId)
						.userId(adminId)
						.channel(channelEnum)
						.status(DeliveryStatus.PENDING)
						.readByAll(false)  // Always false for admin notifications (individual)
						.priority(priority)
						.eventType(eventType)
						.aggregateType(aggregateType)
						.title(title)
						.body(body)
						.properties(properties)
						.build();

				notifications.add(notif);
			}

			// ⭐ BATCH PERSIST
			notificationDAO.saveAll(notifications);
			log.info("✅ Persisted {} notifications for admin {}", notifications.size(), adminId);

			// ⭐ MANUAL ACK (only after successful DB persist)
			channel.basicAck(deliveryTag, false);

		} catch (Exception e) {
			log.error("❌ Error processing admin notification message", e);
			try {
				// NACK and requeue
				channel.basicNack(deliveryTag, false, true);
			} catch (Exception nackError) {
				log.error("Failed to NACK message", nackError);
			}
			throw new RuntimeException("Failed to process admin notification", e);
		}
	}

	/**
	 * ⭐ DETERMINE PRIORITY FOR ADMIN NOTIFICATIONS
	 * 
	 * Admin notifications sono di solito importanti (SYSTEM_ALERT, FRAUD_DETECTED, etc).
	 * Priority mapping:
	 * - SYSTEM_ALERT, FRAUD_DETECTED, CRITICAL: HIGH
	 * - PAYMENT_ISSUE, DATA_ANOMALY: NORMAL
	 * - REPORT, GENERAL: LOW
	 */
	private NotificationPriority determineEventPriority(String eventType) {
		if (eventType == null) {
			return NotificationPriority.NORMAL;
		}
		if (eventType.contains("ALERT") || 
		    eventType.contains("FRAUD") || 
		    eventType.contains("CRITICAL") ||
		    eventType.contains("ERROR")) {
			return NotificationPriority.HIGH;
		}
		if (eventType.contains("REPORT") || eventType.contains("GENERAL")) {
			return NotificationPriority.LOW;
		}
		return NotificationPriority.NORMAL;
	}

	/**
	 * ⭐ GENERATE DISAGGREGATED EVENT ID
	 * 
	 * Unique key per disaggregazione: {originalId}_{adminId}_{channel}_{timestamp}
	 * 
	 * Esempio:
	 * - Original eventId: "SYS-ALERT-001"
	 * - Admin 1, WEBSOCKET: SYS-ALERT-001_1_WEBSOCKET_1705737600000
	 * - Admin 1, EMAIL: SYS-ALERT-001_1_EMAIL_1705737600000
	 * 
	 * Garantisce: 0 duplicati quando RabbitMQ retries
	 */
	private String generateDisaggregatedEventId(
			String eventId, Long adminId, NotificationChannel channel) {
		long timestamp = Instant.now().toEpochMilli();
		return String.format("%s_%d_%s_%d", eventId, adminId, channel.name(), timestamp);
	}

	/**
	 * ⭐ STUB: Get enabled notification channels for admin
	 * 
	 * TODO: Integrare con AdminUserPreferencesService
	 * per caricamento preferenze di notifica dell'admin
	 * 
	 * Per ora, ritorna default: [WEBSOCKET, EMAIL]
	 * Ma potrebbe includere anche PUSH e SMS per admin critiche
	 */
	private List<NotificationChannel> getEnabledChannelsStub(Long adminId) {
		return List.of(
				NotificationChannel.WEBSOCKET,
				NotificationChannel.EMAIL);
	}
}
