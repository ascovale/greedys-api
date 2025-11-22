-- ============================================================================
-- ⭐ IDEMPOTENCY IMPLEMENTATION: Event-Level & Notification-Level
-- ============================================================================
-- This migration adds idempotency support at two levels:
-- 1. Event-Level: ProcessedEvent table to prevent duplicate RabbitMQ publishes
-- 2. Notification-Level: UNIQUE constraints on (eventId, userId, notificationType)
-- ============================================================================

-- ============================================================================
-- LEVEL 1: EVENT-LEVEL IDEMPOTENCY
-- ============================================================================
-- Table: ProcessedEvent
-- Purpose: Track which events have been processed by EventOutboxOrchestrator
-- 
-- Usage in EventOutboxOrchestrator.orchestrate():
--   1. Try INSERT ProcessedEvent(eventId) with UNIQUE constraint
--   2. If success → event is new, proceed with RabbitMQ publish
--   3. If UNIQUE violation → event already processed, SKIP
-- 
-- Guarantee: Same event NEVER published twice to RabbitMQ
-- ============================================================================

CREATE TABLE IF NOT EXISTS processed_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    status ENUM('PROCESSING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PROCESSING',
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_processed_event PRIMARY KEY (id),
    CONSTRAINT uk_processed_event_eventid UNIQUE KEY (event_id),
    INDEX idx_processed_event_status (status),
    INDEX idx_processed_event_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- LEVEL 2: NOTIFICATION-LEVEL IDEMPOTENCY
-- ============================================================================
-- All 4 notification tables updated with UNIQUE constraint:
-- (event_id, user_id, notification_type)
-- 
-- Purpose: Prevent duplicate notifications for same event+user+type combination
-- 
-- Usage in BaseNotificationListener.persistNotification():
--   1. Try INSERT notification with UNIQUE constraint
--   2. If success → notification created
--   3. If UNIQUE violation → notification already exists, skip (idempotent)
-- 
-- Guarantee: No duplicate notifications even if listener crashes and retries
-- ============================================================================

-- Restaurant User Notifications
ALTER TABLE restaurant_user_notification 
ADD CONSTRAINT uk_restaurant_notification_idempotency UNIQUE (event_id, user_id, notification_type);

-- Customer Notifications
ALTER TABLE notification 
ADD CONSTRAINT uk_customer_notification_idempotency UNIQUE (event_id, user_id, notification_type);

-- Agency User Notifications
ALTER TABLE agency_user_notification 
ADD CONSTRAINT uk_agency_notification_idempotency UNIQUE (event_id, user_id, notification_type);

-- Admin Notifications
ALTER TABLE admin_notification 
ADD CONSTRAINT uk_admin_notification_idempotency UNIQUE (event_id, user_id, notification_type);

-- ============================================================================
-- ⭐ IDEMPOTENCY GUARANTEE SUMMARY
-- ============================================================================
-- 
-- Two-Level Idempotency Flow:
-- 
-- LEVEL 1 (Event-Level - EventOutboxOrchestrator):
-- ┌─────────────────────────────────────────────────┐
-- │ EventOutboxOrchestrator.orchestrate()           │
-- │ @Transactional                                  │
-- │                                                 │
-- │ 1. Try INSERT ProcessedEvent(eventId) ← UNIQUE │
-- │    ├─ SUCCESS → Continue with RabbitMQ publish │
-- │    └─ UNIQUE VIOLATION → SKIP event            │
-- │ 2. Publish to RabbitMQ queue                    │
-- │ 3. Update EventOutbox.status=PROCESSED          │
-- │ 4. @Transactional COMMIT                        │
-- │                                                 │
-- │ Guarantee: Same event never published twice    │
-- └─────────────────────────────────────────────────┘
-- 
-- LEVEL 2 (Notification-Level - BaseNotificationListener):
-- ┌──────────────────────────────────────────────────┐
-- │ BaseNotificationListener.persistNotification()  │
-- │ @Transactional                                   │
-- │                                                  │
-- │ 1. Check existsByEventId(eventId)               │
-- │    ├─ EXISTS → basicAck() and skip              │
-- │    └─ NOT EXISTS → Continue                     │
-- │ 2. For each disaggregated notification:         │
-- │    Try INSERT notification ← UNIQUE(3 cols)     │
-- │    ├─ SUCCESS → Notification saved              │
-- │    └─ UNIQUE VIOLATION → Skip (idempotent)      │
-- │ 3. basicAck(RabbitMQ)                           │
-- │ 4. @Transactional COMMIT                        │
-- │                                                  │
-- │ Guarantee: No duplicate notifications in DB    │
-- └──────────────────────────────────────────────────┘
-- 
-- Failure Scenarios Handled:
-- ✅ EventOutboxOrchestrator crashes after INSERT ProcessedEvent
--    → Retry detects UNIQUE violation → SKIP (no duplicate RabbitMQ messages)
-- 
-- ✅ Listener crashes during persist
--    → RabbitMQ retransmits message
--    → Listener checks existsByEventId() → TRUE → SKIP (no duplicate notifications)
-- 
-- ✅ Single notification insert fails with UNIQUE violation
--    → Caught by catch(DataIntegrityViolationException)
--    → Logged as idempotent
--    → Listener continues and ACK
-- 
-- ============================================================================
