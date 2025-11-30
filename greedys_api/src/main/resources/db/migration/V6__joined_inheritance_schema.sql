-- Migration: V6__joined_inheritance_schema.sql
-- Purpose: Refactor from TABLE_PER_CLASS to JOINED inheritance for AbstractUser and related entities
-- Date: November 30, 2025
--
-- REFACTORING:
-- - BEFORE: TABLE_PER_CLASS with separate tables (customer, ruser, admin, agency_user) each with own id + common columns
-- - AFTER: JOINED inheritance with:
--   * abstract_user table (PK id, common columns, timestamps)
--   * customer table (FK id to abstract_user, customer-specific columns)
--   * ruser table (FK id to abstract_user, restaurant-specific columns)
--   * admin table (FK id to abstract_user, admin-specific columns)
--   * agency_user table (FK id to abstract_user, agency-specific columns)
--
-- BENEFITS:
-- - Unified ID across all user types (no more duplicate IDs across tables)
-- - Polymorphic queries on AbstractUser
-- - Cleaner auditing (createdBy/modifiedBy reference AbstractUser directly)
-- - No more user_type column needed for discrimination (use class type instead)
--
-- MIGRATION STRATEGY:
-- 1. Create abstract_user table with unified sequence
-- 2. Migrate existing user data from old tables
-- 3. Update ForeignKey references to abstract_user.id
-- 4. Remove user_type columns from Reservation and NotificationPreferences

-- ============================================================================
-- PHASE 1: Create abstract_user table (parent table for JOINED inheritance)
-- ============================================================================

