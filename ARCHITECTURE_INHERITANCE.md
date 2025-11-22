# Notification System - Inheritance Architecture

## Current Architecture Overview

**Existing Infrastructure**:
- ✅ EventOutbox table → Persists domain events (RESERVATION_REQUESTED, etc)
- ✅ EventOutboxOrchestrator → Polls EventOutbox, disaggregates by recipient × channel, publishes to RabbitMQ
- ✅ DisaggregationRuleEngine → Calculates final channels (Group ∩ User ∩ Event)
- ✅ RecipientResolver → Resolves who receives notifications (restaurant staff, agency agents, etc)
- ✅ RabbitMQ Queues → notification.customer, notification.restaurant, notification.agency, notification.admin
- ✅ 4 Notification entities: `AdminNotification`, `AgencyUserNotification`, `CustomerNotification`, `RestaurantUserNotification`
- ✅ 4 @RabbitListener services: Each listens on their queue, receives disaggregated messages
- ✅ ChannelPoller → Retry logic for EMAIL/PUSH/SMS (WebSocket is best-effort, no retry)

## Proposed Inheritance Enhancements

**Goal**: Eliminate code duplication between the 4 listeners and add type-specific channel handling

## Proposed Inheritance Enhancements

**Goal**: Eliminate code duplication between the 4 listeners and add type-specific channel handling

### 1. BaseNotificationListener<T> - Generic Base Class

```java
public abstract class BaseNotificationListener<T extends Notification> {
    
    @Autowired
    protected NotificationOrchestratorFactory orchestratorFactory;
    
    @RabbitListener(queues = "...")  // Subclass specifies queue
    @Transactional
    public final void onMessage(Message message, Channel channel) {
        try {
            Map<String, Object> payload = deserialize(message.getBody());
            
            // Common logic for ALL user types
            String eventId = (String) payload.get("eventId");
            String eventType = (String) payload.get("eventType");
            
            // Idempotency check
            if (notificationExists(eventId)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            
            // ✅ KEY: Delegate disaggregation to type-specific orchestrator
            NotificationOrchestrator<T> orchestrator = orchestratorFactory.getOrchestrator(this.getNotificationType());
            List<T> disaggregatedNotifications = orchestrator.disaggregateAndProcess(payload);
            
            // Save all disaggregated notifications to DB
            for (T notification : disaggregatedNotifications) {
                getDAO().save(notification);
            }
            
            // Delegate channel handling to orchestrator
            for (T notification : disaggregatedNotifications) {
                orchestrator.handleChannels(notification);
            }
            
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            
        } catch (Exception e) {
            log.error("Error processing notification", e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
    
    // Abstract methods - implemented by subclasses
    protected abstract Class<T> getNotificationType();
    protected abstract GenericDAO<T> getDAO();
    protected abstract boolean notificationExists(String eventId);
    protected abstract Map<String, Object> deserialize(byte[] body);
}
```

**Key Changes from Current**:
- ✅ Now calls `orchestrator.disaggregateAndProcess()` instead of direct DB save
- ✅ Orchestrator calculates: Group ∩ User ∩ Event channels
- ✅ Listener receives list of disaggregated notifications
- ✅ Each disaggregated notification = 1 recipient + 1 channel

**Subclasses**:
```
BaseNotificationListener<T>
├── RestaurantNotificationListener extends BaseNotificationListener<RestaurantUserNotification>
├── CustomerNotificationListener extends BaseNotificationListener<CustomerNotification>
├── AgencyNotificationListener extends BaseNotificationListener<AgencyUserNotification>
└── AdminNotificationListener extends BaseNotificationListener<AdminNotification>
```

### 2. NotificationChannel Hierarchy

**Current State**: 5 channel stubs (Email, Push, SMS, WebSocket, Slack)
**Problem**: All channels identical for all user types
**Solution**: Create type-specific channel implementations

