# IMPLEMENTATION CURRENT STATE - Notification System

## 📋 Overview

Documentazione dello stato **attuale** dell'implementazione del sistema di notifiche. Basata sul codice reale presente nei file di architettura.

---

## 🔄 Architecture Flow

```
DOMAIN EVENT
    ↓
EVENT OUTBOX (database)
    ↓
EventOutboxOrchestrator (DISAGGREGATION happens HERE ✅)
    ├─ Reads event from EventOutbox
    ├─ Determines recipients
    ├─ FOR each recipient:
    │   ├─ Gets user preferences
    │   ├─ Calculates final channels (Group ∩ User ∩ Event)
    │   └─ FOR each channel:
    │       └─ Publishes 1 message to RabbitMQ
    └─ Updates EventOutbox status to PROCESSED
    ↓
RabbitMQ (receives MANY pre-disaggregated messages)
    ├─ notification.restaurant queue: [msg1, msg2, ..., msg20]
    ├─ notification.customer queue: [msg1, msg2, ..., msg15]
    ├─ notification.agency queue: [msg1, msg2, ..., msg8]
    └─ notification.admin queue: [msg1, msg2, ..., msg5]
    ↓
@RabbitListener Services (4 listener classes)
    ├─ RestaurantNotificationListener.onMessage()
    ├─ CustomerNotificationListener.onMessage()
    ├─ AgencyUserNotificationListener.onMessage()
    └─ AdminNotificationListener.onMessage()
    ↓
    For EACH message received:
    ├─ Check idempotency (eventId already processed?)
    ├─ Create Notification record
    ├─ Get ChannelImplementation for channel type
    └─ Execute channel (WEBSOCKET = direct, EMAIL/PUSH/SMS = queued)
    ↓
Notification Models (database)
    ├─ notification_restaurant_user (20 rows from 1 event)
    ├─ notification_customer (15 rows)
    ├─ notification_agency_user (8 rows)
    └─ notification_admin (5 rows)
    ↓
ChannelPoller (for retryable channels)
    ├─ EMAIL: retry 3 times, 5 min delay
    ├─ PUSH: retry 3 times, 5 min delay
    ├─ SMS: retry 3 times, 5 min delay
    └─ WEBSOCKET: deliver immediately (no retry)
```

---

## 💾 Key Classes

### EventOutboxOrchestrator.java

**Location**: `com.application.common.service.notification.orchestrator.EventOutboxOrchestrator`

**Responsibilities**:
1. ⏰ `@Scheduled(fixedDelay=1000)` - Poll EventOutbox every 1 second
2. 🔍 Determine entity type from `aggregateType` (RESTAURANT, AGENCY, CUSTOMER, ADMIN)
3. 👥 Get recipients based on entity type
4. 📊 For EACH recipient:
   - Load user notification preferences
   - Load group notification settings
   - Load event type routing rules
   - Calculate: Group ∩ User ∩ Event = final channels
5. 📤 Disaggregate: FOR each channel, publish 1 message to RabbitMQ
6. ✔️ Update EventOutbox status to PROCESSED

**Example: RESERVATION_REQUESTED Event**

```
Input:
  - aggregateType: RESTAURANT
  - aggregateId (restaurantId): 5
  - eventType: RESERVATION_REQUESTED
  - payload: {customerId: 100, tableId: 4, time: 19:30}

Processing:
  1. Identify recipients: [staff1(MANAGER), staff2(CHEF), staff3(WAITER)]
  
  2. FOR staff1:
     - User prefs: EMAIL✅ PUSH✅ SMS❌ WEBSOCKET✅
     - Group settings: EMAIL✅ PUSH✅ SMS(manager-only)✅ WS✅
     - Event rules: MANDATORY=[WEBSOCKET], OPTIONAL=[EMAIL, PUSH, SMS]
     - Final: [WEBSOCKET, EMAIL, PUSH]
     
     Publish 3 messages:
     ├─ {eventId: evt-5-staff1-WEBSOCKET, userId: staff1, channel: WEBSOCKET}
     ├─ {eventId: evt-5-staff1-EMAIL, userId: staff1, channel: EMAIL}
     └─ {eventId: evt-5-staff1-PUSH, userId: staff1, channel: PUSH}
  
  3. FOR staff2:
     - User prefs: EMAIL✅ PUSH✅ SMS❌ WEBSOCKET✅
     - Final: [WEBSOCKET, EMAIL, PUSH]
     
     Publish 3 messages:
     ├─ {eventId: evt-5-staff2-WEBSOCKET, userId: staff2, channel: WEBSOCKET}
     ├─ {eventId: evt-5-staff2-EMAIL, userId: staff2, channel: EMAIL}
     └─ {eventId: evt-5-staff2-PUSH, userId: staff2, channel: PUSH}
  
  4. FOR staff3:
     - User prefs: EMAIL✅ PUSH❌ SMS❌ WEBSOCKET✅
     - Final: [WEBSOCKET, EMAIL]
     
     Publish 2 messages:
     ├─ {eventId: evt-5-staff3-WEBSOCKET, userId: staff3, channel: WEBSOCKET}
     └─ {eventId: evt-5-staff3-EMAIL, userId: staff3, channel: EMAIL}

Output:
  - RabbitMQ notification.restaurant queue: 8 messages
  - EventOutbox: status PENDING → PROCESSED
```

