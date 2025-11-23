# Notification System - Main Overview

## Executive Summary

The notification system is a **multi-layer, asynchronous messaging architecture** that powers real-time and delivery-guarantee notifications across all user types (Restaurants, Customers, Agencies, Admins). It combines event-driven architecture with the Outbox Pattern to ensure message delivery durability and idempotency.

## Core Philosophy

**Separation of Concerns Across Layers:**
- **Layer 1 (Producer)**: EventOutboxOrchestrator - publishes 1 generic message per event type
- **Layer 2 (Stream Processing)**: NotificationOrchestrator (4 variants) - disaggregates into recipient-specific records
- **Layer 3 (Delivery)**: ChannelPoller - sends via appropriate channels (WebSocket, Email, Push, SMS)

This design ensures **95% reduction in RabbitMQ message volume** while maintaining clean separation between event production and notification delivery.

## High-Level System Flow

```
┌──────────────────────────────────┐
│ BUSINESS EVENT OCCURS            │
│ (ReservationRequested,           │
│  OrderCreated, etc.)             │
└──────────────────┬───────────────┘
                   │
                   ▼
        ┌──────────────────────────┐
        │ LAYER 1: EVENT OUTBOX    │
        │ Atomically save event    │
        │ in DB transaction        │
        └──────────────┬───────────┘
                       │
                       ▼
        ┌──────────────────────────────────┐
        │ EventOutboxOrchestrator (Poller) │
        │ - Poll every 1 second            │
        │ - Publish 1 msg/event to RabbitMQ│
        │ - Mark event PROCESSED           │
        └──────────────┬────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────┐
        │ RABBITMQ QUEUES (4 routes)       │
        │ - notification.restaurant        │
        │ - notification.customer          │
        │ - notification.agency            │
        │ - notification.admin             │
        └──────────────┬────────────────────┘
                       │
         ┌─────────────┼─────────────┬──────────────┐
         │             │             │              │
         ▼             ▼             ▼              ▼
    ┌────────┐  ┌────────┐  ┌──────────┐  ┌───────┐
    │RestL   │  │CustL   │  │AgencyL   │  │AdminL │
    └────┬───┘  └────┬───┘  └────┬─────┘  └───┬───┘
         │           │           │            │
         ▼           ▼           ▼            ▼
    ┌──────────────────────────────────────────────────┐
    │ LAYER 2: ORCHESTRATORS (DISAGGREGATION)          │
    │ - Load recipients                                │
    │ - Load preferences                               │
    │ - Calculate channels                             │
    │ - Create 1 record per (recipient × channel)      │
    └──────────────────┬───────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────┐
        │ NOTIFICATION PERSISTENCE TABLES  │
        │ - RestaurantUserNotification     │
        │ - CustomerNotification           │
        │ - AgencyUserNotification         │
        │ - AdminNotification              │
        │ Indexed by: eventId (UNIQUE),    │
        │             userId, channel      │
        └──────────────┬────────────────────┘
                       │
         ┌─────────────┴──────────────────┬──────────┐
         │                                │          │
         ▼                                ▼          ▼
    ┌──────────────┐            ┌──────────────┐  ┌──────────┐
    │WebSocketSend │            │ChannelPoller │  │ReadStatus│
    │(Synchronous) │            │ (Scheduled)  │  │Management│
    └──────────────┘            └──────┬───────┘  └──────────┘
         │                             │
         │ REAL-TIME               ┌───┴───────────────┬────┬────┐
         │ (if online)             │                   │    │    │
         │                         ▼                   ▼    ▼    ▼
         └──────────────────►   ┌─────────────────────────────────┐
                               │ DELIVERY CHANNELS               │
                               │ - WebSocket (real-time)         │
                               │ - Email (async)                 │
                               │ - Push (mobile)                 │
                               │ - SMS (urgent only)             │
                               └─────────────────────────────────┘
```

## Nine Core Components

### 1. **Event Outbox Producer** (`event_outbox_producer/`)
- **Purpose**: Publishes events as generic messages to RabbitMQ
- **Responsibility**: Polls EventOutbox table, publishes 1 message per event type
- **Key Class**: EventOutboxOrchestrator
- **Idempotency**: ProcessedEvent table with UNIQUE(eventId)

### 2. **RabbitMQ Messaging** (`rabbitmq_messaging/`)
- **Purpose**: Routes messages to type-specific listeners
- **Queues**: notification.restaurant, notification.customer, notification.agency, notification.admin
- **Pattern**: Topic exchange with routing keys
- **Flow Control**: Manual ACK/NACK with retries

### 3. **Notification Disaggregation** (`notification_disaggregation/`)
- **Purpose**: Converts 1 generic message into N recipient-specific records
- **Responsibility**: Load recipients, preferences, calculate channels
- **Key Classes**: NotificationOrchestrator (base) + 4 variants (Restaurant, Customer, Agency, Admin)
- **Complexity**: Per-recipient channel calculation

