# Notifications System Documentation

## 📚 Documentation Structure

This folder contains comprehensive documentation of the Greedy's notification system - a multi-layer, asynchronous messaging architecture that powers real-time and delivery-guaranteed notifications across all user types.

### Quick Navigation

**Start Here:**
1. [`main_overview.md`](./main_overview.md) - High-level system architecture and flow
2. [`questions_and_issues.md`](./questions_and_issues.md) - Open questions and potential improvements

**Then explore components in order:**

## 🔢 Component Documentation (Ordered)

### 01. Event Outbox Producer
**Purpose**: Publishes business events durably to RabbitMQ  
**Files**: 
- [`01_event_outbox_producer/flow_diagram.txt`](./01_event_outbox_producer/flow_diagram.txt) - Visual flow
- [`01_event_outbox_producer/functional_description.md`](./01_event_outbox_producer/functional_description.md) - What it does
- [`01_event_outbox_producer/implementation_notes.md`](./01_event_outbox_producer/implementation_notes.md) - How it works

**Key Concept**: The Outbox Pattern - guarantees events are eventually published even if system crashes.

---

### 02. RabbitMQ Messaging
**Purpose**: Routes messages to type-specific listeners via persistent queues  
**Files**: 
- [`02_rabbitmq_messaging/flow_diagram.txt`](./02_rabbitmq_messaging/flow_diagram.txt) - Routing topology
- [`02_rabbitmq_messaging/functional_description.md`](./02_rabbitmq_messaging/functional_description.md) - Queue management
- [`02_rabbitmq_messaging/implementation_notes.md`](./02_rabbitmq_messaging/implementation_notes.md) - Spring AMQP config

**Key Concept**: Topic Exchange with 4 separate queues (restaurant, customer, agency, admin)

---

### 03. Notification Disaggregation
**Purpose**: Converts 1 generic message into N recipient-specific notification records  
**Files**: 
- [`03_notification_disaggregation/flow_diagram.txt`](./03_notification_disaggregation/flow_diagram.txt) - Disaggregation logic
- [`03_notification_disaggregation/functional_description.md`](./03_notification_disaggregation/functional_description.md) - Channel calculation
- [`03_notification_disaggregation/implementation_notes.md`](./03_notification_disaggregation/implementation_notes.md) - Orchestrator patterns

**Key Concept**: Strategy Pattern with 4 type-specific orchestrators (Restaurant, Customer, Agency, Admin)

---

### 04. Notification Persistence
**Purpose**: Stores disaggregated notifications with delivery tracking  
**Files**: 
- [`04_notification_persistence/flow_diagram.txt`](./04_notification_persistence/flow_diagram.txt) - Data model
- [`04_notification_persistence/functional_description.md`](./04_notification_persistence/functional_description.md) - Lifecycle & idempotency
- [`04_notification_persistence/implementation_notes.md`](./04_notification_persistence/implementation_notes.md) - DAO & queries

**Key Concept**: 4 notification tables with UNIQUE(eventId) for idempotency

---

### 05. WebSocket Real-Time
**Purpose**: Delivers notifications instantly to connected users  
**Files**: 
- [`05_websocket_realtime/flow_diagram.txt`](./05_websocket_realtime/flow_diagram.txt) - Connection flow
- [`05_websocket_realtime/functional_description.md`](./05_websocket_realtime/functional_description.md) - Real-time delivery
- [`05_websocket_realtime/implementation_notes.md`](./05_websocket_realtime/implementation_notes.md) - STOMP & Spring WebSocket

**Key Concept**: Best-effort delivery (no retry if offline); synchronous send

---

### 06. Channel Delivery
**Purpose**: Sends notifications via Email, Push, SMS based on channel type  
**Files**: 
- [`06_channel_delivery/flow_diagram.txt`](./06_channel_delivery/flow_diagram.txt) - Polling & dispatch
- [`06_channel_delivery/functional_description.md`](./06_channel_delivery/functional_description.md) - Channel implementations
- [`06_channel_delivery/implementation_notes.md`](./06_channel_delivery/implementation_notes.md) - Strategy pattern & retry logic

