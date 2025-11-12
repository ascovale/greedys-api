# 📋 ROADMAP SISTEMA NOTIFICHE - GREEDY'S API

**Status:** ✅ Architettura Implementata  
**Data:** Novembre 2025  
**Pattern:** Event-Driven + Transactional Outbox (2 livelli)

---

## ✅ IMPLEMENTATO (COMPLETO)

### Gerarchia Notifiche
- **AEventNotification**: Entity-level, senza userId/userType ✅
- **ANotification**: Recipient-specific, con userId/userType ✅
- **4 Sottoclassi:** AdminNotification, RestaurantNotification, CustomerNotification, AgencyNotification ✅

### Infrastruttura Outbox (3 livelli)
- **EventOutbox** (LIVELLO 1): event_id, event_type, aggregate_type, aggregate_id, processed_by ✅
- **NotificationOutbox** (LIVELLO 2): notification_id, notification_type, status ✅
- **NotificationChannelSend** (LIVELLO 3): channel_type, is_sent (Channel Isolation) ✅

### DAOs (7 total)
- **EventOutboxDAO**: 12 metodi con idempotency check ✅
- **NotificationOutboxDAO**: 11 metodi per status management ✅
- **NotificationChannelSendDAO**: 15 metodi con granular retry ✅
- **4 NotificationDAOs** (Admin, Restaurant, Customer, Agency): findByUserId, countUnread, markAsRead ✅

### Pollers (3 total)
- **EventOutboxPoller** (@Scheduled 5s): SELECT PENDING → PUBLISH → PROCESSED ✅
- **NotificationOutboxPoller** (@Scheduled 5s): SELECT PENDING → PUBLISH → PUBLISHED ✅
- **ChannelPoller** (@Scheduled 10s): Channel Isolation Pattern ✅

### Listeners (4 total)
- **AdminNotificationListener** (242 lines): RESERVATION_REQUESTED, CUSTOMER_REGISTERED, PAYMENT_RECEIVED ✅
- **RestaurantNotificationListener** (195 lines): RESERVATION_REQUESTED, CONFIRMED, CANCELLED ✅
- **CustomerNotificationListener** (218 lines): RESERVATION_CONFIRMED, REJECTED, REMINDER, PAYMENT_RECEIVED, REWARD_EARNED ✅
- **AgencyNotificationListener** (223 lines): BULK_IMPORTED, HIGH_VOLUME, REVENUE_REPORT, CHURN_ALERT, PERFORMANCE, SYSTEM_ALERT ✅

### Altre entità
- NotificationAction (first-to-act pattern) ✅
- NotificationPreferences ✅
- Context classes (Reservation, Chat, Payment, System) ✅

---

## 🏗️ SCELTE ARCHITETTURALI

| Scelta | Valore |
|--------|--------|
| ID Type | Long (IDENTITY) |
| Status Enums | INLINE nelle entità |
| Gerarchia | AEventNotification → ANotification |
| Outbox Livelli | 2 (Event + Notification) |
| Channels | SMS, EMAIL, PUSH, WEBSOCKET, SLACK |
| User Types | CUSTOMER, RESTAURANT_USER, ADMIN_USER, AGENCY_USER |

---

## 🚀 TODO: LISTENERS & POLLERS

### LIVELLO 1: Event Outbox Poller
**File:** `EventOutboxPoller.java`

```
TimelineOutbox Event:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

T0: Service crea evento
├─ INSERT into event_outbox (status=PENDING)
└─ COMMIT

T1 (Poller, ogni 5 sec):
├─ SELECT * FROM event_outbox WHERE status=PENDING LIMIT 100
├─ PUBLISH to RabbitMQ (exchange: event-stream)
└─ UPDATE status=PROCESSED

T2 (RabbitMQ):
├─ Message in event-stream
└─ Ready per 3 listener in parallelo
```

**Implementazione:**
```java
@Scheduled(fixedDelay = 5000)
void pollPendingEvents() {
    List<EventOutbox> pending = repo.findByStatus(PENDING);
    for (EventOutbox event : pending) {
        rabbitTemplate.convertAndSend("event-stream", event.getPayload());
        event.setStatus(PROCESSED);
        event.setPublishedAt(Instant.now());
        repo.save(event);
    }
}
```

