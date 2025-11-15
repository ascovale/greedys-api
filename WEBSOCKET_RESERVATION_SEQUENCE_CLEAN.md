# 🔁 Sequence Diagram: Customer Reservation → Restaurant WebSocket Notifications

**Data:** 14 Novembre 2025

---

## 📊 Diagramma di Sequenza UML (ASCII Art)

```
Customer          Controller          Service          Repository        Listener        OutboxPoller     ChannelPoller      WebSocket       Staff
   |                  |                   |                  |               |                 |                |                 |              |
   |--POST /ask------>|                   |                  |               |                 |                |                 |              |
   |                  |--createReservation|                  |               |                 |                |                 |              |
   |                  |                   |--save()--------->|               |                 |                |                 |              |
   |                  |                   |                  |--INSERT------>|                 |                |                 |              |
   |                  |                   |<--ok-------------|               |                 |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
   |                  |                   |--publishEvent()  |               |                 |                |                 |              |
   |                  |                   |--ReservationCreatedEvent------->|                 |                |                 |              |
   |                  |                   |<--ok-------------|               |                 |                |                 |              |
   |                  |<--ok-------------|                  |               |                 |                |                 |              |
   |<--200 OK---------|                   |                  |               |                 |                |                 |              |
   |                  |                   |                  |               |                 |                |                 |              |
                                                               |
                                                               |--SELECT staff WHERE restaurant_id=10
                                                               |<--[user_id=1,2,3]
                                                               |
                                                               |--INSERT notification_restaurant (user_id=1)
                                                               |--INSERT notification_outbox (PENDING)
                                                               |
                                                               |--INSERT notification_restaurant (user_id=2)
                                                               |--INSERT notification_outbox (PENDING)
                                                               |
                                                               |--INSERT notification_restaurant (user_id=3)
                                                               |--INSERT notification_outbox (PENDING)
                                                               |--COMMIT
   |                  |                   |                  |               |                 |
                                                                              |<--SELECT notification_outbox WHERE status=PENDING
                                                                              |<--[5000, 5001, 5002]
                                                                              |
                                                                              |--UPDATE notification_outbox SET status=PUBLISHED
                                                                              |
                                                                              |--INSERT notification_channel_send (WEBSOCKET)
                                                                              |--INSERT notification_channel_send (WEBSOCKET)
                                                                              |--INSERT notification_channel_send (WEBSOCKET)
   |                  |                   |                  |               |                 |
                                                                                                 |<--SELECT notification_channel_send (is_sent=false)
                                                                                                 |<--[10000, 10001, 10002]
                                                                                                 |
                                                                                                 |--SELECT notification_restaurant (id=1000)
                                                                                                 |<--{user_id=1, title, body, properties}
                                                                                                 |
                                                                                                 |--convertAndSendToUser(1, /queue/notifications, payload)
                                                                                                 |----------STOMP Frame-------->|
                                                                                                 |                             |--onMessage()
                                                                                                 |                             |--UPDATE UI
                                                                                                 |                             |--✅ Displayed
                                                                                                 |
                                                                                                 |--UPDATE notification_channel_send SET is_sent=true
                                                                                                 |
                                                                                                 |--SELECT notification_restaurant (id=1001)
                                                                                                 |--convertAndSendToUser(2, /queue/notifications, payload)
                                                                                                 |----------STOMP Frame-------->|
                                                                                                 |                             |--onMessage()
                                                                                                 |                             |--✅ Displayed
                                                                                                 |
                                                                                                 |--SELECT notification_restaurant (id=1002)
                                                                                                 |--convertAndSendToUser(3, /queue/notifications, payload)
                                                                                                 |----------STOMP Frame-------->|
                                                                                                 |                             |--onMessage()
                                                                                                 |                             |--✅ Displayed
```

---

## 📝 Spiegazione del Flusso

### 1️⃣ **Customer crea prenotazione**
- Customer fa POST a `/customer/reservation/ask` con il DTO della prenotazione
- Controller chiama il Service per salvare la prenotazione
- Service salva in DB e **pubblica un evento** `ReservationCreatedEvent`

### 2️⃣ **Event Listener intercetta l'evento (SYNCHRONOUS)**
- Il listener viene triggerato subito dopo la pubblicazione dell'evento
- Estrae il `restaurantId` dall'evento
- **Queries tutti i staff** del ristorante
- **Per ogni staff**, crea:
  - Una riga in `notification_restaurant` (la notifica stessa)
  - Una riga in `notification_outbox` con status=PENDING
- Fa il COMMIT della transazione

### 3️⃣ **NotificationOutboxPoller (@5 secondi)**
- Legge tutte le righe `notification_outbox` con status=PENDING
- Le marca come PUBLISHED
- Per ogni riga, crea un'entry in `notification_channel_send` (uno per canale)
- Nel nostro caso: canale WEBSOCKET

### 4️⃣ **ChannelPoller (@10 secondi)**
- Legge tutte le righe `notification_channel_send` con is_sent=false
- Per ogni riga:
  - Legge la notifica da `notification_restaurant`
  - Chiama `SimpMessagingTemplate.convertAndSendToUser()` per inviare tramite WebSocket
  - Marca come `is_sent=true`

### 5️⃣ **RestaurantStaff riceve il messaggio**
- Il WebSocket client riceve il STOMP frame
- La UI si aggiorna in real-time
- La notifica è visualizzata sullo schermo

---

## 🎯 Aspetti chiave

| Aspetto | Dettagli |
|---------|----------|
| **Pattern** | 3-level Outbox (Event → Notification → ChannelSend) |
| **Listener** | SYNCHRONOUS (non @Async) - garantisce consistency |
| **Broadcast** | Un evento → N notifiche (una per staff) |
| **Pollers** | Schedulati con @Scheduled (5s e 10s) |
| **Real-time** | WebSocket con STOMP via SimpMessagingTemplate |
| **Idempotency** | EventOutbox.processed_by e notification_channel_send.is_sent |

---

## 💡 Naming delle API

- **Endpoint HTTP**: `askReservation()` (cosa fa da utente: "ask for a reservation")
- **Service**: `createReservation()` (cosa fa internamente: "create and save")

Questo è il pattern comune in Spring: endpoint descrittivo, service tecnico.

