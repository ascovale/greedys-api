-- Fix old reservations: set userName to a default value for reservations that don't have it
-- This updates all reservations where userName is NULL or empty

-- First, let's see how many need to be fixed
SELECT COUNT(*) as 'Reservations without userName' FROM reservation WHERE userName IS NULL OR userName = '';

-- Update all reservations without userName
-- We'll use a pattern: "Guest" + reservation ID
UPDATE reservation 
SET userName = CONCAT('Guest ', id)
WHERE userName IS NULL OR userName = '';

-- Verify the update
SELECT COUNT(*) as 'Reservations with userName' FROM reservation WHERE userName IS NOT NULL AND userName != '';

-- Show a sample of updated reservations
SELECT id, userName, pax, r_date, slot_id FROM reservation 
WHERE userName LIKE 'Guest%' 
LIMIT 10;

-- Alternative: If you want to set all to a specific name
-- UPDATE reservation 
-- SET userName = 'Test Guest'
-- WHERE userName IS NULL OR userName = '';

-- Alternative: Set names based on patterns (if customer info available)
-- UPDATE reservation r
-- LEFT JOIN customer c ON r.customer_id = c.id
-- SET r.userName = COALESCE(c.firstName, 'Guest')
-- WHERE r.userName IS NULL OR r.userName = '';
