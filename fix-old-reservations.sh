#!/bin/bash

# Script to update old reservations with userName
# Connects to the MySQL database on the remote server and updates reservations

DB_HOST="localhost"  # When SSH tunneling, use localhost
DB_PORT="3306"
DB_USER="root"
DB_NAME="greedys_v1"
DB_PASSWORD="root123"

echo "🔍 Checking reservations without userName..."

# Check how many need fixing
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e \
"SELECT COUNT(*) as 'Reservations without userName' FROM reservation WHERE userName IS NULL OR userName = '';"

echo ""
echo "✏️ Updating reservations with default userName (Guest + ID)..."

# Update reservations
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e \
"UPDATE reservation 
SET userName = CONCAT('Guest ', id)
WHERE userName IS NULL OR userName = '';"

echo ""
echo "✅ Verification - checking updated reservations..."

# Verify
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e \
"SELECT COUNT(*) as 'Reservations with userName' FROM reservation WHERE userName IS NOT NULL AND userName != '';"

echo ""
echo "📋 Sample of updated reservations:"

mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e \
"SELECT id, userName, pax, r_date, slot_id FROM reservation 
WHERE userName LIKE 'Guest%' 
LIMIT 10;"

echo ""
echo "✨ Done!"