### 4. **Notification Persistence** (`notification_persistence/`)
- **Purpose**: Stores disaggregated notifications in database
- **Tables**: 4 notification tables (one per user type)
- **Key Field**: eventId (UNIQUE constraint for idempotency)
- **Lifecycle**: Created PENDING → DELIVERED/FAILED → READ/ARCHIVED

### 5. **Channel Delivery** (`channel_delivery/`)
- **Purpose**: Sends notifications via appropriate channel
- **Channels**: WebSocket, Email, Push, SMS
- **Strategy**: Channel Poller polls DB by channel type
- **Retry Logic**: Exponential backoff, max 3 attempts
- **Pattern**: Strategy pattern (INotificationChannel interface)

### 6. **WebSocket Real-Time** (`websocket_realtime/`)
- **Purpose**: Delivers notifications in real-time to connected clients
- **Delivery**: Synchronous, best-effort (no retry if offline)
- **Transport**: STOMP protocol over WebSocket
- **Topic Format**: `/topic/notifications/{userId}/{recipientType}`
- **Session**: Managed by Spring STOMP broker

### 7. **Security Validation** (`security_validation/`)
- **Purpose**: Enforces JWT authentication and role-based access control
- **Layers**: Handshake, Channel, Destination validation
- **JWT**: Same secret as REST authentication
- **Access Control**: User ID isolation + role-based topic access
- **Components**: Interceptors, validators, token handlers

### 8. **Shared Read Strategy** (`shared_read_strategy/`)
- **Purpose**: Propagates "read" status across related notifications
- **Scopes**: NONE, RESTAURANT, RESTAURANT_HUB, RESTAURANT_HUB_ALL, etc.
- **Use Case**: When one staff member reads a notification, others see it as read too
- **Pattern**: Strategy pattern with scope-specific implementations

### 9. **Read Status Management** (`read_status_management/`)
- **Purpose**: Manages notification read/unread lifecycle
- **Trigger**: User marks notification as read (via WebSocket)
- **Behavior**: Updates DB + broadcasts status to other connected users
- **Broadcast**: Real-time update via WebSocket to affected group members

## Key Design Patterns

### Outbox Pattern (Level 1)
```
Transaction:
  1. Create business entity (Reservation)
  2. Create EventOutbox record (same transaction)
  3. COMMIT atomically

Later (Async):
  EventOutboxOrchestrator polls & publishes
```

**Benefit**: Guarantees event publication even if server crashes after commit.

### Two-Layer Disaggregation
```
Layer 1 (Producer): Publishes 1 generic message
  Event: "RESERVATION_REQUESTED"
  Message: {event_type, aggregate_type, payload}

Layer 2 (Listener): Disaggregates into recipient-specific records
  Input: 1 message
  Processing: Load recipients, preferences, channels
  Output: N notification records (staff1×channel1, staff1×channel2, ...)
```

**Benefit**: 95% reduction in RabbitMQ message volume, clean separation of concerns.

### Idempotency (Level 2)
```
ProcessedEvent table:
  - UNIQUE(eventId)
  - Prevents duplicate messages reaching RabbitMQ

Notification table:
  - UNIQUE(eventId) per user/channel
  - Prevents duplicate DB inserts if listener reprocesses
```

### Strategy Pattern for Channels
```
INotificationChannel interface
  ├─ WebSocketNotificationChannel
  ├─ EmailNotificationChannel
  ├─ PushNotificationChannel
  └─ SMSNotificationChannel

ChannelPoller dynamically selects implementation per channel type.
```

### Factory Pattern for Orchestrators
```
NotificationOrchestratorFactory:
  - Extracts aggregate_type from message
  - Returns correct orchestrator instance
  - Eliminates string-based dispatch errors
```

## Data Flow Example: Reservation Requested

### Scenario
Chef books a table. System needs to notify:
- 10 restaurant staff (via WebSocket + Email)
- 1 customer (via Email only)

### Execution Timeline

**T0: Business Logic (500ms)**
```
CustomerReservationService.createReservation(customerId, restaurantId, ...)
  ├─ INSERT Reservation table
  ├─ INSERT EventOutbox (aggregate_type=RESTAURANT, event_type=RESERVATION_REQUESTED)
  ├─ INSERT EventOutbox (aggregate_type=CUSTOMER, event_type=RESERVATION_REQUESTED)
  └─ COMMIT ✅
```

