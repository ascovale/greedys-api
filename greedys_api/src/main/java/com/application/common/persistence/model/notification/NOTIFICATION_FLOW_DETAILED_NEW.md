# NOTIFICATION FLOW - DIAGRAMMA TEMPORALE

**Versione:** Semplificata + Doppia Gerarchia (AEventNotification → ANotification)  
**Data:** Novembre 2025

---

## DIAGRAMMA TEMPORALE ASCII

```
EVENT (ReservationRequested)
│
T0: ┌─────────────────────────────────────────────┐
    │ SERVICE: ReservationService.create()        │
    │ ├─ INSERT INTO reservation (TX0)            │
    │ ├─ CREATE ReservationRequestedEvent         │
    │ └─ INSERT INTO event_outbox (status=PENDING)│
    │    └─ COMMIT TX0                            │
    └────────────────┬────────────────────────────┘
                     │
T1 (5 sec):  ┌──────▼────────────────────────────┐
             │ EventOutboxPoller                  │
             │ ├─ SELECT status=PENDING          │
             │ ├─ PUBLISH to event-stream RabbitMQ│
             │ └─ UPDATE status=PROCESSED        │
             └──────┬─────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
T2:  ┌──▼──┐   ┌───▼──┐   ┌───▼──┐
     │ADM  │   │RES   │   │CUST  │  (Parallel Listeners)
     │LSTN │   │LSTN  │   │LSTN  │
     └──┬──┘   └───┬──┘   └───┬──┘
        │          │          │
     ┌──▼──────────▼──────────▼──────┐
     │ CREATE Notifications           │
     │ ├─ AdminNotification           │
     │ │  (user_id=1, type=ADMIN)     │
     │ ├─ AdminNotification           │
     │ │  (user_id=2, type=ADMIN)     │
     │ ├─ RestaurantNotification      │
     │ │  (user_id=50, type=RES_USER) │
     │ └─ CustomerNotification        │
     │    (user_id=456, type=CUSTOMER)│
     └──┬──────────────────────────────┘
        │
T3:  ┌──▼──────────────────────────┐
     │ INSERT INTO notification_*   │
     │ INSERT INTO notification_    │
     │  outbox (status=PENDING)     │
     │ INSERT INTO notification_    │
     │  channel_send (5 canali)     │
     └──┬──────────────────────────┘
        │
T4 (5 sec): ┌──────────────────────┐
            │ NotificationOutbox   │
            │ Poller               │
            │ ├─ SELECT status=    │
            │ │   PENDING          │
            │ ├─ PUBLISH to        │
            │ │   notification-    │
            │ │   channel-send     │
            │ └─ UPDATE status=    │
            │    PUBLISHED         │
            └──┬───────────────────┘
               │
T5 (10 sec):┌──▼──────────────────────┐
            │ ChannelPoller            │
            │ SELECT is_sent IS NULL   │
            ├─ SMS Provider → SEND     │
            ├─ Email Provider → SEND   │
            ├─ Firebase → SEND         │
            ├─ WebSocket → BROADCAST   │
            └─ UPDATE is_sent=true/false
```

---

## 4 USER TYPES

| Type | Table | Fields |
|------|-------|--------|
| **CUSTOMER** | customer | email, phone, fcm_token |
| **RESTAURANT_USER** | restaurant_users | email, phone, fcm_token, restaurant_id |
| **ADMIN_USER** | admin_users | email, phone, fcm_token |
| **AGENCY_USER** | agency_users | email, phone, fcm_token, agency_id |

---

## 5 CHANNEL TYPES

| Channel | Use | Resolution |
|---------|-----|------------|
| **SMS** | Restaurant, Customer urgent | phone from user table |
| **EMAIL** | All users | email from user table |
| **PUSH** | Mobile users | FCM token from preferences |
| **WEBSOCKET** | Real-time UI | Connected sockets |
| **SLACK** | Admin alerts | Slack webhook |

---

## GERARCHIA NOTIFICHE

### AEventNotification (Entity-Level)
```
title, body, properties
is_read, shared_read, read_by_user_id (entity-level)
NO userId, NO userType (event-level, non recipient-specific)
```

**Sottoclassi:** AdminEventNotification, RestaurantEventNotification, CustomerEventNotification, AgencyEventNotification

### ANotification extends AEventNotification (Recipient-Specific)
```
+ userId (Long)
+ userType (String: CUSTOMER, RESTAURANT_USER, ADMIN_USER, AGENCY_USER)
```

**Sottoclassi:** AdminNotification, RestaurantNotification, CustomerNotification, AgencyNotification

---

## DOPPIO OUTBOX

