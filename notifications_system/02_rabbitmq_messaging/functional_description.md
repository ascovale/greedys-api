# RabbitMQ Messaging - Functional Description

## Purpose

RabbitMQ is the **message broker** that routes published events to the appropriate listeners. It serves as a highly available, persistent queue system that guarantees messages reach listeners even if services are temporarily down.

## Core Responsibility

- **Store** messages from EventOutboxOrchestrator in named queues
- **Route** messages to correct queue based on aggregate type
- **Deliver** messages to connected listeners with retry guarantees
- **Acknowledge** message receipt from listeners before removing from queue

## Functional Behavior

### 1. **Message Receipt from Producer**

EventOutboxOrchestrator publishes 1 message per event:
```
EventOutboxOrchestrator calls:
  rabbitTemplate.convertAndSend("notification.restaurant", message)
  
RabbitMQ:
  ├─ Receives message
  ├─ Serializes to bytes (JSON text)
  ├─ Stores in notification.restaurant queue
  └─ Waits for consumer
```

### 2. **Queue Routing by Type**

4 primary queues (one per user type):
- **notification.restaurant**: For personal restaurant staff notifications → RestaurantNotificationListener
- **notification.restaurant.reservations**: For TEAM-scoped reservations → RestaurantTeamNotificationListener
- **notification.customer**: For customer notifications → CustomerNotificationListener
- **notification.agency**: For agency staff notifications → AgencyUserNotificationListener  
- **notification.admin**: For admin notifications → AdminNotificationListener

### 3. **Message Storage**

- **Durable queues**: Persist to disk, survive broker restart
- **Unlimited retention**: Messages stay until consumed and ACK'd
- **FIFO order**: Processed in creation order (mostly, retries may reorder)

### 4. **Consumer Delivery**

When listener connects:
```
Listener subscribes: @RabbitListener(queues="notification.restaurant")
  └─ Connection established
  └─ Consumer registered with RabbitMQ
  
RabbitMQ delivers:
  └─ Sends message to consumer
  └─ Waits for ACK (confirmation)
  
If message processed:
  └─ Consumer calls channel.basicAck()
  └─ Message removed from queue
  
If message fails:
  └─ Consumer calls channel.basicNack(requeue=true)
  └─ Message requeued (back to queue)
  └─ Attempted by another consumer or same consumer later
```

### 5. **Error Handling**

- **No response from consumer**: RabbitMQ times out, requeues message
- **Consumer crashes**: Connection drops, RabbitMQ requeues all unack'd messages
- **Persistent failures**: After N retries, message may go to Dead Letter Queue

### 6. **Concurrency & Load Balancing**

Multiple listeners instances of same type:
```
Instance A + Instance B + Instance C
  └─ All connect to notification.restaurant queue
  
RabbitMQ distributes:
  ├─ Message 1 → Instance A
  ├─ Message 2 → Instance B
  ├─ Message 3 → Instance C
  ├─ Message 4 → Instance A (round-robin or per-prefetch)
  └─ Linear throughput increase
```

---

## Message Format & Schema

### Structure
```json
{
  "event_id": "string (unique identifier)",
  "event_type": "string (RESERVATION_REQUESTED, etc.)",
  "aggregate_type": "string (RESTAURANT, CUSTOMER, AGENCY, ADMIN)",
  "aggregate_id": "number (entity ID)",
  "payload": {
    "... (variable fields per event type)"
  }
}
```

### Size
- Typical: 500 bytes - 2 KB per message
- Maximum: Configurable, typically 128 MB

### Content Encoding
- **Serialization**: JSON (text-based)
- **Character Set**: UTF-8
- **Compression**: Optional (not currently enabled)

---

## Queue Configuration Details

### Durability
- **Durable queues**: YES (persisted to disk)
- **Recovery**: On broker restart, queues and their messages recovered
- **Data Loss**: Only if disk failure

### Retention Policy
- **TTL (Time-To-Live)**: None configured (messages stay indefinitely)
- **Max Queue Size**: Unlimited
- **Max Message Size**: Unlimited

