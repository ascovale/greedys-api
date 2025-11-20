# 🔗 Foreign Key Dependencies Analysis

**Data**: 20 Novembre 2025  
**Database**: MySQL 8.0+ (greedys_v1)  
**Purpose**: Identify all FK dependencies before deleting old notification tables

---

## 📊 FK Dependency Map

### 1. Foreign Keys FROM Old Notification Tables TO Other Tables

#### admin_notification Table
```
Table: admin_notification
├─ FKb2pmo1suviqfuinyro2enhfs6
│  └─ admin_notification.idcustomer → admin.id
│     Status: ⚠️ BLOCKS deletion (FK constraint)
│
└─ FKnlp6vvqp97tdtx3tft7otqm19
   └─ admin_notification.idreservation → reservation.id
      Status: ⚠️ BLOCKS deletion (FK constraint)
```

#### notification Table
```
Table: notification
└─ FKbphjf8xwneeadbdwgy8m6t52r
   └─ notification.idcustomer → customer.id
      Status: ⚠️ BLOCKS deletion (FK constraint)
```

#### notification_restaurant Table
```
Table: notification_restaurant
└─ FKqmiydipb8wffvikp4nwx10174
   └─ notification_restaurant.RUser_id → restaurant_user.id
      Status: ⚠️ BLOCKS deletion (FK constraint)
```

---

### 2. Foreign Keys TO Old Notification Tables FROM Other Tables

#### notification_event_properties Table
```
Table: notification_event_properties
└─ FKh7wsfuxndbhgxtx40pwja3qtd
   └─ notification_event_properties.notification_id → agency_notification.id
      Status: ⚠️ CHILD TABLE (depends on agency_notification)
```

#### notification_properties Table
```
Table: notification_properties
└─ FKeyqblauo6l3syct1na5dlptsu
   └─ notification_properties.notification_id → notification_restaurant.id
      Status: ⚠️ CHILD TABLE (depends on notification_restaurant)
```

---

### 3. Dependency Tree (What Depends on What)

```
admin.id ←── admin_notification.idcustomer
              ├─ notification_event_properties.notification_id (not direct)
              └─ notification_outbox.notification_id (references admin_notification)

customer.id ←── notification.idcustomer
                ├─ notification_properties.notification_id
                └─ notification_outbox.notification_id

restaurant_user.id ←── notification_restaurant.RUser_id
                       ├─ notification_properties.notification_id
                       └─ notification_outbox.notification_id

reservation.id ←── admin_notification.idreservation
                   └─ notification_outbox.notification_id

notification_outbox has:
  ├─ notification_id (BIGINT, FK back to notification table)
  ├─ notification_type (VARCHAR - indicates which table)
  ├─ aggregate_id (BIGINT - references the entity id)
  ├─ aggregate_type (VARCHAR - type of entity: 'RESTAURANT', 'CUSTOMER', etc)
  ├─ status ENUM('PENDING', 'PUBLISHED', 'FAILED')
  ├─ event_type (VARCHAR)
  ├─ payload (TEXT)
  ├─ retry_count (INT)
  ├─ created_at, processed_at (TIMESTAMPS)
  └─ error_message (TEXT for debugging)
```

---

## ⚠️ Critical Dependencies (MUST HANDLE BEFORE DELETION)

### 1. notification_outbox Table (IMPORTANT!)
**Status**: ✅ STILL NEEDED - Contains event sourcing records

**Structure**:
- `notification_id` (BIGINT) - References the notification (ANY of the 3 tables)
- `notification_type` (VARCHAR) - Discriminator: 'ADMIN', 'RESTAURANT', 'CUSTOMER'
- `aggregate_id` (BIGINT) - The actual entity ID
- `aggregate_type` (VARCHAR) - Type of entity: 'RESTAURANT', 'CUSTOMER', 'AGENCY', 'ADMIN'
- `status` ENUM('PENDING', 'PUBLISHED', 'FAILED')

**Rows**: Unknown (need to check)

**Migration Path**:
1. ✅ DO NOT DELETE `notification_outbox` - Still needed by new system
2. ✅ Update `notification_outbox` to reference new tables instead:
   - Change `notification_id` FK to point to new 4 tables with UNION approach
   - OR create a view that handles the polymorphic FK
   - OR move `notification_id` values to use `event_id` instead

### 2. notification_event_properties Table (INTERMEDIATE)
**Status**: ⚠️ DEPENDS ON agency_notification → DELETE AFTER deleting agency_notification

**Rows**: Unknown (need to check)

**Safe Deletion Order**:
1. First: Delete child tables that reference main tables
   - `notification_properties` (depends on notification_restaurant)
   - `notification_event_properties` (depends on agency_notification)
2. Then: Delete _SEQ tables (no dependencies)
3. Then: Delete main tables (notification, admin_notification, notification_restaurant, agency_notification)
4. LAST: Update notification_outbox or drop it if no longer needed

### 3. Child Tables Referencing Notification Tables
- `notification_properties.notification_id` → `notification_restaurant.id` ✅ DELETE BEFORE notification_restaurant
- `notification_event_properties.notification_id` → `agency_notification.id` ✅ DELETE BEFORE agency_notification

---

## 🔍 Data State Check (CRITICAL BEFORE DELETION)

Let me verify how much data exists in these dependent tables:

