package com.application.common.notification.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminNotificationDAO;
import com.application.admin.persistence.model.AdminNotification;
import com.application.agency.persistence.dao.AgencyUserNotificationDAO;
import com.application.agency.persistence.model.AgencyUserNotification;
import com.application.common.notification.channel.INotificationChannel;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.persistence.model.notification.NotificationChannel;
import com.application.customer.persistence.dao.CustomerNotificationDAO;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.restaurant.persistence.dao.RestaurantUserNotificationDAO;
import com.application.restaurant.persistence.model.RestaurantUserNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ CHANNEL POLLER SERVICE
 * 
 * Periodicamente poll il DB per notifiche PENDING e le invia via loro channel.
 * 
 * FLOW:
 * 1. @Scheduled(fixedDelay=5000) ogni 5 secondi:
 *    a. Query per notifiche PENDING
 *    b. Per ogni notifica:
 *       - Carica implementazione del channel
 *       - Chiama send(title, body, recipient, properties)
 *       - Se successo → status=DELIVERED
 *       - Se errore → status=FAILED (oppure PENDING con incremented retry_count)
 * 
 * BATCH STRATEGY:
 * - Processa per channel (WEBSOCKET, EMAIL, PUSH, SMS)
 * - Per ogni channel, prende max 100 notifiche per evitare memory overload
 * - Ordina per priority DESC (HIGH prima), creation_time ASC (FIFO within same priority)
 * 
 * RETRY LOGIC:
 * - Se send() fallisce → status rimane PENDING (non FAILED)
 * - ChannelPoller torna a processar il messaggio prossima iterazione
 * - Dopo N retry falliti → status=FAILED (abbandonare)
 * 
 * CHANNEL IMPLEMENTATIONS:
 * ⭐ WebSocketNotificationChannel: NOT POLLED HERE
 *    - Delivery is SYNCHRONOUS in BaseNotificationListener.attemptWebSocketSend()
 *    - Best-effort, no retry if client offline
 *    - No background polling needed
 * 
 * Channels polled by ChannelPoller:
 * 1. EmailNotificationChannel
 *    - Polling interval: 30 seconds (batched)
 *    - Invia email via SMTP
 *    - Integrazione con EmailService/JavaMailSender
 * 
 * 2. PushNotificationChannel
 *    - Polling interval: 10 seconds
 *    - Invia push notification via Firebase Cloud Messaging
 *    - Integrazione con FCM SDK
 * 
 * 3. SMSNotificationChannel
 *    - Polling interval: 60 seconds (rate-limited)
 *    - Invia SMS via Twilio o simile
 *    - Integrazione con SMS provider
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Notification Delivery Polling)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPoller {

	private final RestaurantUserNotificationDAO restaurantDAO;
	private final CustomerNotificationDAO customerDAO;
	private final AgencyUserNotificationDAO agencyDAO;
	private final AdminNotificationDAO adminDAO;

	@Autowired(required = false)
	private Map<String, INotificationChannel> channelImplementations = new HashMap<>();

	// ==================== POLLING LOOPS ====================

	/**
	 * Poll EMAIL channel every 30 seconds.
	 * EMAIL è più lento, batch ogni 30s.
	 * 
	 * NOTE: WebSocket is NOT polled here because:
	 * - WebSocket delivery is SYNCHRONOUS in BaseNotificationListener.attemptWebSocketSend()
	 * - Called immediately after DB persist, not via background polling
	 * - Best-effort: if client offline, fails silently (no retry)
	 */
	@Scheduled(fixedDelayString = "${notification.polling.email.interval:30000}")
	@Transactional
	public void pollEmailChannel() {
		log.debug("🔄 Polling EMAIL channel for pending notifications");
		pollChannel(NotificationChannel.EMAIL);
	}

	/**
	 * Poll PUSH channel every 10 seconds.
	 * PUSH è real-time, ma non veloce come WEBSOCKET.
	 */
	@Scheduled(fixedDelayString = "${notification.polling.push.interval:10000}")
	@Transactional
	public void pollPushChannel() {
		log.debug("🔄 Polling PUSH channel for pending notifications");
		pollChannel(NotificationChannel.PUSH);
	}

	/**
	 * Poll SMS channel every 60 seconds.
	 * SMS è il più lento e costoso, batch ogni 60s.
	 */
	@Scheduled(fixedDelayString = "${notification.polling.sms.interval:60000}")
	@Transactional
	public void pollSmsChannel() {
		log.debug("🔄 Polling SMS channel for pending notifications");
		pollChannel(NotificationChannel.SMS);
	}

	// ==================== CORE POLLING LOGIC ====================

	/**
	 * Poll un specifico channel per notifiche PENDING.
	 */
	private void pollChannel(NotificationChannel channel) {
		try {
			log.info("📬 Polling {} channel for PENDING notifications", channel);

			INotificationChannel channelImpl = channelImplementations.get(channel.name());
			if (channelImpl == null) {
				log.warn("⚠️ No implementation found for channel: {}", channel);
				return;
			}

			if (!channelImpl.isEnabled()) {
				log.debug("⏸️ Channel {} is disabled, skipping", channel);
				return;
			}

			// Query PENDING notifications for this channel from all 4 types
			int processed = 0;
			processed += processPendingNotifications(restaurantDAO.findPendingByChannel(channel.name(), 100), channelImpl, "RESTAURANT");
			processed += processPendingNotifications(customerDAO.findPendingByChannel(channel.name(), 100), channelImpl, "CUSTOMER");
			processed += processPendingNotifications(agencyDAO.findPendingByChannel(channel.name(), 100), channelImpl, "AGENCY");
			processed += processPendingNotifications(adminDAO.findPendingByChannel(channel.name(), 100), channelImpl, "ADMIN");

			if (processed > 0) {
				log.info("✅ Processed {} {} notifications", processed, channel);
			}

		} catch (Exception e) {
			log.error("❌ Error polling {} channel", channel, e);
		}
	}

	/**
	 * Processa lista di notifiche usando il channel implementation.
	 */
	private <T> int processPendingNotifications(List<T> notifications, INotificationChannel channelImpl, String notificationType) {
		int processed = 0;

		for (T notification : notifications) {
			try {
				if (sendNotification(notification, channelImpl, notificationType)) {
					processed++;
				}
			} catch (Exception e) {
				log.error("❌ Error processing notification (type={}): {}", notificationType, e.getMessage());
			}
		}

		return processed;
	}

	/**
	 * Invia una singola notifica via channel implementation.
	 */
	private <T> boolean sendNotification(T notification, INotificationChannel channelImpl, String notificationType) {
		try {
			// Extract common fields based on type
			String title = null;
			String body = null;
			Long recipient = null;
			Map<String, String> properties = null;

			switch (notificationType) {
				case "RESTAURANT":
					RestaurantUserNotification restNotif = (RestaurantUserNotification) notification;
					title = restNotif.getTitle();
					body = restNotif.getBody();
					recipient = restNotif.getUserId();
					properties = restNotif.getProperties();
					break;

				case "CUSTOMER":
					CustomerNotification custNotif = (CustomerNotification) notification;
					title = custNotif.getTitle();
					body = custNotif.getBody();
					recipient = custNotif.getUserId();
					properties = custNotif.getProperties();
					break;

				case "AGENCY":
					AgencyUserNotification agencyNotif = (AgencyUserNotification) notification;
					title = agencyNotif.getTitle();
					body = agencyNotif.getBody();
					recipient = agencyNotif.getUserId();
					properties = agencyNotif.getProperties();
					break;

				case "ADMIN":
					AdminNotification adminNotif = (AdminNotification) notification;
					title = adminNotif.getTitle();
					body = adminNotif.getBody();
					recipient = adminNotif.getUserId();
					properties = adminNotif.getProperties();
					break;

				default:
					log.warn("Unknown notification type: {}", notificationType);
					return false;
			}

			// Send via channel
			boolean success = channelImpl.send(title, body, recipient, notificationType, properties != null ? properties : new HashMap<>());

			// Update status based on result
			if (success) {
				updateNotificationStatus(notification, DeliveryStatus.DELIVERED);
				log.info("📤 {} notification delivered to user {}", notificationType, recipient);
			} else {
				log.warn("⚠️ {} notification failed for user {}", notificationType, recipient);
				// Keep as PENDING for retry (retry logic in channel impl)
			}

			return success;

		} catch (Exception e) {
			log.error("❌ Error sending notification: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Update status di una notifica.
	 */
	private <T> void updateNotificationStatus(T notification, DeliveryStatus status) {
		if (notification instanceof RestaurantUserNotification) {
			RestaurantUserNotification notif = (RestaurantUserNotification) notification;
			notif.setStatus(status);
			restaurantDAO.save(notif);
		} else if (notification instanceof CustomerNotification) {
			CustomerNotification notif = (CustomerNotification) notification;
			notif.setStatus(com.application.customer.persistence.model.CustomerNotification.DeliveryStatus.valueOf(status.name()));
			customerDAO.save(notif);
		} else if (notification instanceof AgencyUserNotification) {
			AgencyUserNotification notif = (AgencyUserNotification) notification;
			notif.setStatus(status);
			agencyDAO.save(notif);
		} else if (notification instanceof AdminNotification) {
			AdminNotification notif = (AdminNotification) notification;
			notif.setStatus(status);
			adminDAO.save(notif);
		}
	}
}
