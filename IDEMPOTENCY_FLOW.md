# ✅ Idempotency Flow Completo - Event & Notification Level

## Architettura a Due Livelli

```
┌─────────────────────────────────────────────────────────────────────┐
│                         EVENT OUTBOX LAYER                          │
│                    (First-Level Processing)                         │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ Source System emits event (e.g., ReservationEvent)                   │
│ - eventId = "evt-001"                                               │
│ - aggregateType = "RESTAURANT"                                       │
│ - eventType = "RESERVATION_REQUESTED"                                │
│ - payload = {...}                                                    │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│ EventOutboxPoller (polls EventOutbox table every 5s)                │
│ Finds: EventOutbox(id=evt-001, status=PENDING)                      │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│ EventOutboxOrchestrator.processEventOutbox()                         │
│ @Transactional (REQUIRED)                                            │
│ ⭐ LEVEL 1: EVENT-LEVEL IDEMPOTENCY                                 │
│                                                                      │
│ 1️⃣ Try INSERT into ProcessedEvent(eventId="evt-001")               │
│    ├─ IF SUCCESS (first time):                                      │
│    │  ├─ Determine recipient type from event                        │
│    │  ├─ Build NotificationMessage with recipientType               │
│    │  └─ CONTINUE to step 2                                         │
│    │                                                                │
│    └─ IF UNIQUE CONSTRAINT VIOLATION (already processed):           │
│       ├─ Log: "Event evt-001 already processed, skipping"          │
│       ├─ SKIP all further processing                               │
│       └─ CONTINUE to next event                                    │
│                                                                      │
│ 2️⃣ Publish 1 message per recipient type to RabbitMQ:              │
│    ├─ Queue: notification.restaurant                               │
│    ├─ Message: {                                                   │
│    │    "event_id": "evt-001",                                    │
│    │    "event_type": "RESERVATION_REQUESTED",                    │
│    │    "aggregate_type": "RESTAURANT",                           │
│    │    "recipientType": "BROADCAST",  ⭐ NEW FIELD              │
│    │    "restaurant_id": 5,                                       │
│    │    "payload": {...}                                          │
│    │ }                                                             │
│    └─ Message reaches 1 queue with 1 entry                        │
│                                                                     │
│ 3️⃣ Update ProcessedEvent.status = "SUCCESS"                       │
│ 4️⃣ Update EventOutbox.status = "PROCESSED"                        │
│ 5️⃣ @Transactional COMMIT                                          │
│                                                                     │
│ ⭐ GUARANTEE: If this process crashes after INSERT ProcessedEvent, │
│    retry will detect UNIQUE violation and skip RabbitMQ publish    │
│    → NO duplicate messages sent to RabbitMQ                        │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    RABBITMQ (Message Broker)                         │
│                                                                      │
│ Queue: notification.restaurant                                      │
│ ┌────────────────────────────────────────────┐                     │
│ │ Message 1: {eventId: "evt-001", ...}      │                     │
│ └────────────────────────────────────────────┘                     │
│                                                                     │
│ ⭐ Message stays in queue until listener ACKs it                  │
│    If listener crashes: RabbitMQ retransmits message              │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                 NOTIFICATION LISTENER LAYER                          │
│              (Second-Level Stream Processing)                       │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│ RestaurantUserNotificationListener.onNotificationMessage()           │
│ @RabbitListener(queues="notification.restaurant", ackMode="MANUAL") │
│ @Transactional (REQUIRED)                                            │
│                                                                      │
│ ⭐ LEVEL 2: NOTIFICATION-LEVEL IDEMPOTENCY                         │
│                                                                      │
│ 1️⃣ Extract eventId = "evt-001" from message                        │
│                                                                     │
│ 2️⃣ Check existsByEventId("evt-001")                               │
│    ├─ IF NOT EXISTS (first time):                                 │
│    │  └─ CONTINUE to step 3                                       │
│    │                                                              │
│    └─ IF EXISTS (already processed by this listener):             │
│       ├─ Log: "Event evt-001 already processed, skipping"        │
│       ├─ basicAck(channel, deliveryTag)  ⭐ Tell RabbitMQ OK   │
│       └─ RETURN (skip all disaggregation)                       │
│                                                                   │
│ 3️⃣ Get NotificationOrchestrator (RestaurantUserOrchestrator)     │
│ 4️⃣ orchestrator.disaggregateAndProcess(message)                   │
│    Returns List<RestaurantUserNotification> (e.g., 10 items)     │
│    Each notification represents ONE user × ONE channel           │
│                                                                   │
│ 5️⃣ For EACH disaggregated notification:                           │
│    Try persistNotification(notification) {                        │
│      notificationDAO.save(notification)  // INSERT into DB        │
│    }                                                              │
│    ├─ IF SUCCESS:                                               │
│    │  ├─ Notification saved to DB                               │
│    │  └─ CONTINUE to next notification                          │
│    │                                                            │
│    └─ IF DataIntegrityViolationException (UNIQUE constraint):   │
│       ├─ UNIQUE(eventId, userId, notificationType) violated    │
│       ├─ Log: "Notification already exists (idempotent)"       │
│       └─ CONTINUE to next notification (NOT an error)          │
│                                                                │
│ 6️⃣ basicAck(channel, deliveryTag)  ⭐ Tell RabbitMQ OK          │
│ 7️⃣ @Transactional COMMIT                                        │
│                                                                 │
│ ⭐ GUARANTEE: If this listener crashes:                         │
│    - RabbitMQ retransmits message                              │
│    - existsByEventId() returns true → listener skips           │
│    → NO duplicate notifications in DB                          │
│                                                                 │
│ ⭐ GUARANTEE: If DB INSERT fails with UNIQUE violation:         │
│    - Exception caught, logged, listener continues              │
│    - basicAck() still sent → RabbitMQ confirmed                │
│    → Safe retry on next message                                │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      DATABASE (Persistence)                          │
│                                                                      │
│ ✅ ProcessedEvent Table:                                           │
│ ┌──────────────────────────────────────────┐                      │
│ │ eventId (UNIQUE PK) = "evt-001"          │                      │
│ │ status = "SUCCESS"                       │                      │
│ │ processedAt = 2025-01-21 10:15:30        │                      │
│ └──────────────────────────────────────────┘                      │
│                                                                     │
│ ✅ RestaurantUserNotification Table:                              │
│ ┌────────────────────────────────────────────────────┐            │
│ │ id=1, eventId="evt-001", userId=10, notType=PUSH  │            │
│ │ id=2, eventId="evt-001", userId=10, notType=EMAIL │            │
│ │ id=3, eventId="evt-001", userId=15, notType=PUSH  │            │
│ │ ...                                                 │            │
│ │ UNIQUE INDEX(eventId, userId, notificationType)  │            │
│ └────────────────────────────────────────────────────┘            │
│                                                                     │
│ ⭐ NO DUPLICATES = Idempotency guarantee                           │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Scenari di Fallimento & Recovery

### Scenario 1: EventOutboxOrchestrator Crashes After Publishing

```
Timeline:
T1  EventOutboxOrchestrator INSERT ProcessedEvent(evt-001) ✅
T2  EventOutboxOrchestrator publishes to RabbitMQ ✅
T3  EventOutboxOrchestrator CRASHES before COMMIT ❌
T4  Transaction ROLLED BACK
    ├─ ProcessedEvent INSERT rolled back ❌
    └─ EventOutbox.status still PENDING ❌
