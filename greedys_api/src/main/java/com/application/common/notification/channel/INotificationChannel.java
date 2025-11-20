package com.application.common.notification.channel;

import java.util.Map;

import com.application.common.persistence.model.notification.NotificationChannel;

/**
 * ⭐ NOTIFICATION CHANNEL INTERFACE
 * 
 * Definisce il contratto per mandare notifiche via diversi canali:
 * - WEBSOCKET: Real-time browser/app notification
 * - EMAIL: Email notification
 * - PUSH: Mobile push notification
 * - SMS: SMS text message
 * 
 * Implementazioni concrete:
 * - WebSocketNotificationChannel
 * - EmailNotificationChannel
 * - PushNotificationChannel
 * - SMSNotificationChannel
 * 
 * FLOW DI UTILIZZO (ChannelPoller):
 * 1. Query DB: SELECT * FROM notification WHERE channel='WEBSOCKET' AND status='PENDING'
 * 2. Per ogni notifica:
 *    a. Carica implementazione del channel
 *    b. Chiama send(notification)
 *    c. Se successo → status='DELIVERED'
 *    d. Se errore → status='FAILED'
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Notification Delivery Channels)
 */
public interface INotificationChannel {

	/**
	 * Identifier del channel.
	 * Deve ritornare uno dei valori di NotificationChannel enum.
	 */
	NotificationChannel getChannelType();

	/**
	 * Invia notifica via questo channel.
	 * 
	 * @param title Titolo della notifica
	 * @param body Corpo della notifica
	 * @param recipient Identificativo del recipient (userId, customerId, staffId, adminId)
	 * @param recipientType Tipo di recipient: CUSTOMER, RESTAURANT_USER, AGENCY_USER, ADMIN
	 * @param properties Map di proprietà aggiuntive (reservation_id, order_id, etc)
	 * @return true se inviata con successo, false se errore
	 * @throws Exception se errore fatale durante invio
	 */
	boolean send(
			String title,
			String body,
			Long recipient,
			String recipientType,
			Map<String, String> properties) throws Exception;

	/**
	 * Check se il channel è abilitato/disponibile.
	 * Utile per skip delivery se channel è offline (es: SMTP server down).
	 */
	boolean isEnabled();

	/**
	 * Retry logic: quante volte tentare di inviare prima di dare up.
	 */
	int getMaxRetries();

	/**
	 * Delay (in ms) prima del prossimo retry attempt.
	 */
	long getRetryDelayMs();

	/**
	 * Timeout (in ms) prima di considerare il tentativo di invio fallito.
	 */
	long getTimeoutMs();
}
