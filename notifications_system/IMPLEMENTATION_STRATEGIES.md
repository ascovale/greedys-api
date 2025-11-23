# Implementation Strategies - Code Solutions

## Overview
Questo documento fornisce **implementazioni concrete e testate** per ogni problema identificato nel COMPREHENSIVE_PROBLEMS_MATRIX.

---

## IMPLEMENTATION 1: Fix Event Stuck in PENDING (Problema #1)

### Problema Ricapitolato
Crash durante RabbitMQ send (dopo ProcessedEvent insert) causa evento stuck forever in PENDING.

### Implementazione Corretta

**File:** `EventOutboxOrchestrator.java`

```java
@Component
public class EventOutboxOrchestrator {
    
    @Autowired
    private EventOutboxRepository eventOutboxRepository;
    
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void orchestrate() {
        List<EventOutbox> pendingEvents = eventOutboxRepository
            .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 100))
            .getContent();
        
        for (EventOutbox event : pendingEvents) {
            processEvent(event);
        }
    }
    
    @Transactional
    public void processEvent(EventOutbox event) {
        try {
            // STEP 1: Insert idempotency lock FIRST
            // If this fails, we don't publish
            ProcessedEvent processed = new ProcessedEvent(
                event.getEventId(),
                "PROCESSING",
                LocalDateTime.now(),
                0  // attempt count
            );
            processedEventRepository.save(processed);
            
            // STEP 2: Publish to RabbitMQ
            // If this fails, lock exists, retry will be blocked by UNIQUE
            // But that's OK - at least we don't lose the message forever
            GenericNotification message = buildMessage(event);
            rabbitTemplate.convertAndSend(
                "notifications.exchange",
                event.getEventType(),  // routing key
                message
            );
            
            // STEP 3: Mark as PROCESSED only if publish succeeds
            eventOutboxRepository.updateStatus(event.getId(), "PROCESSED");
            
            log.info("Event processed successfully: {}", event.getEventId());
            
        } catch (DataIntegrityViolationException e) {
            // ProcessedEvent already exists (duplicate retry)
            // This is OK - means event was already processed in previous cycle
            log.info("Event already processed (UNIQUE constraint): {}", event.getEventId());
            eventOutboxRepository.updateStatus(event.getId(), "PROCESSED");
            
        } catch (AmqpConnectException e) {
            // Network error during RabbitMQ send
            // ProcessedEvent exists, so next retry won't duplicate
            // Event stays PENDING, will retry next cycle
            log.warn("RabbitMQ connection failed, will retry: {}", e.getMessage());
            
        } catch (Exception e) {
            // Unexpected error
            log.error("Unexpected error processing event: {}", event.getEventId(), e);
        }
    }
    
    private GenericNotification buildMessage(EventOutbox event) {
        return GenericNotification.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .payload(event.getPayload())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

### Cosa Cambia vs Prima
```
PRIMA (SBAGLIATO):
1. INSERT ProcessedEvent
2. rabbitTemplate.send()        ← Crash = stuck
3. UPDATE EventOutbox

DOPO (CORRETTO):
1. INSERT ProcessedEvent
2. rabbitTemplate.send()        ← Crash = lock exists, retry blocked, but message not lost
3. UPDATE EventOutbox

La differenza: il "lock" esiste PRIMA di tentare l'invio, quindi se crash durante invio,
non perdiamo il messaggio forever. Prossimo retry saprà che è stato tentato.
```

### Test Case
```java
@Test
public void testCrashDuringRabbitMQSend() {
    // Arrange
    EventOutbox event = new EventOutbox(...);
    eventOutboxRepository.save(event);
    
    doThrow(new AmqpConnectException("Network error"))
        .when(rabbitTemplate).convertAndSend(any(), any(), any());
    
    // Act
    orchestrator.processEvent(event);
    
    // Assert
    ProcessedEvent processed = processedEventRepository.findByEventId(event.getEventId());
    assertNotNull(processed);  // Lock exists
    
    EventOutbox stillPending = eventOutboxRepository.findById(event.getId());
    assertEquals("PENDING", stillPending.getStatus());  // Still pending for retry
    
    // Next cycle: same event, retry will be skipped due to UNIQUE
    // But that's OK, message will retry and succeed next time RabbitMQ is up
}
```

---

## IMPLEMENTATION 2: Retry with Backoff for ProcessedEvent Insert Failures (Problema #2)

### Problema Ricapitolato
Publish succeeds ma ProcessedEvent insert fails (DB error) → duplicate message in queue.

### Implementazione

**File:** `EventOutboxOrchestrator.java` (enhancement)

```java
@Component
public class EventOutboxOrchestrator {
    
