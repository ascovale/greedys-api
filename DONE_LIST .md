# 📊 Database Migration Analysis - Current vs New Schema

**Data**: 19 Novembre 2025  
**Database**: MySQL 8.0+ (greedys_v1)  
**Status**: Ready for migration planning

---

## 🔍 Current Database State (greedys_v1)

### Existing Notification Tables (13 total)

| Table Name | Rows | Type | Engine | Purpose |
|---|---|---|---|---|
| `notification` | 0 | BASE TABLE | InnoDB | Main Single Table Inheritance table |
| `notification_SEQ` | 0 | BASE TABLE | InnoDB | Sequence generator for notification |
| `admin_notification` | 0 | BASE TABLE | InnoDB | Admin notifications |
| `admin_notification_SEQ` | 0 | BASE TABLE | InnoDB | Sequence for admin |
| `agency_notification` | 0 | BASE TABLE | InnoDB | Agency notifications |
| `notification_restaurant` | 0 | BASE TABLE | InnoDB | Restaurant staff notifications |
| `notification_restaurant_SEQ` | 0 | BASE TABLE | InnoDB | Sequence for restaurant |
| `notification_actions` | 0 | BASE TABLE | InnoDB | Action history |
| `notification_channel_send` | 0 | BASE TABLE | InnoDB | Channel delivery tracking |
| `notification_event_properties` | 0 | BASE TABLE | InnoDB | Event metadata |
| `notification_outbox` | 0 | BASE TABLE | InnoDB | Outbox pattern for events |
| `notification_preferences` | 0 | BASE TABLE | InnoDB | User notification preferences |
| `notification_properties` | 0 | BASE TABLE | InnoDB | Generic properties storage |

### Current notification Table Structure

```sql
DESC notification;

+-------------------+------------------+------+-----+---------+-------+
| Field             | Type             | Null | Key | Default | Extra |
+-------------------+------------------+------+-----+---------+-------+
| id                | bigint           | NO   | PRI | NULL    |       |
| body              | varchar(255)     | NO   |     | NULL    |       |
| is_read           | bit(1)           | YES  |     | NULL    |       |
| title             | varchar(255)     | NO   |     | NULL    |       |
| creation_time     | datetime(6)      | YES  |     | NULL    |       |
| idcustomer        | bigint           | YES  | MUL | NULL    |       |
| read_at           | datetime(6)      | YES  |     | NULL    |       |
| read_by_user_id   | bigint           | YES  |     | NULL    |       |
| shared_read       | bit(1)           | YES  |     | NULL    |       |
| user_id           | bigint           | NO   |     | NULL    |       |
| user_type         | varchar(50)      | NO   |     | NULL    |       |
+-------------------+------------------+------+-----+---------+-------+
```

**Issues with Current Design:**
- ❌ Single table with `user_type` discrimination (violates Single Responsibility)
- ❌ Mixes restaurant, customer, agency, admin in one table
- ❌ Uses VARCHAR for enum (should be ENUM type)
- ❌ Missing `channel`, `status`, `priority` columns for new system
- ❌ Missing `event_id` for idempotency (crucial for RabbitMQ)
- ❌ Missing `properties` JSON for flexible metadata
- ❌ Missing FK relationships to restaurants/agencies
- ❌ Needs 13 tables cleaned up

---

## 🎯 New Schema Design (v2)

### New Notification Tables (4 total + 1 metadata)

| Table Name | Purpose | Rows | Columns | Indexes |
|---|---|---|---|---|
| `enum_definitions` | Enum reference table (metadata) | N/A | 3 | PRIMARY (enum_name, enum_value) |
| `notification_restaurant_user` | Restaurant staff notifications | Dynamic | 14 | 6 (channel+status, restaurant+user, event_id, etc) |
| `notification_customer` | Customer notifications | Dynamic | 13 | 4 (channel+status, event_id, status, user) |
| `notification_agency_user` | Agency staff notifications | Dynamic | 14 | 6 (channel+status, agency+user, event_id, etc) |
| `notification_admin` | Admin notifications | Dynamic | 13 | 4 (channel+status, event_id, status, user) |

### New Table Structure Example (notification_restaurant_user)

```sql
CREATE TABLE IF NOT EXISTS notification_restaurant_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    title VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    channel ENUM('WEBSOCKET', 'EMAIL', 'PUSH', 'SMS') NOT NULL,
    status ENUM('PENDING', 'DELIVERED', 'FAILED', 'READ') NOT NULL DEFAULT 'PENDING',
    priority ENUM('HIGH', 'NORMAL', 'LOW') NOT NULL DEFAULT 'NORMAL',
    properties JSON,
    read_by_all BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    INDEX idx_channel_status_created (channel, status, created_at DESC),
    CONSTRAINT fk_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Advantages of New Design:**
- ✅ Disaggregation pattern: Separate tables per notification type
- ✅ Strong typing: ENUM columns for channels, status, priority
- ✅ Event sourcing: `event_id` UNIQUE for idempotency (RabbitMQ deduplication)
- ✅ Flexible metadata: JSON `properties` column
- ✅ Performance: 6 strategic indexes per table
- ✅ Relationships: FK to restaurants/agencies tables
- ✅ Temporal tracking: `created_at`, `updated_at`, `read_at`
- ✅ Batch operations: `read_by_all` flag for shared read updates

---

## 🔄 Migration Path

### Phase 1: Pre-Migration (No Changes)
- ✅ Database: greedys_v1 identified
- ✅ Root credentials: Verified
- ✅ MySQL version: 8.0+ confirmed
- ✅ Current tables: 13 notification tables identified
- ✅ Data state: All tables empty (0 rows)

### Phase 2: Backup & Disable App
```bash
# Backup all databases
mysqldump -u root -p'MinosseCentoXCento2025' --all-databases > /backups/greedys_v1_backup.sql

