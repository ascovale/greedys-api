# 📚 EventType Reference Guide

## Panoramica

L'enum `EventType` definisce tutti i tipi di eventi di dominio utilizzati nel sistema di notifiche.
Questi eventi sono **indipendenti dal meccanismo di trasporto** (WebSocket, REST, RabbitMQ).

**File**: `greedys_api/src/main/java/com/application/common/domain/event/EventType.java`

---

## 📊 Statistiche

| Categoria | Numero Eventi |
|-----------|---------------|
| Reservation Lifecycle | 4 |
| Modification Request | 4 |
| Reservation Details | 5 |
| Messaging | 4 |
| Support | 5 |
| System | 7 |
| **TOTALE** | **29** |

---

## 🎯 Categorie di Eventi

### 1. RESERVATION LIFECYCLE EVENTS

| EventType | Event Name | Initiated By | Notifies |
|-----------|------------|--------------|----------|
| `RESERVATION_REQUESTED` | reservation.requested | CUSTOMER | RESTAURANT STAFF |
| `RESERVATION_CREATED` | reservation.created | RESTAURANT_USER | CUSTOMER |
| `CUSTOMER_RESERVATION_CREATED` | reservation.customer_created | ADMIN | CUSTOMER |
| `RESERVATION_STATUS_CHANGED` | reservation.status_changed | ANY | BOTH |

#### Dettaglio Eventi

##### `RESERVATION_REQUESTED`
- **Quando**: Customer crea una nuova prenotazione
- **Status iniziale**: `NOT_ACCEPTED`
- **Routing**: `notification.restaurant.reservations` (TEAM)
- **Payload**: `reservationId, customerId, restaurantId, email, datetime, pax, kids, notes, initiated_by=CUSTOMER`

##### `RESERVATION_CREATED`
- **Quando**: Restaurant staff crea prenotazione per un cliente
- **Status iniziale**: `ACCEPTED` (auto-approvata)
- **Routing**: `notification.customer` (PERSONAL)
- **Payload**: `reservationId, customerId, restaurantId, email, date, pax, kids, notes, initiated_by=RESTAURANT`

##### `CUSTOMER_RESERVATION_CREATED`
- **Quando**: Admin/Restaurant user crea prenotazione per conto del cliente
- **Status iniziale**: `ACCEPTED`
- **Routing**: `notification.customer` (PERSONAL)
- **Payload**: `reservationId, customerId, restaurantId, email, datetime, pax, kids, notes, initiated_by=ADMIN`

##### `RESERVATION_STATUS_CHANGED`
- **Quando**: Lo status di una prenotazione cambia
- **Transizioni valide**:
  - `NOT_ACCEPTED` → `ACCEPTED`, `REJECTED`, `DELETED`
  - `ACCEPTED` → `SEATED`, `NO_SHOW`, `DELETED`
- **Payload**: `reservationId, oldStatus, newStatus, reason, rejectionReason`

---

### 2. RESERVATION MODIFICATION REQUEST EVENTS

| EventType | Event Name | Initiated By | Notifies |
|-----------|------------|--------------|----------|
| `RESERVATION_MODIFICATION_REQUESTED` | reservation.modification_requested | CUSTOMER | RESTAURANT STAFF |
| `RESERVATION_MODIFICATION_APPROVED` | reservation.modification_approved | RESTAURANT_USER | CUSTOMER |
| `RESERVATION_MODIFICATION_REJECTED` | reservation.modification_rejected | RESTAURANT_USER | CUSTOMER |
| `RESERVATION_MODIFIED_BY_RESTAURANT` | reservation.modified_by_restaurant | RESTAURANT_USER | CUSTOMER |

#### Flusso Modifiche

```
┌─────────────────┐
│    CUSTOMER     │
│ richiede modifica
└────────┬────────┘
         │
         ▼
   RESERVATION_MODIFICATION_REQUESTED
         │
         ▼
┌─────────────────┐
│   RESTAURANT    │
│  approva/rifiuta
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
APPROVED   REJECTED
```

---

### 3. RESERVATION DETAIL EVENTS

| EventType | Event Name | Usage |
|-----------|------------|-------|
| `RESERVATION_MODIFIED` | reservation.modified | Generic modification |
| `RESERVATION_TIME_CHANGED` | reservation.time_changed | Time only |
| `RESERVATION_PARTY_SIZE_CHANGED` | reservation.party_size_changed | Pax change |
| `RESERVATION_NOTES_UPDATED` | reservation.notes_updated | Notes change |
| `RESERVATION_TABLE_CHANGED` | reservation.table_changed | Table assignment |
| `RESERVATION_ASSIGNED_TO_STAFF` | reservation.assigned_to_staff | Staff assignment |

---

### 4. MESSAGING EVENTS