**T1: Event Outbox Polling (1 sec)**
```
EventOutboxOrchestrator.orchestrate() @Scheduled(fixedDelay=1000)
  ├─ SELECT EventOutbox WHERE status='PENDING' LIMIT 100
  ├─ Found 2 events (RESTAURANT, CUSTOMER)
  │
  ├─ Event 1 (RESTAURANT):
  │  ├─ Try INSERT ProcessedEvent(eventId, status=PROCESSING)
  │  ├─ SUCCESS (first time processing)
  │  ├─ Publish to RabbitMQ queue: notification.restaurant
  │  └─ Message: {event_type: "RESERVATION_REQUESTED", restaurant_id: 5, ...}
  │
  ├─ Event 2 (CUSTOMER):
  │  ├─ Try INSERT ProcessedEvent(eventId, status=PROCESSING)
  │  ├─ SUCCESS
  │  ├─ Publish to RabbitMQ queue: notification.customer
  │  └─ Message: {event_type: "RESERVATION_REQUESTED", customer_id: 100, ...}
  │
  └─ Mark as PROCESSED in EventOutbox
```

**T2: RabbitMQ Routing (5-10ms)**
```
RabbitMQ Topic Exchange routes:
  ├─ notification.restaurant queue ← message 1
  └─ notification.customer queue ← message 2
```

**T3: Listeners Disaggregate (100ms)**
```
RestaurantNotificationListener receives message
  ├─ Parse: eventId, eventType, restaurantId
  ├─ Check idempotency: Is eventId already in RestaurantUserNotification? NO
  ├─ Get RestaurantUserOrchestrator
  ├─ Disaggregate:
  │  ├─ Load 10 restaurant staff
  │  ├─ For each staff:
  │  │  ├─ Load preferences: [WEBSOCKET, EMAIL]
  │  │  ├─ Load event rules: mandatory=[WEBSOCKET], optional=[EMAIL]
  │  │  ├─ Final channels: [WEBSOCKET, EMAIL]
  │  │  ├─ Create 2 RestaurantUserNotification records
  │  │  └─ Attempt WebSocket send (synchronous)
  │  └─ Result: 20 records (10 staff × 2 channels)
  ├─ Save 20 records to DB (batch insert)
  └─ ACK to RabbitMQ ✅

CustomerNotificationListener receives message
  ├─ Parse: eventId, eventType, customerId
  ├─ Check idempotency: NO
  ├─ Get CustomerOrchestrator
  ├─ Disaggregate:
  │  ├─ Load 1 customer (trivial)
  │  ├─ Load preferences: [EMAIL] (customers don't get WebSocket)
  │  ├─ Load event rules: mandatory=[EMAIL]
  │  ├─ Final channels: [EMAIL]
  │  └─ Create 1 CustomerNotification record
  ├─ Save 1 record to DB
  └─ ACK ✅
```

**T4: WebSocket Send (Parallel, async)**
```
NotificationWebSocketSender.sendRestaurantNotification()
  └─ For each of 20 staff notifications:
     ├─ Is staff online? Check session
     ├─ If YES:
     │  └─ Send via /topic/notifications/{staffId}/RESTAURANT ✅ (real-time)
     └─ If NO:
        └─ Fail silently (no error, no retry) - best-effort
```

**T5: Channel Poller (10+ seconds later)**
```
ChannelPoller.pollWebSocketChannel() @Scheduled(fixedDelay=5000)
  ├─ Query: SELECT * FROM restaurant_notification 
             WHERE channel='WEBSOCKET' AND status='PENDING' LIMIT 100
  ├─ Result: 20 websocket records
  ├─ For each:
  │  ├─ Get WebSocketNotificationChannel implementation
  │  ├─ Call send(title, body, staffId, properties)
  │  ├─ If SUCCESS: UPDATE status='DELIVERED'
  │  └─ If FAILURE: status remains PENDING (retry later)
  └─ Next cycle (5 sec later): retry any PENDING

ChannelPoller.pollEmailChannel() @Scheduled(fixedDelay=30000)
  ├─ Query: SELECT * FROM restaurant_notification 
             WHERE channel='EMAIL' AND status='PENDING' LIMIT 100
  ├─ Result: 20 email records
  ├─ For each:
  │  ├─ Get EmailNotificationChannel implementation
  │  ├─ Call send() → SMTP send
  │  ├─ If SUCCESS: UPDATE status='DELIVERED'
  │  └─ If FAILURE: status remains PENDING
  │
  ├─ Query: SELECT * FROM customer_notification 
             WHERE channel='EMAIL' AND status='PENDING' LIMIT 100
  ├─ Result: 1 email record
  ├─ For each:
  │  ├─ Send email to customer
  │  └─ UPDATE status='DELIVERED'
  └─ Done
```