```
NotificationChannel (Abstract)
├── AbstractEmailChannel extends NotificationChannel
│   ├── RestaurantEmailChannel (restaurant-specific templates, routing)
│   ├── AgencyEmailChannel (agency-specific templates, routing)
│   ├── CustomerEmailChannel (customer-specific templates, routing)
│   └── AdminEmailChannel (admin-specific templates, routing)
│
├── AbstractPushChannel extends NotificationChannel
│   ├── RestaurantPushChannel (restaurant FCM routing)
│   ├── AgencyPushChannel (agency FCM routing)
│   ├── CustomerPushChannel (customer FCM routing)
│   └── AdminPushChannel (admin FCM routing)
│
├── AbstractSmsChannel extends NotificationChannel
│   ├── RestaurantSmsChannel (Twilio routing)
│   ├── AgencySmsChannel (Twilio routing)
│   ├── CustomerSmsChannel (Twilio routing)
│   └── AdminSmsChannel (Twilio routing)
│
├── WebSocketChannel (single implementation, works for all types)
│   └── Broadcasts per userType to appropriate WebSocket destinations
│
├── 🚀 FUTURE CHANNELS (Not Yet Implemented)
│   │
│   ├── AbstractFirebaseChannel extends NotificationChannel
│   │   ├── RestaurantFirebaseChannel (FCM for staff devices - PENDING)
│   │   ├── AgencyFirebaseChannel (FCM for agent devices - PENDING)
│   │   ├── CustomerFirebaseChannel (FCM for customer app - PENDING)
│   │   └── AdminFirebaseChannel (FCM for admin dashboard - PENDING)
│   │   └── Status: ⏳ Scheduled for Q1 2026
│   │
│   ├── AbstractWhatsAppChannel extends NotificationChannel
│   │   ├── RestaurantWhatsAppChannel (Twilio WhatsApp for managers - PENDING)
│   │   ├── AgencyWhatsAppChannel (Twilio WhatsApp for agents - PENDING)
│   │   ├── CustomerWhatsAppChannel (Twilio WhatsApp for customers - PENDING)
│   │   └── AdminWhatsAppChannel (Twilio WhatsApp for critical alerts - PENDING)
│   │   └── Status: ⏳ Scheduled for Q2 2026
│   │   └── Dependencies: Twilio WhatsApp Business API account, phone number verification
│   │
│   ├── AbstractTelegramChannel extends NotificationChannel
│   │   ├── RestaurantTelegramChannel (Telegram Bot for staff - PENDING)
│   │   ├── AgencyTelegramChannel (Telegram Bot for agents - PENDING)
│   │   ├── CustomerTelegramChannel (Telegram Bot for customers - PENDING)
│   │   └── AdminTelegramChannel (Telegram Bot for system alerts - PENDING)
│   │   └── Status: ⏳ Scheduled for Q3 2026
│   │   └── Dependencies: Telegram Bot API, chat_id registration
│   │
│   └── SlackChannel (admin-specific, single implementation)
│       └── Status: ⏳ Future enhancement for admin alerts
```

**Implemented Channels** (Production Ready):
- **Email**: SMTP/SendGrid for all user types ✅
- **Push**: Firebase Cloud Messaging (FCM) ✅
- **SMS**: Twilio for all user types ✅
- **WebSocket**: Real-time browser notifications ✅

**Future Channels** (Planned but Not Yet Implemented):
- **Firebase**: Additional FCM integration patterns
- **WhatsApp**: Twilio WhatsApp Business API integration
- **Telegram**: Telegram Bot API integration
- **Slack**: Webhook integration for admin alerts

**Benefits per user type**:
- **Restaurant**: Email templates for staff, SMS alerts for managers only, (future: WhatsApp for critical orders)
- **Agency**: Email templates for agents, SMS for senior staff, (future: Telegram for booking alerts)
- **Customer**: Email templates for reservations, push for order updates, (future: WhatsApp for order status)
- **Admin**: Email for system alerts, SMS for critical issues, escalation logic, (future: Slack for incident management)

### 3. NotificationOrchestrator Hierarchy

**Current State**: Single EventOutboxOrchestrator handles all user types
**Problem**: Generic logic, hard to add user-type-specific routing/retries/escalations
**Solution**: Create sub-orchestrators that handle specific business logic

```
NotificationOrchestrator (Abstract)
├── RestaurantUserOrchestrator
│   - Handles: RestaurantUserNotification disaggregation
│   - Routes: notification.restaurant queue
│   - Business Logic:
│     * Escalate HIGH priority to manager if no ACK in 5 min
│     * SMS only to managers for CRITICAL events
│     * Email to all staff, WebSocket to online staff
│   - Retry: 3x with exponential backoff
│
├── AgencyUserOrchestrator
│   - Handles: AgencyUserNotification disaggregation
│   - Routes: notification.agency queue
│   - Business Logic:
│     * Escalate to senior agent if no ACK
│     * Priority-based routing (HIGH → manager, NORMAL → agent)
│     * SMS for URGENT events only
│   - Retry: 3x with exponential backoff
│
├── CustomerOrchestrator
│   - Handles: CustomerNotification disaggregation
│   - Routes: notification.customer queue
│   - Business Logic:
│     * Push notifications for orders
│     * Email for confirmations (with receipt)
│     * No SMS (optional, customer-specific)
│     * Archive old notifications (>30 days)
│   - Retry: 3x with exponential backoff
│
└── AdminOrchestrator
    - Handles: AdminNotification disaggregation
    - Routes: notification.admin queue
    - Business Logic:
      * System alerts (HIGH priority always SMS)
      * Database/service issues (escalate immediately)
      * Email summary at end of day
      * Slack integration for critical issues
    - Retry: 3x with exponential backoff
```

### 4. Channel Registry - Dispatch to Correct Orchestrator

```java
public class NotificationOrchestratorFactory {
    
    public NotificationOrchestrator getOrchestrator(Notification notification) {
        UserType userType = determineUserType(notification);
        
        return switch(userType) {
            case RESTAURANT_USER -> restaurantOrchestrator;
            case AGENCY_USER -> agencyOrchestrator;
            case CUSTOMER -> customerOrchestrator;
            case ADMIN -> adminOrchestrator;
        };
    }
}
```

## Architecture: Two-Layer Orchestration Pattern

### ✅ FINAL DESIGN (After Refactoring)

