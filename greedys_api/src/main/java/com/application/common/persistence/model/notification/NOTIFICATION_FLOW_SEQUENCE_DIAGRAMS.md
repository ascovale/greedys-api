# NOTIFICATION FLOW - DIAGRAMMI DI FLUSSO (SEQUENCE DIAGRAM)

**Versione:** Con strati verticali e frecce di comunicazione  
**Data:** Novembre 2025

---

## DIAGRAMMA 1: CREAZIONE EVENTO → LISTENER

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                         FLOW: Evento generato fino a Listener riceve                            │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘

    ReservationService      EventOutbox         EventOutboxPoller       RabbitMQ        Listener
             │                   │                      │                   │              │
             │                   │                      │                   │              │
    [T0] POST /reservations      │                      │                   │              │
             │──────────────────>│                      │                   │              │
             │ INSERT event_outbox                      │                   │              │
             │ status=PENDING    │                      │                   │              │
             │                   │                      │                   │              │
    [T0.5]   │<──────────────────│                      │                   │              │
             │ COMMIT OK         │                      │                   │              │
             │                   │                      │                   │              │
    [T1] @Scheduled(5 sec)       │                      │                   │              │
             │                   │──────────────────>│  │                   │              │
             │                   │ findByStatus(PENDING)│                   │              │
             │                   │                      │                   │              │
             │                   │<──────────────────│  │                   │              │
             │                   │ List<EventOutbox>    │                   │              │
             │                   │                      │──────────────────>│              │
             │                   │                      │ PUBLISH to        │              │
             │                   │                      │ event-stream      │              │
             │                   │                      │ (payload JSON)    │              │
             │                   │                      │                   │─────────────>│
             │                   │                      │                   │ Message      │
             │                   │                      │                   │ in queue     │
             │                   │                      │                   │              │
             │                   │                      │ UPDATE            │              │
             │                   │<──────────────────│  │ status=PROCESSED  │              │
             │                   │ UPDATE OK            │                   │              │
             │                   │                      │                   │              │
    [T2] @RabbitListener         │                      │                   │              │
             │                   │                      │                   │<─────────────│
             │                   │                      │                   │ Pull from Q  │
             │                   │                      │                   │              │
             │                   │                      │                   │─────────────>│
             │                   │                      │                   │ Process      │
             │                   │                      │                   │              │
```

---

## DIAGRAMMA 2: LISTENER → CREATE NOTIFICHE + OUTBOX (SENZA CHANNEL_SEND)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  FLOW: Listener crea SOLO Notifiche + OutBox (ChannelSend creato dopo)      │
└──────────────────────────────────────────────────────────────────────────────┘

    Listener           AdminNotification      RestaurantNotification      CustomerNotification      NotificationOutbox
       │                     │                         │                         │                         │
       │                     │                         │                         │                         │
[T2] Riceve evento           │                         │                         │                         │
     Idempotency check OK    │                         │                         │                         │
       │                     │                         │                         │                         │
       ├────────────────────>│                         │                         │                         │
       │ CREATE              │                         │                         │                         │
       │ user_id=1,2,3       │                         │                         │                         │
       │ (3 righe)           │                         │                         │                         │
       │                     │                         │                         │                         │
       │<────────────────────│                         │                         │                         │
       │ List<AdminNotif>    │                         │                         │                         │
       │                     │                         │                         │                         │
       ├─────────────────────────────────────────────> │                         │                         │
       │ CREATE                                        │                         │                         │
       │ user_id=50,51,52                              │                         │                         │
       │ restaurant_id=100 (3 righe)                   │                         │                         │
       │                                               │                         │                         │
       │<───────────────────────────────────────────── │                         │                         │
       │ List<RestaurantNotif>                         │                         │                         │
       │                                               │                         │                         │
       ├───────────────────────────────────────────────────────────────────────> │                         │
       │ CREATE                                                                  │                         │
       │ user_id=456 (1 riga)                          │                         │                         │
       │                                               │                         │                         │
       │<─────────────────────────────────────────────────────────────────────── │                         │
       │ List<CustomerNotif>                          │                          │                         │
       │                                               │                         │                         │
       │                                               │                         │                         │
       ├──────────────────────────────────────────────────────────────────────────────────────────────>│
       │ INSERT INTO notification_outbox                                                               │
       │ FOR each notification (7 righe totali)                                                       │
       │ notificationId, notificationType, status=PENDING                                             │
       │ (NO NotificationChannelSend qui - creati dopo!)                                              │
       │                                                                                               │
       │<──────────────────────────────────────────────────────────────────────────────────────────────│
       │ PERSISTED OK                                                                                  │
       │                                               │                         │                     │
       │                                               │                         │                     │
       ├──────────────────────────────────────────────────────────────────────────────────────────────>│
       │ PUBLISH to RabbitMQ (notification-send-queue)                                                │
       │ Payload: {notificationId: 100, userId: 1, userType: 'ADMIN_USER'}                           │
       │                                                                                               │
       │                                               │                         │                     │
       │─────────────────────────────────────────────────────────────────────────────────────────────→│
       │ UPDATE event_outbox SET processedBy='ADMIN_LISTENER'                                         │
       │ COMMIT                                                                                       │
       │                                               │                         │                     │
       └──────────────────────────────────────────────────────────────────────────────────────────────┘

⭐ IMPORTANTE: NotificationChannelSend NON è creato qui!
   Verrà creato dal ChannelPoller, uno per volta per ogni canale.
```

