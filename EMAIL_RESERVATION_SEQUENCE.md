# 📧 Sequence / Timeline: Customer Reservation → Restaurant Email Notifications

**Data:** 14 Novembre 2025

Questo file mostra il diagramma temporale (sequence) che descrive come le notifiche EMAIL utilizzano i **2 LIVELLI DI OUTBOX** (a differenza di WebSocket che è DIRETTO).

---

## 📊 Diagramma di Sequenza UML (ASCII Art)

```
Customer    Controller      Service      EventOutboxPoller    RabbitMQ      Listener     NotifOutboxPoller    ChannelPoller    Email Service
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
   |            |               |                  |  UPDATE status=PUBLISHED|    |                |                   |                |
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
   |            |               |                  |                |              |                |--SELECT notification_outbox[L1]  |                |
   |            |               |                  |                |              |                |  WHERE status=PENDING            |                |
   |            |               |                  |                |              |                |--UPDATE status=PUBLISHED         |                |
   |            |               |                  |                |              |                |--FOR EACH notif:                 |                |
   |            |               |                  |                |              |                |  INSERT notification_channel_send[L2]|             |
   |            |               |                  |                |              |                |                   |                |
   |            |               |                  |                |              |                |      [~10s]       |                |
   |            |               |                  |                |              |                |                   |--SELECT channel_send[L2]|
   |            |               |                  |                |              |                |                   |  WHERE is_sent=NULL    |
   |            |               |                  |                |              |                |                   |--FOR EACH channel:     |
   |            |               |                  |                |              |                |                   |  SELECT notification   |
   |            |               |                  |                |              |                |                   |  SELECT user email     |
   |            |               |                  |                |              |                |                   |  sendEmail()---------->|
   |            |               |                  |                |              |                |                   |<--email sent-----------|
   |            |               |                  |                |              |                |                   |--UPDATE is_sent=true   |
```

---

## 📝 Spiegazione del Flusso (3 LIVELLI DI OUTBOX)

### 1️⃣ **Fase: HTTP Request → Reservation Created + event_outbox [L0]** (Synchronous)

**Componenti:** Customer → Controller → Service → Repository

```
POST /customer/reservation/ask
├─ Service.createReservation(DTO, customerId)
│  ├─ service.save(Reservation)
│  │  └─ Database: INSERT INTO reservation (...)
│  │     └─ Reservation(id=123, status=NOT_ACCEPTED, restaurantId=10)
│  │
│  └─ ⭐ LEVEL 0: CREATE event_outbox (BEFORE publishing)
│     ├─ eventOutboxDAO.save(EventOutbox)
│     │  └─ INSERT event_outbox (event_id=UUID, status=PENDING)
│     │     └─ Persiste l'evento prima di qualsiasi pubblicazione
│     │
│     └─ publishEvent(ReservationCreatedEvent) → RabbitMQ (EventOutboxPoller lo farà)
│        └─ **NON triggera listener direttamente**
│
└─ HTTP 200 OK + ReservationDTO
   ✅ Guaranteed che event_outbox esiste in DB! (At-least-once delivery)
```

**Timing:** < 50ms (tutto synchronous)

**Database State AFTER:**
- `reservation` table: 1 row inserted (id=123)
- `event_outbox` table [L0]: 1 row inserted with status=PENDING (id=7000)

---

### 2️⃣ **Fase: Event Outbox Poller** (Asynchronous, ~5 seconds later)

**Componenti:** EventOutboxPoller → RabbitMQ

```java
@Scheduled(fixedDelay = 5000)  // Every 5 seconds
public void pollAndPublish() {
    // 1. SELECT PENDING events [L0]
    List<EventOutbox> pending = eventOutboxDAO.findByStatus("PENDING");
    // Result: [7000] (event_id=UUID, eventType=RESERVATION_REQUESTED)
    
    // 2. FOR EACH event
    for (EventOutbox event : pending) {
        try {
            // 2a. Pubblica a RabbitMQ (queue: event-stream)
            rabbitTemplate.convertAndSend("event-stream", event.getPayload());
            
            // 2b. UPDATE status = PUBLISHED
            event.setStatus("PUBLISHED");
            eventOutboxDAO.save(event);
            
            log.info("Event published to RabbitMQ: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventId());
            // Riprova al prossimo ciclo (nessun retry count qui, solo select again)
        }
    }
}
```