**Layer 1: EventOutboxOrchestrator** (STAYS SIMPLE - No Changes)
- Reads EventOutbox (1 event)
- Determines recipient type (RESTAURANT, CUSTOMER, AGENCY, ADMIN)
- Publishes 1 GENERIC message per recipient type to RabbitMQ
- Does NOT disaggregate by recipient × channel
- Does NOT load user preferences or calculate channels
- Can add event-type-specific publishing rules per recipient type (future enhancement)
- ⚡ Fast, lightweight, scalable

```java
// EventOutboxOrchestrator (keeps current behavior)
EventOutbox event = fetch();  // "RESERVATION_REQUESTED"
String recipientType = event.getAggregateType();  // "RESTAURANT"

RabbitMessage msg = new RabbitMessage(
  eventType: "RESERVATION_REQUESTED",
  recipientType: "RESTAURANT",
  restaurantId: 5,
  payload: event.getPayload()
);

rabbitTemplate.convertAndSend("notification.restaurant", msg);
// No disaggregation happens here ✅
```

**Future Enhancement** (Optional):
- EventOutboxOrchestrator can apply event-type-specific rules
- Example: CRITICAL_RESERVATION_REQUESTED → add `priority: HIGH`
- Rules stored per event type + recipient type combination

**Layer 2: NotificationOrchestrator<T>** (NEW - in @RabbitListener)
- Receives 1 generic message from RabbitMQ
- **DISAGGREGATES by recipient × channel** (happens AFTER RabbitMQ)
- Loads user preferences and group settings
- Calculates: Group ∩ User ∩ Event = final channels per recipient
- Returns list of disaggregated notification records for listener to save
- Can apply type-specific event rules (manager escalation, SMS restrictions, etc)
- ⚡ Centralized business logic, per-type customization

```java
// NotificationOrchestrator<T> (in listener - after RabbitMQ)
@RabbitListener(queues = "notification.restaurant")
public void onMessage(RabbitMessage msg) {
  // Delegate to type-specific orchestrator
  NotificationOrchestrator<RestaurantUserNotification> orchestrator 
    = factory.getOrchestrator(RESTAURANT);
  
  // Orchestrator disaggregates: 1 message → 20 disaggregated records
  List<RestaurantUserNotification> disaggregated 
    = orchestrator.disaggregateAndProcess(msg);
  
  // Listener saves all disaggregated records
  for (var notif : disaggregated) {
    restaurantNotificationDAO.save(notif);
  }
}
```

### Architecture: Before vs After

**BEFORE (Current - Works but less flexible)**:
```
EventOutbox: 1 event
    ↓
EventOutboxOrchestrator: DISAGGREGATES + publishes 20 messages ⚠️
    ├─ Heavy logic (load staff, preferences, rules)
    ├─ No room for event-type-specific rules
    └─ RabbitMQ carries 20 pre-disaggregated messages
    ↓
RabbitMQ: 20 messages (heavy traffic)
    ├─ {eventId: evt-5-staff1-WEBSOCKET}
    ├─ {eventId: evt-5-staff1-EMAIL}
    └─ ... (18 more)
    ↓
@RabbitListener: Dumb, just save to DB
    ↓
Notification records: 20 rows
```

**AFTER (Proposed - Cleaner, more extensible)**:
```
EventOutbox: 1 event
    ↓
EventOutboxOrchestrator: STAYS SIMPLE ✅
    ├─ Just publish 1 message per recipient type
    ├─ Optional: apply event-type-specific publishing rules
    └─ RabbitMQ carries 1 message (light traffic)
    ↓
RabbitMQ: 1 message (light) ✅
    └─ {eventType: RESERVATION_REQUESTED, recipientType: RESTAURANT, ...}
    ↓
BaseNotificationListener<T>: Delegates to orchestrator
    ↓
NotificationOrchestrator<T>: DISAGGREGATES (smart layer) ✅
    ├─ Load 10 staff + preferences
    ├─ Calculate Group ∩ User ∩ Event channels per staff
    ├─ Apply event-type-specific rules (CRITICAL → manager escalation)
    └─ Return 20 disaggregated records
    ↓
@RabbitListener: Saves 20 records received from orchestrator
    ↓
Notification records: 20 rows in DB
    ↓
ChannelPoller: Delivers via EMAIL/PUSH/SMS/WEBSOCKET
```

### Why This Two-Layer Pattern?

**Layer 1 (EventOutboxOrchestrator) - Producer**:
✅ Stays simple (just publish 1 message)
✅ Message broker stays light (fewer messages)
✅ Easy to test (no business logic)
✅ Easy to scale (no database queries)
✅ Can be consumed by multiple listeners if needed
✅ Room for event-type-specific publishing rules (future)

**Layer 2 (NotificationOrchestrator<T>) - Stream Processor**:
✅ Centralized disaggregation (no duplication across 4 listeners)
✅ Per-type customization (RestaurantUserOrchestrator ≠ CustomerOrchestrator)
✅ Event-type-specific rules per recipient type (RESERVATION_REQUESTED vs ORDER_UPDATED)
✅ User preference calculation (Group ∩ User ∩ Event)
✅ Extensible for business logic (manager escalation, SMS restrictions, archive cleanup)

