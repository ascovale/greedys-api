# 🎓 Spiegazione Dettagliata: Listener, RabbitMQ, Outbox, WebSocket

**Data:** 14 Novembre 2025

---

## ❌ MISCONCEZIONE: "Listener riceve un INSERT"

**SBAGLIATO:**
```
Listener riceve un INSERT (database INSERT operation)
```

**CORRETTO:**
```
Listener riceve un EVENTO (Java object: ReservationCreatedEvent)
```

### Cosa accade davvero:

```
1. Service salva Reservation in DB
   └─ INSERT INTO reservation VALUES (...)

2. Service PUBBLICA un EVENTO
   └─ ReservationCreatedEvent event = new ReservationCreatedEvent(
        reservationId=123,
        restaurantId=10,
        customerId=5,
        email="john@example.com"
      )
   └─ applicationEventPublisher.publishEvent(event)

3. Spring ApplicationContext riceve l'evento
   └─ Cerca tutti gli @EventListener per ReservationCreatedEvent
   └─ CHIAMA il metodo listener SINCRONAMENTE

4. ReservationEventListener.handleRestaurantWebSocketNotification(event)
   ├─ Estrae dati dall'evento: restaurantId = event.getRestaurantId()
   ├─ LEGGE dal database: SELECT restaurant_users WHERE restaurant_id = 10
   ├─ PER OGNI staff:
   │  ├─ INSERT INTO notification_restaurant (user_id=1, title, body, ...)
   │  ├─ INSERT INTO notification_outbox (notification_id, status=PENDING)
   │  └─ COMMIT questa transazione
   └─ Ritorna al Service
```

**Schema visivo:**

```
┌─────────────────────────────────────┐
│ Service                             │
├─────────────────────────────────────┤
│ 1. save(Reservation)                │
│    ↓                                │
│ 2. publishEvent(ReservationCreated) │
│    ↓                                │
│ 3. return response to Controller    │
│    ↓                                │
│ 4. CONTROLLER sends 200 OK to client│
└─────────────────────────────────────┘
         ↓↓↓ (ASYNCHRONOUSLY from client perspective)
┌─────────────────────────────────────┐
│ ApplicationEventPublisher            │
│ (Spring internals)                  │
├─────────────────────────────────────┤
│ 1. CHIAMA tutti gli @EventListener  │
│    for ReservationCreatedEvent      │
│                                     │
│ 2. ReservationEventListener         │
│    .handleRestaurantWebSocketNotif()│
│    ├─ SELECT restaurant_users       │
│    ├─ FOR EACH staff: INSERT notif  │
│    └─ COMMIT                        │
└─────────────────────────────────────┘
```

---

## 🐰 QUANDO SI USA RABBITMQ?

RabbitMQ si usa quando **il listener è ASINCRONO** (@Async), non per il synchronous event listener.

### Scenario 1: Event Listener SYNCHRONOUS (ATTUALMENTE)

```
┌────────────┐         ┌──────────────────┐       ┌────────────┐
│ Customer   │         │ Service          │       │ Listener   │
└────────────┘         └──────────────────┘       └────────────┘
     │                        │                         │
     │─POST /ask─────────────>│                         │
     │                        │─save Reservation─>DB    │
     │                        │<─ok                     │
     │                        │                         │
     │                        │─publishEvent───────--──>│
     │                        │   (ReservationCreated)  │
     │                        │                         │
     │                        │                    (SELECT staff)
     │                        │                    (INSERT notif)
     │                        │<─return OK─────────
     │                        │                         │
     │<─200 OK────────────────│                         │ COMMIT
     │                        │
```

**Vantaggi:**
- ✅ Consistency garantita
- ✅ Se listener fallisce → rollback della notifica
- ✅ Se listener fallisce → client VEDE l'errore (400, 500, etc.)
- ✅ NO dipendenze esterne (NO RabbitMQ needed)

**Svantaggi:**
- ❌ Più lento (aggiunge latenza alla risposta)
- ❌ Se listener è lento → cliente aspetta

---

### Scenario 2: Event Listener ASYNCHRONOUS (@Async + RabbitMQ)

