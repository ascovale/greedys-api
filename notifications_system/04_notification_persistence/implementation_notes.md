# Notification Persistence - Implementation Notes

## DAO Pattern

```java
// Persistence operation (in BaseNotificationListener)
try {
    persistNotification(notification);  // Subclass implements
} catch (DataIntegrityViolationException e) {
    // UNIQUE(eventId) violation → already processed
    log.debug("Idempotent skip: eventId already in DB");
}

// DAO interface
public interface RestaurantUserNotificationDAO {
    RestaurantUserNotification save(RestaurantUserNotification);  // INSERT
    List<RestaurantUserNotification> findByStatusAndChannel(...);  // SELECT
    int markAsRead(...);  // UPDATE
    int deleteOlderThan(LocalDateTime);  // DELETE
}
```

## Constraints & Indexes

```sql
-- Idempotency
UNIQUE KEY uk_event_id (event_id)

-- Audit
FOREIGN KEY fk_event_outbox (event_outbox_id) 
  REFERENCES event_outbox(id)

-- Query Performance
INDEX idx_user_status (user_id, status)
INDEX idx_restaurant_status (restaurant_id, status)
INDEX idx_channel_status (channel, status)
```

## Transaction Management

```java
@Transactional  // At listener level
protected void processNotificationMessage(...) {
    // All INSERTs within one transaction
    // COMMIT on success, ROLLBACK on exception
    for (T notification : disaggregatedList) {
        persistNotification(notification);  // Batch insert
    }
}
```

## Archival Strategy

```java
@Scheduled(fixedDelay = 86400000)  // Daily
@Transactional
public void archiveOldNotifications() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    restaurantDAO.deleteOlderThan(cutoff);
    customerDAO.deleteOlderThan(cutoff);
    agencyDAO.deleteOlderThan(cutoff);
    adminDAO.deleteOlderThan(cutoff);
}
```

## Partition Strategy (For Large Scale)

```sql
-- For tables >50M rows
ALTER TABLE restaurant_user_notification
PARTITION BY RANGE(YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027)
);
```

---

**Document Version**: 1.0  
**Component**: Notification Persistence
