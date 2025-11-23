# Comprehensive Problems & Solutions Matrix

## Overview
Questo documento fornisce una matrice completa di **tutti i problemi identificati nel sistema di notifiche** con descrizioni dettagliate delle cause e soluzioni compatibili.

---

## SEZIONE 1: PROBLEMI DI IDEMPOTENZA E CRASH

### Problema #1: Event Stuck in PENDING dopo crash durante RabbitMQ send

**Descrizione:**
EventOutboxOrchestrator crasha **durante l'invio a RabbitMQ** ma **dopo** aver inserito ProcessedEvent.

**Causa Tecnica:**
1. INSERT ProcessedEvent (evt-123) → OK ✅
2. rabbitTemplate.convertAndSend() → Network muore ❌
3. Messaggio MAI raggiunge RabbitMQ
4. ProcessedEvent ESISTE, messaggio NON in queue
5. Prossimo ciclo: tenta re-INSERT ProcessedEvent
6. ❌ UNIQUE violation (esiste già)
7. App salta evento (pensa sia già elaborato)
8. **Evento STUCK FOREVER** ❌

**Scenario:**
- RabbitMQ down per 30 secondi (maintenance, network hiccup)
- Orchestrator tenta invio
- Network fail durante transmissione TCP
- ProcessedEvent insert SUCCESS, ma messaggio perso

**Impact:**
- Notifiche PERSE permanentemente
- Nessuna recovery automatica
- Customer non riceve notification
- Severità: **CRITICAL** 🔴

**Soluzione:**
Invertire l'ordine delle operazioni:
```
PRIMA (SBAGLIATO):
1. INSERT ProcessedEvent
2. rabbitTemplate.send()        ← Crash qui = STUCK
3. UPDATE EventOutbox

DOPO (CORRETTO):
1. INSERT ProcessedEvent (lock)
2. rabbitTemplate.send()        ← Crash qui = lock già esiste
3. UPDATE EventOutbox
```

**Compatibilità:** ✅ Questa è già la soluzione corretta nel documento CODE_FIX_IMPLEMENTATION

---

### Problema #2: Duplicate messages if publish succeeds but ProcessedEvent insert fails

**Descrizione:**
EventOutboxOrchestrator pubblica a RabbitMQ con **successo**, ma l'INSERT ProcessedEvent **fallisce** (DB down, deadlock).

**Causa Tecnica:**
1. rabbitTemplate.convertAndSend() → OK ✅ (messaggio in queue)
2. 💥 INSERT ProcessedEvent → Exception (DB connection error)
3. App logs error, event rimane PENDING
4. Prossimo ciclo: rilegge event (still PENDING)
5. Ripubblica messaggio → DUPLICATE in queue
6. Listener riceve 2 copie dello stesso messaggio

**Scenario:**
- Database ha connection pool esaurito
- Orchestrator riesce a publishare (usa connection AMQP)
- Ma non può inserire ProcessedEvent (no DB connection)
- RabbitMQ ha 2 copie di stesso messaggio

**Impact:**
- Duplicate notifications create (se listener non ha UNIQUE check)
- Wasted processing
- Severità: **MEDIUM** 🟡

**Soluzione:**
Se ProcessedEvent insert fallisce, **retry con backoff** prima di abbandonare:
```java
try {
    processedEventRepository.save(processed); // Insert lock
} catch (DataIntegrityViolationException e) {
    // Lock già esiste = evento già processato
    eventOutboxRepository.updateStatus(event, PROCESSED);
    return; // Skip this cycle
} catch (Exception e) {
    // DB error = retry prossimo ciclo
    log.warn("ProcessedEvent insert failed, will retry: {}", e.getMessage());
    return; // Don't publish yet
}

// Solo se lock insert success, procedi con publish
rabbitTemplate.convertAndSend(queue, message);
```

**Compatibilità:** ✅ Compatibile con Problema #1

---

### Problema #3: Multiple EventOutboxOrchestrator instances race condition

**Descrizione:**
Se system scalato orizzontalmente con **multiple EventOutboxOrchestrator pods**, entrambi leggono stesso evento PENDING e tentano di processarlo.

**Causa Tecnica:**
1. Pod1: SELECT FROM event_outbox WHERE status='PENDING' LIMIT 100
   → Legge event-123
