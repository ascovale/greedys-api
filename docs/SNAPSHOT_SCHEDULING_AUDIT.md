# Snapshot, Scheduling & Audit System

## 1. Architettura Generale

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              RESTAURANT                                      │
│                                  │                                           │
│                                  ▼                                           │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                            SERVICE                                     │  │
│  │                    (configurazione ATTUALE)                            │  │
│  │                                                                        │  │
│  │   • name: "Pranzo"                                                     │  │
│  │   • color: "#FF5733"                                                   │  │
│  │   • active: true                                                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                  │                                           │
│          ┌───────────────────────┼───────────────────────┐                  │
│          │                       │                       │                  │
│          ▼                       ▼                       ▼                  │
│  ┌───────────────┐      ┌───────────────┐      ┌───────────────────┐       │
│  │ ServiceDay    │      │ServiceSlotConf│      │AvailabilityExcept.│       │
│  │               │      │               │      │                   │       │
│  │ MONDAY:       │      │ startTime:    │      │ 2025-12-25:       │       │
│  │  12:00-15:00  │      │  12:00        │      │  CLOSED           │       │
│  │  19:00-23:00  │      │ endTime:      │      │  (Natale)         │       │
│  │               │      │  23:00        │      │                   │       │
│  │ TUESDAY:      │      │ slotDuration: │      │ 2025-08-15:       │       │
│  │  12:00-15:00  │      │  30 min       │      │  CLOSED           │       │
│  │  19:00-23:00  │      │ maxConcurrent:│      │  (Ferragosto)     │       │
│  │               │      │  10           │      │                   │       │
│  │ ...           │      │               │      │                   │       │
│  └───────────────┘      └───────────────┘      └───────────────────┘       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Snapshot: Contratto della Prenotazione

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PERCHÉ SERVE LO SNAPSHOT                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   SENZA SNAPSHOT (❌ problema)                                              │
│   ─────────────────────────────                                             │
│                                                                             │
│   1 Dicembre: Cliente prenota                                               │
│      └── Reservation { service_id: 1 }                                      │
│      └── Service.slotDuration = 90 min                                      │
│                                                                             │
│   2 Dicembre: Ristorante modifica                                           │
│      └── Service.slotDuration = 60 min                                      │
│                                                                             │
│   3 Dicembre: "Quanto dura la prenotazione di ieri?"                        │
│      └── Leggo Service.slotDuration = 60 min  ❌ SBAGLIATO!                 │
│                                                                             │
│   ═══════════════════════════════════════════════════════════════════════   │
│                                                                             │
│   CON SNAPSHOT (✅ corretto)                                                │
│   ──────────────────────────                                                │
│                                                                             │
│   1 Dicembre: Cliente prenota                                               │
│      └── Reservation {                                                      │
│              service_id: 1,                                                 │
│              bookedSlotDuration: 90  ← SNAPSHOT                             │
│          }                                                                  │
│                                                                             │
│   2 Dicembre: Ristorante modifica                                           │
│      └── Service.slotDuration = 60 min                                      │
│                                                                             │
│   3 Dicembre: "Quanto dura la prenotazione di ieri?"                        │
│      └── Leggo Reservation.bookedSlotDuration = 90 min  ✅ CORRETTO!        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Snapshot Fields in Reservation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            RESERVATION                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  RIFERIMENTI (link alle entity attuali)                             │   │
│   │                                                                     │   │
│   │  • service_id  ──────────────→ Service                              │   │
│   │  • restaurant_id ────────────→ Restaurant                           │   │
│   │  • customer_id ──────────────→ Customer                             │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  SNAPSHOT (valori "fotografati" al momento della prenotazione)      │   │
│   │                                                                     │   │
│   │  • bookedServiceName: "Pranzo"      ← nome al momento               │   │
│   │  • bookedSlotDuration: 90           ← durata al momento             │   │
│   │  • bookedDate: 2025-12-15           ← data prenotazione             │   │
│   │  • bookedTime: 13:00                ← ora prenotazione              │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  DATI PRENOTAZIONE                                                  │   │
│   │                                                                     │   │
│   │  • pax: 4                                                           │   │
│   │  • kids: 1                                                          │   │
│   │  • notes: "Tavolo vicino finestra"                                  │   │
│   │  • status: CONFIRMED                                                │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  AUDIT (chi/quando)                                                 │   │
│   │                                                                     │   │
│   │  • createdAt: 2025-12-01 15:30                                      │   │
│   │  • createdBy: customer_id=789                                       │   │
│   │  • modifiedAt: null                                                 │   │
│   │  • modifiedBy: null                                                 │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Modifica Termini del Contratto

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              FLUSSO: Ristorante Modifica Durata 90min → 60min               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Ristorante: "Voglio ridurre la durata da 90 a 60 minuti"                 │
│                              │                                              │
│                              ▼                                              │
│                  ┌───────────────────────┐                                  │
│                  │  Applica a quali      │                                  │
│                  │  prenotazioni?        │                                  │
│                  └───────────┬───────────┘                                  │
│                              │                                              │
│            ┌─────────────────┼─────────────────┐                            │
│            │                                   │                            │
│            ▼                                   ▼                            │
│   ┌─────────────────┐                 ┌─────────────────┐                   │
│   │  SOLO NUOVE     │                 │ ANCHE ESISTENTI │                   │
│   │  prenotazioni   │                 │  prenotazioni   │                   │
│   └────────┬────────┘                 └────────┬────────┘                   │
│            │                                   │                            │
│            │                                   ▼                            │
│            │                          ┌─────────────────┐                   │
│            │                          │ Per ogni        │                   │
│            │                          │ Reservation     │                   │
│            │                          │ impattata:      │                   │
│            │                          │                 │                   │
│            │                          │ 1. Notifica     │                   │
│            │                          │    cliente      │                   │
│            │                          │                 │                   │
│            │                          │ 2. Cliente      │                   │
│            │                          │    risponde:    │                   │
│            │                          │    • ACCETTA    │                   │
│            │                          │    • RIFIUTA    │                   │
│            │                          │    • CANCELLA   │                   │
│            │                          │                 │                   │
│            │                          │ 3. Aggiorna     │                   │
│            │                          │    snapshot +   │                   │
│            │                          │    audit log    │                   │
│            │                          └────────┬────────┘                   │
│            │                                   │                            │
│            ▼                                   ▼                            │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │                     RISULTATO FINALE                            │       │
│   │                                                                 │       │
│   │  ServiceSlotConfig.slotDuration = 60  ← Nuovo default           │       │
│   │                                                                 │       │
│   │  Reservation #123 (vecchia):                                    │       │
│   │    bookedSlotDuration = 90  ← Mantiene snapshot originale       │       │
│   │    (oppure 60 se cliente accetta modifica)                      │       │
│   │                                                                 │       │
│   │  Reservation #456 (nuova):                                      │       │
│   │    bookedSlotDuration = 60  ← Nuovo snapshot                    │       │
│   │                                                                 │       │
│   └─────────────────────────────────────────────────────────────────┘       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Audit Trail: Chi Ha Fatto Cosa

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DUE TIPI DI AUDIT                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    SCHEDULE_AUDIT_LOG                               │   │
│   │            (modifiche alla configurazione)                          │   │
│   │                                                                     │   │
│   │  • Cambio orari apertura                                            │   │
│   │  • Cambio durata slot                                               │   │
│   │  • Cambio max prenotazioni                                          │   │
│   │  • Aggiunta/rimozione chiusure                                      │   │
│   │  • Modifica giorni lavorativi                                       │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                   RESERVATION_AUDIT_LOG                             │   │
│   │             (modifiche alle prenotazioni)                           │   │
│   │                                                                     │   │
│   │  • Prenotazione creata                                              │   │
│   │  • Cambio orario                                                    │   │
│   │  • Cambio numero persone                                            │   │
│   │  • Cambio tavolo                                                    │   │
│   │  • Accettazione/rifiuto                                             │   │
│   │  • Cancellazione                                                    │   │
│   │  • Modifica termini (snapshot aggiornato)                           │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### ScheduleAuditLog

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SCHEDULE_AUDIT_LOG                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  id: 1                                                                      │
│  service_id: 5                                                              │
│  restaurant_id: 1                                                           │
│  entity_type: SLOT_CONFIG                                                   │
│  entity_id: 12                                                              │
│  action: UPDATED                                                            │
│  user_id: 7 (Mario Rossi - Manager)                                         │
│  old_value: {"slotDuration": 90, "maxConcurrent": 10}                       │
│  new_value: {"slotDuration": 60, "maxConcurrent": 10}                       │
│  change_reason: "Riduzione tempo per aumentare turnover"                    │
│  changed_at: 2025-12-01 10:30:00                                            │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  id: 2                                                                      │
│  service_id: 5                                                              │
│  restaurant_id: 1                                                           │
│  entity_type: DAY_SCHEDULE                                                  │
│  entity_id: 45                                                              │
│  action: UPDATED                                                            │
│  user_id: 7                                                                 │
│  old_value: {"dayOfWeek": "MONDAY", "openingTime": "12:00"}                 │
│  new_value: {"dayOfWeek": "MONDAY", "openingTime": "11:30"}                 │
│  change_reason: "Anticipo apertura per pranzi di lavoro"                    │
│  changed_at: 2025-12-01 11:00:00                                            │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  id: 3                                                                      │
│  service_id: 5                                                              │
│  restaurant_id: 1                                                           │
│  entity_type: AVAILABILITY_EXCEPTION                                        │
│  entity_id: 78                                                              │
│  action: CREATED                                                            │
│  user_id: 7                                                                 │
│  old_value: null                                                            │
│  new_value: {"date": "2025-12-25", "type": "CLOSURE", "reason": "Natale"}   │
│  change_reason: "Chiusura festiva"                                          │
│  changed_at: 2025-12-01 11:15:00                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### ReservationAuditLog

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        RESERVATION_AUDIT_LOG                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  id: 1                                                                      │
│  reservation_id: 123                                                        │
│  restaurant_id: 1                                                           │
│  action: CREATED                                                            │
│  user_id: 456 (cliente)                                                     │
│  user_type: CUSTOMER                                                        │
│  old_value: null                                                            │
│  new_value: {"pax": 4, "date": "2025-12-15", "time": "13:00"}               │
│  changed_at: 2025-12-01 15:30:00                                            │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  id: 2                                                                      │
│  reservation_id: 123                                                        │
│  restaurant_id: 1                                                           │
│  action: STATUS_CHANGED                                                     │
│  user_id: 7 (staff ristorante)                                              │
│  user_type: RESTAURANT_USER                                                 │
│  field_changed: "status"                                                    │
│  old_value: "NOT_ACCEPTED"                                                  │
│  new_value: "ACCEPTED"                                                      │
│  changed_at: 2025-12-01 15:35:00                                            │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  id: 3                                                                      │
│  reservation_id: 123                                                        │
│  restaurant_id: 1                                                           │
│  action: TERMS_CHANGED                                                      │
│  user_id: 7                                                                 │
│  user_type: RESTAURANT_USER                                                 │
│  field_changed: "bookedSlotDuration"                                        │
│  old_value: "90"                                                            │
│  new_value: "60"                                                            │
│  change_reason: "Riduzione durata servizio"                                 │
│  customer_notified: true                                                    │
│  customer_accepted: true                                                    │
│  changed_at: 2025-12-02 10:00:00                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Schema Entity Finale

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ENTITY RELATIONSHIPS                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                           ┌──────────────┐                                  │
│                           │  Restaurant  │                                  │
│                           └──────┬───────┘                                  │
│                                  │                                          │
│                                  │ 1:N                                      │
│                                  ▼                                          │
│                           ┌──────────────┐                                  │
│                           │   Service    │◄─────────────────────────┐       │
│                           └──────┬───────┘                          │       │
│                                  │                                  │       │
│          ┌───────────────────────┼───────────────────────┐          │       │
│          │ 1:N                   │ 1:1                   │ 1:N      │       │
│          ▼                       ▼                       ▼          │       │
│  ┌───────────────┐      ┌────────────────┐      ┌────────────────┐  │       │
│  │  ServiceDay   │      │ServiceSlotConf.│      │AvailabilityExc.│  │       │
│  │               │      │                │      │                │  │       │
│  │ service_id───►│      │ service_id────►│      │ service_id────►│  │       │
│  └───────────────┘      └────────────────┘      └────────────────┘  │       │
│                                                                      │       │
│                                                                      │       │
│  ┌───────────────────────────────────────────────────────────────┐  │       │
│  │                       RESERVATION                             │  │       │
│  │                                                               │  │       │
│  │  service_id ─────────────────────────────────────────────────►│  │       │
│  │                                                               │──┘       │
│  │  ┌─────────────────────────────────────────────────────────┐  │          │
│  │  │ SNAPSHOT FIELDS                                         │  │          │
│  │  │  • bookedServiceName                                    │  │          │
│  │  │  • bookedSlotDuration                                   │  │          │
│  │  └─────────────────────────────────────────────────────────┘  │          │
│  │                                                               │          │
│  └───────────────────────────────────────────────────────────────┘          │
│                                  │                                          │
│                                  │ 1:N                                      │
│                                  ▼                                          │
│                    ┌──────────────────────────┐                             │
│                    │  ReservationAuditLog     │                             │
│                    │                          │                             │
│                    │  reservation_id ────────►│                             │
│                    └──────────────────────────┘                             │
│                                                                             │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────┐          │
│  │                   ScheduleAuditLog                            │          │
│  │                                                               │          │
│  │  service_id ──────────────────────────────────────────────────┼──────────┘
│  │  entity_type: SLOT_CONFIG | DAY_SCHEDULE | EXCEPTION          │
│  │  entity_id                                                    │
│  │                                                               │
│  └───────────────────────────────────────────────────────────────┘
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Confronto: Prima vs Dopo

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PRIMA (❌)                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Service ──► ServiceVersion ──► ServiceVersionSlotConfig                    │
│                    │                                                        │
│                    └──► ServiceVersionDay                                   │
│                    │                                                        │
│                    └──► AvailabilityException                               │
│                    │                                                        │
│                    └──► Reservation                                         │
│                                                                             │
│  Problemi:                                                                  │
│  • ServiceVersion non è vero versioning (dati modificabili)                 │
│  • Nessun audit trail delle modifiche                                       │
│  • Reservation punta a ServiceVersion ma perde dati originali               │
│  • Complessità inutile                                                      │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                              DOPO (✅)                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Service ──► ServiceSlotConfig                                              │
│      │                                                                      │
│      └──► ServiceDay                                                        │
│      │                                                                      │
│      └──► AvailabilityException                                             │
│      │                                                                      │
│      └──► Reservation (con snapshot fields)                                 │
│                 │                                                           │
│                 └──► ReservationAuditLog                                    │
│      │                                                                      │
│      └──► ScheduleAuditLog                                                  │
│                                                                             │
│  Vantaggi:                                                                  │
│  • Schema semplificato (no ServiceVersion)                                  │
│  • Audit completo di ogni modifica                                          │
│  • Snapshot preserva dati originali prenotazione                            │
│  • Modifiche termini tracciate e notificate                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Query Comuni

```sql
-- Chi ha modificato la configurazione del servizio "Pranzo" nell'ultimo mese?
SELECT * FROM schedule_audit_log 
WHERE service_id = 5 
  AND changed_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH)
ORDER BY changed_at DESC;

-- Storico completo di una prenotazione
SELECT * FROM reservation_audit_log 
WHERE reservation_id = 123 
ORDER BY changed_at ASC;

-- Prenotazioni con termini modificati dopo la creazione
SELECT r.* FROM reservation r
JOIN reservation_audit_log ral ON r.id = ral.reservation_id
WHERE ral.action = 'TERMS_CHANGED';

-- Tutte le chiusure create questo mese
SELECT * FROM schedule_audit_log
WHERE entity_type = 'AVAILABILITY_EXCEPTION'
  AND action = 'CREATED'
  AND changed_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH);
```