### EventOutbox (LIVELLO 1)
```sql
┌─ id (PK)
├─ event_id (eventId="RES_REQ_123_456")
├─ event_type ("RESERVATION_REQUESTED")
├─ aggregate_type ("RESERVATION")
├─ aggregate_id (123)
├─ payload (JSON event data)
├─ status (PENDING → PROCESSED)
├─ processed_by (NULL | "ADMIN_LISTENER" | "RESTAURANT_LISTENER" | "CUSTOMER_LISTENER")
├─ created_at
├─ published_at
└─ retry_count

INDEX: status + created_at, event_type + aggregate_id, event_id (UNIQUE)
```

**Flow:**
1. Service INSERT (status=PENDING)
2. EventOutboxPoller PUBLISH to RabbitMQ
3. Listener UPDATE processed_by

### NotificationOutbox (LIVELLO 2)
```sql
┌─ id (PK)
├─ notification_id (FK → admin_notification.id | restaurant_notification.id | ...)
├─ notification_type ("ADMIN" | "RESTAURANT" | "CUSTOMER" | "AGENCY")
├─ status (PENDING → PUBLISHED)
├─ created_at
└─ retry_count

INDEX: status + created_at, notification_id
```

**Flow:**
1. Listener INSERT (status=PENDING)
2. NotificationOutboxPoller PUBLISH to RabbitMQ
3. ChannelPoller starts sending

### NotificationChannelSend (LIVELLO 3)
```sql
┌─ id (PK)
├─ notification_id (FK)
├─ channel_type (SMS | EMAIL | PUSH | WEBSOCKET | SLACK)
├─ is_sent (NULL | true | false)
├─ sent_at (Instant)
├─ attempt_count (Int)
├─ last_error (String)
├─ created_at
└─ updated_at

UNIQUE: notification_id + channel_type
```

**Flow:**
1. Listener INSERT for each channel (is_sent=NULL)
2. ChannelPoller SEND via provider
3. UPDATE is_sent=true/false

---

## NOTIFICATION ACTION (First-To-Act)

```sql
┌─ id (PK)
├─ notification_id (FK)
├─ actor_id (Long, user che ha agito)
├─ action_type (CONFIRMED | REJECTED | POSTPONED | DISMISSED | CUSTOM)
└─ created_at

PATTERN: Se sharedRead=true, solo PRIMO actor registrato è "first-to-act"
         Altri vedono "Gestito da [Nome]"
```

**Scenario:**
- ReservationRequest con sharedRead=true
- Manager #1 accetta → NotificationAction(CONFIRMED)
- Tutti altri manager vedono "Accettato da Manager #1"

---

## FAILURE SCENARIOS

### Se Service muore dopo INSERT event_outbox
- ✅ EventOutboxPoller ritrova (status=PENDING) al restart
- ✅ Ripubblica

### Se Listener muore dopo CREATE notification
- ✅ Event rimane in processed_by=NULL
- ⚠️ Poller rielabora (rischio duplicati se no idempotency)
- **Soluzione:** Idempotency check su notification_id oppure event_id

### Se ChannelPoller muore
- ✅ NotificationChannelSend rimane con is_sent=NULL
- ✅ Riparte da dove ha lasciato (retry logic)
- **Max retries:** attempt_count >= 5 → is_sent=false (fallito)

### Se SMS provider timeout
- ✅ Catch exception, increment attempt_count
- ✅ Rimane in queue per retry prossimo ciclo
- ⚠️ **Attenzione:** Non duplicare invio (check is_sent before send)

---

## PRINCIPI ARCHITETTURALI

1. **Separation of Concerns**
   - AEventNotification: Cosa (title, body)
   - ANotification: Chi (userId, userType)
   - NotificationChannelSend: Come (channel type)

2. **At-Least-Once Delivery**
   - 2 Outbox levels garantiscono persistenza
   - Retry logic con backoff

3. **Idempotency**
   - Listener: Check event_id prima di CREATE
   - ChannelPoller: Check is_sent prima di SEND

4. **Multi-Recipient Support**
   - Un AEventNotification → N ANotification (uno per recipient)
   - Un ANotification → N NotificationChannelSend (uno per channel)

5. **Async Processing**
   - Service: Salva subito, torna al caller
   - Poller: Pubblica in background
   - Listener: Consuma da RabbitMQ in parallelo

---

## TODO

- [ ] EventOutboxRepository + finder methods
- [ ] AdminNotificationListener, RestaurantNotificationListener, CustomerNotificationListener
- [ ] EventOutboxPoller (@Scheduled)
- [ ] NotificationOutboxPoller (@Scheduled)
- [ ] ChannelPoller (@Scheduled)
- [ ] RabbitMQ config (exchanges, queues, bindings)