**Key Concept**: INotificationChannel interface with pluggable implementations

---

### 07. Security Validation
**Purpose**: Enforces JWT authentication and role-based WebSocket access control  
**Files**: 
- [`07_security_validation/flow_diagram.txt`](./07_security_validation/flow_diagram.txt) - Auth layers
- [`07_security_validation/functional_description.md`](./07_security_validation/functional_description.md) - Access control rules
- [`07_security_validation/implementation_notes.md`](./07_security_validation/implementation_notes.md) - Interceptors & validators

**Key Concept**: Multi-layer validation (handshake, channel, destination)

---

### 08. Shared Read Strategy
**Purpose**: Propagates read status across related group notifications  
**Files**: 
- [`08_shared_read_strategy/flow_diagram.txt`](./08_shared_read_strategy/flow_diagram.txt) - Scope propagation
- [`08_shared_read_strategy/functional_description.md`](./08_shared_read_strategy/functional_description.md) - Scope levels & behavior
- [`08_shared_read_strategy/implementation_notes.md`](./08_shared_read_strategy/implementation_notes.md) - Strategy implementations

**Key Concept**: Strategy Pattern with scope-based update queries (NONE, RESTAURANT, RESTAURANT_HUB, etc.)

---

### 09. Read Status Management
**Purpose**: Handles user marking notifications as read with broadcast updates  
**Files**: 
- [`09_read_status_management/flow_diagram.txt`](./09_read_status_management/flow_diagram.txt) - Read lifecycle
- [`09_read_status_management/functional_description.md`](./09_read_status_management/functional_description.md) - Read/unread workflow
- [`09_read_status_management/implementation_notes.md`](./09_read_status_management/implementation_notes.md) - WebSocket handlers & DB updates

**Key Concept**: Real-time broadcast of read status changes to connected group members

---

## 📊 High-Level Architecture

```
┌─ LAYER 1: EVENT PRODUCTION ─────────────────────────────┐
│ 01. Event Outbox Producer                               │
│     └─ Polls EventOutbox, publishes 1 msg/event         │
└─────────────────────────────┬──────────────────────────┘
                              │
┌─ LAYER 2: MESSAGE ROUTING ──▼──────────────────────────┐
│ 02. RabbitMQ Messaging                                  │
│     └─ Routes to 4 type-specific queues                 │
└─────────────────────────────┬──────────────────────────┘
                              │
┌─ LAYER 3: DISAGGREGATION ───▼──────────────────────────┐
│ 03. Notification Disaggregation                         │
│     └─ 1 message → N recipient × channel records        │
└─────────────────────────────┬──────────────────────────┘
                              │
┌─ LAYER 4: PERSISTENCE ──────▼──────────────────────────┐
│ 04. Notification Persistence                            │
│     └─ Stores with idempotency & delivery tracking      │
└─────────────────────────────┬──────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
    ┌───▼────┐          ┌──────▼─────┐         ┌──▼─────┐
    │ LAYER 5 │          │ LAYER 6     │         │ LAYER 7 │
    │WebSocket│          │Channel      │         │Security │
    │Real-time│          │Delivery     │         │Validation
    │05.      │          │06.          │         │07.      │
    └────┬────┘          └──────┬─────┘         └────┬────┘
         │                      │                    │
         └──────────────────────┼────────────────────┘
                                │
         ┌──────────────────────┴─────────────────────┐
         │                                            │
    ┌────▼────┐                               ┌──────▼──┐
    │ LAYER 8  │                               │ LAYER 9  │
    │Shared    │                               │Read      │
    │Read      │                               │Status    │
    │Strategy  │                               │Management
    │08.       │                               │09.       │
    └──────────┘                               └──────────┘
```

---

## 🎯 Key Characteristics

| Aspect | Value |
|--------|-------|
| **Message Volume** | 95% reduction (1 event → 1 RabbitMQ msg) |
| **Idempotency** | Two-level (Event + Notification) |
| **Delivery Guarantee** | At-least-once (with possible duplicates) |
| **WebSocket Latency** | 100-500ms (best-effort, no retry) |
| **Email Latency** | 30-60 seconds (batched, async) |
| **Scalability** | Horizontal (multi-instance listeners) |
| **Failure Mode** | Graceful degradation per channel |
| **Database** | 4 notification tables + EventOutbox + ProcessedEvent |

