# Notification System Deployment - Complete Checklist

## ENVIRONMENT VARIABLES (Keep this visible!)

```
SSH_HOST="46.101.209.92"
SSH_USER="deployer"
SSH_KEY="/home/valentino/workspace/greedysgroup/greedys_api/deploy_key"

DB_HOST="localhost"
DB_USER="root"
DB_PASS="MinosseCentoXCento2025"
DB_NAME="greedys_v1"
DB_CONTAINER="greedys_api_db.1.i3gmqkfwxu4tsc5qgyxflbeuq"

RABBITMQ_USER="greedys_user"
RABBITMQ_PASS="greedys_rabbitmq_pass_2025"
RABBITMQ_VHOST="greedys"
RABBITMQ_CONTAINER="greedys_rabbitmq"

APP_CONTAINER_SERVICE="greedys_api_spring-app"
APP_VERSION="2.0.0"
```

## IMPORTANT NOTES

1. **STATUS**: PREPARING PROMPTS - NOT EXECUTING YET
2. **WORKFLOW**: Execute ONE step at a time
3. **AFTER EACH STEP**: Verify success criteria before proceeding
4. **COMPLETED STEPS**: Move to DONE_LIST.md with timestamp
5. **ERRORS**: Document in ERROR_LOG.md and iterate


---

## PHASE 1: DATABASE MIGRATION

### Step 1: Stop Greedys Application

**What**: Disable app to prevent writes during schema migration
**How**: Scale Docker service to 0 replicas

**Commands**:
```bash
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "docker service ls | grep spring-app"
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "docker service update --force greedys_api_spring-app --replicas=0"
sleep 10
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "docker ps | grep spring-app"
```

**Success Criteria**:
- [ ] Service shows 0 running tasks
- [ ] No spring-app containers running
- [ ] No new logs appearing

---

### Step 2: Create Database Backup (Pre-Migration)

**What**: Safety backup before schema changes
**Why**: Rollback protection

**Commands**:
```bash
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "mkdir -p /home/deployer/backups/$(date +%Y%m%d)"
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec $DB_CONTAINER mysqldump -u $DB_USER -p'$DB_PASS' \
   --all-databases --single-transaction > \
   /home/deployer/backups/$(date +%Y%m%d)/greedys_backup_$(date +%s).sql"
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "ls -lh /home/deployer/backups/*/*.sql"
```

**Success Criteria**:
- [ ] Backup file created
- [ ] File size > 1MB (indicates non-empty database)
- [ ] File is readable

---

### Step 3: Delete Old Notification Tables

**What**: Remove 13 old notification tables
**Why**: Clean slate for new schema
**Status**: All tables confirmed empty (0 rows)

**Commands**:
```bash
chmod +x /home/valentino/workspace/greedysgroup/greedys_api/delete_old_notification_tables.sh
bash /home/valentino/workspace/greedysgroup/greedys_api/delete_old_notification_tables.sh
```

**Expected Output**:
- Creates backup
- Disables FK checks temporarily
- Drops all 13 notification tables
- Re-enables FK checks
- Message: "✅ DELETION COMPLETE"

**Success Criteria**:
- [ ] Script completes without errors
- [ ] All 13 tables deleted
- [ ] Final verification: "No tables found - Deletion successful!"

---

### Step 4: Execute Flyway Migration

**What**: Create new 4-table notification schema via Flyway
**How**: Maven builds and Flyway auto-runs migrations

**Creates**:
- enum_definitions (metadata table)
- notification_restaurant_user (4 columns + 6 indexes)
- notification_customer (4 columns + 4 indexes)
- notification_agency_user (4 columns + 6 indexes)
- notification_admin (4 columns + 4 indexes)
- v_pending_notifications (UNION view)
- v_unread_notifications (UNION view)

**Commands**:
```bash
cd /home/valentino/workspace/greedysgroup/greedys_api/greedys_api
mvn clean package -DskipTests -Pprod
```

**Verify**:
```bash
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec $DB_CONTAINER mysql -u $DB_USER -p'$DB_PASS' $DB_NAME \
   -e \"SHOW TABLES LIKE 'notification_%'; SHOW FULL TABLES WHERE Table_type='VIEW';\""
```

**Success Criteria**:
- [ ] Build succeeds: "BUILD SUCCESS"
- [ ] All 4 notification tables created
- [ ] Both views created
- [ ] enum_definitions populated (12 entries)

---

## PHASE 2: RABBITMQ SETUP

### Step 5: Install RabbitMQ Docker Container

**What**: Deploy RabbitMQ message broker
**Port**: 5672 (AMQP), 15672 (Management UI)

**Commands**:
```bash
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "docker network ls | grep greedys"
# Create /home/deployer/rabbitmq-compose.yml with compose content
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "cd /home/deployer && docker-compose -f rabbitmq-compose.yml up -d"
sleep 30
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "docker ps | grep rabbitmq"
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "docker logs greedys_rabbitmq | tail -20"
```

