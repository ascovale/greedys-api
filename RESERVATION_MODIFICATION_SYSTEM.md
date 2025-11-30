# 📋 Implementazione Sistema di Modifica Prenotazioni con Approvazione

## 📌 Situazione Risolta

### Problema
Non era chiaro come gestire il flusso di modifica delle prenotazioni quando:
- Un **CUSTOMER** chiede una modifica → deve attendere approvazione
- Un **RESTAURANT USER** o **ADMIN** modifica → applica direttamente

### Soluzione Implementata

La soluzione implementa tre scenari distinti con rispettive notifiche via Event Outbox:

---

## 🎯 Scenario 1: Customer Richiede Modifica (PENDING_APPROVAL)

**Flusso:**
```
Customer Request → ReservationModificationRequest (PENDING_APPROVAL) → EVENT → RestaurantUser/Admin Review
```

**Metodo:** `CustomerReservationService.requestModifyReservation()`

**Cosa succede:**
1. Customer chiama l'endpoint per richiedere una modifica
2. Viene creato un `ReservationModificationRequest` con stato `PENDING_APPROVAL`
3. Vengono salvati ENTRAMBI i valori:
   - **Original values**: Valori attuali della prenotazione
   - **Requested values**: Nuovi valori richiesti dal customer
4. **📌 EVENT CREATO**: `RESERVATION_MODIFICATION_REQUESTED`
   - Tipo evento: `RESERVATION_MODIFICATION_REQUESTED`
   - Aggregate Type: `CUSTOMER` (mostra che è iniziato dal customer)
   - Notifica: Restaurant staff per approvazione/rifiuto
   - Payload include: modifiche richieste + motivo customer

**Event Payload Esempio:**
```json
{
  "modificationRequestId": 123,
  "reservationId": 456,
  "customerId": 789,
  "restaurantId": 1001,
  "email": "customer@example.com",
  "originalDate": "2025-12-25",
  "requestedDate": "2025-12-26",
  "originalPax": 4,
  "requestedPax": 6,
  "originalKids": 1,
  "requestedKids": 2,
  "originalNotes": "Window seat",
  "requestedNotes": "Window seat, no garlic",
  "customerReason": "Parent is arriving later",
  "initiated_by": "CUSTOMER"
}
```

---

## ✅ Scenario 2: Restaurant Approva la Modifica

**Flusso:**
```
RestaurantUser Review → approveModificationRequest() → Reservation Updated → EVENT → Customer Notified
```

**Metodo:** `ReservationService.approveModificationRequest(Long modificationRequestId, AbstractUser approverUser)`

**Cosa succede:**
1. Restaurant user (o admin) clicca "Approva" sulla modifica richiesta
2. Vengono applicate le modifiche richieste al Reservation:
   - `pax` = `requestedPax`
   - `kids` = `requestedKids`
   - `notes` = `requestedNotes`
   - `reservationDateTime` = `requestedDateTime`
3. `ReservationModificationRequest.status` → `APPROVED`
4. Reservation aggiornata con `modifiedAt` e `modifiedBy`
5. **📌 EVENT CREATO**: `RESERVATION_MODIFICATION_APPROVED`
   - Tipo evento: `RESERVATION_MODIFICATION_APPROVED`
   - Aggregate Type: `RESTAURANT` (mostra approvazione da restaurant)
   - Notifica: Customer (conferma che modifica è approvata)
   - Payload include: date/pax originali e approvate + user type che ha approvato

**Audit Trail:** `ReservationAudit` con `action = MODIFICATION_APPROVED`

**Event Payload Esempio:**
```json
{
  "modificationRequestId": 123,
  "reservationId": 456,
  "customerId": 789,
  "restaurantId": 1001,
  "email": "customer@example.com",
  "originalDate": "2025-12-25",
  "approvedDate": "2025-12-26",
  "originalPax": 4,
  "approvedPax": 6,
  "approverUserType": "RUser",
  "initiated_by": "RESTAURANT"
}
```

---

## ❌ Scenario 3: Restaurant Rifiuta la Modifica

**Flusso:**
```
RestaurantUser Review → rejectModificationRequest() → EVENT → Customer Notified
```

**Metodo:** `ReservationService.rejectModificationRequest(Long modificationRequestId, String rejectReason, AbstractUser approverUser)`