**Timing:** Starts ~5 seconds after reservation is created

**Database State AFTER:**
- `event_outbox` table [L0]: 1 row updated (status=PUBLISHED)

**RabbitMQ State:**
- Message published to queue `event-stream` (all 4 listeners listening)

---

### 3️⃣ **Fase: Event Listeners (4 in parallelo)** (Asynchronous - triggered by RabbitMQ)

**Componenti:** RestaurantNotificationListener + RabbitMQ → Database

```java
@RabbitListener(queues = "event-stream")  // Riceve da RabbitMQ
@Transactional
public void onEventReceived(String eventPayload) {
    // 1. Parse evento
    EventData event = objectMapper.readValue(eventPayload, EventData.class);
    String eventId = event.getEventId();  // UUID
    
    // 2. ⭐ IDEMPOTENCY CHECK sul L0
    if (eventOutboxDAO.existsByEventIdAndProcessedBy(eventId, "RESTAURANT_LISTENER")) {
        log.warn("Event already processed by RESTAURANT_LISTENER");
        return;
    }
    
    // 3. Query restaurant staff
    List<RestaurantUser> staff = restaurantUserDAO.findByRestaurantId(event.getRestaurantId());
    
    // 4. FOR EACH staff
    for (RestaurantUser user : staff) {
        // 4a. Create notification
        RestaurantNotification notif = new RestaurantNotification();
        notif.setUserId(user.getId());
        notif.setTitle("📧 Nuova prenotazione ricevuta");
        notif.setBody("Cliente ha richiesto una prenotazione");
        notificationRestaurantDAO.save(notif);
        // Result: id=1000, 1001, 1002
        
        // 4b. Create LEVEL 1 outbox (notification publishing)
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setNotificationId(notif.getId());
        outbox.setStatus("PENDING");
        notificationOutboxDAO.save(outbox);
        // Result: id=5000, 5001, 5002
    }
    
    // 5. Mark event as processed (idempotency)
    eventOutboxDAO.updateProcessedBy(eventId, "RESTAURANT_LISTENER", Instant.now());
    
    log.info("Event processed: created 3 notifications for restaurant");
}
```

**Timing:** Starts immediately after RabbitMQ receives message from EventOutboxPoller

**Database State AFTER:**
- `notification_restaurant` table: 3 rows inserted (id=1000, 1001, 1002)
- `notification_outbox` table [L1]: 3 rows inserted with status=PENDING (id=5000, 5001, 5002)
- `event_outbox` table [L0]: 1 row updated (processed_by='RESTAURANT_LISTENER')

---

### 4️⃣ **Fase: Notification Outbox Poller** (Asynchronous, ~5 seconds after listener)

**Componenti:** NotificationOutboxPoller → Database

```java
@Scheduled(fixedDelay = 5000)  // Every 5 seconds (starts after listener finishes)
public void pollAndProcessOutbox() {
    // 1. SELECT PENDING outbox records [L1]
    List<NotificationOutbox> pending = notificationOutboxDAO.findByStatus("PENDING");
    // Result: [5000, 5001, 5002]
    
    // 2. FOR EACH outbox
    for (NotificationOutbox outbox : pending) {
        // 2a. UPDATE to PUBLISHED (mark as "processed for channel sending")
        outbox.setStatus("PUBLISHED");
        notificationOutboxDAO.save(outbox);
        
        // 2b. ⭐ LEVEL 2: INSERT channel_send entries
        // Crea una riga per ogni canale PERSISTENTE (EMAIL, SMS, PUSH, SLACK)
        // WebSocket viene saltato (DIRECT - no persistence)
        
        // EMAIL channel
        NotificationChannelSend emailSend = new NotificationChannelSend();
        emailSend.setNotificationId(outbox.getNotificationId());
        emailSend.setChannelType("EMAIL");
        emailSend.setIsSent(null);  // NULL = pending
        notificationChannelSendDAO.save(emailSend);
        // Result: id=10000
        
        // SMS channel
        NotificationChannelSend smsSend = new NotificationChannelSend();
        smsSend.setNotificationId(outbox.getNotificationId());
        smsSend.setChannelType("SMS");
        smsSend.setIsSent(null);
        notificationChannelSendDAO.save(smsSend);
        // Result: id=10001
        
        // PUSH channel
        NotificationChannelSend pushSend = new NotificationChannelSend();
        pushSend.setNotificationId(outbox.getNotificationId());
        pushSend.setChannelType("PUSH");
        pushSend.setIsSent(null);
        notificationChannelSendDAO.save(pushSend);
        // Result: id=10002
    }
}
```

