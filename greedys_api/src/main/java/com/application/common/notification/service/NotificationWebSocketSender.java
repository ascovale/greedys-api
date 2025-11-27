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
 * TWO DESTINATION TYPES:
 * 1. /topic/{userType}/{userId}/notifications
 *    - General notifications (info, alerts, new reservations)
 *    - Includes badge count for UI badge/icon
 *    - Sent to all connected clients (bell icon updates)
 * 
 * 2. /topic/{userType}/{userId}/reservations
 *    - Reservation status updates (accepted, rejected, modified)
 *    - Sent only when reservation state changes
 *    - Updates live reservation list without page refresh
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
 * STOMP TOPICS - NOTIFICATIONS:
 * - Destination format: /topic/{userType}/{userId}/notifications
 * - Each user type has own topic (userId can be same across tables)
 * - Example: /topic/restaurant/50/notifications (for restaurant staff user 50)
 * - Example: /topic/customer/123/notifications (for customer 123)
 * - Example: /topic/agency/45/notifications (for agency user 45)
 * - Example: /topic/admin/10/notifications (for admin user 10)
 * 
 * STOMP TOPICS - RESERVATIONS (Status Updates):
 * - Destination format: /topic/{userType}/{userId}/reservations
 * - Used ONLY for reservation state changes (ACCEPTED, REJECTED, MODIFIED)
 * - Example: /topic/restaurant/50/reservations (updates live list for staff 50)
 * - Example: /topic/customer/123/reservations (updates customer's own reservations)
 * 
 * @author Greedy's System
 * @since 2025-11-22 (Synchronous WebSocket Delivery with dual destinations)
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
	 * Send reservation status update via WebSocket (accepted/rejected/modified).
	 * 
	 * Uses separate /topic/{userType}/{userId}/reservations destination
	 * so list clients can update live without refreshing entire page.
	 * 
	 * @param reservationId ID of updated reservation
	 * @param userId user to notify (restaurant staff or customer)
	 * @param userType user type (restaurant, customer, agency, admin)
	 * @param status new reservation status (ACCEPTED, REJECTED, MODIFIED, etc)
	 * @param reservationDetails full reservation data for list update
	 * @return true if sent successfully
	 */
	public boolean sendReservationStatusUpdate(
			Long reservationId,
			Long userId,
			String userType,
			String status,
			Map<String, Object> reservationDetails) {
		try {
			String userTypePath = userType.toLowerCase();
			String destination = String.format("/topic/%s/%d/reservations", userTypePath, userId);

			Map<String, Object> payload = new HashMap<>();
			payload.put("reservationId", reservationId);
			payload.put("status", status);
			payload.put("userType", userType);
			payload.put("timestamp", System.currentTimeMillis());
			
			// Include full reservation data for list update
			if (reservationDetails != null) {
				payload.putAll(reservationDetails);
			}

			messagingTemplate.convertAndSend(destination, payload);

			log.info("📤📤📤 [WEBSOCKET-SENT] Reservation update via STOMP: reservationId={}, userId={}, status={}, destination={}", 
				reservationId, userId, status, destination);
			return true;

		} catch (Exception e) {
			log.warn("⚠️ Reservation status update failed for reservationId={}, userId={}: {}", 
				reservationId, userId, e.getMessage());
			return false;
		}
	}

	/**
	 * Internal WebSocket send implementation.
	 * 
	 * Sends to Spring WebSocket STOMP topic: /topic/{userType}/{userId}/notifications
	 * Uses userType in path to disambiguate userId (can be same across tables)
	 * 
	 * @param notificationId DB primary key (for client dedup)
	 * @param eventId unique event identifier (for client dedup)
	 * @param userId recipient user ID (local to userType)
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
			// ⭐ DESTINATION PRIORITY:
			// 1. If properties contain explicit destination (e.g., team channel for TEAM scope)
			//    → Use that (e.g., "/topic/restaurant/3/reservations" for TEAM notifications)
			// 2. Else fallback to personal channel
			//    → "/topic/{userType}/{userId}/notifications"
			String destination;
			if (properties != null && properties.containsKey("destination")) {
				destination = properties.get("destination");
				log.debug("📍 Using explicit destination from properties: {}", destination);
			} else {
				// Fallback to personal notification channel
				String userTypePath = recipientType.toLowerCase();  // "restaurant", "customer", "agency", "admin"
				destination = String.format("/topic/%s/%d/notifications", userTypePath, userId);
				log.debug("📍 Using fallback personal channel: {}", destination);
			}

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

			log.info("📤📤📤 [WEBSOCKET-SENT] Sent via STOMP: notificationId={}, userId={}, recipientType={}, destination={}", 
				notificationId, userId, recipientType, destination);
			return true;

		} catch (Exception e) {
			log.warn("⚠️ WebSocket send failed for notificationId={}, userId={}: {}", 
				notificationId, userId, e.getMessage());
			return false;
		}
	}
}