**Cosa succede:**
1. Restaurant user (o admin) clicca "Rifiuta" sulla modifica richiesta
2. `ReservationModificationRequest.status` → `REJECTED`
3. Viene salvato il `approvalReason` (motivo del rifiuto)
4. **LA PRENOTAZIONE RIMANE INVARIATA** - solo il request viene rifiutato
5. **📌 EVENT CREATO**: `RESERVATION_MODIFICATION_REJECTED`
   - Tipo evento: `RESERVATION_MODIFICATION_REJECTED`
   - Aggregate Type: `RESTAURANT` (mostra rifiuto da restaurant)
   - Notifica: Customer (comunica che modifica non è approvata + motivo)
   - Payload include: modifiche richieste + motivo rifiuto

**Audit Trail:** `ReservationAudit` con `action = MODIFICATION_REJECTED`

**Event Payload Esempio:**
```json
{
  "modificationRequestId": 123,
  "reservationId": 456,
  "customerId": 789,
  "restaurantId": 1001,
  "email": "customer@example.com",
  "requestedDate": "2025-12-26",
  "requestedPax": 6,
  "rejectionReason": "Date is fully booked",
  "rejectorUserType": "RUser",
  "initiated_by": "RESTAURANT"
}
```

---

## 🚀 Scenario 4: Restaurant Modifica Direttamente (NO APPROVAL NEEDED)

**Flusso:**
```
RestaurantUser Edit → modifyReservationDirectly() → Reservation Updated → EVENT → Customer Notified
```

**Metodo:** `ReservationService.modifyReservationDirectly(Long reservationId, Integer pax, Integer kids, String notes, AbstractUser modifiedByUser)`

**Cosa succede:**
1. Restaurant staff ha permessi per modificare direttamente (es. admin, manager)
2. Modifica viene applicata DIRETTAMENTE senza richiedere approvazione
3. **NON** viene creato `ReservationModificationRequest`
4. Reservation aggiornata con:
   - Nuovi pax/kids/notes
   - `modifiedAt` = now
   - `modifiedBy` = staff user
5. **📌 EVENT CREATO**: `RESERVATION_MODIFIED_BY_RESTAURANT`
   - Tipo evento: `RESERVATION_MODIFIED_BY_RESTAURANT`
   - Aggregate Type: `RESTAURANT`
   - Notifica: Customer (comunicazione che il ristorante ha modificato)
   - Payload include: nuovi valori + user type che ha modificato

**Audit Trail:** `ReservationAudit` con `action = MODIFIED_BY_RESTAURANT`

**Event Payload Esempio:**
```json
{
  "reservationId": 456,
  "customerId": 789,
  "restaurantId": 1001,
  "email": "customer@example.com",
  "modifiedDate": "2025-12-25",
  "pax": 6,
  "kids": 2,
  "notes": "Window seat, no garlic",
  "modifierUserType": "RUser",
  "initiated_by": "RESTAURANT"
}
```

---

## 📊 Entità Aggiunte/Modificate

### 1️⃣ ReservationModificationRequest (NEW)
```
Table: reservation_modification_request

Campi Principali:
├── id (PK)
├── reservation_id (FK) → Reservation originale
├── status (ENUM) → PENDING_APPROVAL | APPROVED | REJECTED | CANCELLED
│
├── ORIGINAL VALUES (for comparison)
│   ├── original_date
│   ├── original_datetime
│   ├── original_pax
│   ├── original_kids
│   └── original_notes
│
├── REQUESTED NEW VALUES
│   ├── requested_date
│   ├── requested_datetime
│   ├── requested_pax
│   ├── requested_kids
│   └── requested_notes
│
├── REASON FIELDS
│   ├── customer_reason (why customer wants to change)
│   └── approval_reason (why restaurant approved/rejected)
│
└── AUDITING
    ├── requested_by (Customer)
    ├── requested_at
    ├── reviewed_by (Restaurant staff)
    └── reviewed_at
```

### 2️⃣ ReservationAudit.AuditAction (UPDATED)
```
ENUM values added:
├── MODIFICATION_REQUESTED (customer richiede modifica)
├── MODIFICATION_APPROVED (restaurant approva)
├── MODIFICATION_REJECTED (restaurant rifiuta)
├── MODIFICATION_APPLIED (modifica applicata al reservation)
└── MODIFIED_BY_RESTAURANT (restaurant ha modificato direttamente)
```

### 3️⃣ Reservation (UNCHANGED)
```
Campi già esistenti che vengono usati per tracking:
├── modifiedAt → LocalDateTime
├── modifiedBy → AbstractUser (chef, ruser, o admin che ha modificato)
└── status → rimane uguale, non cambia per modifiche
```

---

## 📌 Event Types Creati