2. Pod2: SELECT FROM event_outbox WHERE status='PENDING' LIMIT 100
   → Legge stesso event-123
3. Pod1: INSERT ProcessedEvent (event-123) → OK
4. Pod2: INSERT ProcessedEvent (event-123) → ❌ UNIQUE violation
5. Pod2: Catch exception, skip event (ma non ritenta publish)
6. Pod1: Pubblica 1 messaggio
7. Pod2: Pubblica altro messaggio? (se error handling non corretto)
   → POTENTIAL DUPLICATE

**Scenario:**
- Production con 2 EventOutboxOrchestrator pods
- Both running @Scheduled(fixedDelay=1000)
- No distributed lock mechanism

**Impact:**
- Duplicate RabbitMQ messages possibili
- Wasted processing
- Severità: **MEDIUM-HIGH** 🟠

**Soluzione:**
Usare distributed lock (Redis o DB):
```java
@Scheduled(fixedDelay=1000)
public void orchestrate() {
    // Acquire distributed lock
    if (!lockService.acquireLock("event-orchestrator", 5)) {
        log.debug("Lock not acquired, skipping cycle");
        return; // Retry next cycle
    }
    
    try {
        // Only one pod executes this
        List<EventOutbox> pending = eventOutboxRepository.findByStatusPending();
        for (EventOutbox event : pending) {
            processEvent(event);
        }
    } finally {
        lockService.releaseLock("event-orchestrator");
    }
}
```

Alternative:
- Single pod deployment (simpler, current likely state)
- Database SELECT FOR UPDATE (pessimistic lock)

**Compatibilità:** ✅ Compatibile con Problema #1-2

---

## SEZIONE 2: PROBLEMI DI RABBITMQ E MESSAGE QUEUE

### Problema #4: RabbitMQ outage cause indefinite PENDING events

**Descrizione:**
Se RabbitMQ **down per ore**, EventOutbox table cresce indefinitely con PENDING events.

**Causa Tecnica:**
1. RabbitMQ down
2. Orchestrator continua polling (ogni 1 secondo)
3. rabbitTemplate.convertAndSend() throws AmqpConnectException
4. Event rimane PENDING, EventOutbox non aggiornato
5. Se RabbitMQ down per 24 ore: ~86,400 PENDING events
6. EventOutbox table cresce, queries diventano lente

**Scenario:**
- RabbitMQ container crash
- Kubernetes non riavvia subito (pending restart)
- Orchestrator loop tries to publish continuously

**Impact:**
- Database bloat
- Slow queries per EventOutbox
- Manual intervention needed per clean up
- Severità: **MEDIUM** 🟡

**Soluzione:**
Implementare **max retry count** + **dead letter**:
```java
@Transactional
public void processEvent(EventOutbox event) {
    ProcessedEvent processed = new ProcessedEvent(
        event.getEventId(),
        "PROCESSING",
        LocalDateTime.now(),
        event.getAttemptCount() + 1  // Track attempts
    );
    
    processedEventRepository.save(processed);
    
    try {
        rabbitTemplate.convertAndSend(queue, message);
        eventOutboxRepository.updateStatus(event, PROCESSED);
    } catch (AmqpConnectException e) {
        int newAttempts = event.getAttemptCount() + 1;
        
        if (newAttempts >= 3) {
            // Move to dead letter
            eventOutboxRepository.updateStatus(event, DEAD_LETTER);
            alertService.sendAlert("Event moved to DLQ: " + event.getEventId());
        } else {
            // Retry next cycle
            eventOutboxRepository.updateAttemptCount(event.getId(), newAttempts);
        }
    }
}
```

**Compatibilità:** ✅ Compatibile con Problema #1-3

---

### Problema #5: No Dead Letter Queue for permanently failed messages

**Descrizione:**
Se un messaggio fallisce N volte (listener crash, DB error), **non c'è DLQ** per salvarlo.

**Causa Tecnica:**
1. Listener riceve messaggio
2. Disaggregation logic ha bug
3. Crash durante INSERT notification
4. @Transactional rollback
5. Message NOT ACK'd
6. RabbitMQ requeue dopo timeout
7. Loop infinito: retry forever
8. Nessuna visibility in problematic messages

