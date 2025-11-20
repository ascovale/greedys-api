package com.application.common.persistence.model.notification;

/**
 * ⭐ DELIVERY STATUS ENUM (shared across all notification models)
 * 
 * Stato della consegna della notifica.
 * 
 * - PENDING: Appena creata, in attesa di invio (via ChannelPoller)
 * - DELIVERED: Inviata via channel con successo (ChannelPoller ha inviato)
 * - FAILED: Errore durante invio (es: email bounce, push failed, WebSocket offline)
 * - READ: User ha letto la notifica (WebSocket handler)
 * 
 * Transizioni:
 * PENDING → DELIVERED (channel send successful)
 * PENDING → FAILED (channel send failed, after retries)
 * DELIVERED/PENDING → READ (user action)
 * 
 * ChannelPoller query:
 * - SELECT * FROM notification_* WHERE status='PENDING' LIMIT 100
 * 
 * Dashboard query:
 * - SELECT * FROM notification_* WHERE status IN ('DELIVERED', 'PENDING') AND user_id=?
 * 
 * Unread count:
 * - SELECT COUNT(*) FROM notification_* WHERE user_id=? AND status != 'READ'
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation)
 */
public enum DeliveryStatus {
    PENDING,    // In coda per invio
    DELIVERED,  // Inviata
    FAILED,     // Errore (dopo retries)
    READ        // User ha letto
}
