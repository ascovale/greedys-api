-- Migration V7: Hybrid Service Scheduling Architecture
-- SCHEMA CHANGES:
-- 1. service_version_day: Recurring weekly schedules (7 records per version)
-- 2. service_version_slot_config: Slot generation parameters (replaces JSON)
-- 3. availability_exception: Enhanced with partial-day closure support
-- Allows queryable, flexible scheduling without JSON parsing

-- Create service_version_slot_config table (replaces slotGenerationParams JSON)
-- Each ServiceVersion has slot generation config (start_time, end_time, duration, buffer, max_concurrent)
CREATE TABLE IF NOT EXISTS service_version_slot_config (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_version_id BIGINT NOT NULL,
    start_time TIME NOT NULL COMMENT 'When to START generating slots (e.g., 09:00)',
    end_time TIME NOT NULL COMMENT 'When to STOP generating slots (e.g., 23:00)',
    slot_duration_minutes INT NOT NULL DEFAULT 30 COMMENT 'Length of each slot in minutes (e.g., 30)',
    buffer_minutes INT NOT NULL DEFAULT 0 COMMENT 'Gap between slots in minutes (e.g., 5)',
    max_concurrent_reservations INT NOT NULL DEFAULT 10 COMMENT 'Max concurrent bookings per slot (e.g., 10 covers)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_version_slot_config_service_version_id FOREIGN KEY (service_version_id) REFERENCES service_version(id) ON DELETE CASCADE,
    KEY idx_service_version_slot_config_service_version_id (service_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Slot generation parameters for ServiceVersion - replaces JSON slotGenerationParams field';

-- Create service_version_day table for recurring weekly schedules
-- Each ServiceVersion has 7 records (one per day of week: 0=SUN to 6=SAT)
CREATE TABLE IF NOT EXISTS service_version_day (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_version_id BIGINT NOT NULL,
    day_of_week INT NOT NULL COMMENT '0=SUN, 1=MON, 2=TUE, 3=WED, 4=THU, 5=FRI, 6=SAT',
    opening_time TIME COMMENT 'Opening time for this day (e.g., 09:00). NULL means closed',
    closing_time TIME COMMENT 'Closing time for this day (e.g., 23:00)',
    is_closed BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'If TRUE, restaurant is closed this day (overrides opening/closing times)',
    max_reservations INT COMMENT 'Maximum concurrent reservations for this day',
    slot_duration INT NOT NULL DEFAULT 30 COMMENT 'Duration of each reservation slot in minutes',
    break_start TIME COMMENT 'Break start time (e.g., 14:00 for siesta)',
    break_end TIME COMMENT 'Break end time (e.g., 15:30)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_version_day_service_version_id FOREIGN KEY (service_version_id) REFERENCES service_version(id) ON DELETE CASCADE,
    CONSTRAINT uk_service_version_day_version_dayofweek UNIQUE KEY (service_version_id, day_of_week),
    KEY idx_service_version_day_service_version_id (service_version_id),
    KEY idx_service_version_day_dayofweek (day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Weekly recurring schedules for service versions - allows queryable day-of-week configuration';

-- Update availability_exception table to support partial-day closures
ALTER TABLE availability_exception ADD COLUMN start_time TIME COMMENT 'Start time for partial-day exception (e.g., 12:00). NULL = whole day' AFTER exception_date;
ALTER TABLE availability_exception ADD COLUMN end_time TIME COMMENT 'End time for partial-day exception (e.g., 14:00)' AFTER start_time;
ALTER TABLE availability_exception ADD COLUMN override_opening_time TIME COMMENT 'Override opening time for this date (if different from ServiceVersionDay)' AFTER end_time;
ALTER TABLE availability_exception ADD COLUMN override_closing_time TIME COMMENT 'Override closing time for this date' AFTER override_opening_time;
ALTER TABLE availability_exception ADD COLUMN is_fully_closed BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'If TRUE, entire day is closed (no reservations accepted)' AFTER override_closing_time;

-- Create index for efficient querying of exceptions by date
CREATE INDEX idx_availability_exception_date_type ON availability_exception(exception_date, exception_type);

-- Add index for common query pattern: find exceptions for a specific day
CREATE INDEX idx_availability_exception_service_version_date ON availability_exception(service_version_id, exception_date);

-- Add comment documenting the improved architecture
ALTER TABLE availability_exception 
COMMENT='Date-specific exceptions and overrides to ServiceVersionDay - supports partial-day closures and custom hours';

-- Drop old view if it exists (will be recreated if needed)
DROP VIEW IF EXISTS closure_dates;

-- Create improved view for closure dates (now includes time ranges)
CREATE OR REPLACE VIEW closure_dates AS
SELECT 
    sv.service_id,
    ae.exception_date,
    ae.start_time,
    ae.end_time,
    ae.exception_type,
    ae.notes,
    ae.is_fully_closed
FROM service_version sv
INNER JOIN availability_exception ae ON sv.id = ae.service_version_id
WHERE ae.exception_type IN ('CLOSURE', 'MAINTENANCE', 'STAFF_SHORTAGE')
  AND ae.is_fully_closed = TRUE
ORDER BY sv.service_id, ae.exception_date;

-- Create view for partial-day exceptions (useful for maintenance windows, breaks)
CREATE OR REPLACE VIEW partial_closures AS
SELECT 
    sv.service_id,
    ae.exception_date,
    ae.start_time,
    ae.end_time,
    ae.exception_type,
    ae.notes
FROM service_version sv
INNER JOIN availability_exception ae ON sv.id = ae.service_version_id
WHERE ae.start_time IS NOT NULL 
  AND ae.end_time IS NOT NULL
  AND ae.is_fully_closed = FALSE
ORDER BY sv.service_id, ae.exception_date, ae.start_time;