---

### LIVELLO 2: Event Listeners (4 paralleli) ✅
**Location:** `listener/` (4 file)

```
T2: 4 Listener in parallelo ricevono da RabbitMQ (event-stream)
│
├─ AdminNotificationListener (242 lines)
│  ├─ Events: RESERVATION_REQUESTED, CUSTOMER_REGISTERED, PAYMENT_RECEIVED
│  ├─ Crea: AdminNotification per admin (userId=admin_id)
│  └─ Idempotency: processed_by='ADMIN_LISTENER'
│
├─ RestaurantNotificationListener (195 lines)
│  ├─ Events: RESERVATION_REQUESTED, CONFIRMED, CANCELLED
│  ├─ Crea: RestaurantNotification per staff (restaurantId FK)
│  └─ Idempotency: processed_by='RESTAURANT_LISTENER'
│
├─ CustomerNotificationListener (218 lines)
│  ├─ Events: RESERVATION_CONFIRMED, REJECTED, REMINDER, PAYMENT_RECEIVED, REWARD_EARNED
│  ├─ Crea: CustomerNotification per cliente (userId=customer_id)
│  └─ Idempotency: processed_by='CUSTOMER_LISTENER'
│
└─ AgencyNotificationListener (223 lines)
   ├─ Events: RESERVATION_BULK_IMPORTED, HIGH_VOLUME, REVENUE_REPORT, CHURN_ALERT, PERFORMANCE, SYSTEM_ALERT
   ├─ Crea: AgencyNotification per agency manager (userId=agency_user_id)
   └─ Idempotency: processed_by='AGENCY_LISTENER'

IMPORTANTE: Un SINGOLO event_outbox ha 4 processedBy (uno per listener)

**Pattern:** (Ogni listener uguale)
```java
@RabbitListener(queues = "event-stream")
void handle(NotificationEvent event) {
    // 1. Idempotency
    if (eventOutboxRepo.existsByEventIdAndProcessedBy(event.getEventId(), "ADMIN")) {
        return;
    }
    
    // 2. Crea notifiche
    List<AdminNotification> notifications = createAdminNotifications(event);
    for (AdminNotification n : notifications) {
        notificationRepo.save(n);
        
        // 3. Salva in notification_outbox
        NotificationOutbox outbox = NotificationOutbox.builder()
            .notificationId(n.getId())
            .notificationType("ADMIN")
            .status(PENDING)
            .build();
        outboxRepo.save(outbox);
    }
    
    // 4. Mark event as processed
    eventOutboxRepo.updateProcessedBy(event.getEventId(), "ADMIN_LISTENER");
}
```

---

### LIVELLO 3: Notification Outbox Poller
**File:** `NotificationOutboxPoller.java`

```
Timeline Notification Outbox:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

T3: Listener inserisce in notification_outbox
├─ For each AdminNotification inserted:
│  ├─ INSERT INTO notification_outbox (notification_id, status=PENDING)
│  └─ INSERT INTO notification_channel_send (notification_id, channel=SMS|EMAIL|PUSH)
│
├─ For each RestaurantNotification inserted:
│  ├─ INSERT INTO notification_outbox (notification_id, status=PENDING)
│  └─ INSERT INTO notification_channel_send (x5 channels)
│
└─ For each CustomerNotification inserted:
   ├─ INSERT INTO notification_outbox (notification_id, status=PENDING)
   └─ INSERT INTO notification_channel_send (x4 channels)

