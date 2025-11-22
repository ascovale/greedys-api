-- ============================================================================
-- Migration: V4__migrate_notification_tables_to_new_schema.sql
-- Purpose: Migrate from old notification table schema to new disaggregated schema
-- Date: 22 November 2025
--
-- OLD SCHEMA (what exists in DB):
-- - notification_restaurant
-- - admin_notification
-- - agency_notification
-- - notification (customer?)
--
-- NEW SCHEMA (what code expects):
-- - notification_restaurant_user
-- - notification_customer
-- - notification_agency_user
-- - notification_admin
--
-- STRATEGY:
-- 1. Create new tables
-- 2. Migrate data from old to new
-- 3. Add missing columns (notification_type for idempotency)
-- 4. Drop old tables
-- ============================================================================

-- ============================================================================
-- STEP 1: Create new notification_restaurant_user table
-- ============================================================================
CREATE TABLE IF NOT EXISTS notification_restaurant_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique notification ID',
    event_id VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique event identifier for idempotency',
    user_id BIGINT NOT NULL COMMENT 'RUser ID (staff member)',
    restaurant_id BIGINT COMMENT 'Restaurant ID (foreign key)',
    title VARCHAR(255) NOT NULL COMMENT 'Notification title',
    body LONGTEXT NOT NULL COMMENT 'Notification body/content',
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL COMMENT 'Delivery channel',
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING' COMMENT 'Delivery status',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL' COMMENT 'Notification priority',
    properties JSON COMMENT 'Flexible JSON properties',
    read_by_all BOOLEAN DEFAULT FALSE COMMENT 'Shared read flag',
    notification_type VARCHAR(50) COMMENT 'Notification type for idempotency (RESERVATION, ORDER, etc)',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update',
    read_at TIMESTAMP NULL COMMENT 'Read time',
    
    -- Indexes for performance
    INDEX idx_channel_status_created (channel, status, created_at DESC) COMMENT 'ChannelPoller optimization',
    INDEX idx_restaurant_user_created (restaurant_id, user_id, created_at DESC) COMMENT 'User listing',
    UNIQUE INDEX idx_event_id (event_id) COMMENT 'Idempotency',
    UNIQUE INDEX uk_restaurant_notification_idempotency (event_id, user_id, notification_type) COMMENT 'Triple idempotency',
    INDEX idx_batch_read (event_id, restaurant_id, channel) COMMENT 'Batch read update',
    INDEX idx_status_updated (status, updated_at DESC) COMMENT 'Cleanup',
    INDEX idx_user_created (user_id, created_at DESC) COMMENT 'User queries'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Restaurant staff notifications (new schema)';

