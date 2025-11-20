# ⭐ ARCHITETTURA COMPLETA: EventOutbox → RabbitMQ → Notification Models

## 🔄 IL FLUSSO COMPLETO

```
┌─────────────────────────────────────────────────────────────────────┐
│ 1. DOMAIN EVENT (creato durante business logic)                     │
│    Es: ReservationRequestedEvent(customerId, restaurantId, ...)    │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2. EVENT OUTBOX (salvato nel DB della transazione)                 │
│    Table: event_outbox                                             │
│    Columns:                                                        │
│      - event_id (PK)                                               │
│      - aggregate_id (es: customerId = 100)                         │
│      - aggregate_type (es: "CUSTOMER") ◄─── WHO ACTED              │
│      - event_type (es: "RESERVATION_REQUESTED")                    │
│      - payload (JSON con tutti dati evento)                        │
│      - created_at                                                  │
│                                                                    │
│    ✅ ATOMICO: Salvato STESSO COMMIT di reservation creata         │
│    ✅ GARANTITO: Se transazione commit → evento guaranteed          │
│    ✅ PERSISTENTE: Kafka/RabbitMQ offline? Nessun problema          │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 3. OUTBOX POLLER (scheduled job)                                   │
│    - Poll event_outbox ogni 5 secondi                              │
│    - Leggi N rows (limit=100)                                      │
│    - Per ogni row: pubblica su RabbitMQ queue                      │
│    - Update: event_outbox.published_at = NOW()                     │
│                                                                    │
│    QUEUE ROUTING (in base a aggregateType):                        │
│    - aggregate_type="CUSTOMER" → queue: notification.customer      │
│    - aggregate_type="RESTAURANT_USER" → queue: notification.restaurant
│    - aggregate_type="AGENCY_USER" → queue: notification.agency     │
│    - aggregate_type="ADMIN" → queue: notification.admin            │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 4. RABBITMQ QUEUES (message broker)                                │
│                                                                    │
│    Queue: notification.customer                                    │
│    ├─ Message 1: {aggregate_type: "CUSTOMER", event_type: ...}     │
│    ├─ Message 2: {aggregate_type: "CUSTOMER", event_type: ...}     │
│    └─ ...                                                          │
│                                                                    │
│    Queue: notification.restaurant                                  │
│    ├─ Message 1: {aggregate_type: "RESTAURANT_USER", event_type...}│
│    ├─ Message 2: {aggregate_type: "CUSTOMER", event_type: ...}     │
│    └─ ...                                                          │
│                                                                    │
│    (Simile per notification.agency, notification.admin)            │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 5. @RabbitListener SERVICES (4 listener per queue)                 │
│                                                                    │
│    @RabbitListener(queues = "notification.customer")              │
│    void onCustomerNotificationMessage(Map<String, Object> msg) {  │
│        // Riceve message da queue                                 │
│        // Legge: event_type, aggregate_type, payload              │
│        // DISAGGREGA per recipient × channel                       │
│        // Crea N CustomerNotification rows                        │
│    }                                                              │
│                                                                    │
│    Simile per:                                                     │
│    - @RabbitListener(queues = "notification.restaurant")          │
│    - @RabbitListener(queues = "notification.agency")              │
│    - @RabbitListener(queues = "notification.admin")               │
│                                                                    │
│    ✅ DISAGGREGAZIONE:                                             │
│       Input: 1 message su RabbitMQ                                 │
│       Output: N NotificationModel rows nel DB                      │
│               (per recipient × channel)                            │
│                                                                    │
│       Es: RESERVATION_REQUESTED                                    │
│       - Recipients: 10 restaurant staff                            │
│       - Channels per staff: [WEBSOCKET, EMAIL]                     │
│       → Crea: 10 × 2 = 20 RestaurantUserNotification rows         │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 6. NOTIFICATION MODELS (Database)                                  │
│                                                                    │
│    Table: restaurant_user_notification                             │
│    Columns:                                                        │
│      - id (PK)                                                    │
│      - event_id (UNIQUE) ◄─── PER IDEMPOTENCY                     │
│      - user_id (restaurant staff)                                 │
│      - restaurant_id ◄─── PER BATCH OPERATIONS                    │
│      - channel (WEBSOCKET, EMAIL, PUSH, SMS)                       │
│      - status (PENDING, DELIVERED, FAILED, READ)                  │
│      - read_by_all (true/false) ◄─── PER SHARED READ             │
│      - title, body, properties                                    │
│      - created_at, updated_at, read_at                            │
│                                                                    │
│    Table: customer_notification                                    │
│    Columns: (simile, ma NO restaurantId, NO readByAll)            │
│      - id, event_id (UNIQUE), user_id, channel, status            │
│      - title, body, properties                                    │
│      - created_at, updated_at, read_at                            │
│                                                                    │
│    Table: agency_user_notification                                │
│    Columns: (come restaurant_user_notification, con agencyId)     │
│                                                                    │
│    Table: admin_notification                                      │
│    Columns: (simile, NO readByAll, individual only)               │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 7. CHANNEL POLLER (scheduled job)                                  │
│    - Poll notification_* tables per CHANNEL                        │
│    - Query: SELECT * WHERE status='PENDING' AND channel='EMAIL'   │
│    - Per ogni row: invia via channel (Email, Push, SMS, etc)       │
│    - Update: status='DELIVERED' (o FAILED)                         │
│                                                                    │
│    ⚠️ IMPORTANTE:                                                  │
│    - WebSocket: IMMEDIATO (subscriber connesso? sì → send)        │
│    - Email/Push/SMS: RITENTATO se fallisce (exponential backoff)  │
│    - Fallito 3 volte? → mark as FAILED, alert admin               │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 8. USER READS NOTIFICATION (UI/WebSocket)                          │
│                                                                    │
│    User vede notifica → clicca → app invia WebSocket message:     │
│    {action: "read_notification", notification_id: 123}            │
│                                                                    │
│    WebSocket Handler:                                              │
│    1. UPDATE notification_* SET status='READ', read_at=NOW()       │
│    2. Se read_by_all=true:                                         │
│       → UPDATE ALL other rows con SAME eventId + restaurantId     │
│       → Tutti gli staff vedono LETTO subito                       │
│    3. Broadcast WebSocket message:                                 │
│       {notification_id: 123, status: 'READ', readByUser: 'John'}  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## ❓ RISPOSTA ALLE TUE DOMANDE

### Q1: "Cosa è Notification?"

**Notification = Record nel database che rappresenta UNA disaggregazione (1 recipient + 1 channel)**

Tipi:
- `RestaurantUserNotification` → per staff ristorante
- `CustomerNotification` → per customer
- `AgencyUserNotification` → per staff agency
- `AdminNotification` → per admin

**Ogni Notification è SEPARATA per:**
- **Recipient** (es: John Doe, staff ID 50)
- **Channel** (WEBSOCKET, EMAIL, PUSH, SMS)

**Esempio ReservationRequested:**
```
EventOutbox pubblica:
  {event_type: "RESERVATION_REQUESTED", restaurant_id: 5, ...}