| EventType | Event Name | Description |
|-----------|------------|-------------|
| `RESERVATION_MESSAGE_SENT` | reservation.message_sent | Message in conversation |
| `RESERVATION_MESSAGE_READ` | reservation.message_read | Message read |
| `RESERVATION_CONVERSATION_CLOSED` | reservation.conversation_closed | Conversation archived |
| `RESERVATION_CONVERSATION_REOPENED` | reservation.conversation_reopened | Conversation reopened |

---

### 5. SUPPORT EVENTS

| EventType | Event Name | Description |
|-----------|------------|-------------|
| `SUPPORT_TICKET_OPENED` | support.ticket_opened | New ticket |
| `SUPPORT_TICKET_CLOSED` | support.ticket_closed | Ticket resolved |
| `SUPPORT_MESSAGE_SENT` | support.message_sent | Message in ticket |
| `SUPPORT_TICKET_ASSIGNED` | support.ticket_assigned | Ticket reassigned |
| `SUPPORT_TICKET_ESCALATED` | support.ticket_escalated | Ticket escalated |

---

### 6. SYSTEM EVENTS (Infrastructure)

| EventType | Event Name | Description |
|-----------|------------|-------------|
| `CLIENT_WS_CONNECTED` | system.client_ws_connected | WebSocket connect |
| `CLIENT_WS_DISCONNECTED` | system.client_ws_disconnected | WebSocket disconnect |
| `CLIENT_WS_RECONNECTED` | system.client_ws_reconnected | WebSocket reconnect |
| `CLIENT_SYNC_REQUIRED` | system.client_sync_required | Sync needed |
| `CLIENT_FULL_SYNC_REQUESTED` | system.client_full_sync_requested | Full refresh |
| `CLIENT_DELTA_SYNC_REQUESTED` | system.client_delta_sync_requested | Delta sync |
| `CLIENT_SYNC_COMPLETED` | system.client_sync_completed | Sync done |

> ⚠️ **NOTA**: Gli eventi System sono per coordinazione interna e NON dovrebbero essere inviati ai client via WebSocket.

---

## 🔄 Helper Methods

L'enum fornisce helper methods per categorizzare gli eventi:

```java
EventType type = EventType.RESERVATION_REQUESTED;

type.isReservationEvent();   // true (starts with "reservation.")
type.isSystemEvent();        // false
type.isMessagingEvent();     // false
type.isSupportEvent();       // false
type.isStatusChangeEvent();  // false
```

---

## 📍 Utilizzo nel Codice

### ✅ CORRETTO - Usare l'enum

```java
import com.application.common.domain.event.EventType;

EventOutbox eventOutbox = EventOutbox.builder()
    .eventId(EventType.RESERVATION_REQUESTED.name() + "_" + id)
    .eventType(EventType.RESERVATION_REQUESTED.name())
    .build();
```

### ❌ SBAGLIATO - Stringhe hardcoded

```java
// NON FARE COSÌ!
EventOutbox eventOutbox = EventOutbox.builder()
    .eventType("RESERVATION_REQUESTED")  // ← String hardcoded
    .build();
```

---

## 📁 File che Usano EventType

### Correttamente con Enum

| File | Metodo |
|------|--------|
| `CustomerReservationService.java` | `createReservationRequestedEvent()` |
| `ReservationService.java` | `createRestaurantReservationCreatedEvent()` |
| `ReservationService.java` | `createReservationModificationApprovedEvent()` |
| `ReservationService.java` | `createReservationModificationRejectedEvent()` |
| `ReservationService.java` | `createReservationModifiedByRestaurantEvent()` |
| `AdminReservationService.java` | `createCustomerReservationCreatedEvent()` |
| `ReservationEventListener.java` | `handleRestaurantWebSocketNotification()` |

---

## 🛠️ Aggiungere un Nuovo EventType

1. **Aggiungi all'enum** in `EventType.java`:
   ```java
   /**
    * Description of the event
    * Payload: field1, field2, ...
    */
   NEW_EVENT_TYPE("category.event_name"),
   ```

2. **Usa l'enum nel codice**:
   ```java
   .eventType(EventType.NEW_EVENT_TYPE.name())
   ```

3. **Aggiorna questa documentazione**

---

## 📅 Changelog

| Data | Modifica |
|------|----------|
| 2024-01-XX | Aggiunto `RESERVATION_REQUESTED` (mancante) |
| 2024-01-XX | Aggiunto `CUSTOMER_RESERVATION_CREATED` (mancante) |
| 2024-01-XX | Aggiunto `RESERVATION_MODIFICATION_REQUESTED` (mancante) |
| 2024-01-XX | Aggiunto `RESERVATION_MODIFICATION_APPROVED` (mancante) |
| 2024-01-XX | Aggiunto `RESERVATION_MODIFICATION_REJECTED` (mancante) |
| 2024-01-XX | Aggiunto `RESERVATION_MODIFIED_BY_RESTAURANT` (mancante) |
| 2024-01-XX | Migrati tutti i servizi da stringhe hardcoded a enum |
