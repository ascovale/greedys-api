package com.application.common.notification.channel.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.common.notification.channel.INotificationChannel;
import com.application.common.persistence.model.notification.NotificationChannel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ EMAIL NOTIFICATION CHANNEL (STUB)
 * 
 * Invia notifiche via email.
 * 
 * TODO: Implementare integrando con:
 * - JavaMailSender (Spring Framework)
 * - Email templates (Thymeleaf)
 * - SMTP configuration
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Email Notification Channel)
 */
@Slf4j
@Service("EMAIL")
@RequiredArgsConstructor
public class EmailNotificationChannel implements INotificationChannel {

	@Override
	public NotificationChannel getChannelType() {
		return NotificationChannel.EMAIL;
	}

	@Override
	public boolean send(
			String title,
			String body,
			Long recipient,
			String recipientType,
			Map<String, String> properties) throws Exception {

		try {
			log.info("📧 [STUB] Sending EMAIL notification to user {} (title={})", recipient, title);
			
			// TODO: Implementare logica reale:
			// 1. Load user email address da DB (user_id -> email)
			// 2. Render template HTML con title, body, properties
			// 3. Invia via JavaMailSender
			// 4. Return true se successo, false se errore
			
			// Per ora: simulare successo
			return true;

		} catch (Exception e) {
			log.error("❌ Error sending EMAIL notification: {}", e.getMessage());
			return false;
		}
	}

	@Override
	public boolean isEnabled() {
		// TODO: Verificare che SMTP sia configurato
		return true;
	}

	@Override
	public int getMaxRetries() {
		return 3;
	}

	@Override
	public long getRetryDelayMs() {
		return 10000;  // 10 secondi
	}

	@Override
	public long getTimeoutMs() {
		return 30000;  // 30 secondi
	}
}
