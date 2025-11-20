# 📋 DATABASE MIGRATION GUIDE - Notification System Refactoring

**Database**: MySQL 8.0+  
**Date**: 19 November 2025  
**Status**: Pre-Migration Planning  
**Risk Level**: MEDIUM (new tables, schema changes, FK dependencies)

---

## 🎯 Migration Overview

This guide explains how to migrate from the current database schema to the new notification system schema with **minimal downtime** and **zero data loss**.

### What Changes
- ❌ **DELETE**: Old notification tables (if any)
- ✅ **CREATE**: New 4-table disaggregation schema for notifications (MySQL)
- ✅ **CREATE**: New enum definitions via metadata table (MySQL ENUM columns)
- ✅ **ADD**: New indexes for performance
- ⚠️ **VERIFY**: No breaking changes to existing tables (Reservations, Customers, Restaurants, etc)

---

## 📊 Pre-Migration Checklist

### Before Running Migration

```bash
# 1. BACKUP DATABASE (Critical!)
mysqldump -u greedys_user -p --all-databases > /backups/greedys_db_$(date +%Y%m%d_%H%M%S).sql

# 2. VERIFY CURRENT SCHEMA
mysql -u greedys_user -p greedys_db -e "SHOW TABLES;"

# 3. CHECK FOR NOTIFICATION TABLES
mysql -u greedys_user -p greedys_db -e "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='greedys_db' AND TABLE_NAME LIKE '%notification%';"

# 4. VERIFY NO ACTIVE CONNECTIONS
mysql -u root -p -e "SELECT ID, USER, HOST, COMMAND, TIME, STATE FROM PROCESSLIST WHERE DB='greedys_db';"

# 5. VERIFY CHARSET AND COLLATION
mysql -u greedys_user -p greedys_db -e "SELECT DATABASE(), @@CHARACTER_SET_DATABASE, @@COLLATION_DATABASE;"
```

---

## 🔧 Step-by-Step Migration Process

### Phase 1: Disable Application (Maintenance Mode)

```sql
-- 1a. STOP application (docker service update)
-- Done via: docker service update --force greedys_api_spring-app

-- 1b. WAIT FOR GRACEFUL SHUTDOWN
-- Monitor: docker service ps greedys_api_spring-app

-- 1c. VERIFY NO DB CONNECTIONS FROM APP
SHOW PROCESSLIST;
-- Expected: Only your migration connection should remain
```

---

### Phase 2: Create New Enum Types

```sql
-- ⚠️ NOTE: MySQL does NOT have native enum types like PostgreSQL
-- Instead, we'll use ENUM column type directly in table definition
-- The enum values will be defined in Phase 4 when creating the table

-- Alternative: Store enum definitions in a metadata table for reference
CREATE TABLE IF NOT EXISTS enum_definitions (
    enum_name VARCHAR(100) NOT NULL,
    enum_value VARCHAR(50) NOT NULL,
    description TEXT,
    PRIMARY KEY (enum_name, enum_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2a. Insert DeliveryStatus enum values
INSERT INTO enum_definitions (enum_name, enum_value, description) VALUES
('notification_delivery_status', 'PENDING', 'Notification created, awaiting delivery'),
('notification_delivery_status', 'DELIVERED', 'Successfully sent via channel'),
('notification_delivery_status', 'FAILED', 'Failed to send'),
('notification_delivery_status', 'READ', 'Recipient has read the notification');

-- 2b. Insert NotificationChannel enum values
INSERT INTO enum_definitions (enum_name, enum_value, description) VALUES
('notification_channel_type', 'WEBSOCKET', 'Real-time WebSocket delivery'),
('notification_channel_type', 'EMAIL', 'Email delivery'),
('notification_channel_type', 'PUSH', 'Push notification delivery'),
('notification_channel_type', 'SMS', 'SMS text message delivery');

-- 2c. Insert NotificationPriority enum values
INSERT INTO enum_definitions (enum_name, enum_value, description) VALUES
('notification_priority_type', 'HIGH', 'Immediate delivery required'),
('notification_priority_type', 'NORMAL', 'Standard priority'),
('notification_priority_type', 'LOW', 'Low priority, can be delayed');

-- Verify insertion
SELECT * FROM enum_definitions;
```