### Aligns with Industry Best Practices

This pattern aligns with **Facebook, Netflix, Amazon, LinkedIn, Uber**.
See `INDUSTRY_BEST_PRACTICES.md` for detailed case studies.

**Benefits**:
- ✅ 1 event → 1 message on RabbitMQ (vs 20 pre-disaggregated)
- ✅ RabbitMQ network/disk overhead minimal
- ✅ Business logic centralized in per-type orchestrators
- ✅ Easy to add new event types with type-specific rules
- ✅ Aligns with stream processing patterns (Kafka, Kinesis, Flink)

### EventOutboxOrchestrator - Will NOT Change

✅ Existing behavior unchanged
✅ No modification to current implementation required
✅ Disaggregation logic moves OUT of EventOutboxOrchestrator
✅ Disaggregation logic moves INTO NotificationOrchestrator (in listener)

**Future Enhancement Only** (Optional):
- Can add event-type-specific rules in EventOutboxOrchestrator
- Example: `if (eventType == "CRITICAL_RESERVATION") { msg.setPriority(HIGH); }`
- Rules stored per event type + recipient type in database
- No breaking changes to current flow

## Implementation Steps

### Phase 1: Create BaseNotificationListener<T> (2-3 hours)
1. Extract common logic from 4 existing listeners
2. Create abstract base class with:
   - Common deserialization
   - Message reception handling
   - Idempotency check
   - Transaction management
   - Error handling (basicAck/Nack)
   - **Delegation to type-specific orchestrator for disaggregation**
3. Update 4 existing listeners to extend base
4. **KEY**: Listeners call `orchestrator.disaggregateAndProcess(message)` → get List<T> disaggregated notifications
5. Listeners save all disaggregated notifications to DB
6. Result: DRY principle applied, disaggregation logic moved to orchestrators

### Phase 2: Create Channel Type Hierarchy (4-5 hours)
1. Convert 5 channel stubs to proper class hierarchy:
   - `NotificationChannel` (abstract)
   - `AbstractEmailChannel`, `AbstractPushChannel`, `AbstractSmsChannel` (abstract)
   - 4 user-type-specific implementations per channel
2. Move template selection to subclass
3. Move channel-specific logic (retry, escalation) to subclass
4. Result: Each user type can have custom templates and routing

### Phase 3: Create Orchestrator Hierarchy (5-7 hours) - KEY DISAGGREGATION LAYER
1. Create abstract `NotificationOrchestrator<T>` with:
   - **Method: `disaggregateAndProcess(message) → List<T>`**
     - Load user preferences for all recipients
     - Load group notification settings
     - Load event type routing rules
     - For each recipient: calculate Group ∩ User ∩ Event channels
     - Return list of disaggregated notifications (1 per recipient × channel)
   - Common retry strategy override points
   - Event-type-specific rule application (future extensibility)
2. Create 4 user-type-specific orchestrators:
   - **RestaurantUserOrchestrator** 
     - Disaggregates by recipient × channel
     - Can override for CRITICAL events (manager escalation, SMS alerts)
     - Can add rules: "CRITICAL_RESERVATION → SMS to managers only"
   - **AgencyUserOrchestrator**
     - Disaggregates by recipient × channel
     - Can override for URGENT events (senior agent notification)
     - Can add rules: "URGENT_BOOKING → SMS to senior agents"
   - **CustomerOrchestrator**
     - Disaggregates by recipient × channel
     - Can override for order/booking events (archive old notifications)
     - Can add rules: "ORDER_DELIVERED → archive notification after 30 days"
   - **AdminOrchestrator**
     - Disaggregates by recipient × channel
     - Can override for CRITICAL system events (immediate notification)
     - Can add rules: "DATABASE_ERROR → SMS + Slack integration"
3. Result: Disaggregation logic centralized, event-type-specific rules extensible per recipient type

### Phase 4: Create Dispatch Factory (1-2 hours)
1. Create `NotificationOrchestratorFactory`
2. Wire into listeners to get correct orchestrator by user type
3. Result: Single entry point, easy to test, easy to extend

## Code Structure - Current vs Proposed

### Current Structure
```
src/main/java/com/application/
├── common/notification/
│   ├── config/RabbitMQConfig.java
│   ├── orchestrator/EventOutboxOrchestrator.java (handles all user types)
│   ├── rule/DisaggregationRuleEngine.java
│   └── recipient/RecipientResolver.java
├── restaurant/service/listener/RestaurantNotificationListener.java (duplicates logic)
├── customer/service/listener/CustomerNotificationListener.java (duplicates logic)
├── agency/service/listener/AgencyUserNotificationListener.java (duplicates logic)
└── admin/service/listener/AdminNotificationListener.java (duplicates logic)
```