---

## DIAGRAMMA 3: CHANNEL POLLER - ISOLATION PATTERN (UNO PER VOLTA)

```
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│     FLOW: ChannelPoller elabora UN CANALE per volta (Channel Isolation Pattern)               │
└────────────────────────────────────────────────────────────────────────────────────────────────┘

    NotificationOutbox      ChannelPoller              NotificationChannelSend        Provider (SMS/Email/etc)
            │                    │                              │                              │
            │                    │                              │                              │
    [T3] Listener               │                              │                              │
    inserisce qui               │                              │                              │
            │                    │                              │                              │
    [T4] @Scheduled(10 sec)      │                              │                              │
            │                    │                              │                              │
            │<───────────────────│                              │                              │
            │ SELECT DISTINCT    │                              │                              │
            │ notificationIds    │                              │                              │
            │ WHERE ...          │                              │                              │
            │                    │                              │                              │
            │──────────────────>│                              │                              │
            │ List<notifIds>     │                              │                              │
            │                    │                              │                              │
            │                    ├─────────────────────────────>│                              │
            │                    │ CHECK: EXISTS                │                              │
            │                    │ (notifId=100, SMS)           │                              │
            │                    │                              │                              │
            │                    │<─────────────────────────────│                              │
            │                    │ NOT EXISTS → CREATE NOW      │                              │
            │                    │                              │                              │
            │                    │ INSERT notification_         │                              │
            │                    │ channel_send (SMS, is_sent=NULL)                            │
            │                    │                              │                              │
            │                    ├─────────────────────────────────────────────────────────────>│
            │                    │                              │ sendSMS(notif)              │
            │                    │                              │ ✅ SUCCESS or ❌ FAIL       │
            │                    │                              │                              │
            │                    │<─────────────────────────────────────────────────────────────│
            │                    │                              │ RESULT: true/false          │
            │                    │                              │                              │
            │                    ├─────────────────────────────>│                              │
            │                    │ UPDATE is_sent=true/false    │                              │
            │                    │ sent_at=NOW() (if true)      │                              │
            │                    │ attempt_count++ (if false)   │                              │
            │                    │                              │                              │
            │                    │<─────────────────────────────│                              │
            │                    │ PERSISTED                    │                              │
            │                    │                              │                              │
            │                    ├─────────────────────────────>│  ← NEXT ITERATION           │
            │                    │ CHECK: EXISTS                │                              │
            │                    │ (notifId=100, EMAIL)         │                              │
            │                    │                              │                              │
            │                    │<─────────────────────────────│                              │
            │                    │ NOT EXISTS → CREATE NOW      │                              │
            │                    │                              │                              │
            │                    │ INSERT notification_         │                              │
            │                    │ channel_send (EMAIL, is_sent=NULL)                          │
            │                    │                              │                              │
            │                    ├────────────────────────────────────────────────────────────>│
            │                    │                              │ sendEmail(notif)            │
            │                    │                              │ ✅ SUCCESS                  │
            │                    │                              │                              │
            │                    │<────────────────────────────────────────────────────────────│
            │                    │                              │ RESULT: true                │
            │                    │                              │                              │
            │                    ├─────────────────────────────>│                              │
            │                    │ UPDATE is_sent=true          │                              │
            │                    │ sent_at=NOW()                │                              │
            │                    │                              │                              │
            │                    │<─────────────────────────────│                              │
            │                    │ PERSISTED                    │                              │
            │                    │                              │                              │
            │                    ├─────────────────────────────>│  ← NEXT ITERATION           │
            │                    │ CHECK: EXISTS                │                              │
            │                    │ (notifId=100, PUSH)          │                              │
            │                    │                              │                              │
            │                    │<─────────────────────────────│                              │
            │                    │ NOT EXISTS → CREATE NOW      │                              │
            │                    │ ...                          │                              │
            │                    │ ...                          │                              │

⭐ PATTERN: Per ogni notifica, per ogni canale:
   1. CHECK: esiste NotificationChannelSend per questo canale?
   2. NO? CREATE con is_sent=NULL
   3. SEND via provider
   4. UPDATE is_sent=true/false

⭐ VANTAGGIO: Se SMS fallisce, EMAIL è ancora pendente
   Prossimo ciclo: SMS riprova, EMAIL va avanti
   Se EMAIL fallisce, PUSH non è creato → verrà fatto dopo
```