**Scenario:**
- Nuovo listener deployment con bug
- Listener crashes su ogni messaggio di certo tipo
- Messages requeued forever
- Queue blocked, no other messages processed

**Impact:**
- Message queue stuck
- New events never processed
- Manual intervention required
- Severità: **CRITICAL** 🔴

**Soluzione:**
Implementare **dead letter exchange (DLX)** in RabbitMQ + **retry counter**:
```java
@RabbitListener(queues = "notification.restaurant")
public void handleMessage(Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    int retryCount = getRetryCount(message);
    
    try {
        RestaurantUserNotification notification = parseMessage(message);
        persistNotification(notification);
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        if (retryCount >= 3) {
            // Send to DLQ
            channel.basicNack(deliveryTag, false, false);
            sendToDLQ(message, e);
            alertService.sendAlert("Message sent to DLQ after 3 retries");
        } else {
            // Requeue with retry count
            message.getMessageProperties().setHeader("retry-count", retryCount + 1);
            channel.basicNack(deliveryTag, false, true); // requeue
        }
    }
}
```

**Compatibilità:** ✅ Compatibile con Problema #1-4

---

## SEZIONE 3: PROBLEMI DI LISTENER E MESSAGE PROCESSING

### Problema #6: Listener crashes after disaggregation but before INSERT

**Descrizione:**
Listener disaggrega 20 records, poi crasha prima di INSERT database.

