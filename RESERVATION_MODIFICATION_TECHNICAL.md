# 🔧 Documentazione Tecnica: Metodi di Modifica Prenotazioni

## CustomerReservationService

### requestModifyReservation()

**Scopo**: Customer richiede una modifica alla prenotazione  
**Responsabilità**: Crea ReservationModificationRequest + Event

```java
public void requestModifyReservation(
    Long oldReservationId, 
    CustomerNewReservationDTO dTO, 
    Customer currentUser)
```

**Step-by-step**:
1. Carica Reservation per ID
2. Valida stato: deve essere NOT_ACCEPTED o ACCEPTED
3. Valida data: `validateReservationDateAvailability()`
4. Crea `ReservationModificationRequest` con:
   - Original values (da Reservation attuale)
   - Requested values (da DTO richiesta)
   - `requestedBy = currentUser` (il customer)
   - `requestedAt = now`
   - `status = PENDING_APPROVAL`
5. Salva nel DB
6. Chiama `createReservationModificationRequestedEvent()`
7. Log dell'azione

**Output**: Void (ma modRequest viene salvato)  
**Exceptions**: IllegalStateException, NoSuchElementException  
**Event creato**: RESERVATION_MODIFICATION_REQUESTED → RestaurantUser

---

### createReservationModificationRequestedEvent()

**Scopo**: Crea EventOutbox per notificare il restaurant

```java
private void createReservationModificationRequestedEvent(
    Reservation reservation, 
    ReservationModificationRequest modRequest)
```

**Step-by-step**:
1. Genera `eventId = "RESERVATION_MODIFICATION_REQUESTED_" + modRequest.getId() + "_" + timestamp`
2. Crea payload JSON via `buildReservationModificationPayload()`
3. Crea EventOutbox con:
   - `eventType = "RESERVATION_MODIFICATION_REQUESTED"`
   - `aggregateType = "CUSTOMER"` (mostra origine dalla customer)
   - `aggregateId = modRequest.getId()`
   - `status = PENDING`
4. Salva nel DB (EventOutboxDAO)
5. Log dell'evento

**Payload Fields**:
```
modificationRequestId, reservationId, customerId, restaurantId, email,
originalDate, requestedDate,
originalPax, requestedPax,
originalKids, requestedKids,
originalNotes, requestedNotes, customerReason,
initiated_by=CUSTOMER
```

**Routing**: EventOutboxOrchestrator vede `initiated_by="CUSTOMER"` → invia a `notification.restaurant.reservations` (TEAM queue)

---

### buildReservationModificationPayload()

**Scopo**: Costruisce il JSON payload per il modification event

```java
private String buildReservationModificationPayload(
    Reservation reservation, 
    ReservationModificationRequest modRequest)
```

**Logica**:
1. Estrae IDs (customerId, restaurantId)
2. Escapa JSON special chars in notes
3. Formatta come JSON string con tutti i campi
4. Include `"initiated_by":"CUSTOMER"` per routing

**Return**: JSON string (non pretty-printed)

---

---

## ReservationService

### approveModificationRequest()

**Scopo**: Restaurant approva la richiesta di modifica del customer  
**Responsabilità**: Applica modifiche + crea Event + aggiorna ReservationModificationRequest

```java
@Transactional
public ReservationDTO approveModificationRequest(
    Long modificationRequestId, 
    com.application.common.persistence.model.user.AbstractUser approverUser)
```

**Precondizioni**:
- ReservationModificationRequest.status == PENDING_APPROVAL
- Utente ha permission di approvare (RestaurantUser o Admin)

**Step-by-step**:
1. Carica `ReservationModificationRequest` per ID
2. Valida stato: `if (status != PENDING_APPROVAL) throw IllegalStateException`
3. Carica Reservation associato
4. Valida nuova data: `validateReservationDateAvailability()` con `requestedDate`
5. **Applica modifiche al Reservation**:
   ```
   pax = modRequest.requestedPax
   kids = modRequest.requestedKids
   notes = modRequest.requestedNotes
   reservationDateTime = modRequest.requestedDateTime
   modifiedAt = now
   modifiedBy = approverUser
   ```
6. Salva Reservation
7. **Aggiorna ReservationModificationRequest**:
   ```
   status = APPROVED
   reviewedBy = approverUser
   reviewedAt = now
   ```
