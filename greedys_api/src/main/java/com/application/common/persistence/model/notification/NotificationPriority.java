package com.application.common.persistence.model.notification;

/**
 * ⭐ PRIORITY ENUM (shared across all notification models)
 * 
 * Priorità dell'invio della notifica.
 * 
 * - HIGH: Invio immediato (reservation confirmed, urgent alert, payment)
 * - NORMAL: Invio entro ~5 minuti (regular notification)
 * - LOW: Invio entro ~1 ora (promotional, optional updates)
 * 
 * ChannelPoller query con ORDER BY:
 * - SELECT * FROM notification_* 
 *   WHERE status='PENDING' AND channel='EMAIL'
 *   ORDER BY priority DESC, created_at ASC
 *   LIMIT 100
 * 
 * → HIGH priority elaborati prima
 * → NORMAL dopo
 * → LOW per ultimi
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation)
 */
public enum NotificationPriority {
    HIGH,       // Immediato
    NORMAL,     // ~5 min
    LOW         // ~1 ora
}
