-- Flyway Migration: V2__notification_schema.sql
-- Database: MySQL 8.0+
-- Purpose: Create notification tables for disaggregation-based notification system
-- Date: 19 November 2025

-- ============================================================================
-- PHASE 1: Create metadata table for enum definitions (reference only)
-- ============================================================================

CREATE TABLE IF NOT EXISTS enum_definitions (
    enum_name VARCHAR(100) NOT NULL,
    enum_value VARCHAR(50) NOT NULL,
    description TEXT,
    PRIMARY KEY (enum_name, enum_value),
    INDEX idx_enum_name (enum_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert enum value definitions
INSERT IGNORE INTO enum_definitions (enum_name, enum_value, description) VALUES
-- DeliveryStatus
('notification_delivery_status', 'PENDING', 'Notification created, awaiting delivery'),
('notification_delivery_status', 'DELIVERED', 'Successfully sent via channel'),
('notification_delivery_status', 'FAILED', 'Failed to send'),
('notification_delivery_status', 'READ', 'Recipient has read the notification'),
-- NotificationChannel
('notification_channel_type', 'WEBSOCKET', 'Real-time WebSocket delivery'),
('notification_channel_type', 'EMAIL', 'Email delivery'),
('notification_channel_type', 'PUSH', 'Push notification delivery'),
('notification_channel_type', 'SMS', 'SMS text message delivery'),
-- NotificationPriority
('notification_priority_type', 'HIGH', 'Immediate delivery required'),
('notification_priority_type', 'NORMAL', 'Standard priority'),
('notification_priority_type', 'LOW', 'Low priority, can be delayed');

-- ============================================================================
-- PHASE 2: Create notification tables (4 separate physical tables)
-- ============================================================================

-- 2a. Table for Restaurant User Notifications
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
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update',
    read_at TIMESTAMP NULL COMMENT 'Read time',
    
    -- Indexes for performance
    INDEX idx_channel_status_created (channel, status, created_at DESC) COMMENT 'ChannelPoller optimization',
    INDEX idx_restaurant_user_created (restaurant_id, user_id, created_at DESC) COMMENT 'User listing',
    UNIQUE INDEX idx_event_id (event_id) COMMENT 'Idempotency',
    INDEX idx_batch_read (event_id, restaurant_id, channel) COMMENT 'Batch read update',
    INDEX idx_status_updated (status, updated_at DESC) COMMENT 'Cleanup',
    INDEX idx_user_created (user_id, created_at DESC) COMMENT 'User queries'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Restaurant staff notifications';

-- 2b. Table for Customer Notifications
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
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_channel_status_created (channel, status, created_at DESC),
    UNIQUE INDEX idx_event_id (event_id),
    INDEX idx_status_updated (status, updated_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Customer notifications';

-- 2c. Table for Agency User Notifications
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
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_channel_status_created (channel, status, created_at DESC),
    INDEX idx_agency_user_created (agency_id, user_id, created_at DESC),
    UNIQUE INDEX idx_event_id (event_id),
    INDEX idx_batch_read (event_id, agency_id, channel),
    INDEX idx_status_updated (status, updated_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Agency staff notifications';

-- 2d. Table for Admin Notifications
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
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_channel_status_created (channel, status, created_at DESC),
    UNIQUE INDEX idx_event_id (event_id),
    INDEX idx_status_updated (status, updated_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Admin notifications';

-- ============================================================================
-- PHASE 3: Create views for easier querying
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
