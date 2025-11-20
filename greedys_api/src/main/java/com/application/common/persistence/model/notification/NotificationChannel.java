package com.application.common.persistence.model.notification;

/**
 * ⭐ DELIVERY CHANNEL ENUM (shared across all notification models)
 * 
 * Ogni disaggregazione ha UN channel per cui inviare.
 * 
 * - WEBSOCKET: Real-time browser notification (se user online)
 * - EMAIL: Email notification (reliable, async)
 * - PUSH: Mobile push notification (iOS, Android)
 * - SMS: SMS text message (fallback, high priority)
 * 
 * ChannelPoller queries per channel:
 * - SELECT * FROM notification_* WHERE channel='WEBSOCKET' AND status='PENDING'
 * - SELECT * FROM notification_* WHERE channel='EMAIL' AND status='PENDING'
 * - etc
 * 
 * @author Greedy's System
 * @since 2025-01-20 (RabbitListener Disaggregation)
 */
public enum NotificationChannel {
    WEBSOCKET,  // Real-time
    EMAIL,      // Async
    PUSH,       // Mobile
    SMS         // SMS
}
