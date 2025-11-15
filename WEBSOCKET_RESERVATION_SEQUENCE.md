# 🔌 Sequence / Timeline: Customer Reservation → Restaurant WebSocket Notifications (DIRECT - NO L2)

**Data:** 14 Novembre 2025

Questo file mostra il diagramma temporale (sequence) per **WEBSOCKET**, che usa un pattern **DIRETTO** senza il terzo livello di outbox (notification_channel_send).

---

## 📊 Diagramma di Sequenza UML (ASCII Art)

```
Customer    Controller      Service      EventOutboxPoller    RabbitMQ      Listener     NotifOutboxPoller    ChannelPoller    WebSocket
   |            |               |                  |                |              |                |                   |                |
   |--POST /ask-|               |                  |                |              |                |                   |                |
   |            |--save()------->|                  |                |              |                |                   |                |
   |            |               |--INSERT Reservation               |              |                |                   |                |
   |            |               |--INSERT event_outbox[L0]          |              |                |                   |                |
   |            |               |  (status=PENDING)                 |              |                |                   |                |
   |            |<--200 OK-------|                  |                |              |                |                   |                |
   |<--200 OK---|               |                  |                |              |                |                   |                |
   |            |               |                  |                |              |                |                   |                |
   |            |               |        [~5s]     |                |              |                |                   |                |
   |            |               |   SELECT event_outbox[L0]         |              |                |                   |                |
   |            |               |                  |--PUBLISH----->|              |                |                   |                |
   |            |               |                  |  UPDATE status|              |                |                   |                |
   |            |               |                  |                |              |                |                   |                |
   |            |               |                  |                |<--event message--|           |                   |                |
   |            |               |                  |                |              |--@RabbitListener triggered          |                |
   |            |               |                  |                |              |--Idempotency check                 |                |
   |            |               |                  |                |              |--SELECT restaurant_users           |                |
   |            |               |                  |                |              |--FOR EACH staff:                   |                |
   |            |               |                  |                |              |  INSERT notification_restaurant(×3)|                |
   |            |               |                  |                |              |  INSERT notification_outbox[L1](×3)|               |
   |            |               |                  |                |              |  UPDATE event_outbox processed_by |                |
   |            |               |                  |                |              |                |                   |                |
   |            |               |                  |                |              |       [~5s]    |                   |                |
   |            |               |                  |                |              |                |--SELECT notif_outbox[L1]|
   |            |               |                  |                |              |                |  (status=PENDING)      |
   |            |               |                  |                |              |                |--UPDATE status=PUBLISHED|
   |            |               |                  |                |              |                |                   |                |
   |            |               |                  |                |              |                |  ⭐ SKIP [L2] creation|
   |            |               |                  |                |              |                |  (WebSocket=DIRECT)    |
   |            |               |                  |                |              |                |                   |                |
   |            |               |                  |                |              |                |      [~10s]       |                |
   |            |               |                  |                |              |                |                   |--SELECT notif_rest   |
   |            |               |                  |                |              |                |                   |  (DIRECT from [L1])  |
   |            |               |                  |                |              |                |                   |--convertAndSendToUser|
   |            |               |                  |                |              |                |                   |--STOMP Frame-------->|
   |            |               |                  |                |              |                |                   |                |✅ Real-time Delivered
```

---

## 📝 Spiegazione del Flusso (WEBSOCKET: DIRECT PATTERN)

### 1️⃣ **Fase: HTTP Request → Reservation Created + event_outbox [L0]** (Synchronous)

**Componenti:** Customer → Controller → Service → Repository

Uguale a EMAIL. Vedi `EMAIL_RESERVATION_SEQUENCE.md` linea 1️⃣.

---

### 2️⃣ **Fase: Event Outbox Poller** (Asynchronous, ~5 seconds later)

**Componenti:** EventOutboxPoller → RabbitMQ

Uguale a EMAIL. Vedi `EMAIL_RESERVATION_SEQUENCE.md` linea 2️⃣.

---

### 3️⃣ **Fase: Event Listener** (Triggered by RabbitMQ)

**Componenti:** Listener → Database (notification_restaurant + notification_outbox [L1])

Uguale a EMAIL. Vedi `EMAIL_RESERVATION_SEQUENCE.md` linea 3️⃣.

**Database State AFTER:**
- `notification_restaurant` table: 3 rows inserted
- `notification_outbox` table [L1]: 3 rows inserted with status=PENDING