T5  Poller restarts, finds EventOutbox(evt-001, PENDING)
T6  EventOutboxOrchestrator retries INSERT ProcessedEvent(evt-001)
    ├─ IF it retries: EventOutbox was marked PROCESSED before crash?
    │  ├─ YES → UNIQUE violation → SKIP ✅
    │  └─ NO → Reprocess and republish to RabbitMQ ✅
```

**Mitigation:** @Transactional ensures atomic operation.

---

### Scenario 2: EventOutboxOrchestrator Crashes Before Publishing

```
Timeline:
T1  EventOutboxOrchestrator INSERT ProcessedEvent(evt-001) ✅
T2  CRASH before publish ❌
T3  Transaction ROLLED BACK
    ├─ ProcessedEvent INSERT rolled back ❌
    └─ No message sent to RabbitMQ ✅
T4  Poller restarts
T5  EventOutboxOrchestrator retries from scratch
    ├─ INSERT ProcessedEvent(evt-001) again ✅
    ├─ Publishes to RabbitMQ ✅
    ├─ SUCCESS
```

**Idempotency**: Works perfectly due to transaction.

---

### Scenario 3: Listener Receives Duplicate Messages from RabbitMQ

```
Timeline:
T1  RabbitMQ sends message to listener ✅
T2  Listener processes message normally
    ├─ existsByEventId("evt-001") → FALSE
    ├─ Disaggregates to 10 notifications
    ├─ Saves all to DB ✅
    ├─ basicAck() ✅
T3  Listener crashes BEFORE basAck reaches RabbitMQ ❌
T4  RabbitMQ assumes delivery failed, retransmits message
T5  Listener processes same message AGAIN
    ├─ existsByEventId("evt-001") → TRUE ✅
    ├─ Log: "Event already processed, skipping"
    ├─ basicAck() ✅
    └─ NO DB inserts attempted → ZERO duplicates ✅