**Compose File** (/home/deployer/rabbitmq-compose.yml):
```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: greedys_rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: greedys_user
      RABBITMQ_DEFAULT_PASS: greedys_rabbitmq_pass_2025
      RABBITMQ_DEFAULT_VHOST: greedys
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - greedys_api_default
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 10s
      timeout: 5s
      retries: 5
volumes:
  rabbitmq_data:
networks:
  greedys_api_default:
    external: true
```

**Success Criteria**:
- [ ] Container running and healthy
- [ ] Port 5672 listening
- [ ] Management UI accessible on 15672
- [ ] Logs show "RabbitMQ ... started"

---

### Step 6: Configure RabbitMQ User and Permissions

**What**: Create application user and grant permissions
**User**: greedys_user
**VHost**: greedys
**Permissions**: Configure, Write, Read (all resources)

**Commands**:
```bash
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl add_vhost greedys"

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl add_user greedys_user greedys_rabbitmq_pass_2025"

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl set_permissions -p greedys greedys_user '.*' '.*' '.*'"

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl list_users"

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl list_vhosts"
```

**Success Criteria**:
- [ ] User greedys_user created
- [ ] VHost greedys created
- [ ] Permissions configured: [".*" ".*" ".*"]
- [ ] `rabbitmqctl list_users` shows greedys_user

---

### Step 7: Create Docker Secrets for RabbitMQ

**What**: Store credentials securely in Docker Swarm
**Method**: Docker secrets (encrypted at rest)

**Commands**:
```bash
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "echo 'amqp://greedys_user:greedys_rabbitmq_pass_2025@localhost:5672/greedys' | \
   docker secret create rabbitmq_uri -"

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST "docker secret ls"
```

**Success Criteria**:
- [ ] Secret created: rabbitmq_uri
- [ ] `docker secret ls` shows rabbitmq_uri

---

## PHASE 3: APPLICATION DEPLOYMENT

### Step 8: Update docker-compose.yml

**What**: Configure Spring Boot to connect to RabbitMQ
**File**: docker-compose.prod.yml

**Add to environment section**:
```yaml
SPRING_RABBITMQ_HOST: rabbitmq
SPRING_RABBITMQ_PORT: 5672
SPRING_RABBITMQ_USERNAME: greedys_user
SPRING_RABBITMQ_PASSWORD: greedys_rabbitmq_pass_2025
SPRING_RABBITMQ_VIRTUALHOST: greedys
SPRING_RABBITMQ_REQUESTHEARTBEAT: 60
SPRING_RABBITMQ_CONNECTIONTIMEOUT: 5000
NOTIFICATION_ENABLED: "true"
NOTIFICATION_CHANNELS: WEBSOCKET,EMAIL,PUSH,SMS
```

**Verify**:
```bash
docker-compose -f docker-compose.prod.yml config 2>&1 | grep -i rabbitmq
```

**Success Criteria**:
- [ ] All RabbitMQ env vars added
- [ ] YAML parses without errors
- [ ] Environment vars visible in config output

---

### Step 9: Build New Application JAR

**What**: Compile and package v2.0.0 with 22 new notification files
**Time**: ~5-10 minutes

**Commands**:
```bash
cd /home/valentino/workspace/greedysgroup/greedys_api/greedys_api
mvn clean package -DskipTests -Pprod 2>&1 | tee build.log

ls -lh target/greedys_api-*.jar
grep "BUILD SUCCESS" build.log
```

**Success Criteria**:
- [ ] JAR created: target/greedys_api-*.jar
- [ ] JAR size > 50MB
- [ ] Log shows "BUILD SUCCESS"

---

### Step 10: Create Docker Image

**What**: Package JAR into Docker image v2.0.0
**Base**: OpenJDK 21-jdk-slim

**Commands**:
```bash
cd /home/valentino/workspace/greedysgroup/greedys_api
docker build -t greedys_api:2.0.0 -f Dockerfile .

docker images | grep greedys_api
```

**Success Criteria**:
- [ ] Image created: greedys_api:2.0.0
- [ ] Image size 500MB-1GB
- [ ] `docker images` shows new image

---

### Step 11: Deploy Application to Server

**What**: Upload Docker image and update service
**Steps**: Save → Copy → Load → Update

**Commands**:
```bash
# Local: Save image
docker save greedys_api:2.0.0 | gzip > greedys_api_2.0.0.tar.gz
ls -lh greedys_api_2.0.0.tar.gz

# Local: Copy to server
scp -i $SSH_KEY greedys_api_2.0.0.tar.gz deployer@46.101.209.92:/home/deployer/images/

# Remote: Load image
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "cd /home/deployer/images && docker load < greedys_api_2.0.0.tar.gz"

# Remote: Update service
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker service update --image greedys_api:2.0.0 greedys_api_spring-app && sleep 30"

# Remote: Verify
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker service ps greedys_api_spring-app"
```

**Success Criteria**:
- [ ] Image tar.gz copied to server
- [ ] Image loaded on server
- [ ] Service updated with new image
- [ ] Container running (not restarting)

---

## PHASE 4: VERIFICATION & TESTING