---

## DIAGRAMMA 4: CHANNEL POLLER INVIA VIA PROVIDER (CON ISOLATION)

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│     FLOW: ChannelPoller invia UN CANALE per volta via SMS/Email/Push/WebSocket/Slack     │
└──────────────────────────────────────────────────────────────────────────────────────────┘

    NotificationChannelSend      ChannelPoller      SMSProvider      EmailProvider      FirebaseProvider      WebSocketManager
            │                         │                  │                 │                  │                       │
            │                         │                  │                 │                  │                       │
    [T5] @Scheduled(10 sec)           │                  │                 │                  │                       │
            │                         │                  │                 │                  │                       │
            │<────────────────────────│                  │                 │                  │                       │
            │ SELECT DISTINCT         │                 │                 │                  │                       │
            │ notificationIds         │                 │                 │                  │                       │
            │ WHERE is_sent IS NULL   │                 │                 │                  │                       │
            │ LIMIT 100               │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │─────────────────────────>│                 │                 │                  │                       │
            │ List<notifIds>          │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │  [LOOP for each notifId and each channel]  │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │<────────────────────────│                 │                 │                  │                       │
            │ SELECT EXISTS           │                 │                 │                  │                       │
            │ (notifId, SMS)          │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │─────────────────────────>│                 │                 │                  │                       │
            │ NO → CREATE             │                 │                 │                  │                       │
            │ INSERT (SMS, is_sent=NULL)               │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │                         ├────────────────>│ sendSMS()       │                  │                       │
            │                         │                 │                 │                  │                       │
            │                         │<────────────────│ OK/FAIL         │                  │                       │
            │<────────────────────────│                 │                 │                  │                       │
            │ UPDATE is_sent=true/false                │                 │                  │                       │
            │ sent_at / attempt_count++                │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │  ← NEXT CHANNEL (EMAIL) │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │<────────────────────────│                 │                 │                  │                       │
            │ SELECT EXISTS           │                 │                 │                  │                       │
            │ (notifId, EMAIL)        │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │─────────────────────────>│                 │                 │                  │                       │
            │ NO → CREATE             │                 │                 │                  │                       │
            │ INSERT (EMAIL, is_sent=NULL)             │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │                         ├────────────────────────────────>│ sendEmail()      │                       │
            │                         │                 │                 │                  │                       │
            │                         │<────────────────────────────────│ OK               │                       │
            │<────────────────────────│                 │                 │                  │                       │
            │ UPDATE is_sent=true     │                 │                 │                  │                       │
            │ sent_at=NOW()           │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │  ← NEXT CHANNEL (PUSH)  │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │<────────────────────────│                 │                 │                  │                       │
            │ SELECT EXISTS           │                 │                 │                  │                       │
            │ (notifId, PUSH)         │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │─────────────────────────>│                 │                 │                  │                       │
            │ NO → CREATE             │                 │                 │                  │                       │
            │ INSERT (PUSH, is_sent=NULL)              │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │                         ├────────────────────────────────────────────────────>│ sendPush()          │
            │                         │                 │                 │                  │                      │
            │                         │<────────────────────────────────────────────────────│ OK                  │
            │<────────────────────────│                 │                 │                  │                       │
            │ UPDATE is_sent=true     │                 │                 │                  │                       │
            │ sent_at=NOW()           │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │
            │  ← NEXT CHANNEL (WS)    │                 │                 │                  │                       │
            │  ← NEXT CHANNEL (SLACK) │                 │                 │                  │                       │
            │                         │                 │                 │                  │                       │

