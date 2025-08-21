package com.application.common.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.application.common.service.ReliableNotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock Reliable Notification Service per sviluppo minimal
 * Si attiva solo quando notifications.enabled=false
 */
@Service
@Primary
@ConditionalOnProperty(name = "notifications.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockReliableNotificationService extends ReliableNotificationService {

    public MockReliableNotificationService() {
        log.warn("🔧 MOCK: ReliableNotificationService attivato - modalità sviluppo minimal");
    }

    @Override
    public void sendEmailWithRetry(String email, Long reservationId) {
        log.info("🔧 MOCK: Email con retry simulato");
        log.info("   📧 Email: {}", email);
        log.info("   🎫 Reservation ID: {}", reservationId);
        log.info("✅ MOCK: Email inviato con successo (simulato)");
    }
}
