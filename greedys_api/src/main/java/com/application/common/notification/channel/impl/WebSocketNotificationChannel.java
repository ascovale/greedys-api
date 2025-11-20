package com.application.common.notification.channel.impl;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.application.common.notification.channel.INotificationChannel;
import com.application.common.persistence.model.notification.NotificationChannel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WEBSOCKET NOTIFICATION CHANNEL
 * 
 * Invia notifiche real-time via WebSocket ai client connessi.
 * 
 * FLOW:
 * 1. ChannelPoller legge notifiche PENDING con channel=WEBSOCKET
 * 2. Per ogni notifica, chiama send()
 * 3. send() usa SimpMessagingTemplate per inviare al client
 * 4. Client riceve via WebSocket e mostra notifica in tempo reale
 * 
 * IMPLEMENTAZIONE:
 * - Usa Spring WebSocket + STOMP
 * - Invia a destination: /topic/notifications/{userId}/{notificationType}
 * - Client si subscribe: /topic/notifications/123/RESTAURANT
 * 
 * RETRY LOGIC:
 * - Se client non connesso: è ok, proverà di nuovo al prossimo poll
 * - Se client connesso: invia immediatamente
 * - Status DELIVERED quando msg arriva al client (o quando rimane nel queue se offline)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (WebSocket Notification Channel)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationChannel implements INotificationChannel {

	private final SimpMessagingTemplate messagingTemplate;

	@Override
	public NotificationChannel getChannelType() {
		return NotificationChannel.WEBSOCKET;
	}

	@Override
	public boolean send(
			String title,
			String body,
			Long recipient,
			String recipientType,
			Map<String, String> properties) throws Exception {

		try {
			log.debug("📤 Sending WebSocket notification to {} (type={})", recipient, recipientType);

			// Crea payload per WebSocket
			Map<String, Object> notification = Map.of(
					"title", title != null ? title : "",
					"body", body != null ? body : "",
					"recipient", recipient,
					"recipientType", recipientType,
					"properties", properties != null ? properties : Map.of(),
					"timestamp", System.currentTimeMillis());

			// Invia via STOMP to /topic/notifications/{userId}/{notificationType}
			String destination = String.format("/topic/notifications/%d/%s", recipient, recipientType);
			messagingTemplate.convertAndSend(destination, notification);

			log.info("✅ WebSocket notification sent to {} at {}", recipient, destination);
			return true;

		} catch (Exception e) {
			log.error("❌ Error sending WebSocket notification to {}: {}", recipient, e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean isEnabled() {
		// WebSocket channel è sempre abilitato (parte di Spring app)
		return true;
	}

	@Override
	public int getMaxRetries() {
		// WebSocket: 1 retry (if fail, move on, client will get it next poll)
		return 1;
	}

	@Override
	public long getRetryDelayMs() {
		// Retry dopo 1 secondo se fallisce
		return 1000;
	}

	@Override
	public long getTimeoutMs() {
		// WebSocket send è veloce (in-memory), 5 secondi timeout
		return 5000;
	}
}
