# Notification Persistence - Functional Description

Stores disaggregated notification records with idempotency via UNIQUE(eventId) constraint.

## 4 Tables (One Per User Type)

- **RestaurantUserNotification**: Staff notifications
- **CustomerNotification**: Customer notifications  
- **AgencyUserNotification**: Agency staff notifications
- **AdminNotification**: Admin notifications

## Lifecycle Stages

1. **PENDING**: Created by listener, awaiting delivery
2. **DELIVERED**: Channel send succeeded
3. **READ**: User opened/acknowledged
4. **ARCHIVED**: Deleted after 30+ days

## Key Features

- **Idempotency**: UNIQUE(eventId) prevents duplicate inserts
- **Audit Trail**: FK to EventOutbox links to original event
- **Status Tracking**: Per-channel delivery status
- **Shared Read**: readByAll flag for group notifications
- **Soft Delete**: deleted_at for compliance

## Database Operations

- INSERT: Listener creates record
- UPDATE: ChannelPoller marks DELIVERED, ReadStatusService marks READ
- DELETE: Cleanup job archives old records
- SELECT: ChannelPoller queries by (channel, status)

## Volume Characteristics

- Input: 1 event message from RabbitMQ
- Output: 1-1000 notification records (depends on recipients × channels)
- Retention: 30+ days
- Scale: Millions of records after 1 year (monthly partitioning recommended)

---

**Document Version**: 1.0  
**Component**: Notification Persistence