| Event Type | Triggered By | Notifies | Scenario | Payload Keys |
|---|---|---|---|---|
| `RESERVATION_MODIFICATION_REQUESTED` | Customer | Restaurant Staff | Customer chiede modifica | modificationRequestId, originalDate, requestedDate, etc. |
| `RESERVATION_MODIFICATION_APPROVED` | Restaurant Staff | Customer | Restaurant approva richiesta | modificationRequestId, approvedDate, approverUserType |
| `RESERVATION_MODIFICATION_REJECTED` | Restaurant Staff | Customer | Restaurant rifiuta richiesta | modificationRequestId, rejectionReason, rejectorUserType |
| `RESERVATION_MODIFIED_BY_RESTAURANT` | Restaurant Staff | Customer | Restaurant modifica direttamente | reservationId, modifierUserType, pax, kids |

---

## 🔄 Sequence Diagram: Scenario Customer Modifica

```
CUSTOMER                      SYSTEM                    RESTAURANT STAFF
    │                            │                            │
    ├─ requestModifyReservation()─→
    │                            ├─ Create ReservationModificationRequest
    │                            │   (status: PENDING_APPROVAL)
    │                            │
    │                            ├─ Create EventOutbox
    │                            │   RESERVATION_MODIFICATION_REQUESTED
    │                            │
    │                            └─ Send to notification.restaurant queue
    │                                   │
    │                                   └──→ Notification received ─→
    │                                            Restaurant sees request
    │                                            [Customer wants 6 pax instead of 4]
    │                                            │
    │                                            │ [Restaurant clicks APPROVE]
    │                                            ├─ approveModificationRequest()
    │                                            │   ├─ Apply changes to Reservation
    │                                            │   ├─ Set status: APPROVED
    │                                            │   └─ Create EventOutbox
    │                                            │       RESERVATION_MODIFICATION_APPROVED
    │                                            │       └─ Send to notification.customer queue
    │
    ← ─ ─ ─ ─ Notification received ─ ─ ─ ─ ─ ─┤
         Customer sees: "Your modification was approved!"
```

---

## 📋 ReservationModificationRequest DAO

```java
interface ReservationModificationRequestDAO {
    findPendingByReservationId(Long reservationId) → List
    findPendingByRestaurantId(Long restaurantId) → List
    findByRestaurantIdAndStatus(Long restaurantId, Status status) → List
    findById(Long id) → Optional
}
```

---

## 🎯 Implementazione Completata

✅ **ReservationModificationRequest.java** - Entity per tracking richieste
✅ **ReservationModificationRequestDAO.java** - Repository for DB operations
✅ **ReservationAudit.AuditAction** - 5 nuove audit actions
✅ **CustomerReservationService.requestModifyReservation()** - Customer richiesta
✅ **CustomerReservationService.createReservationModificationRequestedEvent()** - Event creation
✅ **ReservationService.approveModificationRequest()** - Approvazione
✅ **ReservationService.rejectModificationRequest()** - Rifiuto
✅ **ReservationService.modifyReservationDirectly()** - Modifica diretta
✅ **ReservationService - 4 metodi event creation** - Event payloads
✅ **All with 0 compilation errors** ✅

---

## 🚀 Prossimi Passi (se necessario)

1. **Controller Endpoints**:
   - `PUT /restaurant/reservation/approve-modification/{modId}` → approveModificationRequest()
   - `PUT /restaurant/reservation/reject-modification/{modId}` → rejectModificationRequest()
   - `PUT /restaurant/reservation/{id}/modify-direct` → modifyReservationDirectly()
   - `GET /restaurant/reservation/pending-modifications` → findPendingByRestaurantId()

2. **Test Suite**:
   - Unit tests per ogni scenario (approve, reject, direct modify)
   - Integration tests end-to-end con event verification
   - Test per audit trail tracking

3. **UI Changes**:
   - Approval interface per restaurant staff
   - Notification UI per customer
   - Comparison view (original vs requested values)

4. **Permission Checks**:
   - Verifica che solo RestaurantUser/Admin possono approvare/rifiutare
   - Verifica che solo permissioned users possono modificare direttamente

---

## 📞 Logica di Routing Eventi (Existing System)

Gli events creati seguono la logica di routing già implementata:

```
EventOutbox con initiated_by=CUSTOMER
    ↓
EventOutboxOrchestrator legge initiate_by
    ↓
Route to: notification.restaurant.reservations (TEAM queue)
    ↓
RestaurantUserNotification creata per tutti gli staff

---

EventOutbox con initiated_by=RESTAURANT
    ↓
EventOutboxOrchestrator legge initiated_by
    ↓
Route to: notification.customer (PERSONAL queue)
    ↓
CustomerNotification creata per il customer
```

Tutti gli eventi di modifica seguono questo pattern!