```
┌────────────┐    ┌──────────────┐    ┌────────────┐     ┌────────────┐
│ Customer   │    │ Service      │    │ RabbitMQ   │     │ Listener   │
└────────────┘    └──────────────┘    └────────────┘     └────────────┘
     │                   │                    │                │
     │─POST /ask────────>│                    │                │
     │                   │─save Reservation   │                │
     │                   │<─ok                │                │
     │                   │                    │                │
     │                   │─publishEvent  ────>│                │
     │                   │(async to RabbitMQ) │                │
     │                   │<─ return OK        │                │
     │<─200 OK───────────│                    │                │
     │                   │                    │─send message ─>│
     │                   │                    │                │
                                              │          (async processing)
                                              │          (SELECT staff)
                                              │          (INSERT notif)
                                              │          (COMMIT)
```

**Codice (con @Async):**

```java
@Service
public class CustomerReservationService {
    
    @Transactional
    public ReservationDTO createReservation(CustomerNewReservationDTO dto, Customer customer) {
        // Save reservation
        Reservation saved = reservationDAO.save(reservation);
        
        // 🚀 PUBLISH ASYNC EVENT (goes to RabbitMQ)
        applicationEventPublisher.publishEvent(
            new ReservationCreatedEvent(
                reservationId = saved.getId(),
                restaurantId = saved.getRestaurant().getId(),
                ...
            )
        );
        
        // ⚡ IMMEDIATAMENTE return to client (non aspetta il listener!)
        return new ReservationDTO(saved);
    }
}

@Component
@Async  // ← ASINCRONO!
public class ReservationEventListener {
    
    public void handleRestaurantWebSocketNotification(ReservationCreatedEvent event) {
        // Questo metodo viene eseguito in un thread separato da RabbitMQ
        // Se fallisce, il client ha già ricevuto 200 OK
        
        try {
            restaurantNotificationDAO.createNotificationsForRestaurant(...);
        } catch (Exception e) {
            // ❌ Errore qui: client non saprà mai!
            // Solo i log sapranno che è fallito
        }
    }
}
```

**Vantaggi:**
- ✅ Risposta VELOCE al client (non aspetta il listener)
- ✅ Scalabile (RabbitMQ gestisce la coda)
- ✅ Resilienza (se listener down, i messaggi restano in coda)

**Svantaggi:**
- ❌ Inconsistency possibile (client vede 200 OK ma notifica non creata)
- ❌ Dipendenza da RabbitMQ (deve essere running)
- ❌ Debugging più difficile (errori asincroni)

---

## 🔐 PERCHÉ OUTBOX DEVE ESSERE CREATO PRIMA DI RISPONDERE

Hai perfettamente ragione!

### ❌ SBAGLIATO: Creare outbox DOPO la risposta

```
Service.createReservation():
  1. save(Reservation)           ← DB INSERT
  2. publishEvent(...)            ← Evento
  3. ✅ return response            ← HTTP 200 OK sent to client
  
(poi, asincronamente, eventualmente:)
  4. Listener crea notification_restaurant
  5. Listener crea notification_outbox
  
PROBLEMA: Se step 4-5 fallisce, client ha già ricevuto 200 OK!
```

### ✅ CORRETTO: Creare outbox PRIMA di rispondere (SYNCHRONOUS LISTENER)

```
Service.createReservation():
  1. save(Reservation)                    ← DB INSERT
  2. publishEvent(...)                    ← Evento
  3. [BLOCK HERE - aspetta il listener]   ← SYNCHRONOUS!
     ├─ Listener.handle() ESEGUITO
     ├─ Listener SELECT restaurant_users
     ├─ Listener FOR EACH staff:
     │  ├─ INSERT notification_restaurant
     │  ├─ INSERT notification_outbox
     │  └─ DB COMMIT
     └─ [Listener ritorna]
  4. ❌ IF listener fallisce → exception
     ✅ IF listener OK → continue
  5. ✅ return response ← HTTP 200 OK (garantito che outbox esiste!)

VANTAGGIO: Consistency garantito. Se non c'è eccezione, la notifica SICURAMENTE sarà inviata.
```

### Il codice deve essere così:

