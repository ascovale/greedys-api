-- Migration script for Restaurant Verification functionality
-- Add verification fields to restaurant table and create verification table

-- 1. Add verification fields to the restaurant table
ALTER TABLE restaurant 
ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS phone_verified_at DATETIME NULL;

-- 2. Create restaurant_verification table
CREATE TABLE IF NOT EXISTS restaurant_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    verification_sid VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    verified_at DATETIME NULL,
    attempts INT NOT NULL DEFAULT 0,
    updated_at DATETIME NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_verification_restaurant 
        FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) 
        ON DELETE CASCADE,
    
    -- Indexes for better performance
    INDEX idx_restaurant_verification_restaurant_id (restaurant_id),
    INDEX idx_restaurant_verification_status (status),
    INDEX idx_restaurant_verification_created_at (created_at),
    INDEX idx_restaurant_verification_expires_at (expires_at),
    INDEX idx_restaurant_verification_sid (verification_sid)
);

-- 3. Comments for documentation
ALTER TABLE restaurant_verification 
COMMENT = 'Stores phone verification records for restaurants using Twilio Verify';

-- 4. Add constraints for status enum values
ALTER TABLE restaurant_verification 
ADD CONSTRAINT chk_verification_status 
CHECK (status IN ('NOT_STARTED', 'PENDING', 'VERIFIED', 'FAILED', 'EXPIRED', 'CANCELLED'));

-- 5. Update existing restaurants to have phone_verified = FALSE if NULL
UPDATE restaurant 
SET phone_verified = FALSE 
WHERE phone_verified IS NULL;
