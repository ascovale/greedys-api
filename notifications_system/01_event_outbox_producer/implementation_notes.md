# Event Outbox Producer - Implementation Notes

## Class Structure

### EventOutboxOrchestrator Service

**Location**: `com.application.common.service.notification.orchestrator.EventOutboxOrchestrator`

**Key Methods**:
```java
@Scheduled(fixedDelay=1000, initialDelay=2000)
public void orchestrate()
```

**Responsibilities**:
1. Poll EventOutbox table for PENDING events
2. For each event: attempt ProcessedEvent insert (idempotency)
3. Publish to correct RabbitMQ queue
4. Mark as PROCESSED in EventOutbox

**Dependencies** (injected):
- `EventOutboxDAO`: Data access for EventOutbox table
- `ProcessedEventDAO`: Data access for ProcessedEvent table
- `RabbitTemplate`: Spring AMQP template for publishing

---

## Data Access Patterns

### Reading Events (Polling)

```java
List<EventOutbox> events = eventOutboxRepository.findByStatus("PENDING", 100);
```

**Expected Result**: List of up to 100 EventOutbox records ordered by creation time (oldest first).

**Database Query** (conceptual):
```sql
SELECT * FROM event_outbox
WHERE status = 'PENDING'
ORDER BY created_at ASC
LIMIT 100
```

**Index Requirement**: Index on `(status, created_at)` for performance.

### Idempotency Check

```java
ProcessedEvent processed = new ProcessedEvent();
processed.setEventId(event.getEventId());
processed.setStatus(ProcessingStatus.PROCESSING);
processedEventRepository.save(processed);  // INSERT
```

**Expected**: 
- If this is first time: INSERT succeeds
- If duplicate eventId: UNIQUE constraint violation → DataIntegrityViolationException

**Database Operation**:
```sql
INSERT INTO processed_event (event_id, status, created_at)
VALUES (?, 'PROCESSING', NOW())
-- Constraint: UNIQUE(event_id)
```

### Updating Status

```java
void markAsProcessed(EventOutbox event) {
    event.setStatus(Status.PROCESSED);
    event.setPublishedAt(new Date());
    eventOutboxRepository.save(event);  // UPDATE
}
```

**Database Operation**:
```sql
UPDATE event_outbox
SET status = 'PROCESSED',
    published_at = NOW()
WHERE event_id = ?
```

---

## Message Publishing Logic

### RabbitMQ Routing

```java
private void publishEvent(EventOutbox event) {
    // Determine queue based on aggregateType
    String queue = switch(event.getAggregateType()) {
        case "RESTAURANT" -> "notification.restaurant";
        case "CUSTOMER" -> "notification.customer";
        case "AGENCY" -> "notification.agency";
        case "ADMIN" -> "notification.admin";
        default -> throw new IllegalArgumentException(...);
    };
    
    // Build message
    Map<String, Object> message = buildMessage(event);
    
    // Publish
    rabbitTemplate.convertAndSend(queue, message);
}
```

### Message Building

```java
private Map<String, Object> buildMessage(EventOutbox event) {
    Map<String, Object> msg = new HashMap<>();
    msg.put("event_id", event.getEventId());
    msg.put("event_type", event.getEventType());
    msg.put("aggregate_type", event.getAggregateType());
    msg.put("aggregate_id", event.getAggregateId());
    msg.put("payload", event.getPayload());  // Already JSON
    return msg;
}
```

**Note**: Message does NOT include:
- Recipient list
- User preferences
- Channel info
- Notification properties

These are calculated in Layer 2.

---

## Event Type Handling

### Supported Event Types

The system expects EventOutbox.eventType to be one of these values:
- `RESERVATION_REQUESTED`
- `RESERVATION_CONFIRMED`
- `RESERVATION_CANCELLED`
- `ORDER_CREATED`
- `ORDER_READY`
- `KITCHEN_ALERT`
- `CUSTOMER_REGISTERED`
- `STAFF_INVITED`
- (Any application-defined event type)

**Important**: EventOutboxOrchestrator doesn't filter by event type. It publishes all PENDING events regardless of type.

**Event Type Routing**: Happens in Layer 2 (NotificationOrchestrator) based on event_type field.

---

## Error Handling & Resilience

### Exception Types & Handling

#### 1. DataIntegrityViolationException
```java
try {
    processedEventRepository.save(processed);  // INSERT
    publishEvent(event);
    markAsProcessed(event);
} catch (DataIntegrityViolationException e) {
    // UNIQUE constraint violation = already processed
    log.warn("Event already processed: {}", event.getEventId());
    // Skip (no action needed)
    return;
}
```

