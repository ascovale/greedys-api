# 📋 SISTEMA NOTIFICHE - ANALISI ARCHITETTURA
**Data:** 13 Novembre 2025  
**Status:** ⚠️ Parzialmente Funzionante - Mancano implementazioni critiche

---

## 🏗️ ARCHITETTURA (3 LIVELLI OUTBOX)

```
T0: ReservationService crea prenotazione
    └─ INSERT event_outbox (status=PENDING)

T1 (@1s): EventOutboxPoller (@Scheduled)
    └─ SELECT PENDING → PUBLISH RabbitMQ → UPDATE status=PROCESSED

T2 (@0ms): 4 Listener PARALLELI (AdminListener, RestaurantListener, CustomerListener, AgencyListener)
    ├─ Parse evento da RabbitMQ
    ├─ Idempotency check (processed_by='LISTENER_NAME')
    ├─ CREATE notification entity (AdminNotification, RestaurantNotification, etc)
    ├─ INSERT notification_outbox (status=PENDING)
    └─ UPDATE event_outbox.processed_by

T3 (@5s): NotificationOutboxPoller (@Scheduled)
    └─ SELECT PENDING → UPDATE status=PUBLISHED

T4 (@10s): ChannelPoller (@Scheduled) ⭐ CHANNEL ISOLATION
    ├─ Per OGNI notifica
    └─ Per OGNI canale (SMS, EMAIL, PUSH, WEBSOCKET, SLACK)
       ├─ CREATE NotificationChannelSend se non esiste
       ├─ SEND via provider
       └─ UPDATE is_sent indipendentemente
```

---

## ✅ IMPLEMENTATO

### Entità (11 file)
- ✅ `AEventNotification.java` - Base entity-level (no userId)
- ✅ `ANotification.java` - Recipient-specific (+ userId, userType)
- ✅ `EventOutbox.java` - L1: Event tracking
- ✅ `NotificationOutbox.java` - L2: Notification tracking
- ✅ `NotificationChannelSend.java` - L3: Channel isolation
- ✅ 4 Notification subclass (Admin, Restaurant, Customer, Agency)
- ✅ NotificationAction, NotificationPreferences, Context classes

### DAO (7 file, 80+ query methods)
- ✅ EventOutboxDAO (12 metodi)
- ✅ NotificationOutboxDAO (11 metodi)
- ✅ NotificationChannelSendDAO (15 metodi)
- ✅ AdminNotificationDAO, RestaurantNotificationDAO, CustomerNotificationDAO, AgencyNotificationDAO

### Listeners (4 file, 878 righe)
- ✅ `AdminNotificationListener.java` (242 righe)
  - Eventi: RESERVATION_REQUESTED, CUSTOMER_REGISTERED, PAYMENT_RECEIVED
- ✅ `RestaurantNotificationListener.java` (195 righe)
  - Eventi: RESERVATION_REQUESTED, CONFIRMED, CANCELLED
- ✅ `CustomerNotificationListener.java` (218 righe)
  - Eventi: RESERVATION_CONFIRMED, REJECTED, REMINDER, PAYMENT_RECEIVED, REWARD_EARNED
- ✅ `AgencyNotificationListener.java` (223 righe)
  - Eventi: BULK_IMPORTED, HIGH_VOLUME, REVENUE_REPORT, CHURN_ALERT, PERFORMANCE, SYSTEM_ALERT

### Pollers (3 file, 530+ righe)
- ✅ `EventOutboxPoller.java` (127 righe) - @Scheduled(fixedDelay=1s)
- ✅ `NotificationOutboxPoller.java` (122 righe) - @Scheduled(fixedDelay=5s)
- ✅ `ChannelPoller.java` (280+ righe) - @Scheduled(fixedDelay=10s) ⭐ ISOLATION PATTERN

---

## ❌ COSA MANCA (CRITICO)