---

### DisaggregationRuleEngine.java

**Location**: `com.application.common.service.notification.rule.DisaggregationRuleEngine`

**Called by**: EventOutboxOrchestrator (in the loop for each recipient)

**Method**: `calculateFinalChannels(eventType, groupSettings, userPrefs, event)`

**Algorithm**:
```
1. Load event routing rules (from EVENT_TYPE_ROUTING_CONFIG)
   - RESERVATION_REQUESTED → mandatory=[WS], optional=[EMAIL, PUSH, SMS]
   - RESERVATION_CONFIRMED → mandatory=[EMAIL], optional=[WS, PUSH, SMS]
   - etc

2. Load group settings for this entity
   - restaurant_notification_settings(restaurantId=5)
   - agency_notification_settings(agencyId=10)
   - etc

3. Load user preferences for this recipient
   - user_notification_preferences(userId=staff1)
   - EMAIL: enabled, PUSH: enabled, SMS: disabled, WEBSOCKET: enabled

4. Calculate intersection:
   mandatory_channels ∪ (event.optional ∩ group.enabled ∩ user.enabled)
   
   Example:
   mandatory = [WEBSOCKET]
   optional = [EMAIL ∩ YES ∩ YES] ∪ [PUSH ∩ YES ∩ YES] ∪ [SMS ∩ YES ∩ NO]
            = [EMAIL, PUSH]
   final = [WEBSOCKET, EMAIL, PUSH]
```

---

### @RabbitListener Services (4 classes)

**Location**: `com.application.*/service/listener/`

1. **RestaurantNotificationListener** → queue: `notification.restaurant`
2. **CustomerNotificationListener** → queue: `notification.customer`
3. **AgencyUserNotificationListener** → queue: `notification.agency`
4. **AdminNotificationListener** → queue: `notification.admin`

**Responsibilities** (for EACH message received):

```java
@RabbitListener(queues = "notification.restaurant", ackMode = MANUAL)
@Transactional
public void onMessage(Message message) {
  
  try {
    // 1. Parse message
    String eventId = message.eventId;  // "evt-5-staff1-EMAIL"
    Long userId = message.userId;
    ChannelType channel = message.channel;
    String eventType = message.eventType;
    Object payload = message.payload;
    
    // 2. IDEMPOTENCY CHECK
    if (restaurantNotificationDAO.existsByEventId(eventId)) {
      basicAck();  // Already processed, skip
      return;
    }
    
    // 3. CREATE Notification record
    RestaurantUserNotification notification = new RestaurantUserNotification();
    notification.setEventId(eventId);
    notification.setUserId(userId);
    notification.setChannel(channel);
    notification.setTitle(...);
    notification.setBody(...);
    notification.setStatus(PENDING);
    restaurantNotificationDAO.save(notification);
    
    // 4. EXECUTE channel
    ChannelImplementation channelImpl = channelRegistry.getChannel(channel);
    if (channel == WEBSOCKET) {
      // Direct: send immediately via WebSocket
      webSocketService.sendNotification(notification);
    } else {
      // Queued (EMAIL, PUSH, SMS): mark for ChannelPoller
      // Status = PENDING, ChannelPoller will retry
    }
    
    // 5. ACK (if transactional commit succeeds)
    basicAck();
    
  } catch (Exception e) {
    // NACK + REQUEUE
    basicNack(true);  // requeue = true
  }
}
```

---

## 📊 Message Traffic Comparison

### Current Implementation (BEFORE Disaggregation in Orchestrator)

