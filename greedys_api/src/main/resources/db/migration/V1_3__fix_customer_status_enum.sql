-- Migration script to fix customer status enum mapping
-- This script handles the conversion from ordinal values to string values
-- for the customer status column

-- First, let's see what invalid status values we have
-- SELECT DISTINCT status FROM customer WHERE status NOT IN ('BLOCKED', 'DELETED', 'ENABLED', 'DISABLED', 'VERIFY_TOKEN', 'AUTO_DELETE');

-- Backup the current status values (if needed for rollback)
ALTER TABLE customer ADD COLUMN IF NOT EXISTS status_backup VARCHAR(20);
UPDATE customer SET status_backup = status WHERE status_backup IS NULL;

-- Convert any numeric/ordinal values to their corresponding string values
-- Based on the enum definition:
-- BLOCKED = 0, DELETED = 1, ENABLED = 2, DISABLED = 3, VERIFY_TOKEN = 4, AUTO_DELETE = 5

-- Handle potential ordinal values that might exist
UPDATE customer 
SET status = CASE 
    WHEN status = '0' OR status = 0 THEN 'BLOCKED'
    WHEN status = '1' OR status = 1 THEN 'DELETED'
    WHEN status = '2' OR status = 2 THEN 'ENABLED'
    WHEN status = '3' OR status = 3 THEN 'DISABLED'
    WHEN status = '4' OR status = 4 THEN 'VERIFY_TOKEN'
    WHEN status = '5' OR status = 5 THEN 'AUTO_DELETE'
    ELSE 'VERIFY_TOKEN'  -- Default fallback for any invalid values (like 50)
END
WHERE status NOT IN ('BLOCKED', 'DELETED', 'ENABLED', 'DISABLED', 'VERIFY_TOKEN', 'AUTO_DELETE');

-- Handle any remaining invalid values (like the value 50 causing the error)
-- Set them to a default status
UPDATE customer 
SET status = 'VERIFY_TOKEN'
WHERE status NOT IN ('BLOCKED', 'DELETED', 'ENABLED', 'DISABLED', 'VERIFY_TOKEN', 'AUTO_DELETE');

-- Ensure the column is properly defined as VARCHAR with constraints
ALTER TABLE customer 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VERIFY_TOKEN';

-- Add a constraint to ensure only valid enum values are allowed
ALTER TABLE customer 
ADD CONSTRAINT IF NOT EXISTS chk_customer_status 
CHECK (status IN ('BLOCKED', 'DELETED', 'ENABLED', 'DISABLED', 'VERIFY_TOKEN', 'AUTO_DELETE'));

-- Add comment for documentation
ALTER TABLE customer 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VERIFY_TOKEN' 
COMMENT 'Customer status: BLOCKED, DELETED, ENABLED, DISABLED, VERIFY_TOKEN, AUTO_DELETE';

-- Optionally, you can remove the backup column after verification
-- ALTER TABLE customer DROP COLUMN status_backup;
