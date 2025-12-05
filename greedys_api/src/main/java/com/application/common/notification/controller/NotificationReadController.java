package com.application.common.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.notification.service.ReadStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ NOTIFICATION READ STATUS CONTROLLER
 * 
 * REST endpoint per marking notifications as READ.
 * 
 * ENDPOINTS:
 * 1. POST /api/notifications/{id}/read?type=RESTAURANT
 *    Marca singola notifica come READ
 *    Query param: type = RESTAURANT | CUSTOMER | AGENCY | ADMIN
 *    
 *    Response: {
 *      "status": "ok",
 *      "updatedCount": 5,  // 1 per single read, or N per shared read
 *      "message": "5 notifications marked as read"
 *    }
 * 
 * 2. POST /api/notifications/read-bulk
 *    Marca multiple notifiche come READ in bulk
 *    
 *    Request body: {
 *      "notificationIds": [1, 2, 3],
 *      "type": "RESTAURANT"
 *    }
 *    
 *    Response: {
 *      "status": "ok",
 *      "updatedCount": 10,
 *      "message": "10 notifications marked as read"
 *    }
 * 
 * ⭐ SHARED READ LOGIC:
 * Se notifica ha readByAll=true e user legge → tutti gli altri utenti con stesso
 * eventId/restaurantId OR agencyId/channel vedono come READ
 * 
 * ⭐ AUTHENTICATION:
 * Verifica che userId nel token JWT corrisponde al userId della notifica
 * (user non può marcare notifica di un altro user come read)
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Notification Read Status Handling)
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationReadController {

	private final ReadStatusService readStatusService;

	/**
	 * Mark a single notification as READ.
	 * 
	 * @param notificationId ID della notifica
	 * @param type Tipo di notifica (RESTAURANT, CUSTOMER, AGENCY, ADMIN)
	 * @param auth Spring Security authentication (contiene userId)
	 * @return Response con numero di record aggiornati
	 */
	@PostMapping("/{notificationId}/read")
	public ResponseEntity<NotificationReadResponse> markAsRead(
			@PathVariable Long notificationId,
			@RequestBody MarkAsReadRequest request,
			Authentication auth) {

		try {
			Long userId = extractUserIdFromAuth(auth);
			log.info("📖 User {} marking notification {} as read (type={})", userId, notificationId, request.getType());

			// Call appropriate service method based on type
			int updatedCount = switch (request.getType().toUpperCase()) {
				case "RESTAURANT" -> readStatusService.markRestaurantNotificationAsRead(notificationId, userId);
				case "CUSTOMER" -> readStatusService.markCustomerNotificationAsRead(notificationId, userId);
				case "AGENCY" -> readStatusService.markAgencyNotificationAsRead(notificationId, userId);
				case "ADMIN" -> readStatusService.markAdminNotificationAsRead(notificationId, userId);
				default -> throw new IllegalArgumentException("Unknown notification type: " + request.getType());
			};

			return ResponseEntity.ok(new NotificationReadResponse(
					"ok",
					updatedCount,
					String.format("%d notification(s) marked as read", updatedCount)));

		} catch (IllegalArgumentException e) {
			log.warn("⚠️ Invalid request: {}", e.getMessage());
			return ResponseEntity.badRequest().body(new NotificationReadResponse(
					"error",
					0,
					e.getMessage()));

		} catch (Exception e) {
			log.error("❌ Error marking notification as read", e);
			return ResponseEntity.internalServerError().body(new NotificationReadResponse(
					"error",
					0,
					"Internal server error: " + e.getMessage()));
		}
	}

	/**
	 * Mark multiple notifications as READ in bulk.
	 * 
	 * @param request MarkBulkAsReadRequest con lista di IDs e tipo
	 * @param auth Spring Security authentication
	 * @return Response con numero totale di record aggiornati
	 */
	@PostMapping("/read-bulk")
	public ResponseEntity<NotificationReadResponse> markBulkAsRead(
			@RequestBody MarkBulkAsReadRequest request,
			Authentication auth) {

		try {
			Long userId = extractUserIdFromAuth(auth);
			log.info("🔄 User {} bulk marking {} notifications as read (type={})", userId, request.getNotificationIds().size(), request.getType());

			int totalUpdated = readStatusService.markBulkAsRead(
					request.getNotificationIds(),
					userId,
					request.getType());

			return ResponseEntity.ok(new NotificationReadResponse(
					"ok",
					totalUpdated,
					String.format("%d notification(s) marked as read", totalUpdated)));

		} catch (Exception e) {
			log.error("❌ Error bulk marking notifications as read", e);
			return ResponseEntity.internalServerError().body(new NotificationReadResponse(
					"error",
					0,
					"Internal server error: " + e.getMessage()));
		}
	}

	/**
	 * Extract userId from JWT token.
	 * 
	 * @param auth Spring Security authentication
	 * @return userId from principal
	 */
	private Long extractUserIdFromAuth(Authentication auth) {
		if (auth == null || !auth.isAuthenticated()) {
			throw new IllegalArgumentException("User not authenticated");
		}

		// TODO: Estrarre userId dal JWT token
		// Per ora: usare getName() come placeholder (dovrebbe essere userId)
		try {
			return Long.parseLong(auth.getName());
		} catch (NumberFormatException e) {
			// Fallback: prova a cercare nei claims/principal
			log.warn("⚠️ Could not parse userId from auth principal: {}", auth.getName());
			throw new IllegalArgumentException("Invalid user ID in token");
		}
	}

	// ==================== DTOs ====================

	/**
	 * Request DTO per marking singola notifica come READ.
	 */
	public static class MarkAsReadRequest {
		private String type;

		public MarkAsReadRequest() {
		}

		public MarkAsReadRequest(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

	/**
	 * Request DTO per marking bulk notifications come READ.
	 */
	public static class MarkBulkAsReadRequest {
		private java.util.List<Long> notificationIds;
		private String type;

		public MarkBulkAsReadRequest() {
		}

		public MarkBulkAsReadRequest(java.util.List<Long> notificationIds, String type) {
			this.notificationIds = notificationIds;
			this.type = type;
		}

		public java.util.List<Long> getNotificationIds() {
			return notificationIds;
		}

		public void setNotificationIds(java.util.List<Long> notificationIds) {
			this.notificationIds = notificationIds;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

	/**
	 * Response DTO per notification read operations.
	 */
	public static class NotificationReadResponse {
		private String status;
		private int updatedCount;
		private String message;

		public NotificationReadResponse() {
		}

		public NotificationReadResponse(String status, int updatedCount, String message) {
			this.status = status;
			this.updatedCount = updatedCount;
			this.message = message;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public int getUpdatedCount() {
			return updatedCount;
		}

		public void setUpdatedCount(int updatedCount) {
			this.updatedCount = updatedCount;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