### 1️⃣ RabbitMQ Integration (0% implementato)
**File:** `RabbitMQConfig.java`  
**Cosa serve:**
- [ ] Exchange: `event-stream` (topic)
- [ ] Exchange: `notification-channel-send` (topic)
- [ ] Queue: `event-stream-queue`
- [ ] Queue: `notification-channel-send-queue`
- [ ] Binding tra exchange e queue
- [ ] Application.yml: spring.rabbitmq.* properties
- [ ] @RabbitListener annotations su 4 listener

**Impatto:** Senza RabbitMQ:
- Gli eventi creati NON vengono ascoltati
- Nessuna notifica viene generata
- Listeners NON ricevono messaggi

### 2️⃣ Channel Send Implementation (0% implementato)
**File:** `ChannelPoller.java`  
**Metodi TODO:**
- [ ] `sendSMS()` - AWS SNS / Twilio / Nexmo
- [ ] `sendEmail()` - JavaMailSender / SendGrid / AWS SES
- [ ] `sendPush()` - Firebase Cloud Messaging
- [ ] `sendWebSocket()` - Spring WebSocket broadcast
- [ ] `sendSlack()` - Slack API

**Impatto:** Senza implementazioni:
- Notifiche create ma NEVER sent
- NotificationChannelSend.is_sent rimane NULL
- Users non ricevono SMS/Email/Push

### 3️⃣ Event Trigger
**File:** `ReservationService.java`  
**Cosa serve:**
- [ ] ReservationService deve creare EventOutbox dopo INSERT reservation
- [ ] EVENT TYPE: "RESERVATION_REQUESTED"
- [ ] PAYLOAD: { customerId, restaurantId, reservationId, ... }

**Attualmente:** ReservationEventListener esiste ma usa vecchio pattern @EventListener

---

## 🧪 TEST END-TO-END (COME FUNZIONA ADESSO)

### Scenario: Customer crea prenotazione

```
1. POST /api/reservations (customer)
   ├─ Service crea Reservation in DB ✅
   ├─ Pubblica ReservationCreatedEvent ✅
   └─ OLD ReservationEventListener ascolta (non outbox pattern) ❌

2. EventOutboxPoller gira ogni 1s
   ├─ Cerca EventOutbox.status=PENDING ❌ (nessuno creato per via non-outbox)
   └─ Niente da fare

3. ChannelPoller gira ogni 10s
   ├─ Cerca NotificationChannelSend.is_sent IS NULL ❌
   └─ Niente da fare

❌ RISULTATO: Customer NON riceve SMS/Email di conferma
❌ RISULTATO: Restaurant staff NON riceve SMS/Email di nuova prenotazione
```

---

## ✨ COSA SERVE PER FUNZIONARE (PRIORITÀ)

### FASE 1: Enable RabbitMQ Config (2-3 ore)
```
1. Crea RabbitMQConfig.java in com/application/common/config/
2. Configura application.yml con spring.rabbitmq
3. Aggiungi @RabbitListener su 4 listener
4. Start RabbitMQ container: docker run -d rabbitmq:3-management
5. Test RabbitMQ connection
```

### FASE 2: Implement Channel Send (4-6 ore)
```
1. sendSMS() - Scelta provider (Twilio/AWS SNS)
2. sendEmail() - Scelta provider (SendGrid/AWS SES)
3. sendPush() - Firebase Cloud Messaging setup
4. sendWebSocket() - Spring WebSocket broker
5. sendSlack() - Slack API token
```

### FASE 3: Wire ReservationService (1-2 ore)
```
1. Modifica ReservationService.createNewReservation():
   - DOPO INSERT reservation
   - Crea EventOutbox (eventType="RESERVATION_REQUESTED")
   - Salva in eventOutboxDAO

2. Rimuovi vecchio pattern @EventListener da ReservationEventListener
```

---

## 📊 STATO PER COMPONENTE

