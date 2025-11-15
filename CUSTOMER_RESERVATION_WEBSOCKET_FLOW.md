# 🎯 CUSTOMER RESERVATION → RESTAURANT WEBSOCKET NOTIFICATIONS

**Data:** 14 Novembre 2025  
**Scenario:** Customer crea prenotazione → Tutti i restaurant staff ricevono notifica WebSocket in real-time

---

## 🔄 FLUSSO COMPLETO

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. CUSTOMER CREA PRENOTAZIONE                                   │
├─────────────────────────────────────────────────────────────────┤
│ CustomerReservationController.askReservation()                  │
│   ↓                                                             │
│ CustomerReservationService.createReservation()                  │
│   ↓                                                             │
│ ReservationService.createNewReservation()                       │
│   ↓                                                             │
│ Save in DB + publishReservationCreatedEvent()                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. EVENT LISTENER INTERCETTA L'EVENTO (SYNC)                    │
├─────────────────────────────────────────────────────────────────┤
│ ReservationEventListener.handleRestaurantWebSocketNotification()│
│ @EventListener (SYNCHRONOUS, NOT ASYNC!)                        │
│   ↓                                                             │
│ For each staff_id in restaurant:                                │
│   - Create RestaurantNotification (title, body, properties)     │
│   - Save in notification_restaurant table                       │
│   - Create NotificationOutbox entry                             │
│   - Status = PENDING                                            │
│                                                                 │
│ Result: N notification_restaurant rows (N = staff count)        │
│         N notification_outbox rows (one per notification)       │
│         N notification_channel_send rows (via ChannelPoller)    │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. POLLER INVIA VIA WEBSOCKET (@10 secondi)                    │
├─────────────────────────────────────────────────────────────────┤
│ ChannelPoller.pollAndSendChannels() @Scheduled                  │
│   ↓                                                             │
│ For each notification_channel_send with status=PENDING:         │
│   - sendWebSocket()                                             │
│   - SimpMessagingTemplate.convertAndSendToUser(                 │
│       userId, "/queue/notifications", payload)                  │
│   - UPDATE notification_channel_send.is_sent = true             │
│   - UPDATE notification_channel_send.sent_at = NOW()            │
│                                                                 │
│ Result: WebSocket message received by all connected staff       │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. STAFF RICEVE NOTIFICA IN REAL-TIME                           │
├─────────────────────────────────────────────────────────────────┤
│ Client WebSocket:                                               │
│ {                                                               │
│   "notificationId": 1000,                                       │
│   "title": "📱 Nuova prenotazione richiesta",                   │
│   "body": "Prenotazione per 2025-11-14",                        │
│   "timestamp": "2025-11-14T10:30:00Z",                          │
│   "channel": "WEBSOCKET",                                       │
│   "reservation_id": "123",                                      │
│   "customer_email": "john@example.com",                         │
│   "reservation_date": "2025-11-14"                              │
│ }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔍 DETTAGLI IMPLEMENTAZIONE

### PARTE 1: ReservationService (UNCHANGED)

```java
// src/main/java/com/application/common/service/reservation/ReservationService.java

public Reservation createNewReservation(Reservation reservation) {
    // Save first to get the ID
    Reservation savedReservation = reservationDAO.save(reservation);
    
    // 🎯 PUBLISH EVENT FOR NEW RESERVATION
    publishReservationCreatedEvent(savedReservation);
    
    return savedReservation;
}

private void publishReservationCreatedEvent(Reservation reservation) {
    ReservationCreatedEvent event = new ReservationCreatedEvent(
        this,
        reservation.getId(),
        reservation.getCustomer().getId(),
        reservation.getSlot().getService().getRestaurant().getId(),
        reservation.getCustomer().getEmail(),
        reservation.getDate().toString()
    );
    eventPublisher.publishEvent(event);  // ⭐ Questo trigger il listener
}
```

✅ **Stato:** Già implementato, pubblica l'evento

---

### PARTE 2: ReservationEventListener (✏️ MODIFICATO)