---

### Phase 3: Delete Old Notification Tables (if exist)

⚠️ **CRITICAL**: Only run if old tables exist!

```sql
-- 3a. Check what exists
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'greedys_db'
AND TABLE_NAME LIKE '%notification%';

-- 3b. Check foreign keys pointing TO these tables
SELECT CONSTRAINT_NAME, TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE REFERENCED_TABLE_NAME = 'notification_outbox';

-- 3c. Check foreign keys pointing FROM these tables
SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'notification_outbox';

-- 3d. DROP old tables (careful with FK constraints!)
-- Disable FK checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification_outbox;
DROP TABLE IF EXISTS old_notification_listeners;
DROP TABLE IF EXISTS old_notification_channels;

SET FOREIGN_KEY_CHECKS = 1;

-- ⚠️ DO NOT DROP EVENT_OUTBOX - IT IS STILL USED BY THE NEW SYSTEM!
-- Only drop if it's a duplicate/old version
```

---

### Phase 4: Create New Notification Schema

```sql
-- 4a. Create base notification table (Single Table Inheritance)
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dtype VARCHAR(50) NOT NULL,  -- Discriminator: RESTAURANT_USER, CUSTOMER, AGENCY_USER, ADMIN
    
    -- Base fields (common to all notification types)
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL,
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL',
    properties JSON,  -- Flexible properties per notification
    read_by_all BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    -- Entity-specific fields (used by subclasses)
    restaurant_id BIGINT NULL,
    agency_id BIGINT NULL,
    
    -- Constraints
    CONSTRAINT chk_dtype CHECK (dtype IN ('RESTAURANT_USER', 'CUSTOMER', 'AGENCY_USER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4b. Create child table views (MySQL doesn't have true table inheritance like PostgreSQL)
-- We'll use a single table with dtype discriminator instead

-- ⚠️ ALTERNATIVE: Create separate physical tables for each type
-- This provides better isolation and query performance

-- For Restaurant Users
CREATE TABLE IF NOT EXISTS notification_restaurant_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL,
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL',
    properties JSON,
    read_by_all BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- For Customers  
CREATE TABLE IF NOT EXISTS notification_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL,
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL',
    properties JSON,
    read_by_all BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- For Agency Users
CREATE TABLE IF NOT EXISTS notification_agency_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    agency_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL,
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL',
    properties JSON,
    read_by_all BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    FOREIGN KEY (agency_id) REFERENCES agencies(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- For Admins
CREATE TABLE IF NOT EXISTS notification_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL,
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL',
    properties JSON,
    read_by_all BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verify tables created
SHOW TABLES LIKE 'notification%';
DESC notification_restaurant_user;
```

---

### Phase 5: Create Indexes

```sql
-- 5a. Index for ChannelPoller (query by channel + status)
CREATE INDEX idx_notification_channel_status_created 
ON notification_restaurant_user(channel, status, created_at DESC);

CREATE INDEX idx_notification_channel_status_created_customer
ON notification_customer(channel, status, created_at DESC);

CREATE INDEX idx_notification_channel_status_created_agency
ON notification_agency_user(channel, status, created_at DESC);

CREATE INDEX idx_notification_channel_status_created_admin
ON notification_admin(channel, status, created_at DESC);

-- 5b. Index for user listings (query by restaurant_id/agency_id)
CREATE INDEX idx_notification_restaurant_user_created 
ON notification_restaurant_user(restaurant_id, created_at DESC);

CREATE INDEX idx_notification_agency_user_created 
ON notification_agency_user(agency_id, created_at DESC);

-- 5c. Index for idempotency (query by event_id)
CREATE UNIQUE INDEX idx_notification_event_id 
ON notification_restaurant_user(event_id);

CREATE UNIQUE INDEX idx_notification_event_id_customer 
ON notification_customer(event_id);

CREATE UNIQUE INDEX idx_notification_event_id_agency 
ON notification_agency_user(event_id);

CREATE UNIQUE INDEX idx_notification_event_id_admin 
ON notification_admin(event_id);

-- 5d. Index for batch read updates (readByAll=true)
CREATE INDEX idx_notification_batch_read 
ON notification_restaurant_user(event_id, restaurant_id, channel) 
WHERE read_by_all = TRUE;

CREATE INDEX idx_notification_batch_read_agency 
ON notification_agency_user(event_id, agency_id, channel) 
WHERE read_by_all = TRUE;

-- 5e. Index for cleanup (old read notifications)
CREATE INDEX idx_notification_status_updated 
ON notification_restaurant_user(status, updated_at DESC) 
WHERE status = 'READ';

-- 5f. Index for user queries (fast lookup)
CREATE INDEX idx_notification_user_id
ON notification_restaurant_user(user_id, created_at DESC);

CREATE INDEX idx_notification_user_id_customer
ON notification_customer(user_id, created_at DESC);

CREATE INDEX idx_notification_user_id_agency
ON notification_agency_user(user_id, created_at DESC);

CREATE INDEX idx_notification_user_id_admin
ON notification_admin(user_id, created_at DESC);

-- Verify indexes created
SHOW INDEX FROM notification_restaurant_user;
SHOW INDEX FROM notification_customer;
SHOW INDEX FROM notification_agency_user;
SHOW INDEX FROM notification_admin;
```