T4 (Poller, ogni 5 sec):
├─ SELECT * FROM notification_outbox WHERE status=PENDING LIMIT 100
├─ PUBLISH to RabbitMQ (exchange: notification-channel-send)
└─ UPDATE status=PUBLISHED
```

**Implementazione:**
```java
@Scheduled(fixedDelay = 5000)
void pollPendingNotifications() {
    List<NotificationOutbox> pending = repo.findByStatus(PENDING);
    for (NotificationOutbox notif : pending) {
        rabbitTemplate.convertAndSend("notification-channel-send", notif.getPayload());
        notif.setStatus(PUBLISHED);
        notif.setPublishedAt(Instant.now());
        repo.save(notif);
    }
}
```

---

### LIVELLO 4: Channel Poller ✅
**File:** `ChannelPoller.java` (280+ lines)

**⭐ CHANNEL ISOLATION PATTERN (Core Architecture):**
```java
@Scheduled(fixedDelay = 10000)
void pollAndSendChannels() {
    // Get all notifications with pending channels
    Set<Long> notificationIds = repo.getNotificationsWithPendingChannels();
    
    for (Long notificationId : notificationIds) {
        // ⭐ KEY: For EACH channel independently
        for (ChannelType channel : ChannelType.values()) {  // SMS, EMAIL, PUSH, WEBSOCKET, SLACK
            
            // Step 1: Check exists
            if (!repo.existsByNotificationIdAndChannelType(notificationId, channel)) {
                // Step 2: CREATE this channel only (not batch)
                NotificationChannelSend send = new NotificationChannelSend();
                send.setNotificationId(notificationId);
                send.setChannelType(channel);
                send.setIsSent(null);  // Pending
                repo.save(send);
            }
            
            // Step 3: Get entry for this channel
            NotificationChannelSend send = repo.findByNotificationIdAndChannelType(notificationId, channel);
            
            if (send != null && send.getIsSent() == null) {  // Pending?
                try {
                    // Step 4: SEND via provider
                    sendViaChannel(send);
                    
                    // Step 5: UPDATE this channel ONLY (independent success)
                    send.setIsSent(true);
                    send.setSentAt(Instant.now());
                    repo.save(send);
                    
                } catch (Exception e) {
                    // Step 6: ERROR handling - only this channel retries
                    send.setAttemptCount(send.getAttemptCount() + 1);
                    send.setLastError(e.getMessage());
                    
                    if (send.getAttemptCount() >= MAX_RETRIES) {
                        send.setIsSent(false);  // Mark failed
                    }
                    repo.save(send);
                    // ⭐ ISOLATION: Don't block other channels, continue loop
                }
            }
        }
    }
}

private void sendViaChannel(NotificationChannelSend send) {
    switch(send.getChannelType()) {
        case SMS -> sendSMS(send);
        case EMAIL -> sendEmail(send);
        case PUSH -> sendPush(send);
        case WEBSOCKET -> sendWebSocket(send);
        case SLACK -> sendSlack(send);
    }
}
```

**Vantaggi Channel Isolation:**
- SMS fails? → Only SMS retries, EMAIL/PUSH/WS/SLACK unaffected
- Granular error tracking per canale (attemptCount, lastError)
- No batch overhead: creazione uno per volta
- Easy debugging: vedi quale canale ha problemi

---

## 📊 FLOW TEMPORALE COMPLETO (CON 4 LISTENERS)

```
EVENT (Reservation Requested)
│
T0: ┌─────────────────────────────┐
    │ ReservationService          │
    │ INSERT into event_outbox    │
    └──────────────┬──────────────┘
                   │ (status=PENDING)
                   │
T1: ┌──────────────▼──────────────┐
    │ EventOutboxPoller (@5s)     │
    │ SELECT PENDING, PUBLISH     │
    │ UPDATE PROCESSED            │
    └──────────────┬──────────────┘
                   │ (to event-stream via RabbitMQ)
     ┌─────────────┼──────────────┬──────────────┐
     │             │              │              │
T2: ┌▼──┐       ┌──▼┐       ┌────▼┐       ┌─────▼┐
    │ADM│       │RES│       │CUS │       │AGENCY│  (Parallelo)
    │LIS│       │LIS│       │LIS │       │LIS  │
    │TEN│       │TEN│       │TEN │       │TEN  │
    └──┬┘       └───┬┘       └────┘       └──────┘
       │            │         │              │
       │ CREATE Notification  │              │
       │ INSERT notification_ │              │
       │ outbox + UPDATE      │              │
       │ processed_by         │              │
       │
       └────────────┬─────────────┬──────────────────┘
                    │             │
T3:                │             │
    ┌───────────────▼─────────────▼──────────────┐
    │ NotificationOutboxPoller (@5s) - OPZIONALE │
    │ SELECT PENDING, PUBLISH, UPDATE PUBLISHED  │
    └───────────────┬───────────────────────────┘
                    │ (to notification-channel-send via RabbitMQ)