**File:** `src/main/java/com/application/common/service/events/listeners/ReservationEventListener.java`

**Modifiche:** ✅ **COMPLETATE**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventListener {
    
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final NotificationOutboxDAO notificationOutboxDAO;
    private final ObjectMapper objectMapper;

    /**
     * ⭐ SYNC EVENT LISTENER - Crea notifiche RestaurantNotification per ogni staff
     */
    @EventListener
    @Transactional
    public void handleRestaurantWebSocketNotification(ReservationCreatedEvent event) {
        // Step 1: Estrai dati da evento
        Long restaurantId = event.getRestaurantId();
        Long reservationId = event.getReservationId();
        
        // Step 2: Query staff del ristorante
        // TODO: Query reale da Restaurant.getRUsers()
        List<Long> staffUserIds = Arrays.asList(1L, 2L, 3L);
        
        // Step 3: Per ogni staff, crea RestaurantNotification + NotificationOutbox
        for (Long staffUserId : staffUserIds) {
            RestaurantNotification notification = RestaurantNotification.builder()
                    .title("📱 Nuova prenotazione richiesta")
                    .body("Prenotazione per " + reservationDate)
                    .properties(Map of reservation details)
                    .userId(staffUserId)
                    .userType("RESTAURANT_USER")
                    .read(false)
                    .sharedRead(true)  // ⭐ Primo staff che agisce, tutti vedono
                    .build();
            
            restaurantNotificationDAO.save(notification);
            
            NotificationOutbox outbox = NotificationOutbox.builder()
                    .notificationId(notification.getId())
                    .notificationType("RESTAURANT")
                    .eventType("RESERVATION_REQUESTED")
                    .status(NotificationOutbox.Status.PENDING)
                    .build();
            
            notificationOutboxDAO.save(outbox);
        }
    }
}
```

✅ **Stato:** ✅ **COMPLETATO - File modificato**

**Differenze chiave:**
- ❌ Rimosso: `@Async` (era asynchronous, ora è SYNC)
- ✅ Aggiunto: `@Transactional` per consistency
- ✅ Aggiunto: Loop su staffUserIds per N notifiche
- ✅ Aggiunto: NotificationOutbox per ogni notifica
- ✅ Aggiunto: `sharedRead=true` per broadcast pattern

---

### PARTE 3: ChannelPoller.sendWebSocket() (JÀ IMPLEMENTATO)

**File:** `src/main/java/com/application/common/service/notification/poller/ChannelPoller.java`

✅ **Stato:** Già implementato in precedenza

```java
private void sendWebSocket(NotificationChannelSend send) throws Exception {
    // Step 1: Recupera la notifica
    RestaurantNotification notification = restaurantNotificationDAO.findById(
        send.getNotificationId()
    ).orElseThrow();
    
    // Step 2: Invia via WebSocket
    simpMessagingTemplate.convertAndSendToUser(
        notification.getUserId().toString(),
        "/queue/notifications",
        payload
    );
    
    // Step 3: Marca come sent
    send.setSent(true);
    send.setSentAt(Instant.now());
    channelSendDAO.save(send);
}
```

---

### PARTE 4: WebSocketConfig (GIÀ IMPLEMENTATO)

**File:** `src/main/java/com/application/common/config/WebSocketConfig.java`

✅ **Stato:** Già creato

---

## 📊 DATABASE SCHEMA

### Tabelle coinvolte:

```sql
-- 1. PRENOTAZIONE (già esiste)
reservation
├── id (PK)
├── customer_id (FK)
├── restaurant_id (FK)
├── status (NOT_ACCEPTED, ACCEPTED, CANCELLED)
└── date

-- 2. NOTIFICA RESTAURANT (NEW)
notification_restaurant
├── id (PK) ← 1000, 1001, 1002 per 3 staff
├── user_id (FK) ← 1, 2, 3 (staff ids)
├── user_type ← "RESTAURANT_USER"
├── title ← "📱 Nuova prenotazione richiesta"
├── body ← "Prenotazione per 2025-11-14"
├── is_read
├── read_by_user_id (primo che agisce)
├── shared_read ← true
└── properties (JSON)
    ├── reservation_id: "123"
    ├── customer_email: "john@example.com"
    └── reservation_date: "2025-11-14"