-- ============================================================================
-- STEP 2: Create new notification_customer table
-- ============================================================================
CREATE TABLE IF NOT EXISTS notification_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique notification ID',
    event_id VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique event identifier',
    user_id BIGINT NOT NULL COMMENT 'Customer ID',
    title VARCHAR(255) NOT NULL COMMENT 'Notification title',
    body LONGTEXT NOT NULL COMMENT 'Notification body',
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL COMMENT 'Delivery channel',
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING' COMMENT 'Status',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL' COMMENT 'Priority',
    properties JSON COMMENT 'JSON properties',
    read_by_all BOOLEAN DEFAULT FALSE COMMENT 'Always false for customers',
    notification_type VARCHAR(50) COMMENT 'Notification type for idempotency',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_channel_status_created (channel, status, created_at DESC),
    UNIQUE INDEX idx_event_id (event_id),
    UNIQUE INDEX uk_customer_notification_idempotency (event_id, user_id, notification_type),
    INDEX idx_status_updated (status, updated_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Customer notifications (new schema)';

-- ============================================================================
-- STEP 3: Create new notification_agency_user table
-- ============================================================================
CREATE TABLE IF NOT EXISTS notification_agency_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique notification ID',
    event_id VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique event identifier',
    user_id BIGINT NOT NULL COMMENT 'Agency user ID',
    agency_id BIGINT COMMENT 'Agency ID (foreign key)',
    title VARCHAR(255) NOT NULL COMMENT 'Notification title',
    body LONGTEXT NOT NULL COMMENT 'Notification body',
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL COMMENT 'Delivery channel',
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING' COMMENT 'Status',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL' COMMENT 'Priority',
    properties JSON COMMENT 'JSON properties',
    read_by_all BOOLEAN DEFAULT FALSE COMMENT 'Shared read flag',
    notification_type VARCHAR(50) COMMENT 'Notification type for idempotency',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_channel_status_created (channel, status, created_at DESC),
    INDEX idx_agency_user_created (agency_id, user_id, created_at DESC),
    UNIQUE INDEX idx_event_id (event_id),
    UNIQUE INDEX uk_agency_notification_idempotency (event_id, user_id, notification_type),
    INDEX idx_batch_read (event_id, agency_id, channel),
    INDEX idx_status_updated (status, updated_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Agency staff notifications (new schema)';

-- ============================================================================
-- STEP 4: Create new notification_admin table
-- ============================================================================
CREATE TABLE IF NOT EXISTS notification_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique notification ID',
    event_id VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique event identifier',
    user_id BIGINT NOT NULL COMMENT 'Admin user ID',
    title VARCHAR(255) NOT NULL COMMENT 'Notification title',
    body LONGTEXT NOT NULL COMMENT 'Notification body',
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL COMMENT 'Delivery channel',
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING' COMMENT 'Status',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL' COMMENT 'Priority',
    properties JSON COMMENT 'JSON properties',
    read_by_all BOOLEAN DEFAULT FALSE COMMENT 'Always false for admins',
    notification_type VARCHAR(50) COMMENT 'Notification type for idempotency',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_channel_status_created (channel, status, created_at DESC),
    UNIQUE INDEX idx_event_id (event_id),
    UNIQUE INDEX uk_admin_notification_idempotency (event_id, user_id, notification_type),
    INDEX idx_status_updated (status, updated_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Admin notifications (new schema)';

-- ============================================================================
-- STEP 5: Migrate data from old tables to new tables (if old tables exist)
-- ============================================================================

-- Migrate from notification_restaurant to notification_restaurant_user
INSERT IGNORE INTO notification_restaurant_user (user_id, restaurant_id, title, body, channel, status, priority, properties, read_by_all, created_at, updated_at, read_at)
SELECT user_id, restaurant_id, title, body, COALESCE(channel, 'WEBSOCKET'), status, priority, properties, read_by_all, created_at, updated_at, read_at
FROM notification_restaurant
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_restaurant');

-- Migrate from admin_notification to notification_admin
INSERT IGNORE INTO notification_admin (user_id, title, body, channel, status, priority, properties, read_by_all, created_at, updated_at, read_at)
SELECT user_id, title, body, COALESCE(channel, 'WEBSOCKET'), status, priority, properties, read_by_all, created_at, updated_at, read_at
FROM admin_notification
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin_notification');

-- Migrate from agency_notification to notification_agency_user
INSERT IGNORE INTO notification_agency_user (user_id, agency_id, title, body, channel, status, priority, properties, read_by_all, created_at, updated_at, read_at)
SELECT user_id, agency_id, title, body, COALESCE(channel, 'WEBSOCKET'), status, priority, properties, read_by_all, created_at, updated_at, read_at
FROM agency_notification
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agency_notification');

-- Migrate from notification (customer) to notification_customer
INSERT IGNORE INTO notification_customer (user_id, title, body, channel, status, priority, properties, read_by_all, created_at, updated_at, read_at)
SELECT user_id, title, body, COALESCE(channel, 'WEBSOCKET'), status, priority, properties, read_by_all, created_at, updated_at, read_at
FROM notification
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification');

-- ============================================================================
-- STEP 6: Create views for easier querying (same as V2)
-- ============================================================================
CREATE OR REPLACE VIEW v_pending_notifications AS
SELECT 
    'RESTAURANT' as notification_type,
    id, event_id, user_id, restaurant_id as entity_id,
    title, body, channel, status, priority,
    created_at
FROM notification_restaurant_user
WHERE status = 'PENDING'

UNION ALL

SELECT 
    'CUSTOMER' as notification_type,
    id, event_id, user_id, NULL as entity_id,
    title, body, channel, status, priority,
    created_at
FROM notification_customer
WHERE status = 'PENDING'

UNION ALL

SELECT 
    'AGENCY' as notification_type,
    id, event_id, user_id, agency_id as entity_id,
    title, body, channel, status, priority,
    created_at
FROM notification_agency_user
WHERE status = 'PENDING'

UNION ALL

SELECT 
    'ADMIN' as notification_type,
    id, event_id, user_id, NULL as entity_id,
    title, body, channel, status, priority,
    created_at
FROM notification_admin
WHERE status = 'PENDING'

ORDER BY priority DESC, created_at ASC;

CREATE OR REPLACE VIEW v_unread_notifications AS
SELECT 
    'RESTAURANT' as notification_type,
    id, event_id, user_id, restaurant_id as entity_id,
    title, body, channel,
    created_at
FROM notification_restaurant_user
WHERE status != 'READ'

UNION ALL

SELECT 
    'CUSTOMER' as notification_type,
    id, event_id, user_id, NULL as entity_id,
    title, body, channel,
    created_at
FROM notification_customer
WHERE status != 'READ'

UNION ALL

SELECT 
    'AGENCY' as notification_type,
    id, event_id, user_id, agency_id as entity_id,
    title, body, channel,
    created_at
FROM notification_agency_user
WHERE status != 'READ'

UNION ALL

SELECT 
    'ADMIN' as notification_type,
    id, event_id, user_id, NULL as entity_id,
    title, body, channel,
    created_at
FROM notification_admin
WHERE status != 'READ'

ORDER BY created_at DESC;
