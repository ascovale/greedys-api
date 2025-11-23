# Event Outbox Producer - Functional Description

## What is the Event Outbox Producer?

The Event Outbox Producer is the **first layer** of the notification system. It serves as a **durable event publisher** that guarantees all business events are eventually published to RabbitMQ, even if the system crashes.

## Core Responsibility

Transform business events (stored atomically in the EventOutbox table) into RabbitMQ messages routed to the appropriate listener queue based on user type.

## Functional Behavior

### 1. **Polling Mechanism**
- Continuously monitors the EventOutbox database table
- Polls every 1 second (configurable)
- Processes up to 100 pending events per cycle
- Runs in background via `@Scheduled` annotation

### 2. **Event Selection**
- Looks for events with `status = 'PENDING'`
- Sorts by creation time (FIFO processing)
- Filters by time range (optional, for batching)

### 3. **Idempotency Assurance (Level 1)**
Before publishing an event, verifies it hasn't been published before:
- Attempts to insert a row in the `ProcessedEvent` table
- Uses `UNIQUE(eventId)` constraint
- If insertion succeeds → first time processing → publish
- If insertion fails (duplicate key) → event already processed → skip

This prevents the same event from being published multiple times if the orchestrator crashes after publishing but before marking complete.

### 4. **Routing Logic**
Routes each message to the correct RabbitMQ queue based on the `aggregateType` field:

| aggregateType | Destination Queue | Listener |
|---|---|---|
| RESTAURANT | notification.restaurant | RestaurantNotificationListener |
| CUSTOMER | notification.customer | CustomerNotificationListener |
| AGENCY | notification.agency | AgencyUserNotificationListener |
| ADMIN | notification.admin | AdminNotificationListener |

### 5. **Message Composition**
Publishes a generic, type-agnostic message containing:
- `event_id`: Unique identifier from the original event
- `event_type`: Type of business event (RESERVATION_REQUESTED, ORDER_CREATED, etc.)
- `aggregate_type`: Type of recipient (determines queue)
- `aggregate_id`: ID of the entity (restaurant_id, customer_id, etc.)
- `payload`: Full event data (reservation details, order info, etc.)

**Important**: The message does NOT include:
- List of recipients
- User preferences
- Channel preferences
- Notification metadata

These are calculated later in Layer 2 (disaggregation).

### 6. **Completion Marking**
After successful RabbitMQ publish:
- Updates the EventOutbox record
- Sets `status = 'PROCESSED'`
- Records `published_at = NOW()`
- Event won't be polled again

### 7. **Error Handling**
- If RabbitMQ publish fails: exception caught, event stays PENDING
- Next polling cycle (1 second later) retries the event
- No exponential backoff (constant 1-second retries)
- No maximum retry limit currently implemented

---

## Why This Matters (Outbox Pattern)

### The Problem (Without Outbox)
```
Business Service publishes event:
  1. INSERT Reservation INTO reservations
  2. Publish event to RabbitMQ
  3. COMMIT

Failure Scenario:
  - Step 1: SUCCESS (reservation created)
  - Step 2: SUCCESS (event published)
  - Step 3: CRASH before commit
  - Result: Reservation in DB, but not committed
  - RabbitMQ received duplicate event? Or no event? Inconsistent.
```

### The Solution (With Outbox)
```
Business Service:
  1. BEGIN transaction
  2. INSERT Reservation
  3. INSERT EventOutbox (same transaction)
  4. COMMIT atomically

Later (async, separate scheduler):
  EventOutboxOrchestrator:
  - Reads EventOutbox
  - Publishes to RabbitMQ
  - Marks as published

Benefits:
  ✅ No race conditions
  ✅ Publication guaranteed
  ✅ Can retry if RabbitMQ fails
  ✅ Can scale independently
```

---

## Interaction with Other Components

### Upstream (What creates events)
- **Reservation Service**: Creates reservations, inserts into EventOutbox
- **Order Service**: Creates orders, inserts into EventOutbox
- **Authentication Service**: User registrations → EventOutbox
- **Any business logic**: That needs notifications

### Downstream (Where messages go)
- **RabbitMQ**: Receives 1 generic message per event
- **4 Notification Listeners**: Consume from queues, disaggregate

### Side Interactions
- **ProcessedEvent table**: For idempotency tracking
- **Database**: Reads EventOutbox, writes ProcessedEvent, updates status

---

## Performance Characteristics

### Throughput
- **Capacity**: 6,000 events/minute (100 per cycle × 60 cycles)
- **Realistic**: 1,000-3,000 events/minute (accounting for processing time)
- **Latency**: 1-2 seconds (until next polling cycle)

### Scalability
- **Single Instance**: Adequate for most deployments
- **Multiple Instances**: Potential race condition on EventOutbox updates
  - Two instances could publish same event twice
  - Mitigation: ProcessedEvent UNIQUE constraint catches it
  - Not ideal but functional