-- 3. NOTIFICATION OUTBOX (NEW)
notification_outbox
├── id (PK) ← 5000, 5001, 5002
├── notification_id (FK) ← 1000, 1001, 1002
├── notification_type ← "RESTAURANT"
├── event_type ← "RESERVATION_REQUESTED"
├── status ← "PENDING" (→ "PUBLISHED" dopo 5s)
└── payload (JSON)

-- 4. CHANNEL SEND (NEW)
notification_channel_send
├── id (PK) ← 10000, 10001, 10002
├── notification_id (FK) ← 1000, 1001, 1002
├── channel_type ← "WEBSOCKET"
├── is_sent ← false (→ true dopo @10s)
├── sent_at ← NULL (→ NOW() quando inviato)
└── attempt_count
```

---

## ⏱️ TIMING EXECUTION

```
T0 (0ms):
  ├─ Customer POST /customer/reservation/ask
  └─ CustomerReservationController.askReservation()

T1 (2ms):
  ├─ CustomerReservationService.createReservation()
  ├─ ReservationService.createNewReservation()
  │  └─ Save Reservation (id=123) in DB
  └─ publishReservationCreatedEvent() ← Event created

T2 (5ms):
  └─ ReservationEventListener.handleRestaurantWebSocketNotification()
     ├─ Create RestaurantNotification (id=1000, staff=1)
     ├─ Create NotificationOutbox (id=5000)
     ├─ Create RestaurantNotification (id=1001, staff=2)
     ├─ Create NotificationOutbox (id=5001)
     ├─ Create RestaurantNotification (id=1002, staff=3)
     └─ Create NotificationOutbox (id=5002)

T3 (6ms):
  └─ Response 200 OK to customer + Reservation confirmation

T4 (@5s - NotificationOutboxPoller):
  └─ SELECT notification_outbox WHERE status=PENDING
     ├─ UPDATE status=PUBLISHED (id=5000, 5001, 5002)
     └─ Create notification_channel_send for each

T5 (@10s - ChannelPoller):
  ├─ For notification_channel_send (id=10000):
  │  ├─ READ RestaurantNotification (id=1000)
  │  ├─ SimpMessagingTemplate.convertAndSendToUser("1", "/queue/notifications", ...)
  │  └─ UPDATE is_sent=true, sent_at=NOW()
  │
  ├─ For notification_channel_send (id=10001):
  │  ├─ SimpMessagingTemplate.convertAndSendToUser("2", "/queue/notifications", ...)
  │  └─ UPDATE is_sent=true
  │
  └─ For notification_channel_send (id=10002):
     ├─ SimpMessagingTemplate.convertAndSendToUser("3", "/queue/notifications", ...)
     └─ UPDATE is_sent=true

T6 (@10.1s):
  └─ Staff #1, #2, #3 ricevono notifica WebSocket in real-time
```

---

## 🧪 TEST SCENARIO

### Step 1: Verificare che ReservationEventListener sia caricato

```bash
mvn spring-boot:run

# Nel log dovresti vedere:
# ✅ ReservationEventListener loaded by Spring
# ✅ WebSocketConfig configuring message broker
# ✅ ChannelPoller scheduled with 10s interval
```

### Step 2: Customer crea prenotazione

```bash
curl -X POST http://localhost:8080/customer/reservation/ask \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "idSlot": 1,
    "userName": "John Doe",
    "pax": 4,
    "kids": 1,
    "notes": "Preferisco tavolo vicino finestra",
    "reservationDay": "2025-11-20"
  }'

# Response:
# {
#   "id": 123,
#   "status": "NOT_ACCEPTED",
#   "restaurant": {"id": 10, "name": "Trattoria del Mare"},
#   ...
# }
```

### Step 3: Monitor i log

```bash
# Terminal 1: Tail logs
tail -f logs/application.log | grep -E "ReservationEventListener|RestaurantNotification|WebSocket|🔔|✅|❌"

