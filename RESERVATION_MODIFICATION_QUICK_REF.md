# 🔄 Quick Reference: Flusso di Modifica Prenotazioni

## 3️⃣ Scenari Implementati

### 1️⃣ CUSTOMER Richiede Modifica
```
CustomerReservationService.requestModifyReservation()
    ↓
ReservationModificationRequest creato (PENDING_APPROVAL)
    ↓
EVENT: RESERVATION_MODIFICATION_REQUESTED
    ↓
RestaurantUser vede notifica per approvazione
```

### 2️⃣ RESTAURANT Approva
```
ReservationService.approveModificationRequest()
    ↓
Reservation aggiornato con requested values
    ↓
ReservationModificationRequest.status → APPROVED
    ↓
EVENT: RESERVATION_MODIFICATION_APPROVED
    ↓
Customer vede notifica: "La tua modifica è stata approvata!"
```

### 3️⃣ RESTAURANT Rifiuta
```
ReservationService.rejectModificationRequest()
    ↓
ReservationModificationRequest.status → REJECTED
    ↓
Reservation RIMANE INVARIATO
    ↓
EVENT: RESERVATION_MODIFICATION_REJECTED
    ↓
Customer vede notifica: "La tua modifica non è stata approvata"
```

### 4️⃣ RESTAURANT Modifica Direttamente
```
ReservationService.modifyReservationDirectly()
    ↓
Reservation aggiornato SENZA creare ReservationModificationRequest
    ↓
EVENT: RESERVATION_MODIFIED_BY_RESTAURANT
    ↓
Customer vede notifica: "Il ristorante ha modificato la tua prenotazione"
```

---

## 📁 File Creati/Modificati

| File | Azione | Cosa Contiene |
|------|--------|---------------|
| **ReservationModificationRequest.java** | ✅ CREATED | Entity per tracking richieste di modifica |
| **ReservationModificationRequestDAO.java** | ✅ CREATED | Repository con queries per finding pending requests |
| **ReservationAudit.java** | 🔄 UPDATED | +5 nuove audit actions per tracking modifiche |
| **CustomerReservationService.java** | 🔄 UPDATED | +requestModifyReservation() +event creation methods |
| **ReservationService.java** | 🔄 UPDATED | +approveModificationRequest() +rejectModificationRequest() +modifyReservationDirectly() +4 event methods |

---

## 🎯 4 Nuovi Event Types

1. **RESERVATION_MODIFICATION_REQUESTED** - Customer richiede modifica (→ Restaurant)
2. **RESERVATION_MODIFICATION_APPROVED** - Restaurant approva (→ Customer)
3. **RESERVATION_MODIFICATION_REJECTED** - Restaurant rifiuta (→ Customer)
4. **RESERVATION_MODIFIED_BY_RESTAURANT** - Restaurant modifica direttamente (→ Customer)

**Routing automatico** via `initiated_by` field nel payload:
- `initiated_by="CUSTOMER"` → notification.restaurant.reservations (TEAM)
- `initiated_by="RESTAURANT"` → notification.customer (PERSONAL)

---

## ✅ Compilation Status

```
✅ ReservationModificationRequest.java         - 0 errors
✅ ReservationModificationRequestDAO.java      - 0 errors
✅ ReservationAudit.java                       - 0 errors
✅ CustomerReservationService.java             - 0 errors
✅ ReservationService.java                     - 0 errors

OVERALL: 0 COMPILATION ERRORS ✅
```

---

## 🔑 Key Differences

| Scenario | Chi lo Richiede | Chi lo Approva? | Status Request | Reservation Cambia? | Event Creato |
|----------|---|---|---|---|---|
| Scenario 1 | Customer | Sì (RestaurantUser) | PENDING → APPROVED | ✅ Sì (approve) | MODIFICATION_REQUESTED |
| Scenario 3 | Customer | Sì (RestaurantUser) | PENDING → REJECTED | ❌ No | MODIFICATION_REQUESTED |
| Scenario 4 | RestaurantUser | No | N/A | ✅ Sì (subito) | MODIFIED_BY_RESTAURANT |

---

## 📊 ReservationModificationRequest States

```
PENDING_APPROVAL  ─→ Customer richiede modifica
                  ├─→ APPROVED ─→ Modifica applicata
                  └─→ REJECTED ─→ Modifica rifiutata
                  
CANCELLED ─→ Customer cancella la richiesta (future)
APPLIED ─→ Modifica è stata applicata (future use)
```

---

## 🔗 Event Payload Examples

### RESERVATION_MODIFICATION_REQUESTED
```json
{
  "modificationRequestId": 123,
  "reservationId": 456,
  "originalDate": "2025-12-25",
  "requestedDate": "2025-12-26",
  "originalPax": 4,
  "requestedPax": 6,
  "initiated_by": "CUSTOMER"
}
```

### RESERVATION_MODIFICATION_APPROVED
```json
{
  "modificationRequestId": 123,
  "reservationId": 456,
  "approvedDate": "2025-12-26",
  "approvedPax": 6,
  "approverUserType": "RUser",
  "initiated_by": "RESTAURANT"
}
```

### RESERVATION_MODIFIED_BY_RESTAURANT
```json
{
  "reservationId": 456,
  "modifiedDate": "2025-12-25",
  "pax": 6,
  "modifierUserType": "RUser",
  "initiated_by": "RESTAURANT"
}
```

---

## 📋 Audit Trail (ReservationAudit)

```
Nuovi Action Types:
├── MODIFICATION_REQUESTED  → When customer requests
├── MODIFICATION_APPROVED   → When restaurant approves
├── MODIFICATION_REJECTED   → When restaurant rejects
├── MODIFICATION_APPLIED    → When modification is applied
└── MODIFIED_BY_RESTAURANT  → When restaurant modifies directly
```

Ogni azione viene loggata con:
- Chi ha eseguito (`changedBy`)
- Quando (`changedAt`)
- Che cosa è cambiato (`changedFields`)
- Perché (`changeReason`)

---

## 🚀 Utilizzo nei Controller

### Per Approvare una Modifica
```java
@PutMapping("/restaurant/reservation/approve-modification/{modId}")
public ResponseEntity<ReservationDTO> approveModification(@PathVariable Long modId) {
    return reservationService.approveModificationRequest(modId, currentUser);
}
```

### Per Rifiutare una Modifica
```java
@PutMapping("/restaurant/reservation/reject-modification/{modId}")
public ResponseEntity<ReservationDTO> rejectModification(
    @PathVariable Long modId,
    @RequestParam String reason) {
    return reservationService.rejectModificationRequest(modId, reason, currentUser);
}
```

### Per Modificare Direttamente
```java
@PutMapping("/restaurant/reservation/{id}/modify-direct")
public ResponseEntity<ReservationDTO> modifyDirect(
    @PathVariable Long id,
    @RequestBody ModifyReservationRequest request) {
    return reservationService.modifyReservationDirectly(
        id, request.getPax(), request.getKids(), 
        request.getNotes(), currentUser);
}
```

---

## 🎓 Regola Business Implementata

> **SE** Customer richiede modifica  
> **ALLORA** Deve attendere approvazione da RestaurantUser/Admin  
> **ALTRIMENTI SE** RestaurantUser ha permessi  
> **ALLORA** Modifica applicata subito  
>   
> **IN TUTTI I CASI** Notifiche via Event Outbox seguono logica routing existing

✅ **Implementato al 100%**