RabbitListener disaggrega:
  Restaurant 5 ha 10 staff
  Ogni staff preferisce: WEBSOCKET + EMAIL

Crea 20 RestaurantUserNotification rows:
  ├─ Row 1: userId=50, channel=WEBSOCKET, status=PENDING
  ├─ Row 2: userId=50, channel=EMAIL, status=PENDING
  ├─ Row 3: userId=51, channel=WEBSOCKET, status=PENDING
  ├─ Row 4: userId=51, channel=EMAIL, status=PENDING
  └─ ... (altri 16 rows)
```

---

### Q2: "Viene creata come EventOutbox? O solo per registrare notifiche da WebSocket?"

**RISPOSTA: NO, è SEPARATO da EventOutbox**

#### EventOutbox:
- **Cosa**: Message per Event (ReservationRequested, CustomerRegistered, etc)
- **Quando**: Durante transazione business (es: customer crea prenotazione)
- **Scopo**: Garantire event è salvato + pubblicato (outbox pattern)
- **Tabella**: `event_outbox`
- **Persistenza**: ✅ SEMPRE persistita nel DB
- **Durabilità**: Se RabbitMQ offline → EventOutbox attende, poller riprova

#### Notification:
- **Cosa**: Disaggregazione di EventOutbox message (1 recipient + 1 channel)
- **Quando**: DOPO RabbitListener riceve message da coda
- **Scopo**: Registrare consegna per tracking (delivery status, read status, etc)
- **Tabelle**: `restaurant_user_notification`, `customer_notification`, `agency_user_notification`, `admin_notification`
- **Persistenza**: ✅ SEMPRE persistita nel DB (per audit, read status, retry)
- **Durabilità**: Se WebSocket fallisce → Notification rimane PENDING → ChannelPoller riprova

---

### Q3: "Non da WebSocket, diciamo?"

**CHIARIMENTO: Notification è INDIPENDENTE da WebSocket**

#### WebSocket:
- **Channel per DELIVERY IMMEDIATO**
- Se user è online → invia subito
- Se user offline → fallisce, status=FAILED

#### Notification record:
- **Sempre salvato nel DB**, che WebSocket riesca o no
- Tracking di COSA è stato tentato inviare e QUANDO
- Se WebSocket fallisce → ChannelPoller riprova con EMAIL/Push/SMS

**Flusso con WebSocket:**

```
1. RabbitListener riceve message
2. Crea Notification row con status=PENDING, channel=WEBSOCKET
3. Salva nel DB

4. ChannelPoller (5 sec interval):
   - Legge: SELECT * WHERE channel='WEBSOCKET' AND status='PENDING'
   - Tenta WebSocket send
   
5a. Se user ONLINE:
    - WebSocket send OK
    - UPDATE status='DELIVERED'
    - Se user legge → UPDATE status='READ' + shared read logic

5b. Se user OFFLINE:
    - WebSocket send FALLISCE
    - UPDATE status='FAILED'
    - Notification rimane nel DB
    - ChannelPoller ritenta con EMAIL/PUSH/SMS
    - UPDATE channel='EMAIL', status='PENDING'
    - Invia email quando user online