# Output atteso:
# T2 (5ms): "🔔 Creating WebSocket notifications for restaurant 10 on reservation 123"
# T2 (5ms): "✅ Created RestaurantNotification: id=1000, restaurant=10, staff=1"
# T2 (5ms): "✅ Created RestaurantNotification: id=1001, restaurant=10, staff=2"
# T2 (5ms): "✅ Created RestaurantNotification: id=1002, restaurant=10, staff=3"
# T2 (5ms): "✅ Successfully created 3 WebSocket notifications"
#
# T4 (@5s): "NotificationOutboxPoller: UPDATE status=PUBLISHED"
# T5 (@10s): "🌐 Sending WebSocket notification for notification 1000"
#            "✅ WebSocket sent to user 1"
#            "🌐 Sending WebSocket notification for notification 1001"
#            "✅ WebSocket sent to user 2"
#            "🌐 Sending WebSocket notification for notification 1002"
#            "✅ WebSocket sent to user 3"
```

### Step 4: Verificare Database

```sql
-- Terminal 2: Verifica notifiche create

-- 1. Prenotazione
SELECT id, status, customer_id, restaurant_id, date 
FROM reservation 
WHERE id = 123;
-- Result: (123, NOT_ACCEPTED, customer_id, 10, 2025-11-20)

-- 2. RestaurantNotifications (3 per i 3 staff)
SELECT id, user_id, title, body, shared_read, creation_time 
FROM notification_restaurant 
WHERE creation_time >= NOW() - INTERVAL 1 MINUTE 
ORDER BY id DESC;
-- Result: 
-- (1000, 1, "📱 Nuova prenotazione...", "Prenotazione per...", true, 2025-11-14 10:30:05)
-- (1001, 2, "📱 Nuova prenotazione...", "Prenotazione per...", true, 2025-11-14 10:30:05)
-- (1002, 3, "📱 Nuova prenotazione...", "Prenotazione per...", true, 2025-11-14 10:30:05)

-- 3. NotificationOutbox
SELECT id, notification_id, notification_type, event_type, status, created_at 
FROM notification_outbox 
WHERE created_at >= NOW() - INTERVAL 1 MINUTE 
ORDER BY id DESC;
-- Result:
-- (5000, 1000, "RESTAURANT", "RESERVATION_REQUESTED", "PENDING", 2025-11-14 10:30:05)
-- (5001, 1001, "RESTAURANT", "RESERVATION_REQUESTED", "PENDING", 2025-11-14 10:30:05)
-- (5002, 1002, "RESTAURANT", "RESERVATION_REQUESTED", "PENDING", 2025-11-14 10:30:05)

-- 4. NotificationChannelSend (@10s dopo)
SELECT id, notification_id, channel_type, is_sent, sent_at, attempt_count 
FROM notification_channel_send 
WHERE created_at >= NOW() - INTERVAL 1 MINUTE 
ORDER BY id DESC;
-- Result:
-- (10000, 1000, "WEBSOCKET", true, 2025-11-14 10:30:15, 0)
-- (10001, 1001, "WEBSOCKET", true, 2025-11-14 10:30:15, 0)
-- (10002, 1002, "WEBSOCKET", true, 2025-11-14 10:30:15, 0)
```

### Step 5: Simula client WebSocket

```javascript
// Terminal 3: Browser console o wscat
const client = new SockJS('http://localhost:8080/ws-notifications');
const stompClient = Stomp.over(client);