---

### Phase 6: Create Views (Optional - for easier querying)

```sql
-- View to see all pending notifications by channel
CREATE VIEW v_pending_notifications AS
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
    id, event_id, user_id, NULL,
    title, body, channel, status, priority,
    created_at
FROM notification_customer
WHERE status = 'PENDING'
UNION ALL
SELECT 
    'AGENCY' as notification_type,
    id, event_id, user_id, agency_id,
    title, body, channel, status, priority,
    created_at
FROM notification_agency_user
WHERE status = 'PENDING'
UNION ALL
SELECT 
    'ADMIN' as notification_type,
    id, event_id, user_id, NULL,
    title, body, channel, status, priority,
    created_at
FROM notification_admin
WHERE status = 'PENDING'
ORDER BY priority DESC, created_at ASC;

-- View to see unread notifications per user
CREATE VIEW v_unread_notifications AS
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
    id, event_id, user_id, NULL,
    title, body, channel,
    created_at
FROM notification_customer
WHERE status != 'READ'
UNION ALL
SELECT 
    'AGENCY' as notification_type,
    id, event_id, user_id, agency_id,
    title, body, channel,
    created_at
FROM notification_agency_user
WHERE status != 'READ'
UNION ALL
SELECT 
    'ADMIN' as notification_type,
    id, event_id, user_id, NULL,
    title, body, channel,
    created_at
FROM notification_admin
WHERE status != 'READ'
ORDER BY created_at DESC;
```

---

### Phase 7: Create/Update Foreign Keys to Related Tables

```sql
-- ⚠️ CRITICAL: Verify these tables exist before adding FKs!

-- 7a. Check what tables exist
SHOW TABLES LIKE 'restaurant%';
SHOW TABLES LIKE 'agency%';
SHOW TABLES LIKE 'customer%';

-- 7b. Add FK to restaurants table (if exists)
ALTER TABLE notification_restaurant_user 
ADD CONSTRAINT fk_notif_restaurant_rest
FOREIGN KEY (restaurant_id) 
REFERENCES restaurants(id) ON DELETE SET NULL;

-- 7c. Add FK to agencies table (if exists)
ALTER TABLE notification_agency_user 
ADD CONSTRAINT fk_notif_agency_agency
FOREIGN KEY (agency_id) 
REFERENCES agencies(id) ON DELETE SET NULL;

-- ⚠️ DO NOT add FK to users table directly because:
-- - user_id can be: RUser (restaurant), Customer, Admin, AgencyUser (different tables)
-- - Use application-level logic to validate user_id belongs to correct type

-- 7d. Verify FKs created
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME LIKE 'notification_%'
AND REFERENCED_TABLE_NAME IS NOT NULL;
```

---

### Phase 8: Grant Permissions