**Recovery**: None needed. This is expected for duplicate processing.

#### 2. AmqpConnectException / AmqpIOException
```java
try {
    rabbitTemplate.convertAndSend(queue, message);
} catch (AmqpException e) {
    log.error("Failed to publish event: {}", e.getMessage());
    // ProcessedEvent still saved, EventOutbox still PENDING
    // Next cycle will retry
    throw e;  // Or handle gracefully
}
```

**Recovery**: Automatic. Event stays PENDING, next cycle retries.

#### 3. DataAccessException (Database unavailable)
```java
List<EventOutbox> events = eventOutboxRepository.findByStatus(...);
// If database down: DataAccessException thrown
// Cycle aborts, scheduled method exits
```

**Recovery**: Automatic. Next cycle (1 second later) retries.

### No Explicit Retry Logic

**Current Implementation**: If any step fails, exception propagates. Spring's scheduler catches it and logs.

**Potential Issue**: No exponential backoff, no max retry count.

**Recommendation**: Consider adding:
```java
@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000))
public void orchestrate() { ... }
```

---

## Transaction Management

### Transaction Boundaries

```java
@Scheduled(fixedDelay=1000)
@Transactional
public void orchestrate() {
    // All 3 operations in one transaction
    List<EventOutbox> events = poll();           // READ
    for (EventOutbox event : events) {
        insert(ProcessedEvent);                  // INSERT
        publish(event);                          // Side effect (RabbitMQ)
        update(EventOutbox);                     // UPDATE
    }
    // COMMIT or ROLLBACK
}
```

### Transaction Semantics

- **Isolation Level**: DEFAULT (typically READ_COMMITTED)
- **Propagation**: REQUIRED (uses existing or creates new)
- **Read-Only**: False (creates/updates records)
- **Timeout**: Depends on Spring config (default 60 seconds)

### Potential Issue

**What if transaction commit is slow?**
- Long-running transactions hold locks
- Other threads waiting for EventOutbox access
- Next cycle might start before previous completes
- Could lead to race conditions

**Mitigation**: Process events in batches, keep transaction short.

---

## Concurrency & Scaling

### Single Instance (Typical)
```
Thread: Spring scheduled executor
Runs every 1000ms
No concurrency issues
Clean mutex-like behavior
```

### Multiple Instances (Load Balanced)

```
Instance 1: polls EventOutbox
├─ Reads 100 events
└─ ProcessedEvent INSERT → acquires "lock"

Instance 2: polls EventOutbox (same time)
├─ Reads 100 events (overlaps with Instance 1)
├─ ProcessedEvent INSERT → UNIQUE violation
└─ Catches exception, skips event

Result: Duplicate RabbitMQ message? NO
  Because ProcessedEvent blocks it
  But event might be published twice? NO
  Because listener (Layer 2) has UNIQUE(eventId)
```

### Database Locking