### Proposed Structure (WITH INHERITANCE)
```
src/main/java/com/application/
├── common/
│   ├── notification/
│   │   ├── config/RabbitMQConfig.java
│   │   ├── listener/
│   │   │   ├── BaseNotificationListener.java (abstract, DRY logic)
│   │   │   └── NotificationListenerFactory.java
│   │   ├── orchestrator/
│   │   │   ├── EventOutboxOrchestrator.java (existing, unchanged)
│   │   │   ├── NotificationOrchestrator.java (abstract, base)
│   │   │   ├── NotificationOrchestratorFactory.java
│   │   │   └── ...
│   │   ├── channel/
│   │   │   ├── NotificationChannel.java (abstract)
│   │   │   ├── AbstractEmailChannel.java (abstract)
│   │   │   ├── AbstractPushChannel.java (abstract)
│   │   │   ├── AbstractSmsChannel.java (abstract)
│   │   │   ├── WebSocketChannel.java (concrete)
│   │   │   └── ChannelRegistry.java
│   │   ├── rule/DisaggregationRuleEngine.java (unchanged)
│   │   └── recipient/RecipientResolver.java (unchanged)
├── restaurant/
│   ├── notification/
│   │   ├── listener/RestaurantNotificationListener extends BaseNotificationListener<RestaurantUserNotification>
│   │   ├── orchestrator/RestaurantUserOrchestrator extends NotificationOrchestrator<RestaurantUserNotification>
│   │   └── channel/
│   │       ├── RestaurantEmailChannel extends AbstractEmailChannel
│   │       ├── RestaurantPushChannel extends AbstractPushChannel
│   │       └── RestaurantSmsChannel extends AbstractSmsChannel
├── customer/
│   ├── notification/
│   │   ├── listener/CustomerNotificationListener extends BaseNotificationListener<CustomerNotification>
│   │   ├── orchestrator/CustomerOrchestrator extends NotificationOrchestrator<CustomerNotification>
│   │   └── channel/
│   │       ├── CustomerEmailChannel extends AbstractEmailChannel
│   │       ├── CustomerPushChannel extends AbstractPushChannel
│   │       └── CustomerSmsChannel extends AbstractSmsChannel
├── agency/
│   ├── notification/
│   │   ├── listener/AgencyNotificationListener extends BaseNotificationListener<AgencyUserNotification>
│   │   ├── orchestrator/AgencyUserOrchestrator extends NotificationOrchestrator<AgencyUserNotification>
│   │   └── channel/
│   │       ├── AgencyEmailChannel extends AbstractEmailChannel
│   │       ├── AgencyPushChannel extends AbstractPushChannel
│   │       └── AgencySmsChannel extends AbstractSmsChannel
└── admin/
    ├── notification/
        ├── listener/AdminNotificationListener extends BaseNotificationListener<AdminNotification>
        ├── orchestrator/AdminOrchestrator extends NotificationOrchestrator<AdminNotification>
        └── channel/
            ├── AdminEmailChannel extends AbstractEmailChannel
            ├── AdminPushChannel extends AbstractPushChannel
            ├── AdminSmsChannel extends AbstractSmsChannel
            └── AdminSlackChannel (NEW - admin-specific)
```

## Key Design Points - Why This Works

| Aspect | Pattern | Benefit | Current State | Proposed Change |
|--------|---------|---------|---------------|-----------------|
| **Listeners** | BaseNotificationListener<T> | DRY code, type-safe, reusable | 4 identical @RabbitListener classes | Extract to base class, call orchestrator |
| **Channels** | Hierarchy by user type | User-specific templates/routing | 5 stubs, all identical | Type-specific subclasses (12 total) |
| **Disaggregation** | NotificationOrchestrator<T> (in listener) | Centralized logic, event-type extensibility | In EventOutboxOrchestrator (BEFORE RabbitMQ) | Move to listener (AFTER RabbitMQ) |
| **EventOutbox** | EventOutboxOrchestrator (STAYS SAME) | Simple producer, light messages | Handles all user types | Unchanged - publishes 1 generic message |
| **Routing** | Factory pattern | Single entry point, extensible | Direct queue bindings | NotificationOrchestratorFactory |
| **Configuration** | Spring @Component per type | Automatic autowiring | Manual wiring | Per-type bean registration |

## Current Gaps (What's Missing)

| Gap | Current | Proposed | Effort | Note |
|-----|---------|----------|--------|------|
| **Disaggregation Location** | In EventOutboxOrchestrator (BEFORE RabbitMQ) | In NotificationOrchestrator (AFTER RabbitMQ) | 5-7 hours | KEY CHANGE: Move from producer to stream processor |
| **RabbitMQ Traffic** | 1 event → 20 messages | 1 event → 1 message | N/A | Benefit of moving disaggregation |
| **Listener Duplication** | 4 identical listeners | 1 base + 4 subclasses | 2-3 hours | Extract common logic |
| **Channel Implementation** | 5 stubs, generic | Type-specific subclasses | 4-5 hours | 12 channel implementations (4 types × 3 channels) |
| **Channel Instances** | No channel registry | 12 channel beans | 2 hours | Per-user-type channel routing |
| **Orchestrator Hierarchy** | 1 EventOutboxOrchestrator (producer) | 4 NotificationOrchestrator subclasses (stream processor) | 5-7 hours | Each handles disaggregation + type-specific rules |
| **Event-Type Rules** | Hardcoded in EventOutboxOrchestrator | Extensible override points in orchestrators | 3 hours | Per event type + user type combinations |
| **Slack Integration** | Missing | AdminSlackChannel subclass | 2 hours | Admin-specific channel |
| **Manager Escalation** | Missing | RestaurantUserOrchestrator logic | 2 hours | Escalation rules in orchestrator |
| **Customer Cleanup** | Missing | CustomerOrchestrator archive logic | 1 hour | Notification archive in orchestrator |