### Command to Check Data:
```sql
-- Check notification_outbox (most critical)
SELECT COUNT(*) as outbox_rows FROM notification_outbox;
SELECT DISTINCT notification_type, COUNT(*) FROM notification_outbox GROUP BY notification_type;

-- Check notification_event_properties
SELECT COUNT(*) FROM notification_event_properties;

-- Check notification_properties
SELECT COUNT(*) FROM notification_properties;

-- Check notification_actions
SELECT COUNT(*) FROM notification_actions;

-- Check notification_channel_send
SELECT COUNT(*) FROM notification_channel_send;

-- Check notification_preferences
SELECT COUNT(*) FROM notification_preferences;
```

---

## 🛑 SAFE DELETION ORDER (With FK Handling)

```
Step 1: Disable FK checks
  SET FOREIGN_KEY_CHECKS = 0;

Step 2: Delete child tables first (have FK constraints)
  DROP TABLE IF EXISTS notification_properties;
  DROP TABLE IF EXISTS notification_event_properties;
  DROP TABLE IF EXISTS notification_channel_send;
  DROP TABLE IF EXISTS notification_actions;
  DROP TABLE IF EXISTS notification_preferences;

Step 3: Check notification_outbox usage before deletion
  SELECT COUNT(*) FROM notification_outbox;
  -- If > 0 rows: MIGRATE DATA before deletion
  -- If = 0 rows: Safe to delete

Step 4: Delete or migrate notification_outbox
  IF empty: DROP TABLE notification_outbox;
  IF has data: Keep it OR migrate to new event_id pattern

Step 5: Delete main notification tables (in order to respect FK if re-enabled)
  DROP TABLE IF EXISTS admin_notification_SEQ;
  DROP TABLE IF EXISTS admin_notification;
  DROP TABLE IF EXISTS notification_restaurant_SEQ;
  DROP TABLE IF EXISTS notification_restaurant;
  DROP TABLE IF EXISTS agency_notification;
  DROP TABLE IF EXISTS notification_SEQ;
  DROP TABLE IF EXISTS notification;

Step 6: Re-enable FK checks
  SET FOREIGN_KEY_CHECKS = 1;
```

---

## ✅ Migration Strategy for notification_outbox

The `notification_outbox` table is part of the **Outbox Pattern** for reliable event publishing. It should be preserved because:

1. **Event Sourcing**: Contains all notification events that occurred
2. **Audit Trail**: Records attempts to publish notifications
3. **Retry Logic**: Tracks failed publishes and retry counts

### Option A: Keep notification_outbox (Recommended)
```sql
-- Keep the table but migrate data to use new event_id
-- Update FKs to point to the new notification_* tables
-- This requires careful FK setup with UNION views or polymorphic FK

-- Problem: MySQL doesn't support polymorphic FKs natively
-- Solution: Remove the notification_id FK and use event_id instead
ALTER TABLE notification_outbox DROP FOREIGN KEY <FK_NAME>;
ALTER TABLE notification_outbox ADD COLUMN event_id VARCHAR(255);
-- Populate event_id from new tables based on aggregate_id
UPDATE notification_outbox 
SET event_id = (SELECT event_id FROM notification_restaurant_user WHERE id = notification_id LIMIT 1)
WHERE notification_type = 'RESTAURANT';
-- Similar for other types...
ALTER TABLE notification_outbox DROP COLUMN notification_id;
ALTER TABLE notification_outbox ADD UNIQUE KEY idx_event_id (event_id);
```

### Option B: Drop notification_outbox (If Not Used by New System)
```sql
-- Check if new RabbitListener uses it
SELECT COUNT(*) FROM notification_outbox;
-- If 0 rows and not referenced in application code: DROP TABLE notification_outbox;
```

---

## 📋 Pre-Deletion Checklist

Before executing the deletion script:

- [ ] Check how many rows in `notification_outbox`
- [ ] Check how many rows in `notification_event_properties`
- [ ] Check how many rows in `notification_properties`
- [ ] Decide: Keep or drop `notification_outbox`?
- [ ] Verify no application code queries old tables directly
- [ ] Verify RabbitListener references only `event_id` (not old notification IDs)
- [ ] Create full backup
- [ ] Disable application
- [ ] Execute deletion with FK_CHECKS=0
- [ ] Verify new tables exist and have correct schema
- [ ] Re-enable application
- [ ] Test notification creation and delivery

---

## 🔗 FK Summary Table

| Source Table | FK Name | Column | Target Table | Target Column | Deletable | Notes |
|---|---|---|---|---|---|---|
| admin_notification | FKb2pmo1suviqfuinyro2enhfs6 | idcustomer | admin | id | ❌ NO | Referenced table still needed |
| admin_notification | FKnlp6vvqp97tdtx3tft7otqm19 | idreservation | reservation | id | ❌ NO | Referenced table still needed |
| notification | FKbphjf8xwneeadbdwgy8m6t52r | idcustomer | customer | id | ❌ NO | Referenced table still needed |
| notification_restaurant | FKqmiydipb8wffvikp4nwx10174 | RUser_id | restaurant_user | id | ❌ NO | Referenced table still needed |
| notification_properties | FKeyqblauo6l3syct1na5dlptsu | notification_id | notification_restaurant | id | ✅ YES | Child table, delete after parent |
| notification_event_properties | FKh7wsfuxndbhgxtx40pwja3qtd | notification_id | agency_notification | id | ✅ YES | Child table, delete after parent |

---

## 🎯 Action Items

1. ✅ Map all FK dependencies (DONE)
2. ⏳ Check data in notification_outbox, notification_properties, notification_event_properties
3. ⏳ Decide: Keep or drop notification_outbox?
4. ⏳ Create safe deletion script with SET FOREIGN_KEY_CHECKS = 0/1
5. ⏳ Execute deletion after confirming rows and strategy