```java
@Service
@RequiredArgsConstructor
public class CustomerReservationService {
    
    private final ReservationDAO reservationDAO;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional  // ← IMPORTANTE!
    public ReservationDTO createReservation(
        CustomerNewReservationDTO dto, 
        Customer customer
    ) {
        // Step 1: Create reservation entity
        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setRestaurant(dto.getRestaurantId());
        reservation.setDate(dto.getDate());
        
        // Step 2: Save to DB
        Reservation saved = reservationDAO.save(reservation);
        
        // Step 3: PUBLISH EVENT (this triggers the listener SYNCHRONOUSLY)
        eventPublisher.publishEvent(
            new ReservationCreatedEvent(
                saved.getId(),
                saved.getRestaurant().getId(),
                customer.getId(),
                customer.getEmail()
            )
        );
        // ← Listener.handleRestaurantWebSocketNotification() eseguito QUI
        // ← notification_restaurant rows SICURAMENTE create
        // ← notification_outbox rows SICURAMENTE create
        
        // Step 4: Se arriviamo qui, tutto OK
        return new ReservationDTO(saved);
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventListener {
    
    private final RestaurantNotificationDAO notificationDAO;
    private final NotificationOutboxDAO outboxDAO;
    private final ObjectMapper mapper;
    
    @EventListener  // ← SYNCHRONOUS (NO @Async)
    @Transactional
    public void handleRestaurantWebSocketNotification(
        ReservationCreatedEvent event
    ) {
        log.info("🔔 Creating WebSocket notifications for reservation {}", 
                 event.getReservationId());
        
        Long restaurantId = event.getRestaurantId();
        Long reservationId = event.getReservationId();
        
        // Step 1: SELECT restaurant staff
        List<Long> staffUserIds = restaurantUserDAO
            .findByRestaurantId(restaurantId)
            .stream()
            .map(RUser::getId)
            .collect(Collectors.toList());
        
        log.debug("Found {} staff for restaurant {}", staffUserIds.size(), restaurantId);
        
        // Step 2: FOR EACH staff, create notification + outbox
        for (Long staffUserId : staffUserIds) {
            try {
                // Create notification
                RestaurantNotification notif = RestaurantNotification.builder()
                    .userId(staffUserId)
                    .userType("RESTAURANT_USER")
                    .title("📱 Nuova prenotazione richiesta")
                    .body("Prenotazione per " + event.getReservationDate())
                    .properties(Map.of(
                        "reservation_id", String.valueOf(reservationId),
                        "customer_email", event.getCustomerEmail(),
                        "restaurant_id", String.valueOf(restaurantId)
                    ))
                    .build();
                
                RestaurantNotification saved = notificationDAO.save(notif);
                log.debug("✅ Created notification {} for staff {}", saved.getId(), staffUserId);
                
                // Create outbox entry
                NotificationOutbox outbox = NotificationOutbox.builder()
                    .notificationId(saved.getId())
                    .notificationType("RESTAURANT")
                    .eventType("RESERVATION_REQUESTED")
                    .status(NotificationOutbox.Status.PENDING)
                    .payload(mapper.writeValueAsString(Map.of(
                        "notification_id", saved.getId(),
                        "user_id", staffUserId,
                        "title", notif.getTitle()
                    )))
                    .build();
                
                outboxDAO.save(outbox);
                log.debug("✅ Created outbox {} for notification {}", 
                         outbox.getId(), saved.getId());
                
            } catch (Exception e) {
                log.error("❌ Error creating notification for staff {}", staffUserId, e);
                throw new RuntimeException(
                    "Failed to create notification for staff: " + staffUserId, e
                );
                // ← Questo exception farà rollback della INTERA transazione!
                // ← Client riceverà 500 Server Error
                // ← Niente notification creato (consistency!)
            }
        }
        
        log.info("✅ Successfully created {} WebSocket notifications", staffUserIds.size());
    }
}
```

---

## 🌐 PERCHÉ WEBSOCKET NON USA OUTBOX

WebSocket è **DIRECT**, non usa il pattern 3-level outbox.

### Architettura per EMAIL/SMS (usa 3-level outbox):

```
┌──────────────────────┐
│ EVENT OUTBOX         │
│ (L1: evento raw)     │
├──────────────────────┤
│ event_outbox         │
│ ├─ event_id          │
│ ├─ type: RESERVATION │
│ └─ payload: {...}    │
└──────────────────────┘
         ↓ (EventOutboxPoller @1s)
┌──────────────────────┐
│ NOTIFICATION OUTBOX  │
│ (L2: notifica)       │
├──────────────────────┤
│ notification_outbox  │
│ ├─ notification_id   │
│ ├─ status: PENDING   │
│ └─ payload: {...}    │
└──────────────────────┘
         ↓ (NotificationOutboxPoller @5s)
┌──────────────────────┐
│ CHANNEL SEND         │
│ (L3: per canale)     │
├──────────────────────┤
│ notification_channel_│
│ send                 │
│ ├─ notification_id   │
│ ├─ channel: EMAIL    │
│ ├─ is_sent: false    │
│ └─ attempts: 0       │
└──────────────────────┘
         ↓ (ChannelPoller @10s)
    [SEND EMAIL]
    (con retry se fallisce)
```

