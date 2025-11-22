package com.application.common.notification.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.application.admin.persistence.model.AdminNotification;
import com.application.agency.persistence.model.AgencyUserNotification;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.restaurant.persistence.model.RestaurantUserNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WEBSOCKET NOTIFICATION SENDER
 * 
 * Invia notifiche in TEMPO REALE via WebSocket ai client connessi.
 * 
 * DESIGN PRINCIPLES:
 * - WebSocket is BEST-EFFORT, NOT reliable like Email/Push/SMS
 * - NO RETRY LOGIC: se client offline → notifica NON si invia
 * - NO OUTBOX TABLE: idempotency handled ONLY via Notification table UNIQUE(eventId)
 * - SYNCHRONOUS SEND: called immediately after notification creation in RabbitListener
 * 
 * FLOW:
 * 1. RabbitListener riceve messaggio da EventOutboxOrchestrator
 * 2. Crea NotificationXxx row nel DB (with UNIQUE eventId constraint)
 * 3. SUBITO dopo: NotificationWebSocketSender.send() attempt
 * 4. Se client è online: WebSocket delivery succeeds, client receives immediately
 * 5. Se client offline: send fails silently (no retry)
 * 6. Se server crasha tra step 2-3: client never receives (acceptable, best-effort)
 * 
 * CLIENT-SIDE DEDUPLICATION:
 * - Payload includes (notificationId, eventId, timestamp)
 * - If same message arrives twice (due to bug/crash), client can deduplicate using eventId
 * 
 * STOMP TOPICS:
 * - Destination format: /topic/notifications/{userId}/{recipientType}
 * - Example: /topic/notifications/50/RESTAURANT (for staff user 50)
 * - Example: /topic/notifications/123/CUSTOMER (for customer 123)
 * 
 * @author Greedy's System
 * @since 2025-11-22 (Synchronous WebSocket Delivery)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketSender {

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Send notification via WebSocket to connected client (synchronously).
	 * 
	 * Called immediately after notification is persisted in DB.
	 * If client is online → delivery succeeds, client receives immediately.
	 * If client offline → send fails silently, no retry (best-effort).
	 * 
	 * @param notification RestaurantUserNotification to send
	 * @return true if send attempted successfully, false on exception
	 */
	public boolean sendRestaurantNotification(RestaurantUserNotification notification) {
		try {
			return sendNotificationInternal(
				notification.getId(),
				notification.getEventId(),
				notification.getUserId(),
				"RESTAURANT",
				notification.getTitle(),
				notification.getBody(),
				notification.getProperties()
			);
		} catch (Exception e) {
			log.warn("⚠️ Failed to send RestaurantUserNotification via WebSocket: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Send notification via WebSocket (CustomerNotification variant).
	 */
	public boolean sendCustomerNotification(CustomerNotification notification) {
		try {
			return sendNotificationInternal(
				notification.getId(),
				notification.getEventId(),
				notification.getUserId(),
				"CUSTOMER",
				notification.getTitle(),
				notification.getBody(),
				notification.getProperties()
			);
		} catch (Exception e) {
			log.warn("⚠️ Failed to send CustomerNotification via WebSocket: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Send notification via WebSocket (AgencyUserNotification variant).
	 */
	public boolean sendAgencyNotification(AgencyUserNotification notification) {
		try {
			return sendNotificationInternal(
				notification.getId(),
				notification.getEventId(),
				notification.getUserId(),
				"AGENCY",
				notification.getTitle(),
				notification.getBody(),
				notification.getProperties()
			);
		} catch (Exception e) {
			log.warn("⚠️ Failed to send AgencyUserNotification via WebSocket: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Send notification via WebSocket (AdminNotification variant).
	 */
	public boolean sendAdminNotification(AdminNotification notification) {
		try {
			return sendNotificationInternal(
				notification.getId(),
				notification.getEventId(),
				notification.getUserId(),
				"ADMIN",
				notification.getTitle(),
				notification.getBody(),
				notification.getProperties()
			);
		} catch (Exception e) {
			log.warn("⚠️ Failed to send AdminNotification via WebSocket: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Internal WebSocket send implementation.
	 * 
	 * Sends to Spring WebSocket STOMP topic: /topic/notifications/{userId}/{recipientType}
	 * 
	 * @param notificationId DB primary key (for client dedup)
	 * @param eventId unique event identifier (for client dedup)
	 * @param userId recipient user ID
	 * @param recipientType RESTAURANT, CUSTOMER, AGENCY, ADMIN
	 * @param title notification title
	 * @param body notification body
	 * @param properties additional metadata
	 * @return true if sent successfully
	 */
	private boolean sendNotificationInternal(
			Long notificationId,
			String eventId,
			Long userId,
			String recipientType,
			String title,
			String body,
			Map<String, String> properties) {

		try {
			String destination = String.format("/topic/notifications/%d/%s", userId, recipientType);

			// Build WebSocket payload with client-side deduplication info
			Map<String, Object> payload = new HashMap<>();
			payload.put("notificationId", notificationId);  // DB primary key for dedup
			payload.put("eventId", eventId);                 // Event ID for dedup
			payload.put("title", title);
			payload.put("body", body);
			payload.put("recipientType", recipientType);
			payload.put("timestamp", System.currentTimeMillis());
			if (properties != null) {
				payload.put("properties", properties);
			}

			// Send via Spring WebSocket/STOMP
			messagingTemplate.convertAndSend(destination, payload);

			log.debug("📤 WebSocket notification sent: notificationId={}, userId={}, recipientType={}, destination={}", 
				notificationId, userId, recipientType, destination);
			return true;

		} catch (Exception e) {
			log.warn("⚠️ WebSocket send failed for notificationId={}, userId={}: {}", 
				notificationId, userId, e.getMessage());
			return false;
		}
	}
}