### Resource Usage
- **CPU**: Minimal (mostly I/O wait)
- **Memory**: Constant (100 events batch) regardless of total queue size
- **Database Connections**: 1 connection per cycle
- **Network**: Negligible

---

## Idempotency Guarantee Level

**Type**: Event-Level Idempotency

**Guarantee**: For any given `eventId`, EventOutboxOrchestrator will publish to RabbitMQ **at most once** (but could be zero if table corruption).

**Mechanism**:
- ProcessedEvent table with UNIQUE constraint
- Atomic insert-or-fail pattern
- Only proceeds if insert succeeds

**Failure Mode**: If ProcessedEvent insert fails for database reasons (deadlock, constraint violation unrelated to duplicate), event might be published twice. This is acceptable because Layer 2 (listener) has its own idempotency check via Notification table UNIQUE constraint.

---

## Data Model

### Input: EventOutbox Table
```
Columns:
  - event_id (STRING, PRIMARY KEY)
  - event_type (STRING)
  - aggregate_type (STRING)
  - aggregate_id (BIGINT)
  - status (STRING): PENDING | PROCESSED | FAILED
  - payload (JSON)
  - created_at (TIMESTAMP)
  - published_at (TIMESTAMP, nullable)

Indexes:
  - PRIMARY KEY(event_id)
  - INDEX(status, created_at)
```

### Output: RabbitMQ Messages
```
Queue Names:
  - notification.restaurant
  - notification.customer
  - notification.agency
  - notification.admin

Message Format (JSON):
{
  "event_id": "evt-123-456",
  "event_type": "RESERVATION_REQUESTED",
  "aggregate_type": "RESTAURANT",
  "aggregate_id": 5,
  "payload": {...}
}
```

### Side Effect: ProcessedEvent Table
```
Columns:
  - event_id (STRING, PRIMARY KEY)
  - status (STRING): PROCESSING | SUCCESS | FAILED
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)

Constraint:
  UNIQUE(event_id)
```

---

## Configuration

### Properties (Spring)
```properties
# Polling interval
notification.outbox.poll.interval=1000  (milliseconds)

# Batch size
notification.outbox.batch.size=100

# Initial delay (wait before first poll)
notification.outbox.initial.delay=2000  (milliseconds)

# RabbitMQ connection settings
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

### RabbitMQ Configuration
```
Topic Exchange: notifications.exchange
Binding Rules:
  - routing.key="notification.restaurant" → queue="notification.restaurant"
  - routing.key="notification.customer" → queue="notification.customer"
  - routing.key="notification.agency" → queue="notification.agency"
  - routing.key="notification.admin" → queue="notification.admin"

Queue Settings:
  - Durable: true
  - Auto-delete: false
  - TTL: none (messages stay until processed)
```

---

## Failure Scenarios & Recovery

### Scenario 1: RabbitMQ Temporarily Down
```
T0: EventOutboxOrchestrator attempts publish
  └─ RabbitTemplate.convertAndSend()
  └─ AmqpConnectException thrown
  └─ Exception caught, logged

T1 (1 second later): Next cycle
  └─ Event still PENDING
  └─ Retry publish

T2 (when RabbitMQ back up):
  └─ Publish succeeds
  └─ Event marked PROCESSED
```

### Scenario 2: Database Down
```
T0: Poll EventOutbox
  └─ DataAccessException
  └─ Cycle aborts

T1: Next cycle
  └─ Retry (exponential backoff not implemented)
  └─ Hope database is back
```

### Scenario 3: EventOutboxOrchestrator Crashes
```
T0: Published to RabbitMQ
  └─ Message delivered to queue

T1: Before updating EventOutbox status
  └─ Server crash
  └─ ProcessedEvent.save() incomplete

T2: Server restarts, next cycle
  └─ Event still PENDING in EventOutbox
  └─ Try INSERT ProcessedEvent again
  └─ UNIQUE constraint: already exists from before crash
  └─ Duplicate insert fails silently (caught, logged)
  └─ Skip this event
  └─ RabbitMQ already has it, listener will process
```

### Scenario 4: Listener Crashes Before ACK
```
RabbitMQ keeps message until listener ACKs.
If listener crashes:
  └─ Message requeued after timeout
  └─ Another listener processes it
  └─ UNIQUE constraint on Notification table prevents duplicate DB insert
```

---

## Testing Strategy

### Unit Tests
- Mock EventOutboxDAO
- Mock RabbitTemplate
- Verify idempotency by calling publish twice
- Verify routing by checking queue selection logic

### Integration Tests
- Real EventOutbox table (in-memory DB)
- Embedded RabbitMQ or real instance
- Verify messages arrive in correct queue
- Verify ProcessedEvent creation

### Load Tests
- Insert 10,000 events
- Measure throughput
- Monitor memory/CPU
- Verify no duplicate publishes

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Component**: Event Outbox Producer  
**Status**: Functional documentation for current implementation