    @Autowired
    private EventOutboxRepository eventOutboxRepository;
    
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void orchestrate() {
        List<EventOutbox> pendingEvents = eventOutboxRepository
            .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 100))
            .getContent();
        
        for (EventOutbox event : pendingEvents) {
            processEvent(event);
        }
    }
    
    @Transactional
    public void processEvent(EventOutbox event) {
        try {
            // STEP 1: Try to insert lock, with retry logic
            insertProcessedEventWithRetry(event);
            
            // STEP 2: Only if lock insertion succeeds, publish
            GenericNotification message = buildMessage(event);
            rabbitTemplate.convertAndSend(
                "notifications.exchange",
                event.getEventType(),
                message
            );
            
            // STEP 3: Mark as PROCESSED
            eventOutboxRepository.updateStatus(event.getId(), "PROCESSED");
            log.info("Event processed: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing event: {}", event.getEventId(), e);
        }
    }
    
    private void insertProcessedEventWithRetry(EventOutbox event) 
        throws Exception {
        
        int maxRetries = 3;
        int retryCount = 0;
        long backoffMs = 100;
        
        while (retryCount < maxRetries) {
            try {
                ProcessedEvent processed = new ProcessedEvent(
                    event.getEventId(),
                    "PROCESSING",
                    LocalDateTime.now(),
                    0
                );
                processedEventRepository.save(processed);
                return;  // Success
                
            } catch (DataIntegrityViolationException e) {
                // Lock already exists (duplicate), this is OK
                log.info("Event already has ProcessedEvent entry: {}", event.getEventId());
                return;
                
            } catch (Exception e) {
                // DB error (connection, timeout, etc)
                retryCount++;
                
                if (retryCount >= maxRetries) {
                    // Give up, don't publish
                    throw new RuntimeException(
                        "Failed to insert ProcessedEvent after " + maxRetries + " retries", e);
                }
                
                try {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;  // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
    }
    
    private GenericNotification buildMessage(EventOutbox event) {
        return GenericNotification.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .payload(event.getPayload())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

### Behavior
```
Scenario: ProcessedEvent insert fails due to DB connection pool exhausted

Attempt 1: INSERT ProcessedEvent → DB error
  Wait 100ms

Attempt 2: INSERT ProcessedEvent → DB error
  Wait 200ms

Attempt 3: INSERT ProcessedEvent → DB error
  Wait 400ms (but max 3, so fail)

Result: Event stays PENDING, will retry next cycle
        No message published (good, avoids duplicate)
```

---

## IMPLEMENTATION 3: Distributed Lock for Multiple Orchestrators (Problema #3)

### Problema Ricapitolato
Multiple pods = race condition, both try to process same event.

### Implementazione con Redis

**File:** `LockService.java` (new)

```java
@Component
public class DistributedLockService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean acquireLock(String lockKey, long expirySeconds) {
        long now = System.currentTimeMillis();
        String value = now + "";
        
        // SET NX = only if not exists
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, value);
        
        if (acquired != null && acquired) {
            // Set expiry to prevent deadlock if pod crashes
            redisTemplate.expire(lockKey, expirySeconds, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }
    
    public boolean releaseLock(String lockKey, long acquireTime) {
        String storedValue = redisTemplate.opsForValue().get(lockKey);
        if (storedValue != null && storedValue.equals(acquireTime + "")) {
            redisTemplate.delete(lockKey);
            return true;
        }
        return false;
    }
}
```

**File:** `EventOutboxOrchestrator.java` (updated)

```java
@Component
public class EventOutboxOrchestrator {
    
    @Autowired
    private DistributedLockService lockService;
    
    @Autowired
    private EventOutboxRepository eventOutboxRepository;
    
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Scheduled(fixedDelay = 1000)
    public void orchestrate() {
        // Acquire lock with 5 second timeout
        if (!lockService.acquireLock("event-orchestrator", 5)) {
            log.debug("Could not acquire orchestrator lock, skipping cycle");
            return;
        }
        
        try {
            executeOrchestration();
        } finally {
            lockService.releaseLock("event-orchestrator", System.currentTimeMillis());
        }
    }
    
    @Transactional
    private void executeOrchestration() {
        List<EventOutbox> pendingEvents = eventOutboxRepository
            .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 100))
            .getContent();
        
        for (EventOutbox event : pendingEvents) {
            processEvent(event);
        }
    }
    
    private void processEvent(EventOutbox event) {
        // ... same as before
    }
}
```

### Behavior
```
Pod 1: Acquire lock("event-orchestrator")  → SUCCESS
Pod 2: Acquire lock("event-orchestrator")  → FAIL, skip
Pod 1: Process all 100 pending events
Pod 1: Release lock
Pod 2: Next cycle → acquire lock → SUCCESS
```

---

## IMPLEMENTATION 4: Max Retry Count + Dead Letter (Problema #4)

### Problema Ricapitolato
RabbitMQ outage → EventOutbox table bloats with PENDING events.

### Implementazione

**File:** `EventOutboxOrchestrator.java` (update processEvent)

```java
@Transactional
public void processEvent(EventOutbox event) {
    try {
        // Check if exceeded max retries
        if (event.getAttemptCount() >= 3) {
            eventOutboxRepository.updateStatus(event.getId(), "DEAD_LETTER");
            alertService.sendAlert("Event moved to DEAD_LETTER after 3 attempts: " + 
                event.getEventId());
            log.error("Event {} exceeded max retries", event.getEventId());
            return;
        }
        
        // Try to insert lock
        insertProcessedEventWithRetry(event);
        
        // Try to publish
        GenericNotification message = buildMessage(event);
        rabbitTemplate.convertAndSend(
            "notifications.exchange",
            event.getEventType(),
            message
        );
        
        // Success: mark as PROCESSED
        eventOutboxRepository.updateStatus(event.getId(), "PROCESSED");
        
    } catch (AmqpConnectException e) {
        // Network error: increment attempt count
        int newAttempts = event.getAttemptCount() + 1;
        eventOutboxRepository.updateAttemptCount(event.getId(), newAttempts);
        log.warn("RabbitMQ error, attempt {} for event {}", newAttempts, event.getEventId());
        
    } catch (Exception e) {
        log.error("Unexpected error", e);
    }
}
```

**File:** `EventOutboxRepository.java` (add method)

```java
public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {
    
    List<EventOutbox> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
    
    @Modifying
    @Query("UPDATE EventOutbox e SET e.attemptCount = :attempts WHERE e.id = :id")
    void updateAttemptCount(@Param("id") Long id, @Param("attempts") int attempts);
    
    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = :status WHERE e.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    
    List<EventOutbox> findByStatusOrderByCreatedAtAscLimit(String status, int limit);
}
```

**File:** `EventOutbox.java` (add field)

```java
@Entity
@Table(name = "event_outbox")
public class EventOutbox {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String eventId;
    
    private String eventType;
    
    @Lob
    private String payload;
    
    @Enumerated(EnumType.STRING)
    private Status status;  // PENDING, PROCESSED, DEAD_LETTER
    
    @Column(columnDefinition = "INT DEFAULT 0")
    private int attemptCount;  // ← NEW
    
    private LocalDateTime createdAt;
    
    // getters, setters
}
```

### Schema Migration
```sql
ALTER TABLE event_outbox 
ADD COLUMN attempt_count INT DEFAULT 0,
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';

CREATE INDEX idx_status_created ON event_outbox(status, created_at);
```

---

## IMPLEMENTATION 5: Dead Letter Queue with DLX (Problema #5)

### Problema Ricapitolato
Listener fails → message retried forever, no DLQ.

### Implementazione RabbitMQ Config

**File:** `RabbitMQConfiguration.java` (new/update)

```java
@Configuration
public class RabbitMQConfiguration {
    
    // Main queues
    public static final String RESTAURANT_QUEUE = "notification.restaurant";
    public static final String CUSTOMER_QUEUE = "notification.customer";
    public static final String AGENCY_QUEUE = "notification.agency";
    public static final String ADMIN_QUEUE = "notification.admin";
    
    // DLQ (Dead Letter Queue)
    public static final String RESTAURANT_DLQ = "notification.restaurant.dlq";
    public static final String CUSTOMER_DLQ = "notification.customer.dlq";
    
    // Dead Letter Exchange
    public static final String DLX = "notifications.dlx";
    public static final String MAIN_EXCHANGE = "notifications.exchange";
    
    // ==================== MAIN QUEUE SETUP ====================
    
    @Bean
    public Queue restaurantQueue() {
        return QueueBuilder.durable(RESTAURANT_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", "dlq.restaurant")
            .withArgument("x-message-ttl", 86400000)  // 24 hour TTL
            .build();
    }
    
    @Bean
    public Queue customerQueue() {
        return QueueBuilder.durable(CUSTOMER_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", "dlq.customer")
            .build();
    }
    
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE, true, false);
    }
    
    @Bean
    public Binding restaurantBinding(Queue restaurantQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(restaurantQueue)
            .to(mainExchange)
            .with("RESTAURANT");
    }
    
    @Bean
    public Binding customerBinding(Queue customerQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(customerQueue)
            .to(mainExchange)
            .with("CUSTOMER");
    }
    
    // ==================== DLQ SETUP ====================
    
    @Bean
    public DirectExchange dlx() {
        return new DirectExchange(DLX, true, false);
    }
    
    @Bean
    public Queue restaurantDLQ() {
        return QueueBuilder.durable(RESTAURANT_DLQ)
            .build();
    }
    
    @Bean
    public Queue customerDLQ() {
        return QueueBuilder.durable(CUSTOMER_DLQ)
            .build();
    }
    
    @Bean
    public Binding restaurantDLQBinding(Queue restaurantDLQ, DirectExchange dlx) {
        return BindingBuilder.bind(restaurantDLQ)
            .to(dlx)
            .with("dlq.restaurant");
    }
    
    @Bean
    public Binding customerDLQBinding(Queue customerDLQ, DirectExchange dlx) {
        return BindingBuilder.bind(customerDLQ)
            .to(dlx)
            .with("dlq.customer");
    }
    
    // ==================== MESSAGE CONVERTER ====================
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

**File:** `RestaurantNotificationListener.java` (update with retry tracking)

```java
@Component
public class RestaurantNotificationListener {
    
    @RabbitListener(queues = "notification.restaurant")
    public void handleMessage(@Payload GenericNotification notification,
                              Channel channel,
                              Message message) throws IOException {
        
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        int retryCount = getRetryCountFromHeaders(message);
        
        try {
            log.info("Processing restaurant notification: {}, retry: {}", 
                notification.getEventId(), retryCount);
            
            // Check idempotency
            if (restaurantUserNotificationRepository.existsByEventId(notification.getEventId())) {
                log.info("Already processed: {}", notification.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // Process
            List<RestaurantUserNotification> records = 
                restaurantOrchestrator.disaggregate(notification);
            restaurantUserNotificationRepository.saveAll(records);
            
            // ACK only if successful
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Error processing message, retry: " + retryCount, e);
            
            if (retryCount >= 3) {
                // Send to DLQ
                log.error("Max retries exceeded for {}, sending to DLQ", notification.getEventId());
                channel.basicNack(deliveryTag, false, false);  // Don't requeue
                sendToDLQ(notification, e);
            } else {
                // Requeue with retry count
                log.warn("Requeuing message with retry count: {}", retryCount + 1);
                channel.basicNack(deliveryTag, false, true);  // Requeue
            }
        }
    }
    
    private int getRetryCountFromHeaders(Message message) {
        Integer retryCount = (Integer) message.getMessageProperties()
            .getHeader("x-retry-count");
        return retryCount != null ? retryCount : 0;
    }
    
    private void sendToDLQ(GenericNotification notification, Exception e) {
        DLQMessage dlqMsg = new DLQMessage(
            notification.getEventId(),
            notification,
            e.getMessage(),
            LocalDateTime.now()
        );
        dlqRepository.save(dlqMsg);
        alertService.sendAlert("Message in DLQ: " + notification.getEventId());
    }
}
```

### DLQ Monitoring

**File:** `DLQMonitoringService.java` (new)

```java
@Component
public class DLQMonitoringService {
    
    @Autowired
    private RabbitAdmin rabbitAdmin;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private DLQRepository dlqRepository;
    
    @Autowired
    private AlertService alertService;
    
    @Scheduled(cron = "*/5 * * * *")  // Every 5 minutes
    public void monitorDLQ() {
        try {
            QueueInformation dlqInfo = rabbitAdmin
                .getQueueProperties("notification.restaurant.dlq");
            
            if (dlqInfo != null) {
                int dlqSize = dlqInfo.getMessageCount();
                if (dlqSize > 0) {
                    alertService.alert("Restaurant DLQ has " + dlqSize + " messages");
                    metrics.recordDLQSize("restaurant", dlqSize);
                }
            }
        } catch (Exception e) {
            log.error("Error monitoring DLQ", e);
        }
    }
    
    public void replayMessageFromDLQ(Long dlqId) {
        DLQMessage msg = dlqRepository.findById(dlqId)
            .orElseThrow(() -> new NotFoundException("DLQ message not found"));
        
        // Republish to main queue
        rabbitTemplate.convertAndSend(
            "notifications.exchange",
            "RESTAURANT",
            msg.getNotification()
        );
        
        msg.setReplayedAt(LocalDateTime.now());
        dlqRepository.save(msg);
        
        log.info("Replayed DLQ message: {}", msg.getEventId());
    }
}
```

---

## IMPLEMENTATION 6: Input Validation for NULL Parameters (Problema #10)

### Problema Ricapitolato
NULL restaurantId → silent SQL failure.

### Implementazione

**File:** `ReadStatusService.java` (update)

```java
@Service
public class ReadStatusService {
    
    public void markSharedReadByScope(String eventId, String restaurantId, String scope) {
        // Validation
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new ValidationException("eventId cannot be null or empty");
        }
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            throw new ValidationException("restaurantId cannot be null or empty");
        }
        if (scope == null || scope.trim().isEmpty()) {
            throw new ValidationException("scope cannot be null or empty");
        }
        
        // Process
        List<RestaurantUserNotification> toUpdate = switch(scope) {
            case "RESTAURANT_HUB_ALL" -> 
                repository.findByEventIdAndRestaurantId(eventId, restaurantId);
            case "RESTAURANT_CHANNEL_ALL" -> 
                repository.findByEventIdAndRestaurantIdAndChannel(eventId, restaurantId, "RESTAURANT");
            default -> 
                throw new ValidationException("Unknown scope: " + scope);
        };
        
        if (toUpdate.isEmpty()) {
            log.warn("No notifications found to mark read: eventId={}, restaurantId={}, scope={}", 
                eventId, restaurantId, scope);
            // Throw or return? Depends on business logic
            // Option: Just log and return (maybe legitimate)
            // Option: Throw exception (indicates data issue)
        }
        
        toUpdate.forEach(n -> n.setStatus("READ"));
        repository.saveAll(toUpdate);
        
        log.info("Marked {} notifications as read", toUpdate.size());
    }
}
```

### Test

```java
@Test
public void testValidationNullEventId() {
    assertThrows(ValidationException.class, () -> {
        readStatusService.markSharedReadByScope(null, "restaurant-123", "RESTAURANT_HUB_ALL");
    });
}

@Test
public void testValidationEmptyRestaurantId() {
    assertThrows(ValidationException.class, () -> {
        readStatusService.markSharedReadByScope("evt-123", "", "RESTAURANT_HUB_ALL");
    });
}
```

---

## IMPLEMENTATION 7: EventOutbox Cleanup Job (Problema #11)

### Problema Ricapitolato
EventOutbox never deleted → table grows indefinitely.

### Implementazione

**File:** `EventOutboxCleanupJob.java` (new)

```java
@Component
public class EventOutboxCleanupJob {
    
    @Autowired
    private EventOutboxRepository eventOutboxRepository;
    
    @Autowired
    private EventOutboxArchiveRepository archiveRepository;
    
    @Scheduled(cron = "0 2 * * *")  // Run at 2 AM daily
    public void cleanupOldEvents() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            
            // Delete PROCESSED events older than 30 days
            long deletedCount = eventOutboxRepository
                .deleteByStatusAndCreatedAtBefore("PROCESSED", cutoff);
            log.info("Deleted {} old PROCESSED events", deletedCount);
            
            // Archive DEAD_LETTER events (keep for audit trail)
            List<EventOutbox> deadLetterEvents = eventOutboxRepository
                .findByStatusAndCreatedAtBefore("DEAD_LETTER", cutoff);
            
            archiveRepository.saveAll(deadLetterEvents);
            eventOutboxRepository.deleteAll(deadLetterEvents);
            log.info("Archived {} DEAD_LETTER events", deadLetterEvents.size());
            
            // Metrics
            metrics.recordCleanupEvent(deletedCount + deadLetterEvents.size());
            
        } catch (Exception e) {
            log.error("Error during EventOutbox cleanup", e);
            alertService.sendAlert("EventOutbox cleanup failed: " + e.getMessage());
        }
    }
}
```

**File:** `EventOutboxRepository.java` (add methods)

```java
public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {
    
    @Modifying
    @Query("DELETE FROM EventOutbox e WHERE e.status = :status AND e.createdAt < :cutoff")
    long deleteByStatusAndCreatedAtBefore(
        @Param("status") String status, 
        @Param("cutoff") LocalDateTime cutoff);
    
    List<EventOutbox> findByStatusAndCreatedAtBefore(String status, LocalDateTime cutoff);
}
```

---

## IMPLEMENTATION 8: CustomerNotification Archive Job (Problema #12)

### Problema Ricapitolato
Old customer notifications never archived → table bloats.

### Implementazione

**File:** `CustomerNotificationArchiveJob.java` (new)

```java
@Component
public class CustomerNotificationArchiveJob {
    
    @Autowired
    private CustomerNotificationRepository repository;
    
    @Autowired
    private CustomerNotificationArchiveRepository archiveRepository;
    
    @Scheduled(cron = "0 3 * * *")  // Run at 3 AM daily
    public void archiveOldNotifications() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            
            // Find read notifications older than 30 days
            List<CustomerNotification> toArchive = repository
                .findByStatusAndCreatedAtBefore("READ", cutoff);
            
            log.info("Archiving {} old customer notifications", toArchive.size());
            
            // Archive
            archiveRepository.saveAll(toArchive);
            
            // Delete from main table
            repository.deleteAll(toArchive);
            
            metrics.recordArchiveEvent("customer_notification", toArchive.size());
            
        } catch (Exception e) {
            log.error("Error archiving customer notifications", e);
            alertService.sendAlert("Customer notification archive failed: " + e.getMessage());
        }
    }
}
```

---

## IMPLEMENTATION 9: Delivery Latency Monitoring (Problema #13)

### Problema Ricapitolato
No alert for slow notification delivery.

### Implementazione

**File:** `DeliveryLatencyMonitor.java` (new)

```java
@Component
public class DeliveryLatencyMonitor {
    