**Timing:** Starts ~5 seconds after listener creates notification_outbox [L1]

**Database State AFTER:**
- `notification_outbox` table [L1]: 3 rows updated (status=PUBLISHED)
- `notification_channel_send` table [L2]: 9 rows inserted
  - 3 rows for EMAIL
  - 3 rows for SMS
  - 3 rows for PUSH
  - 0 rows for WEBSOCKET (skipped - direct channel)

**⭐ KEY DIFFERENCE: 2 LEVELS OF OUTBOX**

| **Level** | **Tabella** | **Responsabilità** | **Timing** |
|-----------|-----------|-------------------|-----------|
| L1 | `notification_outbox` | Garantisce che evento → notifiche siano create | SYNCHRONOUS (Listener) |
| L2 | `notification_channel_send` | Garantisce che ogni canale sia tentato | ASYNCHRONOUS (OutboxPoller) |

---

### 5️⃣ **Fase: Channel Poller** (Asynchronous, ~10 seconds later)

**Componenti:** ChannelPoller → Email Service → SMTP Server

```java
@Scheduled(fixedDelay = 10000)  // Every 10 seconds
public void pollAndSendChannels() {
    // 1. SELECT unsent EMAIL channel records [L2]
    List<NotificationChannelSend> emailPending = 
        notificationChannelSendDAO.findByChannelTypeAndIsSentNull("EMAIL");
    // Result: [10000, 10001, 10002]
    
    // 2. FOR EACH unsent EMAIL notification
    for (NotificationChannelSend channelSend : emailPending) {
        try {
            // 2a. Get full notification details
            NotificationRestaurant notification = 
                notificationRestaurantDAO.findById(channelSend.getNotificationId()).orElse(null);
            
            if (notification != null) {
                // 2b. Get user email from userId
                RestaurantUser user = restaurantUserDAO.findById(notification.getUserId()).orElse(null);
                if (user != null && user.getEmail() != null) {
                    
                    // 2c. Prepare email
                    String subject = "📧 " + notification.getTitle();
                    String body = notification.getBody();
                    String to = user.getEmail();
                    
                    // 2d. Send via SMTP (PERSISTENT - can retry)
                    emailService.sendEmail(to, subject, body);
                    
                    // 2e. Mark as SENT
                    channelSendDAO.markAsSent(channelSend.getId(), Instant.now());
                    log.info("Email sent: user={}, notif={}", user.getId(), notification.getId());
                }
            }
            
        } catch (Exception e) {
            // ⭐ PERSISTENT CHANNELS: Retry Logic
            log.error("Email send failed: {}", e.getMessage());
            
            // Increment attempt count
            channelSendDAO.incrementAttempt(channelSend.getId(), Instant.now(), e.getMessage());
            
            // If max retries reached, mark as FAILED (not sent)
            if (channelSend.getAttemptCount() >= MAX_RETRIES) {
                channelSendDAO.markAsFailed(
                    channelSend.getId(), 
                    "Max retries reached: " + e.getMessage(), 
                    Instant.now()
                );
                log.error("Email marked FAILED after {} attempts: {}", MAX_RETRIES, channelSend.getId());
            } else {
                log.warn("Email will retry: attempt {}/{}", 
                    channelSend.getAttemptCount(), MAX_RETRIES);
            }
        }
    }
}

private void sendEmail(String to, String subject, String body) throws Exception {
    // Usa JavaMailSender o AWS SES o altra implementazione
    // Questo è EXTERNAL SYSTEM - può fallire
    // Per questo usiamo L2 Outbox con retry logic
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);
    javaMailSender.send(message);  // ← Può lanciare Exception
}
```

