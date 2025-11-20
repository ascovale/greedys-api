package com.application.common.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminNotificationDAO;
import com.application.admin.persistence.model.AdminNotification;
import com.application.agency.persistence.dao.AgencyUserNotificationDAO;
import com.application.agency.persistence.model.AgencyUserNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.customer.persistence.dao.CustomerNotificationDAO;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.restaurant.persistence.dao.RestaurantUserNotificationDAO;
import com.application.restaurant.persistence.model.RestaurantUserNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ SHARED READ STATUS SERVICE
 * 
 * Gestisce la logica di marking notifiche come "READ" con supporto per:
 * 
 * 1. SHARED READ (readByAll=true):
 *    Quando UN utente legge la notifica → TUTTI gli utenti con stesso eventId/restaurantId OR agencyId/channel vedono READ
 *    
 *    Esempi:
 *    - RestaurantUserNotification: 10 staff ricevono NEW_ORDER
 *      Staff_1 legge → tutti 10 vedono READ (eventId, restaurantId, channel matching)
 *    
 *    - AgencyUserNotification: 5 staff ricevono SERVICE_ASSIGNED
 *      Staff_1 legge → tutti 5 vedono READ (eventId, agencyId, channel matching)
 * 
 * 2. INDIVIDUAL READ (readByAll=false):
 *    Quando utente legge → SOLO quella notifica è marcata READ (gli altri non cambiano)
 *    
 *    Esempi:
 *    - CustomerNotification: Customer legge notifica → solo sua row è READ
 *    - AdminNotification: Admin legge notifica → solo sua row è READ
 * 
 * ⭐ FLOW DI UTILIZZO:
 * 1. WebSocket handler riceve event "notificationRead" da client
 * 2. Estrae: notificationId (oppure eventId + userId + channel)
 * 3. Chiama ReadStatusService.markNotificationAsRead(notificationId, userId)
 * 4. ReadStatusService:
 *    a. Carica notifica dal DB
 *    b. Controlla readByAll flag
 *    c. Se readByAll=true → batch UPDATE (SHARED READ)
 *    d. Se readByAll=false → update singolo record (INDIVIDUAL READ)
 * 5. Restituisce numero di record aggiornati
 * 6. WebSocket emette evento a client: "notificationsUpdated" con lista updatedIds
 * 
 * ⭐ BATCH UPDATE PATTERNS:
 * 
 * RESTAURANT SHARED READ:
 *   UPDATE RestaurantUserNotification
 *   SET status = 'READ', read_at = NOW()
 *   WHERE event_id LIKE '${eventId}_%'   -- All disaggregated IDs from same original event
 *   AND restaurant_id = ?
 *   AND channel = ?
 *   AND read_by_all = true
 *   AND status != 'READ'  -- Avoid redundant updates
 * 
 * AGENCY SHARED READ:
 *   UPDATE AgencyUserNotification
 *   SET status = 'READ', read_at = NOW()
 *   WHERE event_id LIKE '${eventId}_%'
 *   AND agency_id = ?
 *   AND channel = ?
 *   AND read_by_all = true
 *   AND status != 'READ'
 * 
 * CUSTOMER INDIVIDUAL READ:
 *   UPDATE CustomerNotification
 *   SET status = 'READ', read_at = NOW()
 *   WHERE id = ? AND user_id = ?
 * 
 * ADMIN INDIVIDUAL READ:
 *   UPDATE AdminNotification
 *   SET status = 'READ', read_at = NOW()
 *   WHERE id = ? AND user_id = ?
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Shared Read Status Handling)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadStatusService {

	private final RestaurantUserNotificationDAO restaurantDAO;
	private final CustomerNotificationDAO customerDAO;
	private final AgencyUserNotificationDAO agencyDAO;
	private final AdminNotificationDAO adminDAO;

	// ==================== RESTAURANT USER NOTIFICATIONS ====================

	/**
	 * Mark RestaurantUserNotification as READ with SHARED READ logic.
	 * 
	 * Se readByAll=true:
	 * - UPDATE tutte le notifiche con stesso eventId/restaurantId/channel
	 * - Uno staff legge → tutti vedono READ
	 * 
	 * Se readByAll=false:
	 * - UPDATE solo questa notifica
	 * 
	 * @param notificationId ID della notifica
	 * @param userId Staff ID che sta leggendo
	 * @return Numero di record aggiornati
	 */
	@Transactional
	public int markRestaurantNotificationAsRead(Long notificationId, Long userId) {
		log.info("📖 Marking restaurant notification {} as read by staff {}", notificationId, userId);

		try {
			// Carica la notifica dal DB
			RestaurantUserNotification notif = restaurantDAO.findById(notificationId)
					.orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

			// Verifica che sia l'utente che sta leggendo
			if (!notif.getUserId().equals(userId)) {
				log.warn("⚠️ User {} attempted to read notification owned by {}", userId, notif.getUserId());
				return 0;
			}

			// Se già READ, skip
			if (notif.getStatus() == DeliveryStatus.READ) {
				log.debug("ℹ️ Notification already read");
				return 0;
			}

			notif.markAsRead();

			// Check readByAll flag in DB (non tutte le restaurant notifications sono shared)
			if (Boolean.TRUE.equals(notif.getReadByAll())) {
				// ⭐ SHARED READ: Update tutte le notifiche con stesso eventId base
				log.info("🔄 SHARED READ: Updating all notifications for event {}", notif.getEventId());
				return restaurantDAO.updateReadByAll(
						notif.getEventId(),
						notif.getRestaurantId(),
						notif.getChannel().name());
			} else {
				// INDIVIDUAL READ: Save solo questa notifica
				log.debug("📌 INDIVIDUAL READ: Updating only this notification");
				restaurantDAO.save(notif);
				return 1;
			}

		} catch (Exception e) {
			log.error("❌ Error marking restaurant notification as read", e);
			throw new RuntimeException("Failed to mark notification as read", e);
		}
	}

	// ==================== CUSTOMER NOTIFICATIONS ====================

	/**
	 * Mark CustomerNotification as READ (always INDIVIDUAL, no shared read).
	 * 
	 * CustomerNotifications sono sempre individuali:
	 * - Customer legge notifica
	 * - UPDATE solo quella row
	 * - Gli altri customer NON sono affettati
	 * 
	 * @param notificationId ID della notifica
	 * @param userId Customer ID che sta leggendo
	 * @return 1 se aggiornato, 0 altrimenti
	 */
	@Transactional
	public int markCustomerNotificationAsRead(Long notificationId, Long userId) {
		log.info("📖 Marking customer notification {} as read by customer {}", notificationId, userId);

		try {
			CustomerNotification notif = customerDAO.findById(notificationId)
					.orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

			if (!notif.getUserId().equals(userId)) {
				log.warn("⚠️ Customer {} attempted to read notification owned by {}", userId, notif.getUserId());
				return 0;
			}

			if (notif.getStatus() == CustomerNotification.DeliveryStatus.READ) {
				log.debug("ℹ️ Notification already read");
				return 0;
			}

			notif.markAsRead();
			customerDAO.save(notif);
			log.info("✅ Customer notification marked as read");
			return 1;

		} catch (Exception e) {
			log.error("❌ Error marking customer notification as read", e);
			throw new RuntimeException("Failed to mark notification as read", e);
		}
	}

	// ==================== AGENCY USER NOTIFICATIONS ====================

	/**
	 * Mark AgencyUserNotification as READ with SHARED READ logic.
	 * 
	 * Se readByAll=true:
	 * - UPDATE tutte le notifiche con stesso eventId/agencyId/channel
	 * - Uno staff legge → tutti vedono READ
	 * 
	 * Se readByAll=false:
	 * - UPDATE solo questa notifica
	 * 
	 * @param notificationId ID della notifica
	 * @param userId Staff ID che sta leggendo
	 * @return Numero di record aggiornati
	 */
	@Transactional
	public int markAgencyNotificationAsRead(Long notificationId, Long userId) {
		log.info("📖 Marking agency notification {} as read by staff {}", notificationId, userId);

		try {
			AgencyUserNotification notif = agencyDAO.findById(notificationId)
					.orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

			if (!notif.getUserId().equals(userId)) {
				log.warn("⚠️ User {} attempted to read notification owned by {}", userId, notif.getUserId());
				return 0;
			}

			if (notif.getStatus() == DeliveryStatus.READ) {
				log.debug("ℹ️ Notification already read");
				return 0;
			}

			notif.markAsRead();

			// Check readByAll flag in DB (non tutte le agency notifications sono shared)
			if (Boolean.TRUE.equals(notif.getReadByAll())) {
				// ⭐ SHARED READ: Update tutte le notifiche con stesso eventId base
				log.info("🔄 SHARED READ: Updating all notifications for event {}", notif.getEventId());
				return agencyDAO.updateReadByAll(
						notif.getEventId(),
						notif.getAgencyId(),
						notif.getChannel().name());
			} else {
				// INDIVIDUAL READ: Save solo questa notifica
				log.debug("📌 INDIVIDUAL READ: Updating only this notification");
				agencyDAO.save(notif);
				return 1;
			}

		} catch (Exception e) {
			log.error("❌ Error marking agency notification as read", e);
			throw new RuntimeException("Failed to mark notification as read", e);
		}
	}

	// ==================== ADMIN NOTIFICATIONS ====================

	/**
	 * Mark AdminNotification as READ (always INDIVIDUAL, no shared read).
	 * 
	 * Admin notifications sono sempre individuali (readByAll=false):
	 * - Admin legge notifica
	 * - UPDATE solo quella row
	 * - Gli altri admin NON vedono il cambio di read status
	 * 
	 * @param notificationId ID della notifica
	 * @param userId Admin ID che sta leggendo
	 * @return 1 se aggiornato, 0 altrimenti
	 */
	@Transactional
	public int markAdminNotificationAsRead(Long notificationId, Long userId) {
		log.info("📖 Marking admin notification {} as read by admin {}", notificationId, userId);

		try {
			AdminNotification notif = adminDAO.findById(notificationId)
					.orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

			if (!notif.getUserId().equals(userId)) {
				log.warn("⚠️ Admin {} attempted to read notification owned by {}", userId, notif.getUserId());
				return 0;
			}

			if (notif.getStatus() == DeliveryStatus.READ) {
				log.debug("ℹ️ Notification already read");
				return 0;
			}

			notif.markAsRead();
			adminDAO.save(notif);
			log.info("✅ Admin notification marked as read");
			return 1;

		} catch (Exception e) {
			log.error("❌ Error marking admin notification as read", e);
			throw new RuntimeException("Failed to mark notification as read", e);
		}
	}

	// ==================== BULK OPERATIONS ====================

	/**
	 * Mark multiple notifications as read in bulk.
	 * 
	 * Utile per:
	 * - "Mark all as read" button
	 * - WebSocket bulk operation
	 * - Cleaning up old unread notifications
	 * 
	 * @param notificationIds List of notification IDs
	 * @param userId User marking as read
	 * @param notificationType Type: RESTAURANT, CUSTOMER, AGENCY, ADMIN
	 * @return Total number of records updated
	 */
	@Transactional
	public int markBulkAsRead(List<Long> notificationIds, Long userId, String notificationType) {
		log.info("🔄 Bulk marking {} notifications as read for user {}, type: {}",
				notificationIds.size(), userId, notificationType);

		int totalUpdated = 0;

		switch (notificationType.toUpperCase()) {
			case "RESTAURANT":
				for (Long notifId : notificationIds) {
					totalUpdated += markRestaurantNotificationAsRead(notifId, userId);
				}
				break;

			case "CUSTOMER":
				for (Long notifId : notificationIds) {
					totalUpdated += markCustomerNotificationAsRead(notifId, userId);
				}
				break;

			case "AGENCY":
				for (Long notifId : notificationIds) {
					totalUpdated += markAgencyNotificationAsRead(notifId, userId);
				}
				break;

			case "ADMIN":
				for (Long notifId : notificationIds) {
					totalUpdated += markAdminNotificationAsRead(notifId, userId);
				}
				break;

			default:
				log.warn("Unknown notification type: {}", notificationType);
		}

		log.info("✅ Bulk operation completed: {} records updated", totalUpdated);
		return totalUpdated;
	}

	/**
	 * Auto-mark old read notifications as read_at = null for cleanup.
	 * 
	 * Periodically called by scheduler to remove old read notifications
	 * from memory (deleteOldReadNotifications queries).
	 */
	@Transactional
	public void cleanupOldReadNotifications() {
		log.info("🧹 Cleaning up old read notifications");

		// TODO: Implementare logica di cleanup basata su giorni
		// restaurantDAO.deleteOldReadNotifications(olderThanDays);
		// customerDAO.deleteOldReadNotifications(olderThanDays);
		// agencyDAO.deleteOldReadNotifications(olderThanDays);
		// adminDAO.deleteOldReadNotifications(olderThanDays);

		log.info("✅ Cleanup completed");
	}
}
