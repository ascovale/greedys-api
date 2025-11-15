# 🔍 WEBSOCKET RESERVATION SEQUENCE - CODE IMPLEMENTATION ANALYSIS

**Data:** 14 Novembre 2025  
**Status:** ⚠️ PARTIALLY IMPLEMENTED

---

## 📊 Implementation Status Summary

| Componente | Classe | Status | Note |
|-----------|--------|--------|------|
| **L0 Event Creation** | Service (Service.java) | ✅ | Crea event_outbox prima della risposta |
| **L0 Polling** | EventOutboxPoller | ✅ | Pubblica a RabbitMQ ogni 1 secondo |
| **L0 → RabbitMQ** | RabbitTemplate | ✅ | Configurato |
| **Listener** | RestaurantNotificationListener | ⚠️ | Hardcoded staff_id=1 (TODO) |
| **L1 Creation** | RestaurantNotificationListener | ✅ | Crea notification_outbox |
| **L1 Polling** | NotificationOutboxPoller | ⚠️ | RabbitMQ integration commented out |
| **L2 Creation** | NotificationOutboxPoller | ✅ | Crea notification_channel_send |
| **L2 Polling** | ChannelPoller | ⚠️ | WebSocket send() is stub (TODO) |
| **WebSocket Send** | ChannelPoller.sendWebSocketDirect() | ❌ | Not implemented |
| **WebSocket Config** | WebSocketConfig.java | ❌ | MISSING |

---

## 🔴 CRITICAL ISSUES FOUND

### 1️⃣ RestaurantNotificationListener - Hardcoded staff_id=1

**File:** `RestaurantNotificationListener.java`  
**Line:** 122  
**Severity:** 🔴 CRITICAL

```java
// WRONG - Hardcoded!
Long staffUserId = 1L;

RestaurantNotification notification = createNotificationFromEvent(
    eventType, eventData, restaurantId, staffUserId);

// Creates ONLY one notification instead of N (one per staff)
notificationOutboxDAO.save(outbox);
```

**Should be:**
```java
// Get all staff for this restaurant
List<RestaurantUser> staffList = restaurantUserDAO.findByRestaurantId(restaurantId);

// Create one notification per staff
for (RestaurantUser staff : staffList) {
    RestaurantNotification notification = createNotificationFromEvent(
        eventType, eventData, restaurantId, staff.getId());
    
    RestaurantNotification savedNotification = restaurantNotificationDAO.save(notification);
    
    NotificationOutbox outbox = NotificationOutbox.builder()
        .notificationId(savedNotification.getId())
        .notificationType("RESTAURANT")
        .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
        .aggregateId(restaurantId)
        .eventType(eventType)
        .payload(objectMapper.writeValueAsString(eventData))
        .status(NotificationOutbox.Status.PENDING)
        .retryCount(0)
        .build();
    
    notificationOutboxDAO.save(outbox);
}
```

**Impact:**
- ❌ Only staff with id=1 receives notifications
- ❌ All other staff are ignored
- ❌ Broadcast pattern is broken

**Fix Time:** ~10 minutes

---

### 2️⃣ WebSocketConfig.java - MISSING

**File:** Not Found  
**Location:** Should be `src/main/java/com/application/common/config/WebSocketConfig.java`  
**Severity:** 🔴 CRITICAL

**Why needed:**
- `@EnableWebSocketMessageBroker` configuration
- STOMP endpoint registration (`/ws`)
- Message broker configuration
- SimpMessagingTemplate bean creation