**Timing:** Starts ~10 seconds after OutboxPoller created channel_send rows

**Database State AFTER:**
- `notification_channel_send` table [L2]: 3 EMAIL rows updated (is_sent=true, sent_at=NOW)

---

## ⏱️ Timeline Completo (EMAIL con 3 LIVELLI)

| **T** | **Evento** | **Componente** | **DB State** |
|-------|-----------|----------------|--------------|
| T+0ms | POST /ask | Controller | - |
| T+10ms | save(Reservation) | Service → DB | `reservation` (+1) |
| T+20ms | **INSERT event_outbox [L0]** | Service → DB | `event_outbox` (+1, PENDING) |
| T+30ms | **HTTP 200 OK** ← | Controller ← | ✅ **Event guaranteed in DB (at-least-once)** |
| T+5030ms | EventOutboxPoller select | Poller | - |
| T+5050ms | Pubblica a RabbitMQ | Poller → RabbitMQ | RabbitMQ receives message |
| T+5070ms | **UPDATE event_outbox [L0]** | Poller → DB | `event_outbox` (PUBLISHED) |
| T+5080ms | @RabbitListener triggered | Listener | - |
| T+5100ms | **INSERT notification_restaurant (×3)** | Listener → DB | `notification_restaurant` (+3) |
| T+5120ms | **INSERT notification_outbox [L1] (×3)** | Listener → DB | `notification_outbox` (+3, PENDING) |
| T+5130ms | **UPDATE event_outbox [L0]** | Listener → DB | `event_outbox` (processed_by='RESTAURANT_LISTENER') |
| T+10030ms | NotificationOutboxPoller select | Poller | - |
| T+10050ms | **UPDATE notification_outbox [L1]** | Poller → DB | `notification_outbox` (3x PUBLISHED) |
| T+10070ms | **INSERT notification_channel_send [L2] (×9)** | Poller → DB | `notification_channel_send` (+9 = 3 notif × 3 channels) |
| T+15030ms | ChannelPoller select | Poller | - |
| T+15050ms | Select notification_restaurant | Poller → DB | - |
| T+15070ms | SELECT user email | Poller → DB | - |
| T+15100ms | **sendEmail() via SMTP** | ChannelPoller → EmailService | EXTERNAL CALL (can retry) |
| T+15150ms | **UPDATE is_sent=true** | Poller → DB | `notification_channel_send` (3x sent) |
| T+15200ms | 📧 **Email delivered** | SMTP Server | ✅ **Staff receives email** |

---

## 🎯 Confronto: EMAIL (2-Level) vs WEBSOCKET (Direct)

### EMAIL (2-Level Outbox Pattern)

```
✅ VANTAGGI:
  1. Retry Logic: Se SMTP fallisce, riprova fino a MAX_RETRIES
  2. Durability: Email rimane in DB finché non è inviata
  3. Offline-Safe: Se email service è down, aspetta finché non torna online
  4. Audit Trail: Puoi vedere chi/quando è stato contattato via email
  5. Guaranteed Delivery: Quasi certo che arriverà (salvo network issues permanenti)

❌ SVANTAGGI:
  1. Latenza: 5-15 secondi prima di arrivare (poller timing)
  2. Database Overhead: 2 tabelle (outbox L1 + L2)
  3. Complexity: Retry logic, attempt count tracking
  4. Cost: Multiple polling cycles
```

**Usare EMAIL quando:**
- È critico che il messaggio sia consegnato
- L'utente ha email notification preferences
- Puoi tollerare 5-30 secondi di latenza
- Hai sistemi di audit/compliance

---

### WEBSOCKET (Direct Pattern)