### Step 12: Verify Application Startup

**What**: Check logs for startup errors and RabbitMQ connection
**Look for**: Flyway migration, RabbitMQ connection established

**Commands**:
```bash
# Get container ID
CONTAINER_ID=$(ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker service ps greedys_api_spring-app | grep Running | awk '{print \$1}'")

# Check logs (last 100 lines)
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker logs $CONTAINER_ID 2>&1 | head -100"

# Search for Flyway
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker logs $CONTAINER_ID 2>&1 | grep -i 'flyway\|V2__notification'"

# Search for RabbitMQ
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker logs $CONTAINER_ID 2>&1 | grep -i 'rabbitmq\|amqp\|connected'"

# Search for errors
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker logs $CONTAINER_ID 2>&1 | grep -i 'error\|exception\|failed' | head -20"
```

**Success Criteria**:
- [ ] No "Exception" or "Error" messages
- [ ] Flyway shows "Executing migration: V2__notification_schema"
- [ ] RabbitMQ connection: "Channel opened" or "Successfully connected"
- [ ] Application ready message appears

---

### Step 13: Verify Database Schema

**What**: Confirm all new tables and indexes created correctly
**Tables**: 4 notification tables + enum_definitions + 2 views

**Commands**:
```bash
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec $DB_CONTAINER mysql -u $DB_USER -p'$DB_PASS' $DB_NAME \
   -e \"SHOW TABLES LIKE 'notification_%';\""

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec $DB_CONTAINER mysql -u $DB_USER -p'$DB_PASS' $DB_NAME \
   -e \"SELECT COUNT(*) as enum_count FROM enum_definitions;\""

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec $DB_CONTAINER mysql -u $DB_USER -p'$DB_PASS' $DB_NAME \
   -e \"DESC notification_restaurant_user;\""

ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec $DB_CONTAINER mysql -u $DB_USER -p'$DB_PASS' $DB_NAME \
   -e \"SHOW INDEX FROM notification_restaurant_user;\""
```

**Success Criteria**:
- [ ] All 4 notification tables exist
- [ ] enum_definitions has 12 entries
- [ ] notification_restaurant_user has 14 columns
- [ ] All indexes created (5-6 per table)
- [ ] Views exist (v_pending_notifications, v_unread_notifications)

---

### Step 14: Verify RabbitMQ Connection

**What**: Confirm application connected to RabbitMQ and queues created

**Commands**:
```bash
# List active connections
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl list_connections user vhost | grep greedys_user"

# List queues
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl list_queues -p greedys"

# Check queue messages
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl list_queues -p greedys name messages consumers"
```

**Success Criteria**:
- [ ] Connection from spring-app exists
- [ ] User is greedys_user
- [ ] VHost is greedys
- [ ] Queues created (check for notification.*, event.outbox)

---

### Step 15: Test Notification Flow (End-to-End)

**What**: Create a test notification and verify full flow
**Flow**: Create → Store in DB → Publish to RabbitMQ

**Commands**:
```bash
# Create test notification
curl -X POST http://46.101.209.92:8080/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "userType": "RESTAURANT",
    "restaurantId": 1,
    "title": "Test Notification v2",
    "body": "Testing new 4-table notification system",
    "channel": "WEBSOCKET",
    "priority": "HIGH"
  }'

# Verify in database
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec $DB_CONTAINER mysql -u $DB_USER -p'$DB_PASS' $DB_NAME \
   -e \"SELECT id, event_id, title, status, created_at FROM notification_restaurant_user ORDER BY created_at DESC LIMIT 1;\""

# Check message in RabbitMQ
ssh -i $SSH_KEY $SSH_USER@$SSH_HOST \
  "docker exec greedys_rabbitmq rabbitmqctl list_queues -p greedys name messages | head -20"
```

**Success Criteria**:
- [ ] API call succeeds (HTTP 200/201)
- [ ] Notification appears in notification_restaurant_user table
- [ ] Status is PENDING or DELIVERED
- [ ] event_id is unique and populated
- [ ] created_at timestamp is recent
- [ ] Message count increases in RabbitMQ queues

---

## ERROR HANDLING & ROLLBACK

If any step fails:

1. **Document**: Note which step failed in ERROR_LOG.md
2. **Analyze**: Check logs and error messages
3. **Rollback Options**:

   - **Database**: `docker exec -i $DB_CONTAINER mysql -u $DB_USER -p'$DB_PASS' < /backup.sql`
   - **App**: `docker service update --force greedys_api_spring-app --replicas=1`
   - **RabbitMQ**: `docker-compose -f rabbitmq-compose.yml down && docker volume rm rabbitmq_data`

4. **Retry**: Fix issue and re-execute step

---

## NEXT STEPS AFTER COMPLETION

1. Run integration tests
2. Monitor logs for 24 hours
3. Create Git commit with migration details
4. Update documentation
5. Check CI/CD pipeline

---

**STATUS**: PREPARING PROMPTS - NOT EXECUTING YET

Move completed steps to DONE_LIST.md with timestamps!