---

### 4️⃣ **Fase: Notification Outbox Poller** (Asynchronous, ~5 seconds later)

**Componenti:** NotificationOutboxPoller → Database

⭐ **KEY DIFFERENCE: WEBSOCKET SKIPS [L2] CREATION**

```java
@Scheduled(fixedDelay = 5000)
public void pollAndProcessOutbox() {
    List<NotificationOutbox> pending = notificationOutboxDAO.findByStatus("PENDING");
    
    for (NotificationOutbox outbox : pending) {
        // Step 1: UPDATE to PUBLISHED
        outbox.setStatus("PUBLISHED");
        notificationOutboxDAO.save(outbox);
        
        // Step 2: For WEBSOCKET → SKIP notification_channel_send creation!
        // ⭐ This is the key difference from EMAIL/SMS/PUSH
        
        // For EMAIL, you would do:
        //   NotificationChannelSend emailSend = new NotificationChannelSend();
        //   emailSend.setChannelType("EMAIL");
        //   notificationChannelSendDAO.save(emailSend);
        
        // For WEBSOCKET: NOTHING! No L2 persistence
        
        log.info("Notification [L1] marked as PUBLISHED - WebSocket will send DIRECTLY");
    }
}
```

**Timing:** Starts ~5 seconds after listener creates notification_outbox [L1]

