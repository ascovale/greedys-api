# Implementation Verification Report
## ARCHITECTURE_INHERITANCE.md Feasibility Analysis

**Date**: 21 Novembre 2025
**Status**: ✅ FEASIBLE - All required files exist or can be created
**Effort Estimate**: 18-24 hours

---

## 📋 SUMMARY

ARCHITECTURE_INHERITANCE.md proposes refactoring the notification system from:
- **CURRENT STATE**: Disaggregation in EventOutboxOrchestrator (BEFORE RabbitMQ)
- **PROPOSED STATE**: Disaggregation in NotificationOrchestrator (AFTER RabbitMQ)

### Key Insight
✅ **EventOutboxOrchestrator does NOT exist yet** (or not in src/main)
✅ **Listeners already disaggregate** (current implementation)
✅ **Goal**: Extract disaggregation logic into NotificationOrchestrator + create inheritance hierarchy

---

## 🔍 CURRENT STATE ANALYSIS

### 1. @RabbitListener Services ✅ EXIST

**Location**: 4 listener classes
- `/greedys_api/src/main/java/com/application/restaurant/service/listener/RestaurantNotificationListener.java`
- `/greedys_api/src/main/java/com/application/customer/service/listener/CustomerNotificationListener.java`
- `/greedys_api/src/main/java/com/application/agency/service/listener/AgencyUserNotificationListener.java`
- `/greedys_api/src/main/java/com/application/admin/service/listener/AdminNotificationListener.java`

**Current Implementation**:
```java
@RabbitListener(queues = "notification.restaurant", ackMode = "MANUAL")
@Transactional
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void onNotificationMessage(
    @Payload Map<String, Object> message,
    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
    Channel channel
) {
    // 1. Extract eventId, eventType, restaurantId from message
    // 2. Check idempotency: notificationDAO.existsByEventId(eventId)
    // 3. Load group settings: loadGroupSettings(eventType)
    // 4. Load recipients: loadRecipients(restaurantId, eventType, payload)
    // 5. Load channels: getEnabledChannelsStub(recipientStaffIds)
    // 6. DISAGGREGATE: FOR each recipient × channel:
    //    - Generate disaggregatedEventId
    //    - Create RestaurantUserNotification row
    //    - Save to DB
    // 7. ACK message
}
```

**Status**: ✅ **Already implements disaggregation AFTER receiving message from RabbitMQ**