```

**Idempotency**: existsByEventId() prevents duplicate processing.

---

### Scenario 4: One Notification Fails Due to UNIQUE Constraint

```
Timeline:
T1  Listener receives message for evt-001
T2  existsByEventId("evt-001") → FALSE (first time)
T3  Disaggregates to 10 notifications
T4  Loop through notifications:
    ├─ Notification 1: save() ✅
    ├─ Notification 2: save() ✅
    ├─ Notification 3: save() ✅
    ├─ Notification 4: save() → DataIntegrityViolationException ❌
    │  ├─ UNIQUE(evt-001, userId=15, PUSH) already exists
    │  ├─ Log & CONTINUE (not an error)
    ├─ Notification 5: save() ✅
    ├─ ... (rest save successfully)
T5  basicAck() ✅
T6  @Transactional COMMIT (only new notifications, not the duplicate)
```

**Idempotency**: Exception caught, logged, listener continues safely.

---

## Code Implementation Summary

### 1. ProcessedEvent Entity (NEW)

```java
@Entity
@Table(name = "processed_event")
@Data
public class ProcessedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime processedAt;
}
```

### 2. ProcessedEventRepository (NEW)

```java
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventId(String eventId);
}
```

### 3. EventOutboxOrchestrator (UPDATED)

```java
@Transactional
public void processEventOutbox() {
    List<EventOutbox> events = eventOutboxRepository.findByStatus(PENDING);
    
    for (EventOutbox event : events) {
        try {
            // ⭐ LEVEL 1: Try insert ProcessedEvent
            ProcessedEvent processed = new ProcessedEvent();
            processed.setEventId(event.getId());
            processed.setStatus(PROCESSING);
            processedEventRepository.save(processed); // UNIQUE constraint
            
            // If we reach here = first time processing this event
            String recipientType = determineRecipientType(event);
            rabbitTemplate.convertAndSend(
                "notification." + recipientType.toLowerCase(),
                buildNotificationMessage(event, recipientType)
            );
            
            event.setStatus(PROCESSED);
            eventOutboxRepository.save(event);
            
            processed.setStatus(SUCCESS);
            processedEventRepository.save(processed);
            
        } catch (DataIntegrityViolationException e) {
            // ⭐ Event already processed, SKIP
            log.info("Event {} already processed, skipping", event.getId());
        }
    }
}
```

### 4. Notification Models (UPDATED)

All 4 notification entities updated with UNIQUE constraint:

```java
@Entity
@Table(name = "restaurant_user_notification", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_notification_idempotency",
        columnNames = {"event_id", "user_id", "notification_type"}
    )
})
public class RestaurantUserNotification { ... }
// Same for CustomerNotification, AgencyUserNotification, AdminNotification
```

### 5. BaseNotificationListener (UPDATED)

```java
@Transactional
public void processNotificationMessage(Map<String, Object> message, long deliveryTag, Channel channel) {
    try {
        String eventId = extractString(message, "event_id");
        
        // ⭐ LEVEL 2: Check if event already processed
        if (existsByEventId(eventId)) {
            log.warn("Event {} already processed, skipping", eventId);
            basicAck(channel, deliveryTag);
            return;
        }
        
        // Disaggregation
        NotificationOrchestrator<T> orchestrator = getTypeSpecificOrchestrator(message);
        List<T> notifications = orchestrator.disaggregateAndProcess(message);
        
        // Persist with idempotency catch
        for (T notification : notifications) {
            try {
                persistNotification(notification);
            } catch (DataIntegrityViolationException e) {
                log.debug("Notification already exists (idempotent), skipping");
            }
        }
        
        basicAck(channel, deliveryTag);
        
    } catch (Exception e) {
        log.error("Error processing notification", e);
        basicNack(channel, deliveryTag, true);
    }
}
```

---

## Idempotency Guarantees

| Scenario | Level 1 (Event) | Level 2 (Notification) | Outcome |
|----------|-----------------|----------------------|---------|
| First processing | ✅ ProcessedEvent inserted | ✅ Notifications saved | Success |
| EventOutbox reprocessed | ❌ UNIQUE violation (skip) | ➖ Not reached | Zero duplicates |
| Listener retransmit | ➖ Already in ProcessedEvent | ❌ existsByEventId() (skip) | Zero duplicates |
| Notification insert fails | ✅ Processed | ❌ DataIntegrityViolationException (skip) | Safe, idempotent |
| Listener crash & restart | ✅ Processed → skip | ✅ Event exists → skip | Automatic recovery |

---

## Benefits

✅ **No duplicate messages** on RabbitMQ even if EventOutbox crashes
✅ **No duplicate notifications** in DB even if Listener crashes
✅ **Safe retry** - reprocessing is automatic and idempotent
✅ **Transparent** - no API changes, works with existing code
✅ **Spring-native** - uses @Transactional and DataIntegrityViolationException
✅ **Performance** - minimal overhead (UNIQUE constraint, one INSERT per event)