```sql
-- 8a. Grant SELECT to app user (for queries)
GRANT SELECT ON greedys_db.notification_restaurant_user TO 'greedys_user'@'%';
GRANT SELECT ON greedys_db.notification_customer TO 'greedys_user'@'%';
GRANT SELECT ON greedys_db.notification_agency_user TO 'greedys_user'@'%';
GRANT SELECT ON greedys_db.notification_admin TO 'greedys_user'@'%';

-- 8b. Grant INSERT/UPDATE/DELETE (for listener persistence)
GRANT INSERT, UPDATE, DELETE ON greedys_db.notification_restaurant_user TO 'greedys_user'@'%';
GRANT INSERT, UPDATE, DELETE ON greedys_db.notification_customer TO 'greedys_user'@'%';
GRANT INSERT, UPDATE, DELETE ON greedys_db.notification_agency_user TO 'greedys_user'@'%';
GRANT INSERT, UPDATE, DELETE ON greedys_db.notification_admin TO 'greedys_user'@'%';

-- 8c. Grant enum_definitions table permissions
GRANT SELECT ON greedys_db.enum_definitions TO 'greedys_user'@'%';

-- 8d. Grant VIEW permissions
GRANT SELECT ON greedys_db.v_pending_notifications TO 'greedys_user'@'%';
GRANT SELECT ON greedys_db.v_unread_notifications TO 'greedys_user'@'%';

-- 8e. Apply changes
FLUSH PRIVILEGES;

-- 8f. Verify permissions
SHOW GRANTS FOR 'greedys_user'@'%';
```

---

### Phase 9: Verify Migration

```sql
-- 9a. Check table structure
DESC notification_restaurant_user;
DESC notification_customer;
DESC notification_agency_user;
DESC notification_admin;

-- 9b. Check indexes
SHOW INDEX FROM notification_restaurant_user;
SHOW INDEX FROM notification_customer;
SHOW INDEX FROM notification_agency_user;
SHOW INDEX FROM notification_admin;

-- 9c. Check constraints
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_NAME LIKE 'notification_%';

-- 9d. Check foreign keys
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME LIKE 'notification_%'
AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 9e. Verify enum values
SELECT * FROM enum_definitions WHERE enum_name LIKE 'notification_%';

-- 9f. Verify permissions
SHOW GRANTS FOR 'greedys_user'@'%' LIKE '%notification%';

-- 9g. Check table sizes
SELECT 
    TABLE_NAME,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME LIKE 'notification_%'
ORDER BY size_mb DESC;
```

---

### Phase 10: Re-enable Application

```bash
# 10a. Start application
docker service update --force greedys_api_spring-app

# 10b. Monitor startup
docker service logs greedys_api_spring-app -f

# 10c. Verify DB connections
psql -U greedys_user -d greedys_db -c "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;"

# 10d. Test notifications
curl -X POST http://localhost:8080/api/test/create-notification \
  -H "Content-Type: application/json" \
  -d '{"restaurantId": 1, "eventType": "TEST"}'
```

---

## ⚠️ Rollback Plan

If migration fails, follow these steps:

```sql
-- Step 1: Drop views
DROP VIEW IF EXISTS v_unread_notifications;
DROP VIEW IF EXISTS v_pending_notifications;

-- Step 2: Drop new tables
DROP TABLE IF EXISTS notification_admin;
DROP TABLE IF EXISTS notification_agency_user;
DROP TABLE IF EXISTS notification_customer;
DROP TABLE IF EXISTS notification_restaurant_user;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS enum_definitions;

-- Step 3: Restore from backup
-- Using the backup created in Phase 1
mysql -u greedys_user -p greedys_db < /backups/greedys_db_BACKUP.sql

-- Step 4: Verify tables restored
SHOW TABLES;
```

---

## 🚨 Common Issues & Solutions

### Issue 1: Cannot create table because it already exists
```sql
-- Solution: Use IF NOT EXISTS (already in our scripts)
CREATE TABLE IF NOT EXISTS notification_restaurant_user (...)

-- Or drop first
DROP TABLE IF EXISTS notification_restaurant_user;
```