**Perché 3 livelli per EMAIL?**
- Email può fallire (server down, credenziali wrong, etc.)
- Deve avere retry automatici
- Deve tracciare tentatibi e errori
- Non è real-time (OK se arriva dopo 30 secondi)

---

### Architettura per WEBSOCKET (NO outbox, DIRECT):

```
┌──────────────────────┐
│ NOTIFICATION         │
│ (solo questo!)       │
├──────────────────────┤
│ notification_        │
│ restaurant           │
│ ├─ id                │
│ ├─ user_id           │
│ ├─ title             │
│ ├─ body              │
│ └─ properties        │
└──────────────────────┘
         ↓ (SUBITO!)
┌──────────────────────┐
│ CHANNEL POLLER       │
│ (ChannelPoller @10s) │
├──────────────────────┤
│ Legge notification   │
│ e invia SUBITO       │
│ via WebSocket        │
└──────────────────────┘
         ↓
    [SEND WEBSOCKET]
    (no retry - real-time!)
```

**Perché NO outbox per WebSocket?**
- WebSocket è **real-time**, non ha senso mettere in coda
- Se il client non è connesso, il messaggio si perde (OK per real-time)
- Non ha retry (il client richiederà manualmente se ha perso)
- È **sincrono** rispetto al ChannelPoller

---

## 📊 CONFRONTO: Email vs WebSocket

| Aspetto | EMAIL | WEBSOCKET |
|---------|-------|-----------|
| **Pattern** | 3-level outbox | Direct (NO outbox) |
| **Reliability** | Alta (retry, persistence) | Bassa (best-effort) |
| **Speed** | Lenta (minuti/ore) | Veloce (millisecondi) |
| **Real-time?** | No | Sì |
| **Retry** | Automati (max 3 volte) | No |
| **Persisten Failure** | Marked failed in DB | Silently lost |
| **Quando usare** | Comunicazioni importanti | UI updates, live notifications |

---

## 🎯 TIMELINE FINALE CORRETTO

```
T0:  Customer POST /ask
T1:  Service.save(Reservation)                      [COMMIT]
T2:  Service.publishEvent(ReservationCreatedEvent)
T3:  [WAIT FOR LISTENER - SYNCHRONOUS BLOCK]
     └─ Listener.handle() starts
T4:  Listener SELECT restaurant_users               [QUERY]
T5:  Listener INSERT notification_restaurant (×N)   [INSERTS]
T6:  Listener INSERT notification_outbox (×N)       [INSERTS]
T7:  Listener COMMIT                                [COMMIT]
     └─ Listener returns
T8:  [LISTENER DONE - SERVICE CONTINUES]
T9:  Service returns ReservationDTO
T10: Controller returns HTTP 200 OK ← GUARANTEED notifications exist!

T11: (Async from now on)
     NotificationOutboxPoller @5s: SELECT PENDING, UPDATE PUBLISHED
     
T12: @5s+: 
     NotificationOutboxPoller: INSERT notification_channel_send (WEBSOCKET)
     
T13: @10s:
     ChannelPoller: SELECT PENDING channel_send, FOR EACH: sendWebSocket()
     
T14: @10s+:
     WebSocket clients receive STOMP messages
     ✅ Staff see notifications on screen
```

---

## ⚡ RIEPILOGO FINALE

| Domanda | Risposta |
|---------|----------|
| **Listener riceve INSERT?** | NO! Riceve un EVENTO (Java object) |
| **Quando RabbitMQ?** | Solo se usi @Async (scenario 2). Con listener synchronous, non serve. |
| **Outbox prima di rispondere?** | SÌ! Must be SYNCHRONOUS listener per garantire consistency. |
| **WebSocket usa outbox?** | NO! È DIRECT, senza retry. Se client non connesso, messaggio perso (OK per real-time). |
| **Se listener fallisce?** | Client riceve exception, NO 200 OK (consistency garantito) |
| **Se WebSocket fallisce?** | Client non riceve messaggio, ma HTTP 200 già inviato (best-effort) |