8. Salva ReservationModificationRequest
9. Chiama `createReservationModificationApprovedEvent()`
10. Log dell'approvazione

**Output**: ReservationDTO (con valori aggiornati)  
**Exceptions**: IllegalStateException, NoSuchElementException  
**Event creato**: RESERVATION_MODIFICATION_APPROVED → Customer  
**Audit Trail**: ReservationAudit con action=MODIFICATION_APPROVED

---

### rejectModificationRequest()

**Scopo**: Restaurant rifiuta la richiesta di modifica  
**Responsabilità**: NON modifica Reservation, solo rifiuta request + crea Event

```java
@Transactional
public ReservationDTO rejectModificationRequest(
    Long modificationRequestId, 
    String rejectReason, 
    com.application.common.persistence.model.user.AbstractUser approverUser)
```

**Precondizioni**:
- ReservationModificationRequest.status == PENDING_APPROVAL
- Utente ha permission di rifiutare (RestaurantUser o Admin)

**Step-by-step**:
1. Carica `ReservationModificationRequest` per ID
2. Valida stato: `if (status != PENDING_APPROVAL) throw IllegalStateException`
3. Carica Reservation associato
4. **Aggiorna ReservationModificationRequest** (non tocca Reservation):
   ```
   status = REJECTED
   approvalReason = rejectReason
   reviewedBy = approverUser
   reviewedAt = now
   ```
5. Salva ReservationModificationRequest
6. Chiama `createReservationModificationRejectedEvent()`
7. Log del rifiuto

**Output**: ReservationDTO (invariato, con valori originali)  
**Exceptions**: IllegalStateException, NoSuchElementException  
**Event creato**: RESERVATION_MODIFICATION_REJECTED → Customer  
**Audit Trail**: ReservationAudit con action=MODIFICATION_REJECTED  
**Importante**: La Reservation NON viene modificata!

---

### modifyReservationDirectly()

**Scopo**: Restaurant staff modifica la prenotazione SENZA creare ReservationModificationRequest  
**Responsabilità**: Applica modifiche + crea Event (no approval workflow)

```java
@Transactional
public ReservationDTO modifyReservationDirectly(
    Long reservationId, 
    Integer pax, 
    Integer kids, 
    String notes, 
    com.application.common.persistence.model.user.AbstractUser modifiedByUser)
```

**Precondizioni**:
- Utente ha permission diretta (Admin o privileged RestaurantUser)
- No ReservationModificationRequest è creato

**Step-by-step**:
1. Carica Reservation per ID
2. Se `pax != null` → `reservation.pax = pax`
3. Se `kids != null` → `reservation.kids = kids`
4. Se `notes != null` → `reservation.notes = notes`
5. Imposta auditing:
   ```
   modifiedAt = now
   modifiedBy = modifiedByUser
   ```
6. Salva Reservation
7. Chiama `createReservationModifiedByRestaurantEvent()`
8. Log della modifica diretta

**Output**: ReservationDTO (con valori aggiornati)  
**Exceptions**: NoSuchElementException  
**Event creato**: RESERVATION_MODIFIED_BY_RESTAURANT → Customer  
**Audit Trail**: ReservationAudit con action=MODIFIED_BY_RESTAURANT  
**Importante**: Niente approval workflow, modifica applicata subito!

---

### createReservationModificationApprovedEvent()

```java
private void createReservationModificationApprovedEvent(
    Reservation reservation, 
    ReservationModificationRequest modRequest, 
    com.application.common.persistence.model.user.AbstractUser approverUser)
```

**Logica**:
1. Genera `eventId = "RESERVATION_MODIFICATION_APPROVED_" + modRequest.getId() + "_" + timestamp`
2. Crea payload via `buildReservationModificationApprovedPayload()`
3. EventOutbox:
   - `eventType = "RESERVATION_MODIFICATION_APPROVED"`
   - `aggregateType = "RESTAURANT"` (approva da restaurant)
   - `aggregateId = modRequest.getId()`
   - `status = PENDING`
4. Salva e log

**Payload include**: originalDate, approvedDate, originalPax, approvedPax, approverUserType, initiated_by=RESTAURANT

---

### createReservationModificationRejectedEvent()

