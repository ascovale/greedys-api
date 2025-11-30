-- Migration script to create service_version and availability_exception tables
-- for implementing versioned service configuration and availability management

-- Create service_version table
CREATE TABLE IF NOT EXISTS service_version (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    opening_hours JSON COMMENT 'Opening hours configuration as JSON',
    duration INT NOT NULL COMMENT 'Duration of each reservation slot in minutes',
    slot_generation_params JSON COMMENT 'Slot generation parameters as JSON (start_time, end_time, interval_minutes, etc.)',
    notes VARCHAR(1000) COMMENT 'Additional notes or description about this version',
    state VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'State: ACTIVE or ARCHIVED',
    effective_from DATE NOT NULL COMMENT 'Start date for this version validity (inclusive)',
    effective_to DATE COMMENT 'End date for this version validity (inclusive). NULL means ongoing/no end date',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_version_service_id FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    CONSTRAINT chk_service_version_dates CHECK (effective_from <= effective_to OR effective_to IS NULL),
    KEY idx_service_version_service_id (service_id),
    KEY idx_service_version_effective_from (effective_from),
    KEY idx_service_version_state (state),
    KEY idx_service_version_service_from_state (service_id, effective_from, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Versioned service configurations allowing time-based variation of service parameters';

-- Create availability_exception table
CREATE TABLE IF NOT EXISTS availability_exception (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_version_id BIGINT NOT NULL,
    exception_date DATE NOT NULL COMMENT 'Date of the exception',
    exception_type VARCHAR(50) NOT NULL DEFAULT 'CLOSURE' COMMENT 'Type: CLOSURE, MAINTENANCE, SPECIAL_EVENT, STAFF_SHORTAGE, CUSTOM',
    notes VARCHAR(500) COMMENT 'Additional notes or reason for the exception',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_availability_exception_service_version_id FOREIGN KEY (service_version_id) REFERENCES service_version(id) ON DELETE CASCADE,
    CONSTRAINT uk_availability_exception_version_date UNIQUE KEY (service_version_id, exception_date),
    KEY idx_availability_exception_service_version_id (service_version_id),
    KEY idx_availability_exception_date (exception_date),
    KEY idx_availability_exception_service_version_date (service_version_id, exception_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Availability exceptions for service versions on specific dates';

-- Add service_version_id column to reservation table
ALTER TABLE reservation ADD COLUMN service_version_id BIGINT NOT NULL AFTER id COMMENT 'Foreign key to service_version for versioned reservations';
ALTER TABLE reservation ADD CONSTRAINT fk_reservation_service_version_id FOREIGN KEY (service_version_id) REFERENCES service_version(id) ON DELETE RESTRICT;
ALTER TABLE reservation ADD KEY idx_reservation_service_version_id (service_version_id);

-- Optional: Create view for closure dates (commonly used for availability checks)
CREATE OR REPLACE VIEW closure_dates AS
SELECT 
    sv.service_id,
    ae.exception_date,
    ae.exception_type,
    ae.notes
FROM service_version sv
INNER JOIN availability_exception ae ON sv.id = ae.service_version_id
WHERE ae.exception_type IN ('CLOSURE', 'MAINTENANCE', 'STAFF_SHORTAGE')
ORDER BY sv.service_id, ae.exception_date;