⭐ CHANNEL ISOLATION:
   SMS fallisce? → only SMS riprova prossimo ciclo
   EMAIL ok? → EMAIL è fatto
   PUSH ok? → PUSH è fatto
   WS ok? → WS è fatto
   SLACK non creato ancora? → created e inviato prossimo ciclo

⭐ RETRY GRANULARE:
   Solo il canale che fallisce retry
   Gli altri continuano normalmente
```

---

## DIAGRAMMA 5: LETTURA NOTIFICA + FIRST-TO-ACT

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│         FLOW: Recipient legge notifica + First-To-Act pattern                    │
└─────────────────────────────────────────────────────────────────────────────────┘

    Client/App       RestaurantNotificationController      RestaurantNotification      NotificationAction
         │                        │                               │                          │
         │                        │                               │                          │
    [Action] GET /notifications   │                               │                          │
         │──────────────────────>│                               │                          │
         │                        │                               │                          │
         │                        ├──────────────────────────────>│                          │
         │                        │ SELECT by id                  │                          │
         │                        │                               │                          │
         │                        │<──────────────────────────────│                          │
         │<───────────────────────│ RestaurantNotification        │                          │
         │ JSON: {                │                               │                          │
         │   id: 100,             │                               │                          │
         │   title: "...",        │                               │                          │
         │   is_read: false,      │                               │                          │
         │   shared_read: true,   │                               │                          │
         │   read_by_user_id: null│                               │                          │
         │ }                      │                               │                          │
         │                        │                               │                          │
    [Action] PATCH /markAsRead    │                               │                          │
         │──────────────────────>│                               │                          │
         │ {userId: 50,          │                               │                          │
         │  shared: true}        │                               │                          │
         │                        │                               │                          │
         │                        ├──────────────────────────────>│                          │
         │                        │ UPDATE is_read=true           │                          │
         │                        │ read_by_user_id=50            │                          │
         │                        │ read_at=NOW()                 │                          │
         │                        │                               │                          │
         │                        │<──────────────────────────────│                          │
         │<───────────────────────│ OK                            │                          │
         │                        │                               │                          │
    [Action] POST /action         │                               │                          │
         │──────────────────────>│                               │                          │
         │ {notificationId: 100, │                               │                          │
         │  actionType: CONFIRMED}                               │                          │
         │                        │                               │                          │
         │                        ├──────────────────────────────────────────────────────────>│
         │                        │                               │ INSERT NotificationAction│
         │                        │                               │ actorId=50              │
         │                        │                               │ actionType=CONFIRMED    │
         │                        │                               │                          │
         │                        │                               │<─────────────────────────│
         │<───────────────────────│────────────────────────────────│ OK                      │
         │ {status: "OK",         │                               │                          │
         │  message: "Accettato da│                               │                          │
         │           Manager #50"}│                               │                          │
         │                        │                               │                          │

         └─────────────────────────────────────────────────────────────────────────────────┘
```

---

## DIAGRAMMA 6: FULL CYCLE COMPLETO (CON CHANNEL ISOLATION)

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│     FLOW COMPLETO: Event → Service → Listeners → ChannelPoller (uno per volta)                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

ReservationService    EventOutbox    EventOutboxPoller    AdminListener    NotificationOutbox    ChannelPoller    SMSProvider
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │              │
[T0]   ├─────────────────>│                 │                  │                   │                  │              │
       │ INSERT           │                 │                  │                   │                  │              │
       │ (status=PENDING) │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │              │
[T1]   │                  │<────────────────│                  │                   │                  │              │
       │                  │ SELECT PENDING  │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │              │
       │                  │─────────────────────────────────────────────────────────────────────────────────────>│
       │                  │                 │ PUBLISH to event-stream (RabbitMQ)                                   │
       │                  │                 │                  │                   │                  │              │
       │                  │ UPDATE status   │                  │                   │                  │              │
       │                  │ =PROCESSED      │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │              │
[T2]   │                  │                 │                  ├─────────────────> │                  │              │
       │                  │                 │                  │ Riceve from RabbitMQ                 │              │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  ├──────────────────>│                  │              │
       │                  │                 │                  │ CREATE 3 Admin    │                  │              │
       │                  │                 │                  │ Notifications     │                  │              │
       │                  │                 │                  │ (NO ChannelSend yet!)               │              │
       │                  │                 │                  │                   │                  │              │
