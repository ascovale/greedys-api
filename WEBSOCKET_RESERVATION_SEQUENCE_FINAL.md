# 🔁 Sequence Diagram: Customer Reservation → Restaurant WebSocket Notifications

**Data:** 14 Novembre 2025

---

## 📊 Diagramma di Sequenza UML (ASCII Art) - CORRETTO

```
Customer          Controller          Service          Repository        Listener        OutboxPoller     ChannelPoller      WebSocket       Staff
   |                  |                   |                  |               |                 |                |                 |              |
   |--POST /ask------>|                   |                  |               |                 |                |                 |              |
   |                  |--createReservation|                  |               |                 |                |                 |              |
   |                  |                   |--save()--------->|               |                 |                |                 |              |
   |                  |                   |                  |--INSERT Reservation to DB       |                 |                |                 |              |
   |                  |                   |<--Reservation(id=123)            |                 |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
   |                  |                   |--publishEvent(ReservationCreatedEvent)---------->|                 |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
   |                  |                   |   [WAIT - Listener is SYNCHRONOUS]               |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
   |                  |                   |                  |               |--SELECT restaurant_users        |                 |              |
   |                  |                   |                  |               |<--[user_id=1,2,3]              |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
   |                  |                   |                  |               |--FOR EACH staff:                |                 |              |
   |                  |                   |                  |               |  ├─INSERT notification_restaurant(user=1)     |                 |              |
   |                  |                   |                  |               |  ├─INSERT notification_outbox(PENDING)        |                 |              |
   |                  |                   |                  |               |  ├─INSERT notification_restaurant(user=2)     |                 |              |
   |                  |                   |                  |               |  ├─INSERT notification_outbox(PENDING)        |                 |              |
   |                  |                   |                  |               |  ├─INSERT notification_restaurant(user=3)     |                 |              |
   |                  |                   |                  |               |  └─INSERT notification_outbox(PENDING)        |                 |              |
   |                  |                   |                  |               |  └─COMMIT                                       |                 |              |
   |                  |                   |<--Event Processed & Return     |                 |                |                 |              |
   |                  |<--ok-------------|                  |               |                 |                |                 |              |
   |<--200 OK (Guaranteed notifications exist!)|                 |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
                                                                              |                 |
                      [5 SECONDS LATER - NotificationOutboxPoller @Scheduled] |                |                |                 |              |
                                                                              |                 |
                                                                              |<--SELECT notification_outbox WHERE status=PENDING        |                 |              |
                                                                              |<--[5000, 5001, 5002]                  |                |                 |              |
                                                                              |                 |                |                 |              |
                                                                              |--UPDATE notification_outbox SET status=PUBLISHED        |                |                 |              |
                                                                              |                 |                |                 |              |
                                                                              |--FOR EACH outbox:                     |                |                 |              |
                                                                              |  └─INSERT notification_channel_send(WEBSOCKET)        |                |                 |              |
                                                                              |  └─INSERT notification_channel_send(WEBSOCKET)        |                |                 |              |
                                                                              |  └─INSERT notification_channel_send(WEBSOCKET)        |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
                      [10 SECONDS LATER - ChannelPoller @Scheduled]                      |                |                 |              |
                                                                              |                 |                |                 |              |
                                                                              |                 |<--SELECT notification_channel_send (is_sent=false)     |              |
                                                                              |                 |<--[10000, 10001, 10002]              |              |
                                                                              |                 |                |                 |              |
                                                                              |                 |--FOR EACH channel_send:            |              |
                                                                              |                 |  ├─SELECT notification_restaurant(1000) |              |
                                                                              |                 |  ├─convertAndSendToUser(1, /queue, payload)---->|
                                                                              |                 |  |                             |--onMessage()
                                                                              |                 |  |                             |--✅ UI Update
                                                                              |                 |  |                             |
                                                                              |                 |  ├─UPDATE is_sent=true         |              |
                                                                              |                 |  |                             |
                                                                              |                 |  ├─SELECT notification_restaurant(1001) |              |
                                                                              |                 |  ├─convertAndSendToUser(2, /queue, payload)---->|
                                                                              |                 |  |                             |--onMessage()
                                                                              |                 |  |                             |--✅ UI Update
                                                                              |                 |  |                             |
                                                                              |                 |  ├─SELECT notification_restaurant(1002) |              |
                                                                              |                 |  ├─convertAndSendToUser(3, /queue, payload)---->|
                                                                              |                 |  |                             |--onMessage()
                                                                              |                 |  |                             |--✅ UI Update
                                                                              |                 |  └─UPDATE is_sent=true         |              |
```

---

## 🎯 Chi fa cosa?