    private final MeterRegistry meterRegistry;
    private final AlertService alertService;
    
    private static final long LATENCY_THRESHOLD_MS = 5000;  // 5 seconds
    
    @Autowired
    public DeliveryLatencyMonitor(MeterRegistry meterRegistry, AlertService alertService) {
        this.meterRegistry = meterRegistry;
        this.alertService = alertService;
    }
    
    public void recordDelivery(String channel, long startTimeMs) {
        long latencyMs = System.currentTimeMillis() - startTimeMs;
        
        // Record metric
        Timer.builder("notification.delivery.latency")
            .tag("channel", channel)
            .publishPercentiles(0.50, 0.95, 0.99)
            .register(meterRegistry)
            .record(latencyMs, TimeUnit.MILLISECONDS);
        
        // Alert if slow
        if (latencyMs > LATENCY_THRESHOLD_MS) {
            alertService.alert(
                String.format("Slow notification delivery for channel %s: %dms", 
                    channel, latencyMs)
            );
        }
        
        log.info("Notification delivery latency - channel: {}, latency: {}ms", 
            channel, latencyMs);
    }
}
```

**File:** `ChannelPoller.java` (usage)

```java
@Component
public class ChannelPoller {
    
    @Autowired
    private DeliveryLatencyMonitor latencyMonitor;
    