6. Al login:
   - App query: SELECT * FROM notification WHERE user_id=? AND status IN ('DELIVERED', 'PENDING')
   - Mostra in notification center
   - User clicca "read"
   - WebSocket send read status
   - Handler UPDATE status='READ'
   - Se read_by_all=true: UPDATE ALL other staff con same eventId
```

---

## 🎯 SUMMARY

| Aspetto | EventOutbox | Notification Models |
|---------|-------------|-------------------|
| **Cos'è** | Message di evento | Disaggregazione per recipient + channel |
| **Creato da** | Business service (Customer, Admin) | RabbitListener |
| **Tabella** | `event_outbox` | `restaurant_user_notification`, `customer_notification`, etc |
| **Quando** | Durante transazione (reservation, order) | Dopo RabbitMQ message ricevuto |
| **Scopo** | Publish event garantito | Delivery tracking + read status |
| **Recipients** | NON SPECIFICATI (generic event) | ✅ SPECIFICATI (user_id + channel) |
| **WebSocket** | Usato da EventOutboxPoller per pub | Usato da ChannelPoller per delivery |
| **Persistenza** | ✅ SEMPRE (outbox pattern) | ✅ SEMPRE (delivery tracking) |
| **Retry** | Poller riprova se publish fallisce | ChannelPoller riprova se delivery fallisce |

---

## 📝 ESEMPIO COMPLETO: ReservationRequested

```
STEP 1: Customer crea prenotazione
─────────────────────────────────
→ Service: CustomerReservationService.createReservation(customerId, restaurantId, ...)
  ├─ INSERT Reservation table
  ├─ INSERT event_outbox (aggregate_type=CUSTOMER)
  └─ COMMIT ✅

STEP 2: EventOutboxPoller (ogni 5 sec)
──────────────────────────────────────
→ SELECT * FROM event_outbox WHERE published_at IS NULL LIMIT 100
→ Per ogni row:
  ├─ Pubblica su RabbitMQ
  │  ├─ Legge aggregate_type="CUSTOMER"
  │  └─ Pubblica su queue: notification.restaurant
  └─ UPDATE event_outbox SET published_at=NOW()

STEP 3: RabbitListener riceve message
──────────────────────────────────────
→ @RabbitListener(queues="notification.restaurant")
→ Riceve message:
   {
     aggregate_type: "CUSTOMER",
     event_type: "RESERVATION_REQUESTED",
     payload: {customerId: 100, restaurantId: 5, ...}
   }

→ Logica:
  ├─ restaurantId=5 ha 10 staff
  ├─ Legge settings: eventType="RESERVATION_REQUESTED" → broadcast
  ├─ Per ogni staff × per enabled channels:
  │  ├─ Crea RestaurantUserNotification row
  │  └─ status=PENDING, channel=WEBSOCKET/EMAIL/PUSH
  └─ COMMIT ✅ (20 rows create)

STEP 4: ChannelPoller (ogni 10 sec)
────────────────────────────────────
→ Per channel='WEBSOCKET':
   ├─ SELECT * FROM restaurant_user_notification 
        WHERE channel='WEBSOCKET' AND status='PENDING' LIMIT 100
   ├─ Per ogni row:
   │  ├─ Tenta WebSocket send
   │  └─ UPDATE status='DELIVERED'
   └─ COMMIT ✅

→ Per channel='EMAIL':
   ├─ SELECT * FROM restaurant_user_notification 
        WHERE channel='EMAIL' AND status='PENDING' LIMIT 100
   ├─ Per ogni row:
   │  ├─ Invia Email
   │  └─ UPDATE status='DELIVERED'
   └─ COMMIT ✅

STEP 5: Staff legge notifica
────────────────────────────
→ Browser riceve WebSocket notification
→ Staff clicca "leggi"
→ WebSocket handler:
  ├─ UPDATE notification SET status='READ', read_at=NOW()
  ├─ Se read_by_all=true:
  │  └─ UPDATE ALL staff rows: status='READ'
  └─ COMMIT ✅

STEP 6: Broadcast agli altri staff
───────────────────────────────────
→ WebSocket broadcast:
  {
    notification_id: 123,
    status: "READ",
    read_by_user: "John (Staff ID 50)",
    eventId: "RESERVATION_REQUESTED_RES-123_2025-01-20T10:30"
  }

→ Gli altri 9 staff vedono nel notification center:
   "✅ Leggo da John" (oppure badge scompare subito)
```

---

## ⚡ KEY DIFFERENCES

```
❌ SBAGLIATO:
"Notification è come EventOutbox, creata quando WebSocket manda messaggio"

✅ CORRETTO:
"Notification è disaggregazione di EventOutbox, creata da RabbitListener"
"Notification è indipendente da WebSocket, è solo UNO dei channels possibili"
"Notification è SEMPRE salvata nel DB per tracking, che WebSocket riesca o no"
```

Comprensibile? Domande su questa architettura?