**Required:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable in-memory message broker
        config.enableSimpleBroker("/queue", "/topic");
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}
```

**Impact:**
- ❌ WebSocket endpoint not registered
- ❌ STOMP message broker not configured
- ❌ `SimpMessagingTemplate` not available for injection

**Fix Time:** ~5 minutes

---

### 3️⃣ ChannelPoller - WebSocket send() is STUB

**File:** `ChannelPoller.java`  
**Method:** `sendWebSocketDirect(Long notificationId)` - Line 232  
**Severity:** 🔴 CRITICAL

```java
private void sendWebSocketDirect(Long notificationId) throws Exception {
    // TODO: Implementare WebSocket send logic diretto
    // - Leggi notifica (CustomerNotification, AdminNotification, etc)
    // - Broadcast via WebSocket a userId
    // - Usa SimpMessagingTemplate per inviare a "/user/{userId}/queue/notifications"
    log.debug("TODO: Send WebSocket DIRECT for notification {}", notificationId);
}
```

**Should be:**
```java
private void sendWebSocketDirect(Long notificationId) throws Exception {
    try {
        // 1. Get the notification
        Optional<NotificationRestaurant> notif = 
            notificationRestaurantDAO.findById(notificationId);
        
        if (notif.isEmpty()) {
            log.warn("Notification not found: {}", notificationId);
            return;
        }
        
        NotificationRestaurant notification = notif.get();
        
        // 2. Build payload
        Map<String, Object> payload = Map.of(
            "notificationId", notification.getId(),
            "title", notification.getTitle(),
            "body", notification.getBody(),
            "timestamp", LocalDateTime.now(),
            "properties", notification.getProperties()
        );
        
        // 3. Send via WebSocket
        simpMessagingTemplate.convertAndSendToUser(
            notification.getUserId().toString(),
            "/queue/notifications",
            payload
        );
        
        log.info("WebSocket sent: notif={}, user={}", notificationId, notification.getUserId());
        
    } catch (Exception e) {
        log.error("WebSocket send failed: {}", notificationId, e);
        // No retry for WebSocket (best-effort)
        throw e;
    }
}
```

**Also need to inject SimpMessagingTemplate:**
```java
private final SimpMessagingTemplate simpMessagingTemplate;

public ChannelPoller(
    NotificationChannelSendDAO channelSendDAO,
    SimpMessagingTemplate simpMessagingTemplate) {
    this.channelSendDAO = channelSendDAO;
    this.simpMessagingTemplate = simpMessagingTemplate;
}
```

**Impact:**
- ❌ WebSocket messages not sent to clients
- ❌ Staff don't receive real-time notifications
- ❌ Only logging "TODO" message

**Fix Time:** ~10 minutes

---

### 4️⃣ NotificationOutboxPoller - RabbitMQ Integration Commented Out

**File:** `NotificationOutboxPoller.java`  
**Line:** 109  
**Severity:** 🟡 MEDIUM

```java
// TODO: INTEGRATE WITH RABBITMQ WHEN CONFIGURED
// amqpTemplate.convertAndSend("notification-channel-send", notificationType, payload);
```

**Current flow:** DB → directly to notification_channel_send (no RabbitMQ)  
**Intended flow:** DB → RabbitMQ → notification_channel_send

**Status:** This actually works fine for current implementation, but breaks the intended L1→L2 async pattern.

**Impact:**
- ⚠️ NotificationOutboxPoller doesn't use RabbitMQ
- ⚠️ Creates L2 entries directly (synchronous)
- ✅ System still works but doesn't match architecture

**Fix Time:** ~5 minutes (when RabbitMQ is ready)

---

## ✅ WORKING COMPONENTS

### EventOutboxPoller ✅
- Reads `event_outbox` [L0]
- Publishes to RabbitMQ queue `event-stream`
- Updates status to PROCESSED
- Timing: Every 1 second
- **Status:** COMPLETE

### RestaurantNotificationListener ✅ (except staff loop)
- Listens to RabbitMQ `event-stream`
- Idempotency check on processed_by
- Creates RestaurantNotification entries
- Creates notification_outbox [L1] entries
- Updates event_outbox processed_by
- **Status:** 90% COMPLETE (missing staff loop)

### NotificationOutboxPoller ✅ (except RabbitMQ publish)
- Reads `notification_outbox` [L1]
- Updates status to PUBLISHED
- Creates `notification_channel_send` [L2]
- **Status:** 95% COMPLETE (RabbitMQ integration commented out)

### ChannelPoller ✅ (except WebSocket)
- Reads `notification_channel_send` [L2]
- Implements sendWebSocketDirect() route
- Other channels (email, SMS, etc) are stubs
- **Status:** 30% COMPLETE (WebSocket not implemented)

---

## 📋 TODO LIST - PRIORITY ORDER

### Priority 1️⃣ CRITICAL (Blocks WebSocket)

**Task 1.1:** Fix RestaurantNotificationListener staff loop
- **File:** RestaurantNotificationListener.java, line 122
- **Change:** Replace hardcoded `Long staffUserId = 1L;` with loop over all staff
- **Estimated Time:** 10 minutes
- **Testing:** Create reservation, verify 3 notifications created

**Task 1.2:** Create WebSocketConfig.java
- **File:** Create new file in `src/main/java/com/application/common/config/`
- **Content:** @Configuration, @EnableWebSocketMessageBroker, STOMP endpoint
- **Estimated Time:** 5 minutes
- **Testing:** WebSocket endpoint should be accessible at `/ws`

**Task 1.3:** Implement ChannelPoller.sendWebSocketDirect()
- **File:** ChannelPoller.java, method sendWebSocketDirect()
- **Change:** Replace TODO stub with actual SimpMessagingTemplate.convertAndSendToUser()
- **Also:** Add `@Autowired SimpMessagingTemplate simpMessagingTemplate;` in constructor
- **Estimated Time:** 10 minutes
- **Testing:** Subscribe to WebSocket, verify message received

### Priority 2️⃣ HIGH (Async Pattern)

**Task 2.1:** Uncomment RabbitMQ integration in NotificationOutboxPoller
- **File:** NotificationOutboxPoller.java, line 109
- **Change:** Uncomment amqpTemplate.convertAndSend() call
- **Estimated Time:** 5 minutes
- **Testing:** Verify messages published to RabbitMQ

### Priority 3️⃣ MEDIUM (Other Channels)

**Task 3.1:** Implement ChannelPoller.sendEmail()
**Task 3.2:** Implement ChannelPoller.sendSMS()
**Task 3.3:** Implement ChannelPoller.sendPush()
**Task 3.4:** Implement ChannelPoller.sendSlack()

---

## 🧪 TESTING CHECKLIST

### Pre-Requisite: All Fixes Applied

```bash
# 1. Create reservation
curl -X POST http://localhost:8080/customer/reservation/ask \
  -H "Content-Type: application/json" \
  -d '{"customerId": 5, "restaurantId": 10, "partySize": 4, "slotTime": "2025-11-14T20:00:00"}'