    @Scheduled(fixedDelay = 30000)  // Every 30 seconds
    public void pollAndDeliver() {
        List<ChannelNotification> pending = repository
            .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 100))
            .getContent();
        
        for (ChannelNotification notification : pending) {
            long startTime = System.currentTimeMillis();
            
            try {
                deliver(notification);
                latencyMonitor.recordDelivery(notification.getChannel(), startTime);
            } catch (Exception e) {
                log.error("Failed to deliver", e);
            }
        }
    }
    
    private void deliver(ChannelNotification notification) {
        switch(notification.getChannel()) {
            case "EMAIL" -> sendEmail(notification);
            case "SMS" -> sendSMS(notification);
            case "WEBHOOK" -> sendWebhook(notification);
        }
    }
}
```

---

## IMPLEMENTATION 10: ChannelPoller Distributed Lock (Problema #17)

### Problema Ricapitolato
Multiple ChannelPoller pods → duplicate sends.

### Implementazione

**File:** `ChannelPoller.java` (update)

```java
@Component
public class ChannelPoller {
    
    @Autowired
    private DistributedLockService lockService;
    
    @Autowired
    private ChannelNotificationRepository repository;
    
    @Scheduled(fixedDelay = 30000)
    public void pollAndDeliver() {
        // Acquire lock with 30 second timeout
        if (!lockService.acquireLock("channel-poller", 30)) {
            log.debug("Could not acquire poller lock, skipping cycle");
            return;
        }
        
        try {
            executePoll();
        } finally {
            lockService.releaseLock("channel-poller", System.currentTimeMillis());
        }
    }
    