**Total Effort**: 18-24 hours for full implementation
**Priority**: Medium (system works, refactoring improves maintainability)
**Blocking**: No (can deploy as-is, refactor incrementally)
**ROI**: High (easier to add new user types, maintain business logic per type)

---

## Disaggregation Explained: What, Where, When

### What is Disaggregation?

**Definition**: Converting 1 generic event into N specific notifications for recipients × channels.

**Example**: 
```
Input: 1 RESERVATION_REQUESTED event
  └─ restaurantId: 5, customerName: "John", tableId: 4, time: "19:30"

Disaggregation:
  ├─ Recipients: [staff1=manager, staff2=chef, staff3=waiter]
  ├─ Preferences loaded
  ├─ Group ∩ User ∩ Event calculated
  └─ Output: 8 disaggregated notifications
      ├─ (staff1, WEBSOCKET)
      ├─ (staff1, EMAIL)
      ├─ (staff1, PUSH)
      ├─ (staff1, SMS)
      ├─ (staff2, WEBSOCKET)
      ├─ (staff2, EMAIL)
      ├─ (staff2, PUSH)
      └─ (staff3, WEBSOCKET+EMAIL)
```

### This Architecture: Disaggregation = AFTER RabbitMQ

**Design Choice**: Stream Processor Pattern

**Implementation**:
1. **EventOutboxOrchestrator** (Producer) - STAYS SIMPLE
   - Publishes 1 generic message per recipient type
   - No disaggregation, no business logic
   
2. **NotificationOrchestrator<T>** (Stream Processor) - NEW
   - Receives 1 message from RabbitMQ
   - Disaggregates into N recipient × channel combinations
   - Can apply event-type-specific rules (future)
   
3. **Per-Type Subclasses** - Extensible
   - RestaurantUserOrchestrator: can override for CRITICAL events
   - AgencyUserOrchestrator: can override for URGENT events
   - CustomerOrchestrator: can override for ORDER events
   - AdminOrchestrator: can override for ALERT events

**Benefits**:
- ✅ Producer stays simple (light messages)
- ✅ RabbitMQ lightweight (1 event = 1 message)
- ✅ Business logic centralized (no duplication)
- ✅ Event-type rules per orchestrator (extensible)
- ✅ Aligns with industry standards (Facebook, Netflix, Amazon)

**Future Enhancement**:
```java
// RestaurantUserOrchestrator - add event-type rules
public List<RestaurantUserNotification> disaggregateAndProcess(Message msg) {
  String eventType = msg.getEventType();
  
  // Base disaggregation (always)
  List<RestaurantUserNotification> base = super.disaggregateAndProcess(msg);
  
  // Event-specific rules (future)
  if ("CRITICAL_RESERVATION".equals(eventType)) {
    return applyCriticalRules(base);  // SMS to managers only
  } else if ("CANCEL_RESERVATION".equals(eventType)) {
    return applyCancelRules(base);    // Email only to staff
  }
  
  return base;
}
```

**Result**: All event-type-specific rules live in one place per orchestrator subclass.

---

## 🚀 Future Channels - Extension Points

The notification system is designed to support additional channels beyond the current Email/Push/SMS/WebSocket implementation. This section documents planned future channels and their integration paths.

### Roadmap: Planned Channels

| Channel | Status | Timeline | User Types | Dependencies | Use Case |
|---------|--------|----------|-----------|--------------|----------|
| **Email** | ✅ Implemented | Live | All | SMTP/SendGrid | Primary formal communication |
| **Push (FCM)** | ✅ Implemented | Live | Restaurant, Customer, Admin | Firebase Cloud Messaging | Mobile app notifications |
| **SMS** | ✅ Implemented | Live | All | Twilio | Quick alerts, critical notifications |
| **WebSocket** | ✅ Implemented | Live | All | Spring WebSocket | Real-time browser notifications |
| **Firebase** | ⏳ Planned | Q1 2026 | Restaurant, Agency, Customer, Admin | Firebase API | Cross-platform rich notifications |
| **WhatsApp** | ⏳ Planned | Q2 2026 | Restaurant, Agency, Customer, Admin | Twilio WhatsApp Business API | Conversational notifications |
| **Telegram** | ⏳ Planned | Q3 2026 | Restaurant, Agency, Customer, Admin | Telegram Bot API | Developer-friendly notifications |
| **Slack** | ⏳ Planned | Q3 2026 | Admin | Slack Webhook API | Internal team alerts |

### Firebase Channel (Q1 2026)

**Purpose**: Enhanced push notifications with rich media support