**Database State AFTER:**
- `notification_outbox` table [L1]: 3 rows updated (status=PUBLISHED)
- `notification_channel_send` table [L2]: **0 rows** (WebSocket doesn't use it)

---

### 5️⃣ **Fase: Channel Poller - DIRECT WebSocket Send** (Asynchronous, ~10 seconds later)

**Componenti:** ChannelPoller → WebSocket → Staff browsers

⭐ **DIRECT SEND - NO DB PERSISTENCE**

```java
@Scheduled(fixedDelay = 10000, initialDelay = 4000)
public void pollAndSendChannels() {
    try {
        // Step 1: Find notifications with PUBLISHED status in [L1]
        // (Not using [L2] for WebSocket)
        Set<Long> publishedNotificationIds = 
            notificationOutboxDAO.findPublishedNotificationIds();
        
        for (Long notificationId : publishedNotificationIds) {
            // Step 2: Get the actual notification (RestaurantNotification)
            NotificationRestaurant notification = 
                notificationRestaurantDAO.findById(notificationId).orElse(null);
            
            if (notification != null) {
                try {
                    // Step 3: SEND DIRECTLY via WebSocket (NO L2 entry)
                    sendWebSocketDirect(notification);
                    
                    log.info("WebSocket sent DIRECTLY: notification={}, user={}", 
                            notificationId, notification.getUserId());
                    
                    // Step 4: No UPDATE needed (no is_sent tracking)
                    // ⭐ Best-effort only!
                    
                } catch (Exception e) {
                    log.warn("WebSocket send failed (transient): notification={}", 
                            notificationId, e);
                    // ⭐ NO RETRY - just log and continue
                }
            }
        }
        
    } catch (Exception e) {
        log.error("Error in ChannelPoller WebSocket direct send", e);
    }
}

private void sendWebSocketDirect(NotificationRestaurant notification) throws Exception {
    String userId = notification.getUserId().toString();
    
    // Build payload
    Map<String, Object> payload = Map.of(
        "notificationId", notification.getId(),
        "title", notification.getTitle(),
        "body", notification.getBody(),
        "timestamp", LocalDateTime.now(),
        "properties", notification.getProperties()
    );
    
    // Send via SimpMessagingTemplate (in-memory)
    simpMessagingTemplate.convertAndSendToUser(
        userId,
        "/queue/notifications",
        payload
    );
    // ✅ STOMP Frame sent to browser
    // ✅ No database update
    // ✅ No retry logic
}
```

**Timing:** Starts ~10 seconds after NotificationOutboxPoller sets [L1] to PUBLISHED

**Database State AFTER:**
- `notification_outbox` table [L1]: 3 rows still PUBLISHED (no update)
- `notification_channel_send` table [L2]: **0 rows** (unchanged)

**WebSocket State:**
- STOMP frames delivered to 3 staff browsers in real-time
- Client-side JavaScript receives the message
- UI updates immediately

---

## ⏱️ Timeline (WEBSOCKET - DIRECT PATTERN)

| **T** | **Evento** | **Componente** | **DB State** |
|-------|-----------|----------------|--------------|
| T+0ms | POST /ask | Controller | - |
| T+10ms | save(Reservation) | Service → DB | `reservation` (+1) |
| T+20ms | INSERT event_outbox [L0] | Service → DB | `event_outbox` (+1, PENDING) |
| T+30ms | **HTTP 200 OK** ← | Controller ← | ✅ **Event guaranteed** |
| T+5030ms | EventOutboxPoller select | Poller | - |
| T+5050ms | Pubblica a RabbitMQ | Poller → RabbitMQ | RabbitMQ receives |
| T+5080ms | @RabbitListener triggered | Listener | - |
| T+5100ms | INSERT notification_restaurant (×3) | Listener → DB | `notification_restaurant` (+3) |
| T+5120ms | INSERT notification_outbox [L1] (×3) | Listener → DB | `notification_outbox` (+3, PENDING) |
| T+10030ms | NotificationOutboxPoller select | Poller | - |
| T+10050ms | **UPDATE notification_outbox [L1]** | Poller → DB | `notification_outbox` (3x PUBLISHED) |
| T+10070ms | **SKIP [L2] creation** | Poller | ❌ No `notification_channel_send` |
| T+15030ms | ChannelPoller select | Poller | - |
| T+15050ms | SELECT notification_restaurant | Poller → DB | - |
| T+15070ms | **sendWebSocketDirect()** | ChannelPoller → WebSocket | ✅ IN-MEMORY |
| T+15080ms | **convertAndSendToUser()** | WebSocket | - |
| T+15090ms | **STOMP Frame delivered** | WebSocket → Browser | ✅ **Real-time, best-effort** |
| T+15100ms | **onMessage() triggered** | Browser JS | 📱 **UI Updated!** |

---

## 🎯 Confronto: EMAIL (2 Livelli L1+L2) vs WEBSOCKET (Diretto)

### EMAIL Pattern:
```
event_outbox [L0] → notification_outbox [L1] → notification_channel_send [L2] → SEND via SMTP
                                              ↑ Persisted, trackable, with retries
```

### WEBSOCKET Pattern:
```
event_outbox [L0] → notification_outbox [L1] → [DIRECT SEND] → STOMP → Browser
                                              ↑ No [L2], no tracking, no retry
```

### Perché?

| Aspetto | EMAIL | WEBSOCKET |
|---------|-------|-----------|
| **External System** | SMTP (può fallire, lento) | WebSocket (in-memory, veloce) |
| **Delivery Guarantee** | At-least-once (retry) | Best-effort (no retry) |
| **Persistence** | ✅ [L2] entry saved | ❌ No persistence |
| **Tracking** | ✅ is_sent, attempt_count | ❌ Just log |
| **Latency** | ~15 secondi | ~100-200ms (real-time) |
| **Use Case** | Critico, audit-safe | Real-time, informativo |

---

## 🧪 Testing WebSocket Flow (DIRECT)

```bash
# 1. Create reservation
curl -X POST http://localhost:8080/customer/reservation/ask \
  -H "Content-Type: application/json" \
  -d '{"customerId": 5, "restaurantId": 10, "partySize": 4, "slotTime": "2025-11-14T20:00:00"}'

# Response: 200 OK
# DB Check: SELECT * FROM notification_restaurant; → 3 rows
# DB Check: SELECT * FROM notification_outbox; → 3 rows (status=PENDING)

# 2. Wait ~10 seconds for ChannelPoller

# DB Check: SELECT * FROM notification_outbox; → 3 rows (status=PUBLISHED)
# DB Check: SELECT * FROM notification_channel_send; → 0 rows (WebSocket skips L2)

# 3. Connect WebSocket client (Browser or CLI)
wscat -c ws://localhost:8080/ws

# 4. Subscribe to queue
SUBSCRIBE
destination:/user/queue/notifications
id:0
^@

# 5. Receive WebSocket message (should arrive within ~200ms of ChannelPoller cycle)
MESSAGE
destination:/user/queue/notifications
message-id:xyz
content-length:150

{"notificationId": 1000, "title": "📱 Nuova prenotazione", "body": "Per 4 persone", ...}
^@
```

---

**Ultimo aggiornamento:** 14 Novembre 2025  
**Autore:** Engineering Team