```java
private void createReservationModificationRejectedEvent(
    Reservation reservation, 
    ReservationModificationRequest modRequest, 
    com.application.common.persistence.model.user.AbstractUser rejectorUser)
```

**Logica**: Simile a APPROVED  
**Payload include**: requestedDate, requestedPax, rejectionReason, rejectorUserType, initiated_by=RESTAURANT

---

### createReservationModifiedByRestaurantEvent()

```java
private void createReservationModifiedByRestaurantEvent(
    Reservation reservation, 
    com.application.common.persistence.model.user.AbstractUser modifierUser)
```

**Logica**: Crea event per modifica diretta  
**Payload include**: pax, kids, notes, modifierUserType, initiated_by=RESTAURANT

---

---

## ReservationModificationRequestDAO

```java
public interface ReservationModificationRequestDAO 
    extends JpaRepository<ReservationModificationRequest, Long>
```

### Queries disponibili

#### findPendingByReservationId()
```java
@Query("SELECT r FROM ReservationModificationRequest r 
        WHERE r.reservation.id = :reservationId 
        AND r.status = 'PENDING_APPROVAL'")
List<ReservationModificationRequest> findPendingByReservationId(
    @Param("reservationId") Long reservationId)
```

**Use case**: Controllare se una prenotazione ha richieste di modifica in sospeso

---

#### findPendingByRestaurantId()
```java
@Query("SELECT r FROM ReservationModificationRequest r 
        WHERE r.reservation.restaurant.id = :restaurantId 
        AND r.status = 'PENDING_APPROVAL' 
        ORDER BY r.requestedAt DESC")
List<ReservationModificationRequest> findPendingByRestaurantId(
    @Param("restaurantId") Long restaurantId)
```

**Use case**: Dashboard per restaurant per vedere tutte le modifiche in sospeso

---

#### findByReservationId()
```java
List<ReservationModificationRequest> findByReservationId(Long reservationId)
```

**Use case**: Storico di tutte le richieste di modifica per una prenotazione

---

#### findByRestaurantIdAndStatus()
```java
@Query("SELECT r FROM ReservationModificationRequest r 
        WHERE r.reservation.restaurant.id = :restaurantId 
        AND r.status = :status 
        ORDER BY r.requestedAt DESC")
List<ReservationModificationRequest> findByRestaurantIdAndStatus(
    @Param("restaurantId") Long restaurantId, 
    @Param("status") ReservationModificationRequest.Status status)
```

**Use case**: Filtrare richieste per status (APPROVED, REJECTED, etc.)

---

---

## ReservationModificationRequest Entity

### Stati Principali

```
┌─────────────────────────────────────────────────────────┐
│  PENDING_APPROVAL (in attesa di risposta dal restaurant) │
├───────────────────┬─────────────────────────────────────┤
│                   │                                     │
v                   v                                     
APPROVED        REJECTED                              
(modifica       (modifica                             
 applicata)     rifiutata)                             
```

### Campi Chiave

**Original Values** (backup dei valori precedenti):
- `originalDate`: Data prenotazione originale
- `originalDateTime`: Data+ora originale
- `originalPax`: Numero ospiti originale
- `originalKids`: Numero bambini originale
- `originalNotes`: Note originali

**Requested Values** (cosa il customer vuole):
- `requestedDate`: Data richiesta
- `requestedDateTime`: Data+ora richiesta
- `requestedPax`: Numero ospiti richiesto
- `requestedKids`: Numero bambini richiesto
- `requestedNotes`: Note richieste

**Metadata**:
- `status`: PENDING_APPROVAL | APPROVED | REJECTED | CANCELLED | APPLIED
- `customerReason`: Perché il customer vuole cambiare
- `approvalReason`: Perché il restaurant ha approvato/rifiutato
- `requestedBy`: Customer che ha richiesto
- `requestedAt`: Quando è stata richiesta
- `reviewedBy`: RestaurantUser che ha revisionato
- `reviewedAt`: Quando è stata revisionata

---

---

## Flusso di Transazione

### approveModificationRequest() Transaction

```
BEGIN TRANSACTION
├── Load ReservationModificationRequest (SELECT)
├── Validate status
├── Load Reservation (SELECT)
├── Validate date (business logic)
├── Update Reservation (UPDATE)
├── Save Reservation
├── Update ReservationModificationRequest (UPDATE)
├── Save ReservationModificationRequest
├── Create EventOutbox (INSERT)
├── Save EventOutbox
└── COMMIT
```