CREATE TABLE IF NOT EXISTS abstract_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Unified PK for all user types',
    
    -- Common user fields
    name VARCHAR(255) NOT NULL COMMENT 'First name',
    surname VARCHAR(255) NOT NULL COMMENT 'Last name',
    nick_name VARCHAR(255) COMMENT 'Nickname/display name',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT 'Email address (unique across all user types)',
    password VARCHAR(60) COMMENT 'BCrypt hashed password (nullable for UNREGISTERED customers)',
    phone_number VARCHAR(20) COMMENT 'Phone number',
    to_read_notification INT DEFAULT 0 COMMENT 'Count of unread notifications',
    
    -- Auditing fields
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last modification timestamp',
    created_by_user_id BIGINT COMMENT 'FK to abstract_user who created this record',
    modified_by_user_id BIGINT COMMENT 'FK to abstract_user who last modified this record',
    accepted_by_user_id BIGINT COMMENT 'FK to abstract_user who accepted (for reservations)',
    accepted_at DATETIME COMMENT 'When accepted (for reservations)',
    
    CONSTRAINT uk_abstract_user_email UNIQUE KEY (email),
    CONSTRAINT fk_abstract_user_created_by FOREIGN KEY (created_by_user_id) REFERENCES abstract_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_abstract_user_modified_by FOREIGN KEY (modified_by_user_id) REFERENCES abstract_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_abstract_user_accepted_by FOREIGN KEY (accepted_by_user_id) REFERENCES abstract_user(id) ON DELETE SET NULL,
    
    KEY idx_abstract_user_email (email),
    KEY idx_abstract_user_created_at (created_at),
    KEY idx_abstract_user_created_by (created_by_user_id),
    KEY idx_abstract_user_modified_by (modified_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Parent table for JOINED inheritance - unified user base with common fields';

-- ============================================================================
-- PHASE 2: Create customer join table
-- ============================================================================

CREATE TABLE IF NOT EXISTS customer (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'FK to abstract_user.id',
    status ENUM('UNREGISTERED', 'BLOCKED', 'DELETED', 'ENABLED', 'DISABLED', 'VERIFY_TOKEN', 'AUTO_DELETE') 
        NOT NULL DEFAULT 'VERIFY_TOKEN' COMMENT 'Customer account status',
    date_of_birth DATE COMMENT 'Customer date of birth',
    
    CONSTRAINT fk_customer_id FOREIGN KEY (id) REFERENCES abstract_user(id) ON DELETE CASCADE,
    KEY idx_customer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='JOINED inheritance table for Customer - extends AbstractUser';

-- ============================================================================
-- PHASE 3: Create ruser (Restaurant User) join table
-- ============================================================================

CREATE TABLE IF NOT EXISTS ruser (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'FK to abstract_user.id',
    restaurant_id BIGINT NOT NULL COMMENT 'FK to restaurant',
    role_name VARCHAR(100) COMMENT 'Role in restaurant (chef, waiter, manager, etc)',
    is_manager BOOLEAN DEFAULT FALSE COMMENT 'Manager status',
    
    CONSTRAINT fk_ruser_id FOREIGN KEY (id) REFERENCES abstract_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ruser_restaurant_id FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE,
    
    KEY idx_ruser_restaurant_id (restaurant_id),
    KEY idx_ruser_is_manager (is_manager)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='JOINED inheritance table for RUser (Restaurant User) - extends AbstractUser';

-- ============================================================================
-- PHASE 4: Create admin join table
-- ============================================================================

CREATE TABLE IF NOT EXISTS admin (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'FK to abstract_user.id',
    admin_level INT DEFAULT 1 COMMENT 'Admin level/hierarchy',
    
    CONSTRAINT fk_admin_id FOREIGN KEY (id) REFERENCES abstract_user(id) ON DELETE CASCADE,
    KEY idx_admin_level (admin_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='JOINED inheritance table for Admin - extends AbstractUser';

-- ============================================================================
-- PHASE 5: Create agency_user join table
-- ============================================================================

CREATE TABLE IF NOT EXISTS agency_user (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'FK to abstract_user.id',
    agency_id BIGINT NOT NULL COMMENT 'FK to agency',
    is_admin BOOLEAN DEFAULT FALSE COMMENT 'Agency admin status',
    
    CONSTRAINT fk_agency_user_id FOREIGN KEY (id) REFERENCES abstract_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_agency_user_agency_id FOREIGN KEY (agency_id) REFERENCES agency(id) ON DELETE CASCADE,
    
    KEY idx_agency_user_agency_id (agency_id),
    KEY idx_agency_user_is_admin (is_admin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='JOINED inheritance table for AgencyUser - extends AbstractUser';

-- ============================================================================
-- PHASE 6: Update Reservation table - Remove user_type columns, ensure FK references
-- ============================================================================

-- Remove old user_type columns if they exist (backward compatibility)
ALTER TABLE reservation DROP COLUMN IF EXISTS created_by_user_type;
ALTER TABLE reservation DROP COLUMN IF EXISTS modified_by_user_type;

-- Update ForeignKey references to point to abstract_user (if not already done)
ALTER TABLE reservation 
    DROP FOREIGN KEY IF EXISTS fk_reservation_created_by,
    ADD CONSTRAINT fk_reservation_created_by FOREIGN KEY (created_by_user_id) REFERENCES abstract_user(id) ON DELETE SET NULL;

ALTER TABLE reservation 
    DROP FOREIGN KEY IF EXISTS fk_reservation_modified_by,
    ADD CONSTRAINT fk_reservation_modified_by FOREIGN KEY (modified_by_user_id) REFERENCES abstract_user(id) ON DELETE SET NULL;

ALTER TABLE reservation 
    DROP FOREIGN KEY IF EXISTS fk_reservation_accepted_by,
    ADD CONSTRAINT fk_reservation_accepted_by FOREIGN KEY (accepted_by_user_id) REFERENCES abstract_user(id) ON DELETE SET NULL;

-- ============================================================================
-- PHASE 7: Update NotificationPreferences table - Remove user_type column
-- ============================================================================

ALTER TABLE notification_preferences 
    DROP COLUMN IF EXISTS user_type;

-- Update unique constraint (remove user_type, keep only user_id)
ALTER TABLE notification_preferences 
    DROP CONSTRAINT IF EXISTS uk_user,
    ADD CONSTRAINT uk_user_id UNIQUE KEY (user_id);

-- Add FK to abstract_user if not present
ALTER TABLE notification_preferences 
    DROP FOREIGN KEY IF EXISTS fk_notification_preferences_user_id;

ALTER TABLE notification_preferences 
    ADD CONSTRAINT fk_notification_preferences_user_id FOREIGN KEY (user_id) REFERENCES abstract_user(id) ON DELETE CASCADE;

-- ============================================================================
-- PHASE 8: Update AgencyNotification table (if needed for polymorphic FK)
-- ============================================================================

-- Ensure agency_user_id exists and references abstract_user
ALTER TABLE agency_notification 
    DROP FOREIGN KEY IF EXISTS fk_agency_notification_agency_user_id;

ALTER TABLE agency_notification 
    ADD CONSTRAINT fk_agency_notification_agency_user_id FOREIGN KEY (agency_user_id) REFERENCES abstract_user(id) ON DELETE CASCADE;

-- ============================================================================
-- PHASE 9: Create view for polymorphic user queries (optional)
-- ============================================================================

CREATE OR REPLACE VIEW all_users AS
SELECT 
    u.id,
    u.name,
    u.surname,
    u.email,
    u.phone_number,
    'CUSTOMER' as user_type,
    u.created_at,
    u.modified_at
FROM abstract_user u
INNER JOIN customer c ON u.id = c.id

UNION ALL

SELECT 
    u.id,
    u.name,
    u.surname,
    u.email,
    u.phone_number,
    'RESTAURANT_USER' as user_type,
    u.created_at,
    u.modified_at
FROM abstract_user u
INNER JOIN ruser r ON u.id = r.id

UNION ALL

SELECT 
    u.id,
    u.name,
    u.surname,
    u.email,
    u.phone_number,
    'ADMIN' as user_type,
    u.created_at,
    u.modified_at
FROM abstract_user u
INNER JOIN admin a ON u.id = a.id

UNION ALL

SELECT 
    u.id,
    u.name,
    u.surname,
    u.email,
    u.phone_number,
    'AGENCY_USER' as user_type,
    u.created_at,
    u.modified_at
FROM abstract_user u
INNER JOIN agency_user au ON u.id = au.id;

-- ============================================================================
-- Migration Notes
-- ============================================================================
-- This migration assumes that existing user tables (customer, ruser, admin, agency_user)
-- will be replaced by the JOINED inheritance structure.
-- 
-- The data migration from old structure to new structure should be handled by:
-- 1. Application startup logic if data exists in old tables
-- 2. Separate migration script if coordination with old/new structure is needed
-- 3. Flyway callback script if complex transformation is required
--
-- Key points:
-- - ID is now unified across all user types (no more duplicates)
-- - createdBy/modifiedBy/acceptedBy reference abstract_user directly
-- - No more user_type column in Reservation or NotificationPreferences
-- - Use class type (instanceof) or SQL JOINs to determine user type
-- ============================================================================
