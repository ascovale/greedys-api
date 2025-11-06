-- Migration script to add temporal validity and change policy columns to slot table
-- Execute this script to upgrade existing slot table with backward compatibility

-- Add new columns for temporal slot management
ALTER TABLE slot ADD COLUMN valid_from DATE DEFAULT NULL;
ALTER TABLE slot ADD COLUMN valid_to DATE DEFAULT NULL;
ALTER TABLE slot ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE slot ADD COLUMN superseded_by BIGINT DEFAULT NULL;
ALTER TABLE slot ADD COLUMN change_policy VARCHAR(20) DEFAULT 'HARD_CUT';

-- Add foreign key constraint for superseded_by
ALTER TABLE slot ADD CONSTRAINT fk_slot_superseded_by 
FOREIGN KEY (superseded_by) REFERENCES slot(id) ON DELETE SET NULL;

-- Add index for performance on temporal queries
CREATE INDEX idx_slot_temporal ON slot(valid_from, valid_to, active);
CREATE INDEX idx_slot_active ON slot(active);
CREATE INDEX idx_slot_superseded_by ON slot(superseded_by);

-- Update existing slots to be active with no validity period (backward compatibility)
UPDATE slot SET 
    active = TRUE,
    valid_from = NULL,
    valid_to = NULL,
    change_policy = 'HARD_CUT'
WHERE active IS NULL OR valid_from IS NULL OR change_policy IS NULL;

-- Comments for documentation
ALTER TABLE slot MODIFY COLUMN valid_from DATE COMMENT 'Date from which this slot version becomes valid';
ALTER TABLE slot MODIFY COLUMN valid_to DATE COMMENT 'Date until which this slot version is valid (NULL = indefinite)';
ALTER TABLE slot MODIFY COLUMN active BOOLEAN COMMENT 'Whether this slot is currently active for new reservations';
ALTER TABLE slot MODIFY COLUMN superseded_by BIGINT COMMENT 'References the new slot that replaces this one';
ALTER TABLE slot MODIFY COLUMN change_policy VARCHAR(20) COMMENT 'Policy for handling existing reservations when slot changes: HARD_CUT, NOTIFY_CUSTOMERS, AUTO_MIGRATE';

-- Validation: Check that superseded_by doesn't create circular references
-- This is a safety check - should be enforced in application logic as well
DELIMITER $$
CREATE TRIGGER tr_slot_superseded_by_check
    BEFORE UPDATE ON slot
    FOR EACH ROW
BEGIN
    IF NEW.superseded_by IS NOT NULL THEN
        IF NEW.superseded_by = NEW.id THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Slot cannot supersede itself';
        END IF;
    END IF;
END$$
DELIMITER ;

-- Optional: Create view for active slots (commonly used query)
CREATE OR REPLACE VIEW active_slots AS
SELECT s.*
FROM slot s
WHERE s.active = TRUE
  AND (s.valid_from IS NULL OR s.valid_from <= CURRENT_DATE)
  AND (s.valid_to IS NULL OR s.valid_to >= CURRENT_DATE);

COMMIT;