**Rollback conditions**: Any exception during these steps rolls back all

---

### rejectModificationRequest() Transaction

```
BEGIN TRANSACTION
├── Load ReservationModificationRequest (SELECT)
├── Validate status
├── Load Reservation (SELECT) - only for reference
├── Update ReservationModificationRequest (UPDATE) - NOT Reservation
├── Save ReservationModificationRequest
├── Create EventOutbox (INSERT)
├── Save EventOutbox
└── COMMIT
```

**Key**: Reservation is NOT modified, only the request status changes

---

---

## Esecuzione Asincrona degli Event

Gli EventOutbox vengono salvati con `status=PENDING` e poi:

1. **EventOutboxScheduler** (async job) legge i PENDING events
2. Legge il `eventType` e `payload`
3. Legge `initiated_by` dal payload JSON
4. **EventOutboxOrchestrator** determina il routing:
   - Se `initiated_by="CUSTOMER"` → invia a `notification.restaurant.reservations`
   - Se `initiated_by="RESTAURANT"` → invia a `notification.customer`
5. Il team handler o customer handler riceve l'evento
6. Crea le notifiche appropriate (RestaurantUserNotification o CustomerNotification)
7. Marca l'EventOutbox come `status=PUBLISHED`

**Timeout**: Se un event non viene processato in X minuti, retry

---

---

## Best Practices di Utilizzo

### ✅ DO

```java
// 1. Customer richiede modifica
customerReservationService.requestModifyReservation(
    reservationId, newDTO, currentCustomer);

// 2. Restaurant approva (con authorization check)
if (isRestaurantUserOrAdmin(currentUser)) {
    reservationService.approveModificationRequest(
        modRequestId, currentUser);
}

// 3. Restaurant rifiuta con motivo
reservationService.rejectModificationRequest(
    modRequestId, "Date is fully booked", currentUser);

// 4. Restaurant modifica direttamente
if (hasDirectModifyPermission(currentUser)) {
    reservationService.modifyReservationDirectly(
        reservationId, newPax, newKids, newNotes, currentUser);
}
```

### ❌ DON'T

```java
// ❌ Non usare l'old method - è legacy
reservationService.AcceptReservatioModifyRequest(modRequestId);

// ❌ Non modificare Reservation senza passare per service
reservation.setPax(6);  // NO - non crea event!

// ❌ Non creare EventOutbox direttamente
eventOutboxDAO.save(eventOutbox);  // Use service methods

// ❌ Non by-passare la validazione
reservationService.modifyReservationDirectly(id, -5, null, null, user);
```

---

---

## Monitoraggio e Debug

### Log Lines importanti

```
✅ Created EventOutbox RESERVATION_MODIFICATION_REQUESTED: eventId=..., modificationRequestId=..., reservationId=...

✅ Modification request 123 approved by RestaurantUser 456 for reservation 789

✅ Modification request 123 rejected by RestaurantUser 456 for reservation 789

✅ Reservation 789 modified directly by restaurant staff RestaurantUser (user: 456)

✅ Created EventOutbox RESERVATION_MODIFICATION_APPROVED: eventId=..., modificationRequestId=...
```

### Query per debug

```sql
-- Tutte le richieste di modifica in sospeso per un restaurant
SELECT * FROM reservation_modification_request rmr
JOIN reservation r ON rmr.reservation_id = r.id
WHERE r.id_restaurant = ? AND rmr.status = 'PENDING_APPROVAL'
ORDER BY rmr.requested_at DESC;

-- Storico di modifiche per una prenotazione
SELECT * FROM reservation_audit ra
WHERE ra.reservation_id = ? 
  AND ra.action IN ('MODIFICATION_REQUESTED', 'MODIFICATION_APPROVED', 'MODIFICATION_REJECTED', 'MODIFIED_BY_RESTAURANT')
ORDER BY ra.changed_at DESC;

-- Events non processati
SELECT * FROM event_outbox eo
WHERE eo.event_type LIKE 'RESERVATION_MODIFICATION%' 
  AND eo.status = 'PENDING'
ORDER BY eo.created_at;
```