[T3]   │                  │                 │                  │                   │<─────────────────│              │
       │                  │                 │                  │                   │ INSERT notification_outbox    │
       │                  │                 │                  │                   │ (3 righe, status=PENDING)     │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  ├──────────────────>│                  │              │
       │                  │                 │                  │ PUBLISH to        │                  │              │
       │                  │                 │                  │ notification-send │                  │              │
       │                  │                 │                  │ (notificationId=100, etc)           │              │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │ UPDATE event_outbox                 │              │
       │                  │                 │                  │ processedBy=ADMIN │                  │              │
       │                  │                 │                  │                   │                  │              │
[T4]   │                  │                 │                  │                   │<───────────────────────────────│
       │                  │                 │                  │                   │ POLLING ogni 10 sec:         │
       │                  │                 │                  │                   │ SELECT notifIds with pending │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  ├──────────────>│
       │                  │                 │                  │                   │                  │ CICLO 1:     │
       │                  │                 │                  │                   │                  │ notif_id=100 │
       │                  │                 │                  │                   │                  │ channel=SMS  │
       │                  │                 │                  │                   │                  │ CREATE + SEND│
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │<─────────────│
       │                  │                 │                  │                   │                  │ ✅ OK        │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  ├──────────────>│
       │                  │                 │                  │                   │                  │ CICLO 2:     │
       │                  │                 │                  │                   │                  │ channel=EMAIL│
       │                  │                 │                  │                   │                  │ CREATE + SEND│
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │<─────────────│
       │                  │                 │                  │                   │                  │ ✅ OK        │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  ├──────────────>│
       │                  │                 │                  │                   │                  │ CICLO 3:     │
       │                  │                 │                  │                   │                  │ channel=PUSH │
       │                  │                 │                  │                   │                  │ CREATE + SEND│
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │<─────────────│
       │                  │                 │                  │                   │                  │ ✅ OK        │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  ├──────────────>│
       │                  │                 │                  │                   │                  │ CICLO 4:     │
       │                  │                 │                  │                   │                  │ channel=WS   │
       │                  │                 │                  │                   │                  │ CREATE + SEND│
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │<─────────────│
       │                  │                 │                  │                   │                  │ ✅ OK        │
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  ├──────────────>│
       │                  │                 │                  │                   │                  │ CICLO 5:     │
       │                  │                 │                  │                   │                  │ channel=SLACK│
       │                  │                 │                  │                   │                  │ CREATE + SEND│
       │                  │                 │                  │                   │                  │              │
       │                  │                 │                  │                   │                  │<─────────────│
       │                  │                 │                  │                   │                  │ ✅ OK        │
       │                  │                 │                  │                   │                  │              │

⭐ CHANNEL ISOLATION:
   - Listener: Crea SOLO AdminNotification + notification_outbox (niente ChannelSend)
   - ChannelPoller: Per ogni ciclo, crea UN canale + invia
   - Se SMS fallisce (ciclo 1): next polling riprova SMS, EMAIL/PUSH già ok
   - Se EMAIL fallisce (ciclo 2): next polling riprova EMAIL, altri continuano
   
⭐ VANTAGGI:
   ✅ Separazione responsabilità: Listener ≠ ChannelCreator
   ✅ Isolation: Ogni canale è indipendente
   ✅ Retry granulare: Solo il canale che fallisce riprova
   ✅ No batch overhead: Un canale alla volta
   ✅ Facile debug: Puoi vedere quale canale ha problemi

       └─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## LEGEND

```
     ┌──────────────────┐
     │ Component Name   │
     └──────────┬───────┘
                │
                │ ──────────> (Sync call / DB query)
                │
                │ ───────────────> (Async publish to RabbitMQ)
                │
                │<────────── (Response / Result)
                │
           [T0], [T1], etc = Timeline markers
```

---

## SOMMARIO

- **Diagramma 1:** Event creato → Poller pubblica → Listener riceve
- **Diagramma 2:** Listener crea 3 tipi di notifiche + notification_outbox
- **Diagramma 3:** NotificationOutbox → Poller → ChannelPoller
- **Diagramma 4:** ChannelPoller invia via SMS/Email/Push/WebSocket/Slack
- **Diagramma 5:** Lettura notifica + First-To-Act
- **Diagramma 6:** Full cycle completo da inizio a fine