**Row-Level Locks**: EventOutbox.status='PENDING' rows might be locked by:
- ProcessedEvent INSERT (foreign key? No, it's separate table)
- EventOutbox UPDATE for status

**Potential Deadlock**: If multiple instances compete for same event row.

**Current Mitigation**: ProcessedEvent UNIQUE constraint prevents duplicate processing.

**Better Mitigation**: Add optimistic locking (version column) or SELECT ... FOR UPDATE.

---

## Logging & Monitoring

### Log Points

1. **Cycle Start**: `"📬 EventOutboxOrchestrator: polling EventOutbox"`
2. **Events Found**: `"Found N pending events"`
3. **Per Event - Success**: `"✅ Published event: [id] to queue: [name]"`
4. **Per Event - Duplicate**: `"⚠️ Duplicate eventId: [id], skipping"`
5. **Per Event - Publish Failure**: `"❌ Failed to publish event: [id]"` + exception

### Metrics to Monitor

- **Polling Frequency**: How often orchestrate() is called
- **Events Per Cycle**: Average and peak
- **Publish Success Rate**: % of events successfully published
- **Duplicate Rate**: % of events skipped as duplicates
- **Queue Depth**: How many PENDING events in EventOutbox (should be <100 ideally)
- **Latency**: Time from event created to published

### Red Flags

- Queue depth >1000 (processing can't keep up)
- Publish failure rate >5% (RabbitMQ problem?)
- Duplicate rate >10% (ProcessedEvent cleanup issue?)
- Cycle time >5 seconds (database overload?)

---

## Performance Tuning

### Batch Size Optimization

Current: `LIMIT 100` events per cycle

**Trade-offs**:
- Larger batch (500): Process more events/cycle, longer cycle time
- Smaller batch (10): Process fewer, faster cycle, more RabbitMQ calls

**Recommendation**: Keep at 100 unless monitoring shows bottleneck.

### Polling Interval Tuning

Current: `fixedDelay=1000` (1 second)

**Trade-offs**:
- Shorter interval (100ms): Lower latency, higher CPU
- Longer interval (5s): Higher latency, lower CPU

**Recommendation**: 1 second is reasonable. Adjust based on latency requirements.

### Database Query Optimization

Current Query:
```sql
SELECT * FROM event_outbox
WHERE status = 'PENDING'
ORDER BY created_at ASC
LIMIT 100
```

**Current Index**: Assuming index on (status, created_at)

**Potential Optimization**:
- Add PARTITION by status (if using MySQL 5.1+)
- Use prepared statements (likely already done via Hibernate)
- Add column index on aggregate_type (for WHERE filtering, if needed)

---

## Deployment Considerations

### RabbitMQ Connection Pool

Ensure Spring AMQP connection pool is adequate:
```properties
spring.rabbitmq.connection-factory.cache-mode=CHANNEL
spring.rabbitmq.connection-factory.channel-cache-size=10
```

### Database Connection Pool

Ensure sufficient connections for polling:
```properties
spring.datasource.hikari.maximum-pool-size=20  # At least 2-3 for orchestrator
```

### Thread Pool for Scheduler

Configure Spring scheduler pool:
```properties
spring.task.scheduling.pool.size=5  # Number of scheduled tasks
```

### EventOutbox Table Maintenance

Over time, EventOutbox accumulates PROCESSED events.

**Recommended Cleanup**:
```sql
DELETE FROM event_outbox
WHERE status = 'PROCESSED'
  AND published_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

**Schedule**: Weekly batch cleanup job.

---

## Testing Approach

### Unit Tests

```java
@Test
public void testEventPublishedToCorrectQueue() {
    EventOutbox event = new EventOutbox();
    event.setAggregateType("RESTAURANT");
    event.setEventId("evt-123");
    
    orchestrator.publishEvent(event);
    
    // Verify: rabbitTemplate.convertAndSend() called with correct queue
    verify(rabbitTemplate).convertAndSend("notification.restaurant", any());
}

@Test
public void testDuplicateEventSkipped() {
    EventOutbox event1 = new EventOutbox();
    event1.setEventId("evt-123");
    
    orchestrator.orchestrate();  // First time
    // ProcessedEvent created for evt-123
    
    orchestrator.orchestrate();  // Second time, same event
    // Try INSERT ProcessedEvent → UNIQUE violation
    // Event skipped
    
    // Verify: publishEvent() called only once
    verify(rabbitTemplate, times(1)).convertAndSend(any(), any());
}
```

### Integration Tests

```java
@SpringBootTest
@EmbeddedKafka  // Or embedded RabbitMQ
public class EventOutboxOrchestratorIT {
    
    @Test
    public void testEventPublishedToRabbitMQ() {
        // Insert EventOutbox record
        eventOutboxRepository.save(new EventOutbox(...));
        
        // Trigger orchestrator
        orchestrator.orchestrate();
        
        // Verify message in queue
        Message message = rabbitTemplate.receiveAndConvert("notification.restaurant");
        assertNotNull(message);
        assertEquals("RESERVATION_REQUESTED", message.getEventType());
    }
}
```

---

## Maintenance Checklist

### Regular Monitoring
- [ ] Check EventOutbox queue depth (PENDING count)
- [ ] Monitor publish success rate
- [ ] Check RabbitMQ connection health
- [ ] Verify ProcessedEvent table is being cleaned up

### Quarterly Tasks
- [ ] Review and clean old PROCESSED events
- [ ] Analyze event publication latency
- [ ] Check for any orphaned ProcessedEvent records

### Annual Tasks
- [ ] Review scaling assumptions (is 100 events/cycle still adequate?)
- [ ] Analyze RabbitMQ disk usage and memory
- [ ] Plan for table partitioning if EventOutbox >10M rows

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Component**: Event Outbox Producer  
**Implementation Status**: Production-ready with monitoring recommendations