**Causa Tecnica:**
1. Message consumed from queue (NOT ACK'd yet)
2. Disaggregation succeeds (20 records created in memory)
3. 💥 Crash (OOM, network drop, container killed)
4. INSERT never happens
5. Message never ACK'd
6. RabbitMQ waits for ACK timeout
7. Message requeued

**Scenario:**
- Pod receives 1000 events/second
- Memory leak → OOM killer
- Crashes during INSERT batch

**Impact:**
- Message requeued
- Re-processed next time listener restart
- No data loss (transaction rollback)
- Severità: **LOW** 🟢 (handled by @Transactional)

**Soluzione:**
Ensure @Transactional is configured + monitor restart frequency:
```java
@RabbitListener(queues = "notification.restaurant")
@Transactional  // ← Ensure present
public void processNotificationMessage(GenericNotification notification, 
                                       Channel channel,
                                       Message message) throws IOException {
    try {
        List<RestaurantUserNotification> records = 
            restaurantOrchestrator.disaggregate(notification);
        
        // All INSERT in single transaction
        restaurantUserNotificationRepository.saveAll(records);
        
        // Only if INSERT succeeds
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (Exception e) {
        log.error("Failed to process notification", e);
        // Transaction auto-rollback
        // Message NOT ACK'd, will be requeued
        // If crash happens here, container restart needed
    }
}
```

**Compatibilità:** ✅ Compatibile con Problem #1-5

---

### Problema #7: Listener scales horizontally → duplicate processing risk

**Descrizione:**
Se **multiple listener pods**, entrambi potrebbero consumare **stesso messaggio**.

**Causa Tecnica:**
1. Pod1 consumer consuma messaggio da queue
2. Pod2 consumer consuma STESSO messaggio?
3. RabbitMQ round-robin su consumers

Wait: **RabbitMQ round-robin previene questo**. Ma se:
- Pod1: Consumer 1 (max batch size)
- Pod1: Consumer 2
- Pod2: Consumer 3
- Pod2: Consumer 4

Stesso messaggio non può essere consumato 2x se ACK lavora bene.

**Reale Problema:** Se ACK è delayed, e container restart:
1. Pod1 consuma messaggio, start processing
2. Pod1 kills (restart)
3. Message NOT ACK'd
4. Requeued
5. Pod2 riceve SAME messaggio
6. Both processed if overlap

**Impact:**
- Duplicate notifications create se UNIQUE check non funziona
- Severità: **MEDIUM** 🟡

**Soluzione:**
Listeners già hanno UNIQUE constraint check, questo è **protected** ✅
Ma monitor per assicurare:

```java
// Listener-side idempotency check (ALREADY PRESENT)
@RabbitListener(queues = "notification.restaurant")
public void process(GenericNotification notification) {
    // Check if already processed
    if (restaurantUserNotificationRepository.existsByEventId(notification.getEventId())) {
        log.info("Already processed: {}", notification.getEventId());
        return; // Skip
    }
    
    // Process normally
    List<RestaurantUserNotification> records = disaggregate(notification);
    restaurantUserNotificationRepository.saveAll(records);
}
```

**Compatibilità:** ✅ Compatibile con Problema #1-7

---

## SEZIONE 4: PROBLEMI DI READ STATUS E SYNCHRONIZATION

### Problema #8: Shared read scope collision with similar eventIds

**Descrizione:**
Shared read usa **LIKE pattern** per eventId, rischio di false positives.

**Current Code (ISSUE):**
```sql
UPDATE RestaurantUserNotification
SET status = 'READ'
WHERE event_id LIKE 'evt-res-123_%'  -- LIKE pattern ❌
  AND restaurant_id = 123
  AND channel = 'RESERVATION'
```

**Problema:**
- eventId = "evt-res-123" + messaggio = "evt-res-123-order-456"
- eventId = "evt-res-123-sub" = SAME prefix
- LIKE 'evt-res-123_%' matches both ❌

**Scenario:**
- Reservation event evt-res-123_v1 created
- Sub-reservation evt-res-123_v1_sub created
- Mark first as read
- Also marks second as read (unintended)

**Impact:**
- Incorrect read status propagation
- Users see wrong notification state
- Severità: **MEDIUM** 🟡

**Soluzione:**
Use exact event ID + explicit scope:
```java
@Transactional
public void markSharedReadByScope(String eventId, String restaurantId, String scope) {
    List<RestaurantUserNotification> toUpdate;
    
    switch (scope) {
        case "RESTAURANT_HUB_ALL":
            toUpdate = repository.findByEventIdAndRestaurantId(eventId, restaurantId);
            break;
        case "RESTAURANT_CHANNEL_ALL":
            toUpdate = repository.findByEventIdAndRestaurantIdAndChannel(eventId, restaurantId, channel);
            break;
        default:
            throw new IllegalArgumentException("Unknown scope: " + scope);
    }
    
    toUpdate.forEach(n -> n.setStatus("READ"));
    repository.saveAll(toUpdate);
}
```

**Compatibilità:** ✅ Compatibile (isolated dal resto)

---

### Problema #9: Read status sync across multiple WebSocket connections

**Descrizione:**
User ha 2 browser tabs, Mark read in Tab 1 deve aggiornare Tab 2 immediately.

**Causa Tecnica:**
1. Tab 1: sends WebSocket message "mark read"
2. Server: calls ReadStatusService
3. Server: broadcasts to /topic/notifications/{userId}/RESTAURANT
4. Tab 1: riceve broadcast ✅
5. Tab 2: should riceve broadcast
6. BUT: se Tab 2 non ha active connection, perde il messaggio

**Scenario:**
- User ha 2 tabs aperte
- Network issue temp disconnects Tab 2
- Tab 2 reconnect, pero non sa che message was marked read
- Shows stale data

**Impact:**
- Stale UI state
- User confusion
- Severità: **LOW** 🟢 (user can refresh)

**Soluzione:**
Implement message persistence + catch-up:
```java
@GetMapping("/notifications/{userId}/status/{eventId}")
public ResponseEntity<NotificationStatus> getNotificationStatus(
    @PathVariable String userId,
    @PathVariable String eventId) {
    // Tab 2 can query current status on reconnect
    NotificationStatus status = readStatusService.getStatus(userId, eventId);
    return ResponseEntity.ok(status);
}
```

**Compatibilità:** ✅ Compatibile (isolated WebSocket logic)

---

### Problema #10: Read status with NULL restaurantId

**Descrizione:**
SQL query con restaurantId = NULL matcherebbe tutte le notifications con NULL restaurant_id.

**Causa Tecnica:**
```sql
UPDATE RestaurantUserNotification
SET status = 'READ'
WHERE event_id = 'evt-123'
  AND restaurant_id = NULL  -- SQL treats as FALSE, matches nothing
  AND channel = 'RESTAURANT'
```

**Problema:**
- NULL in SQL: `column = NULL` è sempre FALSE
- Query esegue ma non aggiorna niente
- Silent failure, no error thrown

**Scenario:**
- Bug: restaurantId not passed in request
- API calls service with null
- SQL executes silently
- No notification updated
- No error to alert developer

**Impact:**
- Silent failures
- Hard to debug
- Severità: **MEDIUM** 🟡

**Soluzione:**
Validate input before query:
```java
public void markSharedReadByScope(String eventId, String restaurantId, String scope) {
    if (eventId == null || eventId.isEmpty()) {
        throw new IllegalArgumentException("eventId cannot be null or empty");
    }
    if (restaurantId == null || restaurantId.isEmpty()) {
        throw new IllegalArgumentException("restaurantId cannot be null or empty");
    }
    
    List<RestaurantUserNotification> toUpdate = 
        repository.findByEventIdAndRestaurantId(eventId, restaurantId);
    
    if (toUpdate.isEmpty()) {
        log.warn("No notifications found for eventId={}, restaurantId={}", eventId, restaurantId);
    }
    
    toUpdate.forEach(n -> n.setStatus("READ"));
    repository.saveAll(toUpdate);
}
```

**Compatibilità:** ✅ Compatibile con Problema #8

---

## SEZIONE 5: PROBLEMI DI ARCHIVIAZIONE E CLEANUP

### Problema #11: EventOutbox never cleaned up → table grows indefinitely

**Descrizione:**
EventOutbox tiene tutti gli eventi (PENDING, PROCESSED, DEAD_LETTER) per sempre.

**Causa Tecnica:**
1. Event created → PENDING
2. Event published → PROCESSED
3. Never deleted ❌
4. After 1 year: millions of rows
5. Backups become slow
6. Queries become slow

**Scenario:**
- System running for 1 year
- 1000 events/day = 365K events
- All stored in EventOutbox
- SELECT * queries slow
- Backup takes hours

**Impact:**
- Database performance degrades
- Migration costs increase
- Operational complexity
- Severità: **MEDIUM** 🟡

**Soluzione:**
Implement cleanup job:
```java
@Component
@Scheduled(cron = "0 2 * * *")  // Run at 2 AM daily
public class EventOutboxCleanupJob {
    
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        
        // Keep last 30 days
        eventOutboxRepository.deleteByCreatedAtBeforeAndStatus(cutoff, "PROCESSED");
        
        // Archive DEAD_LETTER events
        List<EventOutbox> deadLetters = eventOutboxRepository.findByStatus("DEAD_LETTER");
        eventOutboxArchiveRepository.saveAll(deadLetters);
        eventOutboxRepository.deleteAll(deadLetters);
        
        log.info("Cleanup complete");
    }
}
```

**Compatibilità:** ✅ Compatibile (non-intrusive cleanup)

---

### Problema #12: CustomerNotification grows unboundedly → no auto-archive

**Descrizione:**
Old customer notifications **never deleted**, table grows indefinitely.

**Causa Tecnica:**
1. Notification created in CustomerNotification table
2. User reads it → status = 'READ'
3. Never deleted or archived
4. After 6 months: millions of old read notifications

**Scenario:**
- System running 6 months
- 1000 customers × 10 notifications each = 10M rows
- Still all in DB
- Performance impact

**Impact:**
- Database size bloat
- Query performance degrades
- Backup complexity
- Severità: **MEDIUM** 🟡

**Soluzione:**
Implement auto-archive:
```java
@Component
@Scheduled(cron = "0 3 * * *")  // Run at 3 AM daily
public class CustomerNotificationArchiveJob {
    
    public void archiveOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        
        // Archive read notifications older than 30 days
        List<CustomerNotification> toArchive = 
            repository.findByStatusAndCreatedAtBefore("READ", cutoff);
        
        archiveRepository.saveAll(toArchive);
        repository.deleteAll(toArchive);
        
        log.info("Archived {} old notifications", toArchive.size());
    }
}
```

**Compatibilità:** ✅ Compatibile con Problema #11

---

## SEZIONE 6: PROBLEMI DI MONITORING E OBSERVABILITY

### Problema #13: No alerting for slow notification delivery

**Descrizione:**
Se email takes 5 minutes (unusual), **no alert**. User confused, no visibility.

**Causa Tecnica:**
- ChannelPoller polls ogni 30 secondi
- Sends email via SMTP
- SMTP timeout = 30 seconds
- If slow: no alert, silent delay
- User dunno why email late

**Scenario:**
- SMTP server slow (disk I/O issue)
- ChannelPoller sends takes 10s each
- Backlog accumulates
- No monitoring shows this

**Impact:**
- Poor user experience
- No visibility into system health
- Reactive troubleshooting
- Severità: **MEDIUM** 🟡

**Soluzione:**
Implement latency tracking:
```java
@Component
public class DeliveryLatencyMonitor {
    
    private Timer deliveryTimer;
    
    public void trackDelivery(Notification notif, long startMs) {
        long latencyMs = System.currentTimeMillis() - startMs;
        
        deliveryTimer.record(latencyMs, TimeUnit.MILLISECONDS);
        
        if (latencyMs > 5000) {  // Alert if > 5 seconds
            alertService.alert("Slow delivery for channel " + notif.getChannel() + 
                             ": " + latencyMs + "ms");
        }
        
        metrics.recordLatency(notif.getChannel(), latencyMs);
    }
}
```

**Compatibilità:** ✅ Compatibile (observability addon)

---

### Problema #14: No visibility into permanently failed messages (DLQ monitoring)

**Descrizione:**
Se messages move to DLQ, **no dashboard** to view/replay them.

**Causa Tecnica:**
- Messages fail N times → moved to DLQ
- DLQ grows silently
- No admin visibility
- Manual query needed to see what's failing

**Scenario:**
- Listener bug causes messages to fail
- Messages move to DLQ
- Hours pass before anyone notices
- Manual investigation required

**Impact:**
- Silent failures
- Delayed incident response
- Manual operational burden
- Severità: **MEDIUM** 🟡

**Soluzione:**
Implement DLQ monitoring + replay:
```java
@Component
public class DLQMonitoringService {
    
    @Scheduled(cron = "*/5 * * * *")  // Every 5 minutes
    public void monitorDLQ() {
        long dlqSize = rabbitTemplate.execute(channel -> {
            QueueInformation qi = channel.queueDeclarePassive("notification.dlq");
            return (long) qi.getMessageCount();
        });
        
        if (dlqSize > 0) {
            alertService.alert("DLQ has " + dlqSize + " messages");
            metrics.recordDLQSize(dlqSize);
        }
    }
    
    public void replayMessageFromDLQ(String messageId) {
        Message msg = dlqRepository.findById(messageId);
        rabbitTemplate.convertAndSend(msg.getOriginalQueue(), msg.getBody());
        log.info("Replayed message {} to queue {}", messageId, msg.getOriginalQueue());
    }
}
```

**Compatibilità:** ✅ Compatibile con Problema #5

---

## SEZIONE 7: PROBLEMI DI CONFIGURAZIONE E DESIGN

### Problema #15: Notification preferences precedence unclear

**Descrizione:**
System ha 3 levels: **Global** (restaurant), **User**, **Event**. Quale vince?

**Causa Tecnica:**
- Global: "WebSocket disabled for restaurant"
- User: "WebSocket enabled for me"
- Event: "CRITICAL event (force all channels)"
- Which wins?

**Scenario:**
- Restaurant disables WebSocket to save costs
- Staff member enables it for themselves
- CRITICAL event arrives
- Should it send WebSocket? (overrides user choice?)

**Impact:**
- Unpredictable behavior
- Angry users
- Business logic uncertainty
- Severità: **MEDIUM** 🟡

**Soluzione:**
Establish clear precedence:
```
PRECEDENCE (highest to lowest):
1. EVENT rules (CRITICAL events always send)
2. USER preferences (staff choice)
3. GLOBAL settings (restaurant default)
4. CHANNEL default (always include at least 1 channel)

Code:
channels = Set(global_settings)
channels.intersect(user_preferences)
if event.isCritical():
    channels.add(ALL_CHANNELS)  # Override
```

**Compatibilità:** ✅ Compatibile (policy logic)

---

### Problema #16: EventId generation strategy not documented

**Descrizione:**
Comments show "evt-res-123-order-12345" but actual generation unclear.

**Causa Tecnica:**
- Is eventId generated by EventOutbox?
- Or by business service?
- What format?
- How to handle collisions?

**Scenario:**
- New developer adds event
- Doesn't know eventId format
- Causes issues downstream

**Impact:**
- Confusion
- Inconsistent eventIds
- Hard to debug
- Severità: **LOW** 🟢 (documentation only)

**Soluzione:**
Document eventId schema:
```
Format: evt-{eventType}-{sourceId}-{businessId}

Examples:
- evt-res-123-order-456       (Reservation event from restaurant 123, order 456)
- evt-cust-789-profile        (Customer event, user 789, profile change)
- evt-sys-global-config       (System event, global config change)

Generation:
- Business service creates eventId
- EventOutbox receives pre-generated eventId
- NO collision detection (assumed unique from source)
```

**Compatibilità:** ✅ Compatibile (documentation only)

---

### Problema #17: ChannelPoller scales → duplicate sends possible

**Descrizione:**
If **multiple ChannelPoller instances**, both could send same notification.

**Causa Tecnica:**
1. ChannelPoller polls every 30 seconds
2. SELECT FROM ChannelNotification WHERE status='PENDING'
3. If 2 pods run parallel
4. Both SELECT same notification
5. Both attempt to send email
6. Double-send possible if no locking

**Scenario:**
- Prod has 2 ChannelPoller pods for HA
- Both run same query
- No distributed lock
- Both pick notification-123
- Email sent twice to customer

**Impact:**
- Duplicate channel sends (customer gets 2 emails)
- Poor UX
- Severità: **MEDIUM** 🟡

**Soluzione:**
Use distributed lock or single pod:
```
Option A: Single pod (SIMPLE, CURRENT)
- Deploy ChannelPoller on single pod only
- If pod dies, use K8s restart policy
- Acceptable for email (not real-time critical)

Option B: Distributed lock (COMPLEX)
- Use Redis GETEX command
- Or database SELECT FOR UPDATE
- Multiple pods, only 1 acquires lock
```

**Compatibilità:** ✅ Compatibile (deployment strategy)

---

## Summary Matrix

| # | Problema | Causa | Soluzione | Impact | Compatibilità |
|---|----------|-------|----------|--------|---------------|
| 1 | Event stuck in PENDING | Network fail during send + ProcessedEvent exists | Invert operation order | CRITICAL 🔴 | ✅ |
| 2 | Duplicate if ProcessedEvent insert fails | DB error after publish | Retry with backoff | MEDIUM 🟡 | ✅ |
| 3 | Multiple Orchestrator race | Horizontal scaling, no lock | Distributed lock or single pod | MEDIUM 🟠 | ✅ |
| 4 | RabbitMQ outage bloats EventOutbox | No max retry / dead letter | Max attempts + DEAD_LETTER status | MEDIUM 🟡 | ✅ |
| 5 | No DLQ for failed messages | No dead letter queue | Implement DLX + retry counter | CRITICAL 🔴 | ✅ |
| 6 | Listener crash before INSERT | No transaction protection | Ensure @Transactional | LOW 🟢 | ✅ |
| 7 | Multiple listeners duplicate | Horizontal scaling | Listener-side UNIQUE check | MEDIUM 🟡 | ✅ |
| 8 | Shared read LIKE collision | Fuzzy eventId matching | Exact match + explicit scope | MEDIUM 🟡 | ✅ |
| 9 | WebSocket read sync stale | No message persistence | Query current status on reconnect | LOW 🟢 | ✅ |
| 10 | NULL restaurantId silent fail | No input validation | Validate before query | MEDIUM 🟡 | ✅ |
| 11 | EventOutbox never cleaned | No retention policy | Archive old events daily | MEDIUM 🟡 | ✅ |
| 12 | CustomerNotification unbounded | No auto-archive | Archive read >30 days daily | MEDIUM 🟡 | ✅ |
| 13 | No alert for slow delivery | No latency monitoring | Track P95 latency + alert | MEDIUM 🟡 | ✅ |
| 14 | No DLQ visibility | No admin dashboard | DLQ monitoring + replay UI | MEDIUM 🟡 | ✅ |
| 15 | Preferences precedence unclear | Policy undefined | Event > User > Global precedence | MEDIUM 🟡 | ✅ |
| 16 | EventId format unclear | No documentation | Document schema + examples | LOW 🟢 | ✅ |
| 17 | ChannelPoller duplicate sends | Multiple pods, no lock | Single pod or distributed lock | MEDIUM 🟡 | ✅ |

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Purpose**: Comprehensive problem analysis with solutions  
**Status**: Ready for implementation planning