Response: 200 OK
```

### Test 1: Event Outbox Creation ✅
```bash
# Check event_outbox [L0] created
SELECT * FROM event_outbox WHERE event_type='RESERVATION_REQUESTED' ORDER BY created_at DESC LIMIT 1;

Expected: 1 row with status=PENDING → PROCESSED
```

### Test 2: Listener & L1 Creation ⚠️
```bash
# Wait ~5 seconds, check notification_restaurant
SELECT * FROM notification_restaurant WHERE user_id IN (1,2,3) ORDER BY creation_time DESC LIMIT 3;

Expected AFTER FIX: 3 rows (not 1!)
Currently: 1 row (hardcoded staff_id=1)
```

### Test 3: L1 Outbox Processing ✅
```bash
# Wait ~10 seconds, check notification_outbox [L1]
SELECT * FROM notification_outbox WHERE status='PUBLISHED' ORDER BY created_at DESC LIMIT 1;

Expected AFTER FIX: 3 rows with status=PUBLISHED
Currently: 1 row
```

### Test 4: L2 Channel Send Creation ✅
```bash
# Check notification_channel_send [L2]
SELECT * FROM notification_channel_send WHERE is_sent=false ORDER BY id DESC LIMIT 1;

Expected AFTER FIXES: 0 rows for WebSocket (direct send, no persistence)
Currently: Depends on implementation
```

### Test 5: WebSocket Real-Time Delivery ❌
```bash
# Connect WebSocket client
wscat -c ws://localhost:8080/ws

SUBSCRIBE
destination:/user/queue/notifications
id:0

Expected AFTER FIXES:
  - Connection established
  - MESSAGE received with notification payload
  - UI updates in real-time

Currently: ❌ Not working (config missing, stub not implemented)
```

---

## 📊 Implementation Progress

```
Event → L0 Outbox         ✅ 100%
L0 → RabbitMQ             ✅ 100%
RabbitMQ → Listener       ✅ 100%
Listener → L1 Outbox      ⚠️  90% (missing staff loop)
L1 → L2 Creation          ✅ 100% (RabbitMQ commented out, but doesn't break flow)
L2 → Channel Poller       ✅ 100% (infrastructure ready)
ChannelPoller → WebSocket ❌ 10% (stub only, config missing)
WebSocket → Browser       ❌  0% (blocked by above)

OVERALL: 65% COMPLETE
```

---

## 📈 Next Session Priority

1. **10 min:** Fix RestaurantNotificationListener staff loop
2. **5 min:** Create WebSocketConfig.java
3. **10 min:** Implement ChannelPoller.sendWebSocketDirect()
4. **TEST:** End-to-end WebSocket flow

**Total Fix Time:** ~25 minutes for WebSocket  
**Remaining Work:** Email/SMS/Push/Slack implementations

---

**Last Updated:** 14 Novembre 2025  
**Author:** Code Analysis System