**T6: User Reads Notification (User Action)**
```
UI: Staff member clicks notification
  ├─ Browser sends WebSocket message:
  │  {action: "read_notification", notification_id: 456}
  │
WebSocket Handler receives:
  ├─ Load notification: RestaurantUserNotification(id=456)
  ├─ Check: readByAll=true OR readByAll=false?
  │
  ├─ If readByAll=true (SHARED READ):
  │  ├─ UPDATE all records: eventId=evt-res-123, restaurant_id=5
  │  ├─ WHERE channel=notification.channel AND read_by_all=true
  │  ├─ Result: 20 records marked as READ (all staff see it as read)
  │  └─ Broadcast: /topic/notifications/*/RESTAURANT
  │     {eventId: ..., status: "READ", readBy: "Chef John"}
  │
  └─ If readByAll=false (INDIVIDUAL READ):
     ├─ UPDATE only id=456
     └─ No broadcast (customer notifications don't share)
```

## WebSocket Communication

### Connection Establishment
```
1. User logs in via REST endpoint → receives JWT token
2. Opens WebSocket: ws://api/ws?token={jwtToken}
3. Handshake interceptor validates JWT
4. Session authenticated + stored
5. Client sends STOMP CONNECT frame
6. Connected to message broker
```

### Subscription
```
Client subscribes to:
  /topic/notifications/{userId}/RESTAURANT
  /user/{userId}/queue/notifications

Server validates destination:
  - RESTAURANT users can only subscribe to /topic/notifications/*/RESTAURANT
  - CUSTOMER users can only subscribe to /topic/notifications/*/CUSTOMER
  - Cross-role subscriptions DENIED
```

### Message Delivery
```
Server sends:
  Destination: /topic/notifications/50/RESTAURANT
  Message: {
    notificationId: 123,
    eventId: "evt-res-456",
    title: "New Reservation",
    body: "Table for 4 at 20:00",
    timestamp: 1700000000,
    properties: {...}
  }

Only users 50 with RESTAURANT role receive.
```

## Notification Lifecycle States

```
CREATED
  └─ status=PENDING (new notification)
     ├─ WEBSOCKET channel: Attempted sync send
     │  ├─ SUCCESS → DELIVERED
     │  └─ FAILURE → stays PENDING (ChannelPoller retries)
     │
     ├─ EMAIL/PUSH/SMS channels:
     │  ├─ Polled every 30s/60s
     │  ├─ Attempted send
     │  ├─ SUCCESS → DELIVERED
     │  └─ FAILURE → stays PENDING (retry with exponential backoff)
     │
     └─ After 3 failed attempts → FAILED (abandoned)

DELIVERED
  └─ Channel send successful
     └─ User reads → status=READ

READ
  └─ User has opened/acknowledged notification
     └─ May trigger shared read propagation

ARCHIVED
  └─ Old notifications (>30 days) automatically deleted
```

## Multi-User Type Architecture

### Restaurant Staff
- Receive: WebSocket (real-time), Email, Push
- Recipients: Loaded from RestaurantStaff table
- Preferences: Configurable channels per staff
- Shared Read: Yes (multiple staff can share notifications)

### Customers
- Receive: Email, Push (optional)
- Recipients: Single customer
- Preferences: Basic on/off
- Shared Read: No (individual notifications only)

### Agency Users
- Receive: WebSocket (real-time), Email, SMS (urgent only)
- Recipients: Loaded from AgencyUser table
- Preferences: Role-based routing (managers vs agents)
- Shared Read: Yes (group-level notifications)

### Admins
- Receive: WebSocket (real-time), Slack (critical)
- Recipients: All system admins
- Preferences: Incident-based filtering
- Shared Read: No (individual admin notifications)

## Performance Characteristics

### Message Volume
- **Without optimization**: 1 event → 100 RabbitMQ messages (bad)
- **With this architecture**: 1 event → 1 RabbitMQ message (95% reduction)

### Delivery Latency
- **WebSocket**: 100-500ms (real-time, synchronous)
- **Email**: 30-60 seconds (batched, async)
- **Push**: 5-30 seconds (async)
- **SMS**: 10-60 seconds (rate-limited)

### Database Operations
- **Per notification created**: 1 INSERT (disaggregation output)
- **Per user read action**: 1-N UPDATEs (depending on shared read scope)
- **Polling queries**: Indexed by channel + status, efficient batching

## Integration Points

### Incoming (What triggers notifications)
- Business events (reservations, orders, user actions)
- Injected into EventOutbox within business transaction
- Atomically saved → guaranteed publication

### Outgoing (How notifications are delivered)
- WebSocket: Real-time to connected users
- Email: Via SMTP provider
- Push: Via Firebase Cloud Messaging (FCM)
- SMS: Via Twilio or similar

### Dependencies
- Spring Boot (Web, Messaging, AMQP, WebSocket)
- RabbitMQ (message broker)
- STOMP (WebSocket protocol)
- JWT (authentication)
- Database (MySQL/PostgreSQL)

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Status**: Current Production Implementation