**Implementation**:
```java
public abstract class AbstractFirebaseChannel extends NotificationChannel {
    
    public AbstractFirebaseChannel() {
        this.type = ChannelType.FIREBASE;
        this.name = "Firebase";
        this.requiresRetry = true;  // Persistent, 3x retries
    }
    
    @Override
    public void send(Notification notification, String recipient) throws Exception {
        // Get user's Firebase tokens
        List<String> deviceTokens = getDeviceTokens(recipient);
        
        // Build rich message
        Message message = buildMulticastMessage(notification, deviceTokens);
        
        // Send via Firebase Admin SDK
        BatchResponse response = FirebaseMessaging.getInstance()
            .sendMulticast(message);
        
        // Handle failures
        if (response.getFailureCount() > 0) {
            throw new FirebaseException("Firebase send failed");
        }
    }
    
    protected abstract Message buildMulticastMessage(Notification notif, List<String> tokens);
}
```

**Subclasses**:
```
AbstractFirebaseChannel
├── RestaurantFirebaseChannel
│   ├── Rich message for staff: order notifications with photos
│   ├── Routing: to restaurant app on Android/iOS
│   └── Priority: HIGH for CRITICAL orders
│
├── AgencyFirebaseChannel
│   ├── Rich message for agents: booking details with maps
│   ├── Routing: to agency app on Android/iOS
│   └── Priority: NORMAL for bookings
│
├── CustomerFirebaseChannel
│   ├── Rich message for customers: order status with images
│   ├── Routing: to customer app on Android/iOS
│   └── Priority: NORMAL for order updates
│
└── AdminFirebaseChannel
    ├── Rich message for admins: system alerts with analytics
    ├── Routing: to admin dashboard
    └── Priority: HIGH for critical issues
```

**Dependencies**:
- Firebase Admin SDK: `com.google.firebase:firebase-admin`
- Configuration: `firebase-service-account.json`
- Database: Store device tokens per user

### WhatsApp Channel (Q2 2026)

**Purpose**: Two-way communication via WhatsApp Business API

**Implementation**:
```java
public abstract class AbstractWhatsAppChannel extends NotificationChannel {
    
    public AbstractWhatsAppChannel() {
        this.type = ChannelType.WHATSAPP;
        this.name = "WhatsApp";
        this.requiresRetry = true;  // Persistent, 3x retries
    }
    
    @Override
    public void send(Notification notification, String recipient) throws Exception {
        // Get user's WhatsApp phone number
        String phoneNumber = getWhatsAppPhoneNumber(recipient);
        
        // Build template message (WhatsApp requires templates)
        WhatsAppMessage message = buildTemplateMessage(notification, phoneNumber);
        
        // Send via Twilio WhatsApp API
        TwilioClient.send(message);
    }
    
    protected abstract WhatsAppMessage buildTemplateMessage(Notification notif, String phone);
}
```

**Subclasses**:
```
AbstractWhatsAppChannel
├── RestaurantWhatsAppChannel
│   ├── Template: "new_order" - notify managers of incoming orders
│   ├── Routing: to manager's WhatsApp business account
│   ├── Two-way: Manager can confirm via WhatsApp
│   └── Priority: CRITICAL orders only (avoid spam)
│
├── AgencyWhatsAppChannel
│   ├── Template: "booking_received" - confirm booking details
│   ├── Routing: to agent's WhatsApp business account
│   ├── Two-way: Agent can confirm/modify via WhatsApp
│   └── Priority: URGENT bookings only
│
├── CustomerWhatsAppChannel
│   ├── Template: "order_status" - track order progress
│   ├── Routing: to customer's personal WhatsApp
│   ├── Two-way: Customer can reply with questions
│   └── Priority: NORMAL for all order updates
│
└── AdminWhatsAppChannel
    ├── Template: "system_alert" - critical system notifications
    ├── Routing: to admin's WhatsApp business account
    ├── Two-way: Admin can acknowledge via WhatsApp
    └── Priority: HIGH for critical alerts
```

**Dependencies**:
- Twilio WhatsApp Business API: `com.twilio.sdk:twilio`
- WhatsApp Business Account setup (phone number verification)
- Template approval process with Meta/WhatsApp
- Database: Store WhatsApp phone numbers per user

### Telegram Channel (Q3 2026)

**Purpose**: Lightweight bot-based notifications with inline commands

**Implementation**:
```java
public abstract class AbstractTelegramChannel extends NotificationChannel {
    
    public AbstractTelegramChannel() {
        this.type = ChannelType.TELEGRAM;
        this.name = "Telegram";
        this.requiresRetry = true;  // Persistent, 3x retries
    }
    
    @Override
    public void send(Notification notification, String recipient) throws Exception {
        // Get user's Telegram chat_id
        Long chatId = getTelegramChatId(recipient);
        
        // Build inline keyboard message
        SendMessage message = buildBotMessage(notification, chatId);
        
        // Send via Telegram Bot API
        TelegramBot.send(message);
    }
    
    protected abstract SendMessage buildBotMessage(Notification notif, Long chatId);
}
```