T4:                │
    ┌───────────────▼──────────────────────────────────────┐
    │ ChannelPoller (@10s) - CHANNEL ISOLATION PATTERN    │
    │ FOR each notification:                              │
    │   FOR each channel (SMS, EMAIL, PUSH, WS, SLACK):  │
    │     CREATE IF NOT EXISTS                            │
    │     SEND via provider                               │
    │     UPDATE is_sent=true/false (INDEPENDENTLY)       │
    │     If error: increment attempt_count (THIS ONLY)   │
    └───────────────┬──────────────────────────────────────┘
                    │
T5:                │
    ┌───────────────▼──────────────────────────┐
    │ SMS, Email, Push, WebSocket, Slack       │
    │ Notification delivered (parallelo)       │
    └──────────────────────────────────────────┘
```

⭐ **Channel Isolation:**
   - Se SMS fallisce → only SMS riprova prossimo ciclo (email/push/ws/slack continuano)
   - Granular retry: attempt_count per singolo canale
   - No batch overhead: uno per volta

---

## 📦 FOLDER STRUCTURE (COMPLETO)

```
notification/
├── AEventNotification.java ✅
├── ANotification.java ✅
├── AdminNotification.java ✅
├── RestaurantNotification.java ✅
├── CustomerNotification.java ✅
├── AgencyNotification.java ✅
├── EventOutbox.java ✅
├── NotificationOutbox.java ✅
├── NotificationChannelSend.java ✅
├── NotificationAction.java ✅
├── NotificationPreferences.java ✅
│
├── dao/ (7 DAO - 80+ query methods total)
│   ├── EventOutboxDAO.java ✅ (12 methods)
│   ├── NotificationOutboxDAO.java ✅ (11 methods)
│   ├── NotificationChannelSendDAO.java ✅ (15 methods)
│   ├── AdminNotificationDAO.java ✅
│   ├── RestaurantNotificationDAO.java ✅
│   ├── CustomerNotificationDAO.java ✅
│   └── AgencyNotificationDAO.java ✅
│
├── listener/ (4 Listener classes - 878 total lines)
│   ├── AdminNotificationListener.java ✅ (242 lines)
│   ├── RestaurantNotificationListener.java ✅ (195 lines)
│   ├── CustomerNotificationListener.java ✅ (218 lines)
│   └── AgencyNotificationListener.java ✅ (223 lines)
│
├── poller/ (3 Poller classes - 530+ total lines)
│   ├── EventOutboxPoller.java ✅ (127 lines)
│   ├── NotificationOutboxPoller.java ✅ (122 lines)
│   └── ChannelPoller.java ✅ (280+ lines - Channel Isolation)
│
├── channel/ (6 Channel implementations)
│   ├── INotificationChannel.java ✅
│   ├── EmailNotificationChannel.java ✅
│   ├── SMSNotificationChannel.java ✅
│   ├── FirebaseNotificationChannel.java ✅
│   ├── WebSocketNotificationChannel.java ✅
│   └── SlackNotificationChannel.java ✅
│
└── orchestrator/
    └── (Already exists)

Total Implementation: 1500+ lines of core notification logic
```

---

## 🎯 PROSSIMI STEP

1. ✅ Crea **7 DAOs** (EventOutbox, NotificationOutbox, NotificationChannelSend + 4 Notification DAOs)
2. ✅ Crea **3 Pollers** (EventOutbox, NotificationOutbox, ChannelPoller)
3. ✅ Crea **4 Listeners** (Admin, Restaurant, Customer, Agency)
4. ⏳ Configura **RabbitMQ** (exchanges, queues, bindings)
5. ⏳ Implementa **Channel Send Methods** (SMS, Email, Push, WebSocket, Slack)
6. ⏳ Test end-to-end flow

**Architettura:** COMPLETA (10/10 componenti)  
**Codice:** 878+ righe listener + 280+ righe ChannelPoller + 370+ righe pollers = 1500+ righe core  
**Tempo restante:** 3-4 ore per RabbitMQ + channel implementations