---

## 🔄 Typical Message Flow Example

### Scenario: Restaurant Receives Reservation

```
1. Customer creates reservation
   └─ Service: INSERT Reservation + INSERT EventOutbox (ATOMIC)
   
2. EventOutboxOrchestrator polls (every 1 sec)
   └─ Publishes: notification.restaurant queue
   
3. RestaurantNotificationListener receives
   └─ Disaggregates: 10 staff × 2 channels = 20 notifications
   
4. BaseNotificationListener persists + ACK
   └─ 20 RestaurantUserNotification rows created
   
5. WebSocketSender sends (synchronous)
   └─ If staff online: real-time notification
   └─ If offline: persisted in DB
   
6. ChannelPoller sends via Email (30 sec cycle)
   └─ Email sent to all staff
   
7. Staff opens app, marks as read
   └─ WebSocket handler updates + broadcasts
   └─ If readByAll=true: all 20 records marked READ
```

**Total Latency**: 1-2 seconds to WebSocket, 30-60 sec to Email

---

## 🛠️ For Developers

### Understanding the System

1. **Start with** [`main_overview.md`](./main_overview.md) for the big picture
2. **Read components in order** 01-09 for functional understanding
3. **Check** [`questions_and_issues.md`](./questions_and_issues.md) for edge cases
4. **Deep dive** into implementation_notes.md for code details

### Making Changes

- **Adding new event type?** → Update layer 2 (RabbitMQ routing) and layer 3 (disaggregation logic)
- **New recipient type (e.g., Partner)?** → Create layer 3 orchestrator + layer 4 table
- **New channel?** → Implement INotificationChannel interface + add to layer 6 poller
- **Changing read behavior?** → Modify layer 8 (shared read strategy) or layer 9 (read status)

### Testing

- **Unit tests**: Mock orchestrators, strategies, repositories
- **Integration tests**: Use embedded RabbitMQ, real DB
- **Load tests**: 10,000+ events, measure throughput & latency
- **Failover tests**: RabbitMQ down, database down, listener crashes

---

## 📋 Documentation Standards

Each component folder contains exactly 3 files:

1. **flow_diagram.txt** - ASCII diagrams showing component behavior and integration
2. **functional_description.md** - WHAT the component does (business logic)
3. **implementation_notes.md** - HOW the component works (technical details)

**File Format**:
- Diagrams: Plain text ASCII (no special fonts/symbols)
- Markdown: Standard GitHub-flavored markdown
- Code: Inline Java/SQL examples where relevant
- Headings: Clear hierarchy for navigation

---

## 🚀 Getting Started

### For New Team Members

1. Read [`main_overview.md`](./main_overview.md) (20 min)
2. Study component 01-04 in depth (1 hour)
3. Skim 05-09 for awareness (30 min)
4. Review [`questions_and_issues.md`](./questions_and_issues.md) (15 min)
5. Ask questions on unclear topics

### For Code Review

- Use flow diagrams to understand data flow
- Check implementation notes for deployment considerations
- Refer to functional description for business requirements
- Validate error handling matches patterns

### For Troubleshooting

1. Check which layer the issue is in (producer, broker, persistence, delivery, etc.)
2. Read that component's documentation
3. Review questions_and_issues.md for known gotchas
4. Consult implementation_notes.md for runtime details

---

## 📞 Questions?

This documentation is maintained alongside the codebase. If you find:
- **Unclear sections**: Open an issue or add to [`questions_and_issues.md`](./questions_and_issues.md)
- **Outdated info**: Update the relevant component markdown
- **Missing details**: Expand the applicable flow_diagram.txt or implementation_notes.md

---

**Documentation Version**: 1.0  
**Last Generated**: November 23, 2025  
**Status**: Current Production Implementation  
**Maintenance**: Living documentation - update as system evolves