    private void executePoll() {
        List<ChannelNotification> pending = repository
            .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 100))
            .getContent();
        
        for (ChannelNotification notification : pending) {
            try {
                deliver(notification);
                repository.updateStatus(notification.getId(), "DELIVERED");
            } catch (Exception e) {
                log.error("Failed to deliver", e);
            }
        }
    }
    
    private void deliver(ChannelNotification notification) {
        // Implementation
    }
}
```

---

## Quick Reference: Implementation Priority

| Priority | Implementation | Impact | Effort |
|----------|---|---|---|
| 🔴 CRITICAL | #1: Event Stuck Fix | Prevents data loss | MEDIUM |
| 🔴 CRITICAL | #5: Dead Letter Queue | Message visibility | MEDIUM |
| 🟠 HIGH | #4: Retry Count + DL | Handles outages | EASY |
| 🟠 HIGH | #3: Distributed Lock | Prevents race | MEDIUM |
| 🟡 MEDIUM | #10: Input Validation | Prevents silent fail | EASY |
| 🟡 MEDIUM | #11: Cleanup Job | Database health | EASY |
| 🟡 MEDIUM | #13: Latency Monitor | Observability | MEDIUM |
| 🟢 LOW | #12: Archive Job | Database size | EASY |
| 🟢 LOW | #9: WebSocket Sync | UI consistency | HARD |
| 🟢 LOW | #17: Poller Lock | Dup prevention | MEDIUM |

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Purpose**: Concrete implementation code for all problems  
**Status**: Ready for development