# Verify backup
ls -lh /backups/greedys_v1_backup.sql

# Disable application (Docker)
docker service update --force greedys_api_spring-app
sleep 30
docker service ls
```

### Phase 3: Schema Migration
- Create enum_definitions table
- Create 4 new notification tables
- Create 6 indexes per table
- Create 2 UNION views (v_pending_notifications, v_unread_notifications)
- Add FK constraints to restaurants/agencies tables

### Phase 4: Data Migration (if needed)
- Migrate data from old `notification` table to new tables based on `user_type`:
  - `user_type='RESTAURANT'` → `notification_restaurant_user`
  - `user_type='CUSTOMER'` → `notification_customer`
  - `user_type='AGENCY'` → `notification_agency_user`
  - `user_type='ADMIN'` → `notification_admin`

**Current Status**: ✅ No data migration needed (all tables empty)

### Phase 5: Cleanup Old Tables
```sql
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification_properties;
DROP TABLE IF EXISTS notification_preferences;
DROP TABLE IF EXISTS notification_outbox;
DROP TABLE IF EXISTS notification_event_properties;
DROP TABLE IF EXISTS notification_channel_send;
DROP TABLE IF EXISTS notification_actions;
DROP TABLE IF EXISTS notification_restaurant_SEQ;
DROP TABLE IF EXISTS notification_restaurant;
DROP TABLE IF EXISTS notification_SEQ;
DROP TABLE IF EXISTS admin_notification_SEQ;
DROP TABLE IF EXISTS admin_notification;
DROP TABLE IF EXISTS agency_notification;
DROP TABLE IF EXISTS notification;

SET FOREIGN_KEY_CHECKS = 1;
```

### Phase 6: Grant Permissions
```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON greedys_v1.notification_* TO 'greedys_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON greedys_v1.enum_definitions TO 'greedys_user'@'%';
GRANT SELECT ON greedys_v1.v_pending_notifications TO 'greedys_user'@'%';
GRANT SELECT ON greedys_v1.v_unread_notifications TO 'greedys_user'@'%';
FLUSH PRIVILEGES;
```

### Phase 7: Re-enable App & Verify
```bash
# Re-enable application
docker service update --force greedys_api_spring-app
sleep 30

# Verify tables created
mysql -u root -p'MinosseCentoXCento2025' greedys_v1 -e "SHOW TABLES LIKE 'notification_%';"

# Verify views
mysql -u root -p'MinosseCentoXCento2025' greedys_v1 -e "SHOW FULL TABLES WHERE Table_type='VIEW' AND Tables_in LIKE '%notification%';"

# Check indexes
mysql -u root -p'MinosseCentoXCento2025' greedys_v1 -e "SHOW INDEX FROM notification_restaurant_user;"
```

---

## ⚠️ Critical Differences (Old vs New)

### Columns Removed (if had data)
- `user_type` (discrimination column - no longer needed)
- `idcustomer` (will use `user_id` + table type)
- `read_by_user_id` (unclear semantics)

### Columns Added
- `event_id` ✅ UNIQUE - for idempotency
- `channel` ✅ ENUM - delivery method
- `status` ✅ ENUM - delivery status
- `priority` ✅ ENUM - notification priority
- `properties` ✅ JSON - flexible metadata
- `read_by_all` ✅ BOOLEAN - shared read flag
- `updated_at` ✅ TIMESTAMP - audit trail

### Columns Renamed
- `body` → stays `body` (unchanged)
- `title` → stays `title` (unchanged)
- `creation_time` → `created_at` (different format + naming)
- `read_at` → stays `read_at` (unchanged)
- `is_read` → derived from `status='READ'` (no column)
- `shared_read` → `read_by_all` (clearer semantics)

### FK Relationships (New)
```
notification_restaurant_user.restaurant_id → restaurants.id ON DELETE SET NULL
notification_agency_user.agency_id → agencies.id ON DELETE SET NULL
```

---

## 📋 Pre-Migration Checklist

- [x] Identified current database: `greedys_v1`
- [x] Found all 13 notification tables
- [x] Verified table structure (11 columns)
- [x] Confirmed all tables are empty (0 rows)
- [x] Identified MySQL 8.0+ platform
- [x] Created backup strategy in Phase 2
- [ ] Stop application (pending)
- [ ] Execute Flyway migration (V2__notification_schema.sql)
- [ ] Verify new tables created
- [ ] Test RabbitListener persistence
- [ ] Test ChannelPoller queries
- [ ] Re-enable application
- [ ] Run smoke tests
- [ ] Monitor logs for errors

---

## 🚀 Next Steps

1. **Run DB_MIGRATION_GUIDE.md Phase 1-2** (Backup & Disable)
2. **Execute V2__notification_schema.sql** via Flyway (Creates enum + 4 tables)
3. **Verify Phase 3** (Check tables, indexes, views)
4. **Grant Permissions** (Phase 6)
5. **Re-enable Application** (Phase 7)
6. **Integration Testing** (notification flows)

**Estimated Duration**: 15-20 minutes (all tables empty)

---

## 📞 Support

If issues occur:
1. Check `DB_MIGRATION_GUIDE.md` "Troubleshooting" section
2. Use rollback plan in guide
3. Restore from backup: `mysql -u root -p < /backups/greedys_v1_backup.sql`