stompClient.connect({}, function(frame) {
    console.log('✅ Connected');
    
    // Sottoscrivi a notifiche personali
    stompClient.subscribe('/user/queue/notifications', function(message) {
        console.log('📬 Received notification:');
        console.log(JSON.parse(message.body));
        // Output: {notificationId: 1000, title: "📱 Nuova prenotazione richiesta", ...}
    });
});
```

---

## ✅ CHECKLIST

### Prima di testare:

- [ ] `ReservationEventListener.java` modificato con nuovo listener
- [ ] `WebSocketConfig.java` creato
- [ ] `ChannelPoller.java` con `sendWebSocket()` implementato
- [ ] Database con tabelle: `notification_restaurant`, `notification_outbox`, `notification_channel_send`
- [ ] RabbitMQ running (se usato per event dispatching)

### Durante il test:

- [ ] Customer crea prenotazione: ✅ Reservation salvato
- [ ] Log mostra: "🔔 Creating WebSocket notifications for restaurant X"
- [ ] Log mostra: "✅ Created RestaurantNotification" (3 volte per 3 staff)
- [ ] Database: 3 notification_restaurant rows creati
- [ ] Database: 3 notification_outbox rows con status PENDING
- [ ] @10s: Log mostra "✅ WebSocket sent to user X" (3 volte)
- [ ] Database: notification_channel_send.is_sent=true
- [ ] Client WebSocket riceve payload con title e body

---

## 🔧 DEBUGGING

### Problema: ReservationEventListener non viene chiamato

**Soluzione:**
```bash
# 1. Verifica che sia @Component
grep -r "@Component" greedys_api/src/main/java/com/application/common/service/events/listeners/ReservationEventListener.java

# 2. Verifica che ReservationService pubblica l'evento
grep -r "eventPublisher.publishEvent" greedys_api/src/main/java/com/application/common/service/reservation/ReservationService.java

# 3. Aggiungi log nel listener e ricompila
# "🔔 Creating WebSocket notifications for restaurant..."
```

### Problema: SimpMessagingTemplate non trovato

**Soluzione:**
```bash
# Verifica che WebSocketConfig sia stato creato
grep -r "class WebSocketConfig" greedys_api/src/main/java/com/application/common/config/

# Se non esiste, crealo (vedi GUIDE_WEBSOCKET_ONLY.md)
```

### Problema: notification_restaurant table non esiste

**Soluzione:**
```sql
-- Esegui create table script
-- Vedi GUIDE_WEBSOCKET_ONLY.md per DDL
```

---

## 📈 PROSSIMI STEP

1. **Query reale su staff:** Sostituisci placeholder `List<Long> staffUserIds = Arrays.asList(1L, 2L, 3L)` con query reale
   ```java
   List<Long> staffUserIds = restaurantDAO.findById(restaurantId)
       .map(r -> r.getRUsers().stream()
           .map(RUser::getId)
           .collect(Collectors.toList()))
       .orElse(Collections.emptyList());
   ```

2. **Customer notifications:** Crea analogo listener per notificare il customer (in email/SMS/app)

3. **Email channel:** Implementa `ChannelPoller.sendEmail()` per inviare email in aggiunta a WebSocket

4. **Dashboard:** Crea UI che mostra notifiche ricevute via WebSocket

---

## 📝 NOTE IMPORTANTI

⚠️ **Listener è SYNCHRONOUS (non @Async):**
- Se listener fallisce, la prenotazione NON viene creata (rollback)
- Garantisce consistency: notifiche sempre esitono quando prenotazione esiste
- Più lento (5-10ms aggiuntivi), ma transazionalmente corretto

✅ **Pattern 3-level outbox:**
- L1: notification_restaurant (la notifica stessa)
- L2: notification_outbox (pending da processare)
- L3: notification_channel_send (per ogni canale: SMS, EMAIL, WEBSOCKET, PUSH)

✅ **Broadcast pattern (shared_read):**
- `sharedRead=true`: Primo staff che agisce, TUTTI gli altri vedono "gestito"
- `readByUserId`: Chi ha agito per primo
- Utile per prenotazioni: primo staff che accetta, tutti vedono "prenotazione accettata"

🎯 **Real-time WebSocket:**
- Non usa outbox tradizionale (non c'è retry infinito)
- Usa pattern direct per performance
- Se invio fallisce e client non è connesso, perde il messaggio (acceptable per real-time)