### Issue 2: Foreign key constraint violation
```sql
-- Problem: Referenced table doesn't exist
-- Solution: Create referenced table first or use ON DELETE SET NULL

-- Check what's referencing
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE REFERENCED_TABLE_NAME IS NOT NULL;

-- List all tables to verify they exist
SHOW TABLES;
```

### Issue 3: Permission denied for creating table
```sql
-- Solution: Ensure migration user has schema permissions
GRANT ALL PRIVILEGES ON greedys_db.* TO 'greedys_user'@'%';
FLUSH PRIVILEGES;
```

### Issue 4: ENUM value error
```sql
-- Problem: Invalid ENUM value for a column
-- Solution: Check ENUM definition and insert only valid values

-- List ENUM values
SELECT COLUMN_NAME, COLUMN_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'notification_restaurant_user' 
AND COLUMN_NAME = 'status';

-- Valid values for status: PENDING, DELIVERED, FAILED, READ
```

### Issue 5: JSON functions not supported
```sql
-- If MySQL version < 5.7, JSON type not available
-- Solution: Use TEXT or VARCHAR(max) instead

-- Verify MySQL version
SELECT VERSION();

-- If < 5.7, use TEXT
ALTER TABLE notification_restaurant_user MODIFY properties LONGTEXT;
```

### Issue 6: CHARACTER SET mismatch
```sql
-- Problem: Migration fails with charset errors
-- Solution: Ensure all tables use utf8mb4

-- Set database default
ALTER DATABASE greedys_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Set table charset
ALTER TABLE notification_restaurant_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 📈 Performance Considerations

### Before Migration
- Back up current schema to measure baseline
- Note query times for future comparison
- Document current table sizes

### After Migration
- Run statistics update
```sql
ANALYZE TABLE notification_restaurant_user;
ANALYZE TABLE notification_customer;
ANALYZE TABLE notification_agency_user;
ANALYZE TABLE notification_admin;
```

- Check index usage
```sql
-- MySQL 5.7+
SELECT OBJECT_SCHEMA, OBJECT_NAME, INDEX_NAME, COUNT_STAR
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_NAME LIKE 'notification_%'
ORDER BY COUNT_STAR DESC;
```

- Monitor slow queries
```sql
-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- Log queries > 1 second

-- Check slow log
SHOW VARIABLES LIKE 'slow_query_log%';
```

---

## 🔄 Using Flyway for Automated Migration

Instead of manual SQL, use Flyway (Recommended):

### 1. Place V2__notification_schema.sql in:
```
src/main/resources/db/migration/V2__notification_schema.sql
```

### 2. Flyway automatically:
- Checks if migration was already run
- Executes only new migrations
- Updates flyway_schema_history table
- Rolls back on error

### 3. Verify Flyway status:
```sql
SELECT * FROM flyway_schema_history;

-- Output:
-- version | description | type | installed_by | installed_on | execution_time | success
-- --------|-------------|------|--------------|------|
-- 1       | init        | SQL  | greedys_user | ... | 100  | true
-- 2       | notification_schema | SQL | greedys_user | ... | 200 | true
```

---

## 📝 Checklist for Migration Day

- [ ] Create full database backup
- [ ] Schedule maintenance window (2-3 hours)
- [ ] Notify team of downtime
- [ ] Stop application (docker service update)
- [ ] Run Phase 1-7 SQL scripts
- [ ] Verify schema with Phase 9 checks
- [ ] Test schema with manual inserts
- [ ] Start application
- [ ] Monitor logs for errors
- [ ] Run smoke tests
- [ ] Document actual downtime
- [ ] Archive backup in safe location

---

## 🎯 Success Criteria

✅ All 4 notification tables created  
✅ All 3 enum types created  
✅ All indexes created successfully  
✅ Application starts without errors  
✅ Test notifications can be inserted  
✅ ChannelPoller can query PENDING notifications  
✅ ReadStatusService can update read status  
✅ All FK relationships valid  
✅ No permission errors for greedys_user  

---

**Next Step**: Execute this guide on the production database server  
**Estimated Time**: 30-60 minutes  
**Risk**: MEDIUM (schema change, but no breaking changes to existing tables)
**Database**: MySQL 8.0+
**Character Set**: utf8mb4 (recommended)