### Consumer Behavior
- **Prefetch Count**: 1 (one message per consumer at a time)
- **Acknowledgment**: Manual (explicit ACK/NACK required)
- **Timeout**: DEFAULT (usually 30-60 seconds)

### Dead Letter Queue (DLQ)
- **Purpose**: Capture permanently failed messages
- **Trigger**: After max retries or explicit rejection
- **Queue Name**: `notification.dlq` (if configured)
- **Visibility**: Admin UI for inspection and replay

---

## Topic Exchange Binding

### Exchange Type
- **Type**: Topic Exchange
- **Name**: `notifications.exchange`
- **Durable**: YES

### Routing Keys & Bindings
```
Routing Pattern: notification.{type}

Bindings:
  ├─ notification.restaurant → notification.restaurant queue
  ├─ notification.customer → notification.customer queue
  ├─ notification.agency → notification.agency queue
  └─ notification.admin → notification.admin queue
```

### Routing Rules
```
EventOutboxOrchestrator publishes:
  ├─ To: notifications.exchange
  ├─ Routing Key: "notification.restaurant" 
  └─ → RabbitMQ matches binding
     → Delivers to notification.restaurant queue

Same for other types
```

---

## Delivery Guarantees

### At-Least-Once Semantics
- ✅ Each message delivered at least once
- ✅ No message loss (if disk available)
- ⚠️ Possible duplicates (if producer retries or listener crashes before ACK)
- ⚠️ Order not guaranteed (retries may reorder)

### No Message Loss Guarantees
- Event is in EventOutbox (Layer 1)
- Message is in RabbitMQ queue
- Message will reach listener
- Listener persists to DB with idempotency check

---

## RabbitMQ Configuration (Spring Boot)

```properties
# Connection
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.virtual-host=/

# Connection Pool
spring.rabbitmq.connection-factory.cache-mode=CHANNEL
spring.rabbitmq.connection-factory.channel-cache-size=10

# Message Serialization
spring.rabbitmq.template.default-receive-queue=notification.restaurant
spring.rabbitmq.listener.simple.default-requeue-rejected=true
```

---

## Monitoring & Observability

### Key Metrics
- **Queue Depth**: Number of messages waiting per queue
- **Consumer Count**: Number of active listeners per queue
- **Message Rate**: Messages/second in and out
- **ACK Rate**: % of messages successfully acknowledged

### Alerts
- **Queue Depth > 10,000**: Processing can't keep up
- **Consumer Count = 0**: No listeners connected
- **Message Rate drop**: Producers stopped publishing

### RabbitMQ Admin UI
- Accessible at: `http://localhost:15672` (default)
- Shows: Queues, connections, message throughput
- Useful for: Manual inspection, testing, dead letter queue review

---

## Failure Scenarios

### Scenario 1: Listener Crashes
```
Listener processing message
  └─ Crashes before ACK
  
RabbitMQ detects closed connection
  └─ All unack'd messages requeued
  └─ Message goes to next listener (or same after restart)
```

### Scenario 2: RabbitMQ Broker Restart
```
Messages in queue
  └─ Persisted to disk (durable queues)
  
Broker restarts
  └─ Loads messages from disk
  └─ Delivery resumes
```

### Scenario 3: Network Partition
```
Producer-Broker connection lost
  └─ Producer queues messages locally
  └─ Sends when connection restored
  
Listener-Broker connection lost
  └─ Listener can't receive
  └─ Messages stay in queue (building up)
  └─ Delivery resumes when connection restored
```

---

## Integration Points

### Upstream (Message Producers)
- EventOutboxOrchestrator (only producer)

### Downstream (Message Consumers)
- RestaurantNotificationListener
- CustomerNotificationListener
- AgencyUserNotificationListener
- AdminNotificationListener

### Side Systems
- Database (for verification, not primary flow)
- Monitoring/Observability tools

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Component**: RabbitMQ Messaging
