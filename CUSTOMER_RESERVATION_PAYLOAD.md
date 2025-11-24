# CustomerReservationService - Payload EventOutbox

## Flusso Completo

Quando un **customer crea una prenotazione** in `CustomerReservationService.createReservation()`:

```java
public CustomerReservationDTO createReservation(CustomerNewReservationDTO dTO, Customer currentUser) {
    // 1. Create Reservation entity
    Reservation reservation = Reservation.builder()
        .customer(currentUser)
        .slot(slot)
        .pax(dTO.getPax())
        .kids(dTO.getKids())
        .notes(dTO.getNotes())
        .date(dTO.getReservationDay())
        .status(Reservation.Status.NOT_ACCEPTED)
        .build();
    
    // 2. Save to DB
    Reservation savedReservation = reservationService.createNewReservation(reservation);
    
    // 3. 📌 CREATE RESERVATION_REQUESTED EVENT
    createReservationRequestedEvent(savedReservation);  // ← Payload sent here
    
    // 4. Return DTO to client
    return reservationMapper.toDTO(savedReservation);
}
```

---

## EventOutbox Creato e Salvato

### Metodo: `createReservationRequestedEvent()`

```java
private void createReservationRequestedEvent(Reservation reservation) {
    // Generate unique event ID
    String eventId = "RESERVATION_REQUESTED_" + reservation.getId() + "_" + System.currentTimeMillis();
    
    // Build JSON payload with all reservation data
    String payload = buildReservationPayload(reservation);
    
    // Create EventOutbox entity
    EventOutbox eventOutbox = EventOutbox.builder()
        .eventId(eventId)                        // ← Unique identifier
        .eventType("RESERVATION_REQUESTED")      // ← Event type
        .aggregateType("CUSTOMER")               // ← Who created it
        .aggregateId(reservation.getId())        // ← Reference to reservation
        .payload(payload)                        // ← JSON data
        .status(EventOutbox.Status.PENDING)      // ← Status (polled later)
        .build();
    
    // Save to DB
    eventOutboxDAO.save(eventOutbox);
    
    log.info("✅ Created EventOutbox RESERVATION_REQUESTED: eventId={}, reservationId={}, aggregateType=CUSTOMER, status=PENDING", 
        eventId, reservation.getId());
}
```

---

## Payload JSON Structure

### Metodo: `buildReservationPayload()`

```java
private String buildReservationPayload(Reservation reservation) {
    // Extract data from reservation
    Long customerId = reservation.getCustomer() != null ? reservation.getCustomer().getId() : null;
    String customerEmail = reservation.getCustomer() != null ? reservation.getCustomer().getEmail() : "anonymous";
    Long restaurantId = reservation.getSlot().getService().getRestaurant().getId();
    Integer kids = reservation.getKids() != null ? reservation.getKids() : 0;
    String notes = reservation.getNotes() != null ? reservation.getNotes().replace("\"", "\\\"") : "";
    
    // Build JSON string
    return String.format(
        "{\"reservationId\":%d,\"customerId\":%s,\"restaurantId\":%d,\"email\":\"%s\",\"date\":\"%s\",\"pax\":%d,\"kids\":%d,\"notes\":\"%s\",\"initiated_by\":\"CUSTOMER\"}",
        reservation.getId(),
        customerId != null ? customerId : "null",
        restaurantId,
        customerEmail,
        reservation.getDate().toString(),
        reservation.getPax(),
        kids,
        notes
    );
}
```

### Esempio JSON Output

```json
{
  "reservationId": 12345,
  "customerId": 789,
  "restaurantId": 5,
  "email": "mario@example.com",
  "date": "2025-12-24",
  "pax": 4,
  "kids": 2,
  "notes": "Vegetarian dishes please",
  "initiated_by": "CUSTOMER"
}
```

---

## EventOutbox Record in Database

**Table**: `event_outbox`

| Column | Value | Note |
|--------|-------|------|
| `id` | auto-generated | Primary key |
| `event_id` | `RESERVATION_REQUESTED_12345_1732462890123` | Unique event identifier |
| `event_type` | `RESERVATION_REQUESTED` | Event type |
| `aggregate_type` | `CUSTOMER` | Who initiated |
| `aggregate_id` | `12345` | Reservation ID |
| `payload` | `{"reservationId":12345, "customerId":789, ...}` | JSON payload |
| `status` | `PENDING` | Status (will be PROCESSED after EventOutboxOrchestrator polls) |
| `created_at` | `2025-11-24 10:15:30` | Timestamp |
| `updated_at` | `2025-11-24 10:15:30` | Timestamp |

---

## What Gets Sent to RabbitMQ

### Step 1: EventOutboxOrchestrator Polls (every 1 second)

```java
// EventOutboxOrchestrator.orchestrate() runs continuously
List<EventOutbox> pendingEvents = eventOutboxDAO.findByStatus(PENDING);

for (EventOutbox event : pendingEvents) {
    // Determine target queue based on initiated_by
    String targetQueue = determineTargetQueue(event);
    // → Returns: "notification.restaurant.reservations"
    
    // Build message for RabbitMQ
    Map<String, Object> message = buildMessageForRabbitMQ(event);
    // → Contains the payload + metadata
    
    // Publish to RabbitMQ
    publishToQueue(targetQueue, message);
    
    // Mark as processed
    event.setStatus(EventOutbox.Status.PROCESSED);
    eventOutboxDAO.save(event);
}
```

### Step 2: Message to RabbitMQ

**Queue**: `notification.restaurant.reservations`

**Message Structure**:

```java
Map<String, Object> message = {
    "event_outbox_id": 12345,
    "event_id": "RESERVATION_REQUESTED_12345_1732462890123",
    "event_type": "RESERVATION_REQUESTED",
    "aggregate_type": "CUSTOMER",
    "aggregate_id": 12345,
    "restaurant_id": 5,
    "customer_id": 789,
    "payload": {
        "reservationId": 12345,
        "customerId": 789,
        "restaurantId": 5,
        "email": "mario@example.com",
        "date": "2025-12-24",
        "pax": 4,
        "kids": 2,
        "notes": "Vegetarian dishes please",
        "initiated_by": "CUSTOMER"
    }
}
```

---

## Complete Flow

```
┌────────────────────────────────────────────────────────────┐
│ 1. Customer creates reservation via API                    │
│    POST /api/customer/reservations                         │
│    Body: {pax: 4, kids: 2, date: "2025-12-24", ...}       │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ 2. CustomerReservationService.createReservation()          │
│    ├─ Create Reservation entity                           │
│    ├─ Save to reservation table                           │
│    └─ Call: createReservationRequestedEvent(reservation)  │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ 3. buildReservationPayload()                               │
│    ├─ Extract: customerId, email, restaurantId, ...       │
│    ├─ Add: initiated_by="CUSTOMER"                        │
│    └─ Return: JSON string                                  │
│                                                            │
│    {                                                       │
│      "reservationId": 12345,                              │
│      "customerId": 789,                                    │
│      "restaurantId": 5,                                    │
│      "email": "mario@example.com",                         │
│      "date": "2025-12-24",                                │
│      "pax": 4,                                             │
│      "kids": 2,                                            │
│      "notes": "Vegetarian...",                             │
│      "initiated_by": "CUSTOMER"  ← KEY                    │
│    }                                                       │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ 4. createReservationRequestedEvent()                       │
│    ├─ Create EventOutbox:                                 │
│    │  ├─ eventId: RESERVATION_REQUESTED_12345_...         │
│    │  ├─ eventType: RESERVATION_REQUESTED                 │
│    │  ├─ aggregateType: CUSTOMER                          │
│    │  ├─ payload: [JSON from step 3]                      │
│    │  └─ status: PENDING                                  │
│    └─ Save to event_outbox table                          │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼ (Stored in DB)
┌────────────────────────────────────────────────────────────┐
│ event_outbox table:                                        │
│ ├─ id: 1                                                   │
│ ├─ event_id: RESERVATION_REQUESTED_12345_...              │
│ ├─ event_type: RESERVATION_REQUESTED                      │
│ ├─ payload: {..., "initiated_by": "CUSTOMER"}            │
│ └─ status: PENDING                                         │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼ (1 second later)
┌────────────────────────────────────────────────────────────┐
│ 5. EventOutboxOrchestrator polls (every 1s)              │
│    ├─ Find all PENDING events                             │
│    ├─ determineTargetQueue(event)                         │
│    │  └─ Reads: initiated_by="CUSTOMER"                  │
│    │  └─ Returns: "notification.restaurant.reservations"  │
│    └─ buildMessageForRabbitMQ(event)                      │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ 6. Publish to RabbitMQ                                     │
│    Queue: notification.restaurant.reservations            │
│    Message:                                                │
│    {                                                       │
│      "event_id": "RESERVATION_REQUESTED_12345_...",       │
│      "event_type": "RESERVATION_REQUESTED",               │
│      "restaurant_id": 5,                                   │
│      "customer_id": 789,                                   │
│      "payload": {                                          │
│        ...all fields from step 3...                        │
│      }                                                     │
│    }                                                       │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ 7. RestaurantTeamNotificationListener receives             │
│    @RabbitListener(queues="notification.restaurant.reservations")
│    └─ Triggers: onTeamNotificationMessage()               │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ 8. RestaurantTeamOrchestrator disaggregates               │
│    ├─ Load ALL staff for restaurant 5                     │
│    ├─ For each staff: create notification with read_by_all=true
│    ├─ Set destination: /topic/restaurant/5/reservations  │
│    └─ Send to WebSocket team channel                      │
└────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ ✅ RESULT: ALL restaurant staff see notification           │
│    Topic: /topic/restaurant/5/reservations               │
│    Message: New reservation from customer!                 │
└────────────────────────────────────────────────────────────┘
```

---

## Key Fields in Payload

| Field | Source | Purpose |
|-------|--------|---------|
| `reservationId` | `reservation.getId()` | Reference to reservation |
| `customerId` | `reservation.getCustomer().getId()` | Who made it |
| `restaurantId` | `reservation.getSlot().getService().getRestaurant().getId()` | Target restaurant |
| `email` | `reservation.getCustomer().getEmail()` | Contact info |
| `date` | `reservation.getDate()` | Reservation date |
| `pax` | `reservation.getPax()` | Number of guests |
| `kids` | `reservation.getKids()` | Number of children |
| `notes` | `reservation.getNotes()` | Special requests |
| **`initiated_by`** | **`"CUSTOMER"`** | **Routing decision** ← KEY |

---

## Routing Decision

The `initiated_by` field is **critical** for routing:

```
IF initiated_by == "CUSTOMER" AND eventType contains "RESERVATION"
  THEN route to: notification.restaurant.reservations (TEAM queue)
  
IF initiated_by == "RESTAURANT" AND eventType contains "RESERVATION"
  THEN route to: notification.customer (PERSONAL queue)
```

This ensures that **customer-initiated reservations are sent to the TEAM queue**, where **ALL restaurant staff** receive the notification.
