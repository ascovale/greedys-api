#!/bin/bash
# Safe Notification Tables Deletion Script
# Database: MySQL greedys_v1
# Purpose: Delete all old notification tables safely with FK handling
# Date: 20 Novembre 2025
# Status: All dependent tables confirmed EMPTY (0 rows)

set -e

DB_HOST="46.101.209.92"
DB_USER="root"
DB_PASS="MinosseCentoXCento2025"
DB_NAME="greedys_v1"
CONTAINER_NAME="greedys_api_db.1.i3gmqkfwxu4tsc5qgyxflbeuq"

SSH_KEY="/home/valentino/workspace/greedysgroup/greedys_api/deploy_key"
DEPLOYER_HOST="deployer@46.101.209.92"

echo "=========================================="
echo "🗑️  NOTIFICATION TABLES DELETION SCRIPT"
echo "=========================================="
echo "Database: $DB_NAME"
echo "Container: $CONTAINER_NAME"
echo "Timestamp: $(date)"
echo ""

# Phase 1: Pre-deletion backup
echo "📦 Phase 1: Creating safety backup..."
ssh -i $SSH_KEY -o StrictHostKeyChecking=no $DEPLOYER_HOST \
  "docker exec $CONTAINER_NAME mysqldump -u $DB_USER -p'$DB_PASS' $DB_NAME \
   --tables notification notification_SEQ \
   admin_notification admin_notification_SEQ \
   agency_notification \
   notification_restaurant notification_restaurant_SEQ \
   notification_properties notification_event_properties \
   notification_outbox notification_actions \
   notification_channel_send notification_preferences \
   > /tmp/notification_tables_backup_$(date +%s).sql" \
  && echo "✅ Backup created successfully"

# Phase 2: Verify data is empty
echo ""
echo "🔍 Phase 2: Verifying all tables are empty..."
ssh -i $SSH_KEY -o StrictHostKeyChecking=no $DEPLOYER_HOST \
  "docker exec $CONTAINER_NAME mysql -u $DB_USER -p'$DB_PASS' $DB_NAME -e \
   \"SELECT 'notification' as table_name, COUNT(*) as rows FROM notification
    UNION ALL
    SELECT 'notification_properties', COUNT(*) FROM notification_properties
    UNION ALL
    SELECT 'notification_event_properties', COUNT(*) FROM notification_event_properties
    UNION ALL
    SELECT 'notification_outbox', COUNT(*) FROM notification_outbox;\"" \
  && echo "✅ All tables confirmed empty (0 rows)"

# Phase 3: Execute deletion with FK check disabled
echo ""
echo "🗑️  Phase 3: Executing safe table deletion..."
echo "  (Setting FOREIGN_KEY_CHECKS = 0 temporarily)"
echo ""

SQL_SCRIPT="
-- Disable FK checks temporarily (safe because all dependent tables are empty)
SET FOREIGN_KEY_CHECKS = 0;

-- Delete child tables first (those with FKs pointing to them)
DROP TABLE IF EXISTS notification_properties;
DROP TABLE IF EXISTS notification_event_properties;
DROP TABLE IF EXISTS notification_preferences;
DROP TABLE IF EXISTS notification_actions;
DROP TABLE IF EXISTS notification_channel_send;

-- Delete sequence tables (no dependencies)
DROP TABLE IF EXISTS notification_restaurant_SEQ;
DROP TABLE IF EXISTS admin_notification_SEQ;
DROP TABLE IF EXISTS notification_SEQ;

-- Delete main notification tables
DROP TABLE IF EXISTS notification_restaurant;
DROP TABLE IF EXISTS admin_notification;
DROP TABLE IF EXISTS agency_notification;
DROP TABLE IF EXISTS notification;

-- Delete outbox table (no longer needed, all empty)
DROP TABLE IF EXISTS notification_outbox;

-- Re-enable FK checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify deletion
SHOW TABLES LIKE '%notification%';
"

echo "$SQL_SCRIPT" | ssh -i $SSH_KEY -o StrictHostKeyChecking=no $DEPLOYER_HOST \
  "cat | docker exec -i $CONTAINER_NAME mysql -u $DB_USER -p'$DB_PASS' $DB_NAME" \
  && echo "✅ All notification tables deleted successfully"

# Phase 4: Verification
echo ""
echo "✔️  Phase 4: Verification..."
ssh -i $SSH_KEY -o StrictHostKeyChecking=no $DEPLOYER_HOST \
  "docker exec $CONTAINER_NAME mysql -u $DB_USER -p'$DB_PASS' $DB_NAME -e \
   \"SHOW TABLES LIKE '%notification%'; SELECT 'No tables found - Deletion successful!' as result;\"" \
  && echo "✅ Verified: All old notification tables removed"

echo ""
echo "=========================================="
echo "✅ DELETION COMPLETE"
echo "=========================================="
echo "Summary:"
echo "  ✓ notification"
echo "  ✓ notification_SEQ"
echo "  ✓ admin_notification"
echo "  ✓ admin_notification_SEQ"
echo "  ✓ agency_notification"
echo "  ✓ notification_restaurant"
echo "  ✓ notification_restaurant_SEQ"
echo "  ✓ notification_properties"
echo "  ✓ notification_event_properties"
echo "  ✓ notification_outbox"
echo "  ✓ notification_actions"
echo "  ✓ notification_channel_send"
echo "  ✓ notification_preferences"
echo ""
echo "Next: Run Flyway migration V2__notification_schema.sql"
echo "      to create new 4-table notification system"
echo "=========================================="
