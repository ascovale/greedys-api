-- Migration script to fix reservation status enum mapping
-- This script handles the conversion from ordinal values to string values
-- for the reservation status column

-- First, let's see what invalid status values we have
-- SELECT DISTINCT status FROM reservation WHERE status NOT IN ('NOT_ACCEPTED', 'ACCEPTED', 'REJECTED', 'SEATED', 'NO_SHOW', 'DELETED');

-- Backup the current status values (if needed for rollback)
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS status_backup VARCHAR(20);
UPDATE reservation SET status_backup = status WHERE status_backup IS NULL;

-- Convert any numeric/ordinal values to their corresponding string values
-- Based on the enum definition:
-- NOT_ACCEPTED = 0, ACCEPTED = 1, REJECTED = 2, SEATED = 3, NO_SHOW = 4, DELETED = 5

-- Handle potential ordinal values that might exist
UPDATE reservation 
SET status = CASE 
    WHEN status = '0' OR status = 0 THEN 'NOT_ACCEPTED'
    WHEN status = '1' OR status = 1 THEN 'ACCEPTED'
    WHEN status = '2' OR status = 2 THEN 'REJECTED'
    WHEN status = '3' OR status = 3 THEN 'SEATED'
    WHEN status = '4' OR status = 4 THEN 'NO_SHOW'
    WHEN status = '5' OR status = 5 THEN 'DELETED'
    ELSE 'NOT_ACCEPTED'  -- Default fallback for any invalid values
END
WHERE status NOT IN ('NOT_ACCEPTED', 'ACCEPTED', 'REJECTED', 'SEATED', 'NO_SHOW', 'DELETED');

-- Handle any remaining invalid values (like the value 50 causing the error)
-- Set them to a default status
UPDATE reservation 
SET status = 'NOT_ACCEPTED'
WHERE status NOT IN ('NOT_ACCEPTED', 'ACCEPTED', 'REJECTED', 'SEATED', 'NO_SHOW', 'DELETED');

-- Ensure the column is properly defined as VARCHAR with constraints
ALTER TABLE reservation 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NOT_ACCEPTED';

-- Add a constraint to ensure only valid enum values are allowed
ALTER TABLE reservation 
ADD CONSTRAINT IF NOT EXISTS chk_reservation_status 
CHECK (status IN ('NOT_ACCEPTED', 'ACCEPTED', 'REJECTED', 'SEATED', 'NO_SHOW', 'DELETED'));

-- Add comment for documentation
ALTER TABLE reservation 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NOT_ACCEPTED' 
COMMENT 'Reservation status: NOT_ACCEPTED, ACCEPTED, REJECTED, SEATED, NO_SHOW, DELETED';

-- Optionally, you can remove the backup column after verification
-- ALTER TABLE reservation DROP COLUMN status_backup;