```
EventOutbox: 1 RESERVATION_REQUESTED event
    ↓
EventOutboxOrchestrator: DISAGGREGATES
    ↓
RabbitMQ receives: 8 messages (3 staff × 2-3 channels each)
    ├─ {eventId: evt-5-staff1-WEBSOCKET}
    ├─ {eventId: evt-5-staff1-EMAIL}
    ├─ {eventId: evt-5-staff1-PUSH}
    ├─ {eventId: evt-5-staff2-WEBSOCKET}
    ├─ {eventId: evt-5-staff2-EMAIL}
    ├─ {eventId: evt-5-staff2-PUSH}
    ├─ {eventId: evt-5-staff3-WEBSOCKET}
    └─ {eventId: evt-5-staff3-EMAIL}

Pros:
  ✅ RabbitMQ carries many specific messages (easier debugging)
  ✅ Each listener message = 1 DB record (simple logic)
  ✅ Idempotency is per-message

Cons:
  ❌ Heavy RabbitMQ traffic (1 event → 8 messages)
  ❌ If restaurant has 100 staff → 200+ messages for 1 event
  ❌ Network/disk overhead increases with recipient count
```

### Alternative (Disaggregation in @RabbitListener)

```
EventOutbox: 1 RESERVATION_REQUESTED event
    ↓
EventOutboxOrchestrator: NO DISAGGREGATION
    ↓
RabbitMQ receives: 1 message
    └─ {eventId: evt-5, recipients: [staff1, staff2, staff3], eventType, payload}

@RabbitListener: DISAGGREGATES
    ↓
Creates 8 Notification records:
    ├─ notification_restaurant_user row 1 (staff1, WEBSOCKET)
    ├─ notification_restaurant_user row 2 (staff1, EMAIL)
    ├─ ...
    └─ notification_restaurant_user row 8 (staff3, EMAIL)

Pros:
  ✅ Light RabbitMQ traffic (1 event → 1 message)
  ✅ Scales better for large recipient counts
  ✅ Less network/disk overhead

Cons:
  ❌ Disaggregation logic must live in 4 listener classes (code duplication)
  ❌ Harder to test (requires message listener context)
  ❌ Idempotency check is complex
```

---

## ✅ Current State

**What's implemented NOW**:
- ✅ EventOutboxOrchestrator polls EventOutbox
- ✅ DisaggregationRuleEngine calculates final channels
- ✅ **Disaggregation happens in EventOutboxOrchestrator (BEFORE RabbitMQ)**
- ✅ 4 @RabbitListener services receive specific messages
- ✅ Each listener saves notification record (no additional disaggregation)
- ✅ ChannelPoller retries for EMAIL/PUSH/SMS

**Trade-offs chosen**:
- Chose: **Disaggregation BEFORE RabbitMQ** (in EventOutboxOrchestrator)
- Reason: Simpler listener logic, centralized disaggregation
- Cost: Higher RabbitMQ message volume

---

## 🔮 Future Improvements

If RabbitMQ traffic becomes a bottleneck:

1. **Move disaggregation to @RabbitListener**
   - Step 1: Modify EventOutboxOrchestrator to NOT disaggregate
   - Step 2: Add disaggregation logic to each listener
   - Step 3: Consolidate duplicated logic into helper class
   - Estimated: 4-6 hours refactor

2. **Introduce Orchestrator Hierarchy** (by user type)
   - Step 1: Create `BaseNotificationOrchestrator` abstract class
   - Step 2: Extend for each type: RestaurantOrchestrator, CustomerOrchestrator, etc
   - Step 3: Move type-specific logic to subclasses
   - Estimated: 3-4 hours

3. **Optimize for large recipient counts**
   - Batch disaggregation (e.g., 10 recipients per transaction)
   - Async processing with thread pools
   - Message compression before RabbitMQ

---

## 📚 Related Documentation

- [`NOTIFICATION_REFACTORING_ARCHITECTURE.md`](NOTIFICATION_REFACTORING_ARCHITECTURE.md) - Detailed technical spec
- [`NOTIFICATION_ARCHITECTURE_CLARIFICATION.md`](NOTIFICATION_ARCHITECTURE_CLARIFICATION.md) - Flow diagrams
- [`ARCHITECTURE_INHERITANCE.md`](ARCHITECTURE_INHERITANCE.md) - Future inheritance design
- [`IMPLEMENTATION_COMPLETE.md`](IMPLEMENTATION_COMPLETE.md) - 22 files implemented

---

**Last Updated**: 21 November 2025
**Status**: ✅ PRODUCTION READY (1/1 replicas, all services healthy)