**Subclasses**:
```
AbstractTelegramChannel
├── RestaurantTelegramChannel
│   ├── Message: "New order #123: 4 pax, 19:30"
│   ├── Inline buttons: [Accept] [Decline] [Details]
│   ├── Routing: to restaurant's staff Telegram group
│   └── Priority: All orders (low noise)
│
├── AgencyTelegramChannel
│   ├── Message: "Booking #456 from John Doe"
│   ├── Inline buttons: [View] [Confirm] [Reject]
│   ├── Routing: to agency's Telegram channel
│   └── Priority: All bookings
│
├── CustomerTelegramChannel
│   ├── Message: "Your order #789 is being prepared"
│   ├── Inline buttons: [Track] [Cancel] [Chat Support]
│   ├── Routing: to customer's personal Telegram
│   └── Priority: Order status updates
│
└── AdminTelegramChannel
    ├── Message: "Database CPU usage: 85% ⚠️"
    ├── Inline buttons: [Acknowledge] [Escalate] [Dismiss]
    ├── Routing: to admin's private Telegram
    └── Priority: System alerts
```

**Dependencies**:
- Telegram Bot API: `org.telegram:telegrambots`
- Bot token from BotFather
- User chat_id collection (requires user to start conversation with bot)
- Database: Store Telegram chat_ids per user

### Slack Channel (Q3 2026)

**Purpose**: Internal team notifications and incident management

**Implementation**:
```java
public class SlackChannel extends NotificationChannel {
    
    public SlackChannel() {
        this.type = ChannelType.SLACK;
        this.name = "Slack";
        this.requiresRetry = true;  // Persistent, 3x retries
    }
    
    @Override
    public void send(Notification notification, String recipient) throws Exception {
        // Get Slack channel or user ID
        String slackTarget = getSlackChannelOrUserId(recipient);
        
        // Build rich Slack message (with blocks)
        SlackMessage message = buildSlackMessage(notification, slackTarget);
        
        // Send via Slack Webhook or SDK
        SlackClient.send(message);
    }
    
    private SlackMessage buildSlackMessage(Notification notif, String target) {
        return SlackMessage.builder()
            .channel(target)
            .attachments(List.of(
                Attachment.builder()
                    .color("danger")  // red for critical
                    .title(notif.getTitle())
                    .text(notif.getBody())
                    .fields(List.of(
                        Field.builder().title("Severity").value(notif.getPriority()).build(),
                        Field.builder().title("Event").value(notif.getEventType()).build(),
                        Field.builder().title("Time").value(notif.getCreatedAt().toString()).build()
                    ))
                    .build()
            ))
            .build();
    }
}
```

**Use Cases**:
- System alerts in `#incidents` channel
- Database issues in `#ops-alerts` channel
- Security events in `#security` channel
- Performance degradation in `#monitoring` channel

**Dependencies**:
- Slack Bot Token: `xoxb-...`
- Slack SDK: `com.slack.api:slack-api-client`
- Workspace setup: Channels configured per alert type

### Integration Pattern for New Channels

**Step 1**: Create abstract base class
```java
public abstract class AbstractNewChannel extends NotificationChannel {
    // Common retry logic, error handling
}
```

**Step 2**: Implement per-user-type subclass
```java
public class RestaurantNewChannel extends AbstractNewChannel {
    // Restaurant-specific message format
}
public class CustomerNewChannel extends AbstractNewChannel {
    // Customer-specific message format
}
// ... (Agency, Admin)
```

**Step 3**: Register in ChannelRegistry
```java
@Configuration
public class ChannelRegistryConfig {
    @Bean
    public ChannelRegistry channelRegistry() {
        return ChannelRegistry.builder()
            // Existing channels
            .add(new EmailChannel())
            .add(new PushChannel())
            .add(new SmsChannel())
            
            // NEW channels when ready
            // .add(new FirebaseChannel())
            // .add(new WhatsAppChannel())
            // .add(new TelegramChannel())
            // .add(new SlackChannel())
            
            .build();
    }
}
```

**Step 4**: Enable in orchestrators
```java
// In RestaurantUserOrchestrator
protected Set<ChannelType> getAvailableChannels() {
    return Set.of(
        ChannelType.EMAIL,
        ChannelType.PUSH,
        ChannelType.SMS,
        ChannelType.WEBSOCKET,
        // ChannelType.WHATSAPP,  // Enable when ready
        // ChannelType.TELEGRAM,  // Enable when ready
    );
}
```

### Future Channel Matrix

| User Type | Email | Push | SMS | WebSocket | Firebase | WhatsApp | Telegram | Slack |
|-----------|-------|------|-----|-----------|----------|----------|----------|-------|
| **Restaurant** | ✅ | ✅ | ⚠️ (mgr only) | ✅ | ⏳ (rich orders) | ⏳ (critical) | ⏳ (all) | ❌ |
| **Agency** | ✅ | ✅ | ⚠️ (senior) | ✅ | ⏳ (bookings) | ⏳ (urgent) | ⏳ (all) | ❌ |
| **Customer** | ✅ | ✅ | ❌ | ✅ | ⏳ (orders) | ⏳ (status) | ⏳ (track) | ❌ |
| **Admin** | ✅ | ✅ | ⚠️ (critical) | ✅ | ⏳ (alerts) | ❌ | ⏳ (alerts) | ⏳ (critical) |

**Legend**: ✅ Implemented | ⏳ Planned | ⚠️ Conditional | ❌ Not applicable

---