| Componente | Status | Note |
|-----------|--------|------|
| Entità Notifiche | ✅ 100% | AEventNotification, ANotification, 4 subclass |
| DAO Layer | ✅ 100% | 7 DAO, 80+ metodi |
| Listener Layer | ✅ 95% | 4 listener scritti, mancano @RabbitListener |
| Poller Layer | ✅ 95% | 3 poller scritti, mancano channel send |
| RabbitMQ Config | ❌ 0% | NON IMPLEMENTATO |
| Channel Send | ❌ 0% | 5 placeholder methods |
| Event Trigger | ⚠️ 50% | Pattern vecchio @EventListener |
| **TOTALE** | **⚠️ 70%** | Funzionerà SOLO con implementazioni mancanti |

---

## 🎯 PROSSIMI STEP CONCRETI

### Step 1: RabbitMQ Config
**Crea file:** `src/main/java/com/application/common/config/RabbitMQConfig.java`
```java
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_EVENT = "event-stream";
    public static final String EXCHANGE_NOTIFICATION = "notification-channel-send";
    public static final String QUEUE_EVENT = "event-stream-queue";
    public static final String QUEUE_NOTIFICATION = "notification-channel-send-queue";

    @Bean
    public TopicExchange eventExchange() { return new TopicExchange(EXCHANGE_EVENT); }

    @Bean
    public TopicExchange notificationExchange() { return new TopicExchange(EXCHANGE_NOTIFICATION); }

    @Bean
    public Queue eventQueue() { return new Queue(QUEUE_EVENT); }

    @Bean
    public Queue notificationQueue() { return new Queue(QUEUE_NOTIFICATION); }

    @Bean
    public Binding eventBinding(Queue eventQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(eventQueue).to(eventExchange).with("event.*");
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with("notification.*");
    }
}
```

### Step 2: Add @RabbitListener su AdminNotificationListener
```java
@RabbitListener(queues = "event-stream-queue")
@Transactional
public void onEventReceived(String eventPayload) {
    // Existing implementation
}
```

### Step 3: Modifica ReservationService
```java
public Reservation createNewReservation(Reservation reservation) {
    Reservation saved = reservationDAO.save(reservation);
    
    // NUOVO: Crea evento per outbox
    EventOutbox event = EventOutbox.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("RESERVATION_REQUESTED")
        .aggregateType("RESERVATION")
        .aggregateId(saved.getId())
        .payload(toJson(saved))
        .status(Status.PENDING)
        .build();
    eventOutboxDAO.save(event);
    
    return saved;
}
```

---

## 🚀 RISULTATO FINALE ATTESO

**Timeline: Customer crea prenotazione**
```
T0: Customer POST /api/reservations
    └─ Reservation salvata ✅
    └─ EventOutbox creato ✅

T1 (@1s): EventOutboxPoller
    └─ EventOutbox pubblicato a RabbitMQ ✅

T2 (@0ms): 4 Listener ricevono
    ├─ AdminNotificationListener crea AdminNotification
    ├─ RestaurantNotificationListener crea RestaurantNotification (per staff)
    ├─ CustomerNotificationListener crea CustomerNotification
    └─ Tutti inseriscono in notification_outbox ✅

T3 (@5s): NotificationOutboxPoller
    └─ Marca PUBLISHED ✅

T4 (@10s): ChannelPoller
    ├─ CustomerNotification: SMS → customer phone ✅
    ├─ CustomerNotification: EMAIL → customer email ✅
    ├─ RestaurantNotification: SMS → staff phone ✅
    ├─ RestaurantNotification: EMAIL → staff email ✅
    └─ RestaurantNotification: PUSH → staff app ✅

RISULTATO: ✅ Customer + Restaurant staff ricevono notifiche
```

---

## 📝 CONCLUSIONE

**Architettura:** ⭐⭐⭐⭐⭐ Excellente - Pattern outbox 3-livelli, channel isolation, idempotency

**Implementazione:** 70% completa - Manca RabbitMQ config + channel send methods

**Tempo per funzionare:** 8-12 ore
- RabbitMQ config: 2-3 ore
- Channel send implementation: 4-6 ore  
- Integration testing: 2-3 ore