| Componente | Azione | Timing |
|------------|--------|--------|
| **Customer** | POST /ask | T0 |
| **Controller** | Riceve request | T0 |
| **Service** | **INSERT Reservation** in DB | T1 |
| **Service** | publishEvent() | T2 |
| **Listener** | **SELECT staff** (query) | T3 |
| **Listener** | **INSERT notification_restaurant** (×N) | T4 |
| **Listener** | **INSERT notification_outbox** (×N) | T5 |
| **Service** | return response (AFTER listener) | T6 |
| **Controller** | HTTP 200 OK ✅ | T7 |
| **OutboxPoller** | SELECT + UPDATE + INSERT channel_send | T+5s |
| **ChannelPoller** | SELECT + convertAndSendToUser() | T+10s |
| **WebSocket Client** | Riceve messaggio | T+10.1s |

---

## ✅ Punti importanti

### 1️⃣ **Service INSERT Reservation**
```
Service.createReservation():
  reservationDAO.save(reservation)
  └─ Questo fa: INSERT INTO reservation VALUES (...)
```

### 2️⃣ **Service pubblica l'evento**
```
Service.createReservation():
  applicationEventPublisher.publishEvent(new ReservationCreatedEvent(...))
  └─ Spring chiama il Listener SINCRONAMENTE
```

### 3️⃣ **Listener riceve l'EVENTO (non un INSERT)**
```
ReservationEventListener.handleRestaurantWebSocketNotification(event):
  ├─ event è un oggetto Java: ReservationCreatedEvent
  ├─ Contiene: reservationId, restaurantId, customerId, email
  └─ Il listener LEGGE il database (SELECT staff) e CREA notifiche
```

### 4️⃣ **Listener fa SELECT e INSERT**
```
ReservationEventListener:
  ├─ SELECT restaurant_users WHERE restaurant_id = 10
  │  └─ Torna: [user_id=1, user_id=2, user_id=3]
  │
  ├─ FOR EACH staff:
  │  ├─ INSERT notification_restaurant (...)
  │  └─ INSERT notification_outbox (...) status=PENDING
  │
  └─ COMMIT
```

### 5️⃣ **Outbox e ChannelPoller vengono dopo**
```
OutboxPoller @5s:
  ├─ SELECT notification_outbox WHERE status=PENDING
  ├─ UPDATE status=PUBLISHED
  └─ INSERT notification_channel_send (×3)

ChannelPoller @10s:
  ├─ SELECT notification_channel_send
  ├─ FOR EACH: convertAndSendToUser() via WebSocket
  └─ UPDATE is_sent=true
```

---

## 📝 Spiegazione del Flusso

### 1️⃣ **Customer crea prenotazione**
- Customer fa POST a `/customer/reservation/ask` con il DTO della prenotazione
- Controller chiama il Service per salvare la prenotazione

### 2️⃣ **Service salva e pubblica evento**
- **Service fa un INSERT** in `reservation` table via `reservationDAO.save()`
- Service pubblica l'evento `ReservationCreatedEvent` (un oggetto Java, non un DB INSERT!)

### 3️⃣ **Listener intercetta l'evento (SYNCHRONOUS)**
- Spring chiama il `ReservationEventListener` **IMMEDIATAMENTE** (non asincrono)
- Il listener **riceve l'evento** come parametro (non un INSERT)
- Dal listener:
  - **SELECT** tutti i restaurant_users (query)
  - **INSERT** notification_restaurant (×N, una per staff)
  - **INSERT** notification_outbox (×N, una per notifica)
  - **COMMIT** la transazione
- Il listener ritorna al Service

### 4️⃣ **Service ritorna la response**
- Adesso che il listener ha terminato e commitato
- Service ritorna il ReservationDTO al Controller
- Controller invia HTTP 200 OK al cliente
- ✅ **GARANTITO**: Le notifiche sono state create nel database!

### 5️⃣ **OutboxPoller processa gli outbox (@5 secondi)**
- Legge tutte le `notification_outbox` con `status=PENDING`
- Le marca come `PUBLISHED`
- Crea le righe `notification_channel_send` (una per canale, qui WEBSOCKET)

### 6️⃣ **ChannelPoller invia via WebSocket (@10 secondi)**
- Legge tutte le `notification_channel_send` con `is_sent=false`
- Per ogni riga:
  - Legge la notifica da `notification_restaurant`
  - Chiama `SimpMessagingTemplate.convertAndSendToUser()` per inviare tramite WebSocket
  - Marca come `is_sent=true`

### 7️⃣ **RestaurantStaff riceve il messaggio**
- Il WebSocket client riceve il STOMP frame
- La UI si aggiorna in real-time
- La notifica è visualizzata sullo schermo

---

## 💡 Naming delle API

- **Endpoint HTTP**: `askReservation()` (cosa fa da utente: "ask for a reservation")
- **Service**: `createReservation()` (cosa fa internamente: "create and save")

Questo è il pattern comune in Spring: endpoint descrittivo, service tecnico.

