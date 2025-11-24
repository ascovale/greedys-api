package com.application.common.notification.channel.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.common.notification.channel.INotificationChannel;
import com.application.common.persistence.model.notification.NotificationChannel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ SMS NOTIFICATION CHANNEL (STUB)
 * 
 * Invia notifiche via SMS.
 * 
 * TODO: Implementare integrando con:
 * - Twilio API (o simile SMS provider)
 * - Phone number lookup
 * - SMS template per character limit
 * 
 * @author Greedy's System
 * @since 2025-01-20 (SMS Notification Channel)
 */
@Slf4j
@Service("SMS")
@RequiredArgsConstructor
public class SMSNotificationChannel implements INotificationChannel {

	@Override
	public NotificationChannel getChannelType() {
		return NotificationChannel.SMS;
	}

	@Override
	public boolean send(
			String title,
			String body,
			Long recipient,
			String recipientType,
			Map<String, String> properties) throws Exception {

		try {
			log.info("📲 [STUB] Sending SMS notification to user {} (length={})", recipient, body != null ? body.length() : 0);
			
			// TODO: Implementare logica reale:
			// 1. Load phone number per utente da DB
			// 2. Componi messaggio (title + body, max 160 chars)
			// 3. Invia via Twilio/SMS provider API
			// 4. Handle delivery status callback
			// 5. Return true se successo
			
			// Per ora: simulare successo
			return true;

		} catch (Exception e) {
			log.error("❌ Error sending SMS notification: {}", e.getMessage());
			return false;
		}
	}

	@Override
	public boolean isEnabled() {
		// TODO: Verificare che SMS provider sia configurato
		return true;
	}

	@Override
	public int getMaxRetries() {
		return 2;
	}

	@Override
	public long getRetryDelayMs() {
		return 15000;  // 15 secondi (SMS è lento)
	}

	@Override
	public long getTimeoutMs() {
		return 60000;  // 60 secondi (SMS può richiedere tempo)
	}
}