```
✅ VANTAGGI:
  1. Real-Time: Consegna istantanea (< 200ms)
  2. Low Latency: Nessun polling, nessun outbox
  3. No Database Overhead: Nessuna persistenza
  4. Simple: Invia direttamente, non fallisce = log only
  5. Cost: No extra polling cycles

❌ SVANTAGGI:
  1. Best-Effort Only: Se browser è offline, messaggio si perde
  2. No Retry: Se invio fallisce, nessun retry
  3. Volatile: Dipende da WebSocket connection attiva
  4. No Audit: Nessuna traccia se client non ha ricevuto
  5. Ephemeral: Perfetto per notifiche, non per transazioni critiche
```

**Usare WEBSOCKET quando:**
- Real-time è importante
- Utenti sono tipicamente online
- È OK se qualche notifica si perde
- Notifiche sono informative (non critiche)
- Vuoi bassa latenza

---

## 📊 Architecture Diagram

```
LISTENER (Synchronous)
├─ Create notification_restaurant
├─ Create notification_outbox [L1]
└─ HTTP 200 OK
   
OUTBOX POLLER (Async, ~5s)
├─ Read notification_outbox [L1] (PENDING)
├─ Update notification_outbox [L1] (PUBLISHED)
└─ Create notification_channel_send [L2]
   ├─ For EMAIL: Create L2 entry
   ├─ For SMS: Create L2 entry
   ├─ For PUSH: Create L2 entry
   ├─ For SLACK: Create L2 entry
   └─ For WEBSOCKET: SKIP (direct)
   
CHANNEL POLLER (Async, ~10s)
├─ Read notification_channel_send [L2] for EMAIL
│  ├─ Get notification_restaurant
│  ├─ Get user email
│  ├─ Send via SMTP (EXTERNAL SYSTEM)
│  ├─ If success: UPDATE is_sent=true
│  └─ If failure: INCREMENT attempt_count + retry
│
├─ Read notification_channel_send [L2] for SMS
│  └─ [Same pattern - with retry]
│
├─ Read notification_channel_send [L2] for PUSH
│  └─ [Same pattern - with retry]
│
└─ WEBSOCKET (DIRECT - no L2)
   ├─ Get notification_restaurant
   ├─ Convert to STOMP message
   └─ convertAndSendToUser() (in-memory)
      └─ If success: Log only
         If failure: Log only (no retry)
```

---

## 🧪 Testing EMAIL Flow

```bash
# 1. Create reservation (HTTP)
curl -X POST http://localhost:8080/customer/reservation/ask \
  -H "Content-Type: application/json" \
  -d '{"customerId": 5, "restaurantId": 10, "partySize": 4, "slotTime": "2025-11-14T20:00:00"}'

# Response: 200 OK
# DB Check: SELECT * FROM notification_restaurant; → 3 rows
# DB Check: SELECT * FROM notification_outbox WHERE status='PENDING'; → 3 rows

# 2. Wait ~5 seconds for OutboxPoller
sleep 5

# DB Check: SELECT * FROM notification_outbox WHERE status='PUBLISHED'; → 3 rows
# DB Check: SELECT * FROM notification_channel_send WHERE channel_type='EMAIL'; → 3 rows

# 3. Wait ~10 seconds for ChannelPoller
sleep 10

# DB Check: SELECT * FROM notification_channel_send WHERE is_sent=true; → 3 rows (or attempt_count > 0 if failed)
# 📧 Check email inbox: Should receive 3 emails

# 4. If email failed (e.g., SMTP connection issue)
# DB Check: SELECT * FROM notification_channel_send WHERE channel_type='EMAIL' AND attempt_count > 0;
# ChannelPoller will retry next cycle (every 10 seconds)
```

---

## 🔧 Implementazione Attuale (EMAIL)

| **Classe** | **Metodo** | **Status** |
|-----------|-----------|-----------|
| RestaurantNotificationListener | handleRestaurantEmailNotification() | ⚠️ Needs implementation |
| NotificationOutboxPoller | pollAndProcessOutbox() | ✅ Creates L2 entries for all channels |
| ChannelPoller | sendEmail() | ❌ Stub - needs SMTP implementation |
| EmailService | sendEmail(to, subject, body) | ❌ Missing - needs JavaMailSender |

---

**Ultimo aggiornamento:** 14 Novembre 2025  
**Autore:** Engineering Team