**What's NOT Ideal**:
- ⚠️ Code duplicated across 4 listeners (same structure)
- ⚠️ Disaggregation logic embedded in listener (should be in orchestrator)
- ⚠️ No inheritance hierarchy (can't reuse common logic)
- ⚠️ No event-type-specific rules override points

### 2. NotificationChannel Hierarchy ❌ MISSING

**Current Location**: `/greedys_api/src/main/java/com/application/common/notification/channel/`

**Current Status**:
- `INotificationChannel.java` - Interface (abstract contract)
- `impl/EmailNotificationChannel.java` - Generic (not user-type-specific)
- `impl/PushNotificationChannel.java` - Generic
- `impl/SMSNotificationChannel.java` - Generic
- `impl/WebSocketNotificationChannel.java` - Generic

**Issues**:
- ⚠️ NO inheritance hierarchy (no Abstract base classes)
- ⚠️ NO user-type-specific subclasses:
  - Missing: RestaurantEmailChannel, CustomerEmailChannel, etc
  - Missing: RestaurantPushChannel, CustomerPushChannel, etc
  - Missing: RestaurantSmsChannel, CustomerSmsChannel, etc
- ⚠️ All channels generic (no per-type customization)

**What ARCHITECTURE_INHERITANCE Requires**:
```
INotificationChannel (existing interface ✅)
├── AbstractEmailChannel (needs to be created) ❌
│   ├── RestaurantEmailChannel (needs to be created) ❌
│   ├── CustomerEmailChannel (needs to be created) ❌
│   ├── AgencyEmailChannel (needs to be created) ❌
│   └── AdminEmailChannel (needs to be created) ❌
├── AbstractPushChannel (needs to be created) ❌
│   ├── RestaurantPushChannel ❌
│   ├── CustomerPushChannel ❌
│   ├── AgencyPushChannel ❌
│   └── AdminPushChannel ❌
├── AbstractSmsChannel (needs to be created) ❌
│   ├── RestaurantSmsChannel ❌
│   ├── CustomerSmsChannel ❌
│   ├── AgencySmsChannel ❌
│   └── AdminSmsChannel ❌
└── WebSocketChannel (exists, generic) ✅
```

### 3. Notification Models ✅ EXIST

**Current Entities** (4 user-type-specific models):
1. `/greedys_api/src/main/java/com/application/restaurant/persistence/model/RestaurantUserNotification.java`
   - Extends: `ANotification` (abstract base)
   - Fields: eventId (unique), userId, restaurantId, channel, status, title, body, etc
   - Status: ✅ Fully implemented

2. `/greedys_api/src/main/java/com/application/customer/persistence/model/CustomerNotification.java`
   - Extends: `ANotification`
   - Status: ✅ Exists

3. `/greedys_api/src/main/java/com/application/agency/persistence/model/AgencyUserNotification.java`
   - Extends: `ANotification`
   - Status: ✅ Exists

4. `/greedys_api/src/main/java/com/application/admin/persistence/model/AdminNotification.java`
   - Extends: `ANotification`
   - Status: ✅ Exists

**Base Class**:
- `/greedys_api/src/main/java/com/application/common/persistence/model/notification/ANotification.java`
  - Abstract base class for all notification types
  - Status: ✅ Exists

### 4. EventOutboxOrchestrator ❌ NOT FOUND

**Expected Location**: `com.application.common.service.notification.orchestrator.EventOutboxOrchestrator`

**Current Search Result**: ❌ FILE NOT FOUND IN SRC/MAIN

**What Exists Instead**:
- `EventOutboxRepository` - JPA interface (repo/query layer)
- `EventOutboxDAO` - DAO interface
- `EventOutbox` - Entity model

**Implications**:
- ⚠️ No EventOutboxOrchestrator currently exists
- ⚠️ EventOutbox events are NOT being published to RabbitMQ (no poller job)
- ⚠️ Listeners are receiving messages, but NO ONE IS SENDING THEM

**Action Required**: 
- ❌ Must create EventOutboxOrchestrator service
- Must be scheduled job that:
  1. Polls EventOutbox table (status=PENDING)
  2. Publishes to RabbitMQ queues
  3. Marks EventOutbox as PROCESSED

### 5. NotificationOrchestrator ❌ MISSING

**Expected Location**: `com.application.common.service.notification.orchestrator.NotificationOrchestrator<T>`

**Current Status**: ❌ DOES NOT EXIST

**What ARCHITECTURE_INHERITANCE Requires**:
```
NotificationOrchestrator<T> (abstract base) ❌
├── RestaurantUserOrchestrator ❌
├── CustomerOrchestrator ❌
├── AgencyUserOrchestrator ❌
└── AdminOrchestrator ❌

NotificationOrchestratorFactory ❌
```

**Purpose**:
- Extract disaggregation logic from listeners
- Centralize per-type business logic
- Enable event-type-specific rules
- Support inheritance customization

---

## 📊 FILES MODIFICATION MATRIX

### PHASE 1: Create BaseNotificationListener<T> (2-3 hours)

| File | Status | Action | Effort |
|------|--------|--------|--------|
| `BaseNotificationListener.java` | ❌ NEW | Create abstract base class | 2-3 hrs |
| `RestaurantNotificationListener.java` | ⚠️ MODIFY | Extend BaseNotificationListener<RestaurantUserNotification> | 30 min |
| `CustomerNotificationListener.java` | ⚠️ MODIFY | Extend BaseNotificationListener<CustomerNotification> | 30 min |
| `AgencyUserNotificationListener.java` | ⚠️ MODIFY | Extend BaseNotificationListener<AgencyUserNotification> | 30 min |
| `AdminNotificationListener.java` | ⚠️ MODIFY | Extend BaseNotificationListener<AdminNotification> | 30 min |

### PHASE 2: Create Channel Hierarchy (4-5 hours)

| File | Status | Action | Effort |
|------|--------|--------|--------|
| `AbstractEmailChannel.java` | ❌ NEW | Create abstract email channel | 1 hr |
| `AbstractPushChannel.java` | ❌ NEW | Create abstract push channel | 1 hr |
| `AbstractSmsChannel.java` | ❌ NEW | Create abstract SMS channel | 1 hr |
| `RestaurantEmailChannel.java` | ❌ NEW | Implement restaurant-specific email | 30 min |
| `RestaurantPushChannel.java` | ❌ NEW | Implement restaurant-specific push | 30 min |
| `RestaurantSmsChannel.java` | ❌ NEW | Implement restaurant-specific SMS | 30 min |
| `CustomerEmailChannel.java` | ❌ NEW | Implement customer-specific email | 30 min |
| `CustomerPushChannel.java` | ❌ NEW | Implement customer-specific push | 30 min |
| `CustomerSmsChannel.java` | ❌ NEW | Implement customer-specific SMS | 30 min |
| `AgencyEmailChannel.java` | ❌ NEW | Implement agency-specific email | 30 min |
| `AgencyPushChannel.java` | ❌ NEW | Implement agency-specific push | 30 min |
| `AgencySmsChannel.java` | ❌ NEW | Implement agency-specific SMS | 30 min |
| `AdminEmailChannel.java` | ❌ NEW | Implement admin-specific email | 30 min |
| `AdminPushChannel.java` | ❌ NEW | Implement admin-specific push | 30 min |
| `AdminSmsChannel.java` | ❌ NEW | Implement admin-specific SMS | 30 min |
| `EmailNotificationChannel.java` | ⚠️ DELETE or KEEP | Decide: keep as fallback or delete | 0 min |
| `PushNotificationChannel.java` | ⚠️ DELETE or KEEP | Decide: keep as fallback or delete | 0 min |
| `SMSNotificationChannel.java` | ⚠️ DELETE or KEEP | Decide: keep as fallback or delete | 0 min |

### PHASE 3: Create Orchestrator Hierarchy (5-7 hours)

| File | Status | Action | Effort |
|------|--------|--------|--------|
| `NotificationOrchestrator.java` | ❌ NEW | Create abstract base orchestrator | 2 hrs |
| `NotificationOrchestratorFactory.java` | ❌ NEW | Create factory for orchestrator dispatch | 1 hr |
| `RestaurantUserOrchestrator.java` | ❌ NEW | Create restaurant orchestrator | 1.5 hrs |
| `CustomerOrchestrator.java` | ❌ NEW | Create customer orchestrator | 1.5 hrs |
| `AgencyUserOrchestrator.java` | ❌ NEW | Create agency orchestrator | 1.5 hrs |
| `AdminOrchestrator.java` | ❌ NEW | Create admin orchestrator | 1.5 hrs |

### PHASE 4: Create/Modify EventOutboxOrchestrator (2-3 hours)

| File | Status | Action | Effort |
|------|--------|--------|--------|
| `EventOutboxOrchestrator.java` | ❌ NEW | Create poller service (CRITICAL - NO EXISTING) | 2-3 hrs |
| `RabbitMQConfig.java` | ⚠️ MODIFY | Ensure queues are configured | 30 min |

### PHASE 5: Wire Everything Together (1-2 hours)

| File | Status | Action | Effort |
|------|--------|--------|--------|
| `ApplicationConfiguration.java` or similar | ⚠️ MODIFY | Register orchestrator beans | 1 hr |
| `Spring context XML or Java config` | ⚠️ MODIFY | Wire NotificationOrchestrator beans | 1 hr |

---

## 🎯 CRITICAL FINDINGS

### 1. ❌ MISSING: EventOutboxOrchestrator
**Impact**: HIGH - BLOCKING
- **Problem**: Listeners are ready to receive messages, but NO SOURCE is publishing them
- **Current State**: EventOutbox table exists but events are NOT being published to RabbitMQ
- **Solution**: Must create EventOutboxOrchestrator service to act as poller
- **Effort**: 2-3 hours

### 2. ⚠️ LISTENERS ALREADY DISAGGREGATE
**Impact**: LOW - Already correct pattern
- **Current**: RestaurantNotificationListener already receives message and disaggregates
- **Good News**: Already AFTER RabbitMQ (correct pattern)
- **Action**: Extract disaggregation into NotificationOrchestrator for code reuse
- **Effort**: Part of Phase 1 + Phase 3

### 3. ❌ MISSING: NotificationOrchestrator Hierarchy
**Impact**: MEDIUM - Refactoring opportunity
- **Problem**: Disaggregation logic embedded in 4 listeners (code duplication)
- **Solution**: Create abstract NotificationOrchestrator with 4 subclasses
- **Benefit**: Centralizes logic, enables per-type customization
- **Effort**: 5-7 hours

### 4. ❌ MISSING: Channel Inheritance Hierarchy
**Impact**: LOW - Can be done later
- **Problem**: All 4 channel implementations are generic (no per-type customization)
- **Solution**: Create Abstract base classes + 12 user-type-specific subclasses
- **Benefit**: Enables type-specific templates, routing, retry logic
- **Effort**: 4-5 hours (can be incremental)

### 5. ✅ NOTIFICATION MODELS ARE GOOD
**Impact**: N/A - Already correct
- All 4 user-type-specific models exist
- Inherit from ANotification abstract base
- Have all required fields (eventId, userId, channel, status, etc)
- No changes needed

---

## 🚀 IMPLEMENTATION ROADMAP

### IMMEDIATE (Must Do First)
1. **Create EventOutboxOrchestrator** (2-3 hours)
   - Polls EventOutbox table (status=PENDING)
   - Publishes to RabbitMQ queues
   - Marks as PROCESSED
   - Without this, listeners never receive messages

### SHORT TERM (Phase 1)
2. **Create BaseNotificationListener<T>** (2-3 hours)
   - Extract common logic from 4 listeners
   - Define abstract methods for subclasses
   - Update 4 listeners to extend base

### MEDIUM TERM (Phases 2-3)
3. **Create NotificationOrchestrator Hierarchy** (5-7 hours)
   - Abstract base class
   - 4 user-type-specific subclasses
   - Factory for dispatch

4. **Create Channel Inheritance** (4-5 hours)
   - Abstract base classes per channel type
   - 12 user-type-specific implementations
   - Can be deferred (low priority)

### LONG TERM (Optional Enhancement)
5. **Add Event-Type-Specific Rules** (2-3 hours)
   - Override points in NotificationOrchestrator subclasses
   - Per-type customization for different event types
   - Example: CRITICAL_RESERVATION_REQUESTED → SMS to managers

---

## ✅ VERIFICATION CHECKLIST

Before implementing, verify:

- [ ] EventOutbox table exists and has pending events
- [ ] RabbitMQ queues configured: notification.restaurant, notification.customer, notification.agency, notification.admin
- [ ] 4 listener classes are running and listening to queues
- [ ] INotificationChannel interface is available for extension
- [ ] 4 Notification entity models exist (Restaurant, Customer, Agency, Admin)
- [ ] ANotification abstract base class exists
- [ ] Spring beans for listeners are properly configured

---

## 📊 EFFORT SUMMARY

| Phase | Task | Effort | Priority |
|-------|------|--------|----------|
| IMMEDIATE | EventOutboxOrchestrator | 2-3 hrs | **CRITICAL** |
| Phase 1 | BaseNotificationListener<T> | 2-3 hrs | **HIGH** |
| Phase 2 | Channel Hierarchy | 4-5 hrs | **MEDIUM** |
| Phase 3 | Orchestrator Hierarchy | 5-7 hrs | **HIGH** |
| Phase 4 | Wire & Test | 2-3 hrs | **HIGH** |
| Phase 5 | Event-Type Rules (Optional) | 2-3 hrs | **LOW** |
| **TOTAL** | **Full Implementation** | **18-24 hrs** | — |

---

## 🎯 CONCLUSION

✅ **ARCHITECTURE_INHERITANCE.md is FEASIBLE**

**Current State**:
- ✅ Listeners exist and already disaggregate AFTER RabbitMQ (correct pattern!)
- ✅ Notification models exist with proper inheritance
- ❌ EventOutboxOrchestrator is MISSING (blocking issue)
- ❌ NotificationOrchestrator hierarchy is missing
- ❌ Channel hierarchy is missing (but less critical)

**What's Needed**:
1. Create EventOutboxOrchestrator (CRITICAL - enables entire system)
2. Create BaseNotificationListener<T> (refactors existing code)
3. Create NotificationOrchestrator hierarchy (centralizes disaggregation)
4. Create Channel hierarchy (enhances per-type customization)

**Risk Level**: LOW
- All supporting infrastructure exists
- Listeners already implement correct pattern (disaggregation after RabbitMQ)
- Models are in place
- Changes are additive (no breaking changes to existing system)

**Recommendation**: PROCEED with implementation
- Start with EventOutboxOrchestrator (unblocks everything)
- Then Phase 1 (refactor listeners)
- Then Phase 3 (orchestrator hierarchy)
- Channel hierarchy can be deferred (lower priority)

---

**Report Generated**: 21 November 2025
**Status**: ✅ VERIFIED & FEASIBLE
