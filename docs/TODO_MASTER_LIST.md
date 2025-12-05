# 📋 TODO Master List - Greedys API

> Generato: 1 Dicembre 2025
> Ultima modifica: 1 Dicembre 2025
> Analisi completa del codice sorgente

---

## ✅ COMPLETATI

### Sistema EventType - COMPLETATO ✅

**Data completamento:** 1 Dicembre 2025

**Modifiche effettuate:**
1. ✅ Aggiunto `RESERVATION_REQUESTED` all'enum (mancava!)
2. ✅ Aggiunto `CUSTOMER_RESERVATION_CREATED` all'enum (mancava!)
3. ✅ Aggiunto `RESERVATION_MODIFICATION_REQUESTED` (mancava!)
4. ✅ Aggiunto `RESERVATION_MODIFICATION_APPROVED` (mancava!)
5. ✅ Aggiunto `RESERVATION_MODIFICATION_REJECTED` (mancava!)
6. ✅ Aggiunto `RESERVATION_MODIFIED_BY_RESTAURANT` (mancava!)
7. ✅ Migrato `CustomerReservationService.java` - usa enum
8. ✅ Migrato `ReservationService.java` - usa enum
9. ✅ Migrato `AdminReservationService.java` - usa enum
10. ✅ Migrato `ReservationEventListener.java` - usa enum
11. ✅ Creata documentazione `EVENT_TYPE_REFERENCE.md`

**EventType ora totali: 33** (erano 27, aggiunti 6)

---

## 🎯 Priorità CRITICA (Business-Critical)

### 1. Orchestrator Notification - TODO Services non implementati

| Orchestrator | TODO | File | Riga |
|--------------|------|------|------|
| CustomerOrchestrator | `CustomerPreferencesService.getEnabledChannels()` | CustomerOrchestrator.java | 184-186 |
| CustomerOrchestrator | `NotificationGroupSettingsService.getCustomerSettings()` | CustomerOrchestrator.java | 200-202 |
| CustomerOrchestrator | `CustomerService.findAllActiveCustomers()` | CustomerOrchestrator.java | 163 |
| AdminOrchestrator | `AdminPreferencesService.getEnabledChannels()` | AdminOrchestrator.java | 172-174 |
| AdminOrchestrator | `AdminService.findActiveAdmins()` | AdminOrchestrator.java | 153 |
| RestaurantUserOrchestrator | `RestaurantStaffService.findActiveStaffByRestaurantId()` | RestaurantUserOrchestrator.java | 162 |
| RestaurantUserOrchestrator | `RestaurantUserPreferencesService.getEnabledChannels()` | RestaurantUserOrchestrator.java | 183-185 |
| AgencyUserOrchestrator | `AgencyStaffService.findActiveStaffByAgencyId()` | AgencyUserOrchestrator.java | 154 |
| AgencyUserOrchestrator | `AgencyUserPreferencesService.getEnabledChannels()` | AgencyUserOrchestrator.java | 173-175 |

**Azione:** Creare questi service o collegarli se già esistono

---

### 2. Notification Channels - Stub implementation

| Channel | File | Problema |
|---------|------|----------|
| EmailNotificationChannel | EmailNotificationChannel.java | `// TODO: Implementare logica reale` |
| SMSNotificationChannel | SMSNotificationChannel.java | `// TODO: Implementare logica reale` |
| PushNotificationChannel | PushNotificationChannel.java | `// TODO: Implementare logica reale` |

**Integrazione richiesta:**
- Email: SendGrid o JavaMailSender
- SMS: Twilio (config già in application.properties)
- Push: Firebase Cloud Messaging (FCM)

---

## 🔶 Priorità ALTA

### 3. Reservation Validation - Check incompleti
**File:** `ReservationService.java` (righe 637-708)

```java
// TODO: ServiceVersionDay schedule for that day (has opening hours)
// TODO: AvailabilityException overrides (not fully closed, capacity available)
// TODO: Restaurant closure days (ClosedDayDAO)
// TODO: Check restaurant capacity/availability rules for the date
```

### 5. Agency Authentication - NON implementato
**File:** `AGENCY_MULTI_TENANT_ANALYSIS.md`

| Funzionalità | Stato |
|--------------|-------|
| Hub login | ❌ TODO |
| Single user login | ❌ TODO |
| Select/Switch agency | ❌ TODO |
| Token refresh | ❌ TODO |
| JWT Format per Agency | ❌ NOT DEFINED |

**File con TODO:**
- `AgencyAuthenticationService.java` - tutti i metodi sono stub

### 6. WebSocket Destination Validation - DB lookup mancante
**File:** `WebSocketDestinationValidator.java`

```java
// TODO: If restaurantId is null in JWT, verify via DB that user works for this restaurant
// TODO: Implement database lookup to verify user is member of groupId
// TODO: If agencyId is null in JWT, verify via DB that user works for this agency
```

---

## 🔷 Priorità MEDIA

### 7. Service Deletion - Prenotazioni non gestite
**File:** `ServiceService.java` (riga 235-252)

```java
// TODO: considerare il fatto di annullare le prenotazioni per un servizio
// TODO: Implementare quando il modello di Reservation supporterà il collegamento a Slot
```

### 8. Batch Reservation - Implementazione incompleta
**File:** `BatchReservationController.java`

```java
// TODO: Implement actual reservation creation using ReservationService (riga 138)
// TODO: Implement actual customer creation using CustomerService (riga 216)
```

### 9. Customer Registration - Social login incompleto
**File:** `CustomerRegistrationController.java`

```java
// TODO Registration with facebook verify the restaurant email
// TODO Registration with apple verify the restaurant email
```

### 10. Email Link errato
**File:** `EmailService.java` (riga 68)

```java
// TODO: il link per rimuovere ristorante è sbagliato da sistemare
```

### 11. Notification Cleanup
**File:** `ReadStatusService.java` (riga 366)

```java
// TODO: Implementare logica di cleanup basata su giorni
```

### 12. Restaurant Service - Metodo non implementato
**File:** `RestaurantService.java` (riga 305)

```java
// TODO: Implement findAllPaginatedDisabled
```

---

## 🔹 Priorità BASSA

### 13. Security Config
**File:** `SecurityConfig.java` (riga 63)

```java
// TODO: make sure the authentication filter is not required for login
```

### 14. Customer Controller - Notification preferences
**File:** `CustomerController.java`

```java
// TODO: Implementare tutti i metodi per la configurazione delle notifiche del customer
// TODO: Notification preferences settings
// TODO: System preferences settings
```

### 15. Phone Matching - Metodo DAO mancante
**File:** `CustomerMatchService.java` (riga 115)

```java
// TODO: Partial phone matching would require additional DAO methods
```

### 16. Reservation Logic in Entity
**File:** `Reservation.java` (riga 205)

```java
// TODO: MOVE TO A SERVICE
```

---

## 🗑️ FILE OBSOLETI / LEGACY

| File | Path | Nota |
|------|------|------|
| `ReservationLog.java.OLD` | common/persistence/model/reservation/ | File .OLD con TODO vecchi |
| `AuthResponseMapper.java` | common/persistence/mapper/ | Deprecato |

---

## 📊 Riepilogo per Area

| Area | TODO Critici | TODO Alta | TODO Media | TODO Bassa |
|------|-------------|-----------|------------|------------|
| **Notifiche/EventType** | 12 | 0 | 1 | 0 |
| **Reservation** | 0 | 4 | 2 | 1 |
| **Authentication** | 0 | 6 | 0 | 1 |
| **WebSocket** | 0 | 4 | 0 | 0 |
| **Customer** | 0 | 0 | 2 | 2 |
| **Service/Restaurant** | 0 | 0 | 2 | 1 |
| **Altro** | 0 | 0 | 1 | 0 |
| **TOTALE** | **12** | **14** | **8** | **5** |

---

## 🎯 Roadmap Suggerita

### Sprint 1 - Sistema Notifiche (Priorità Critica)
1. ✅ Allineare EventType enum con usage nel codice
2. ✅ Implementare EventType routing negli Orchestrator
3. ✅ Creare PreferencesService per ogni user type
4. ✅ Implementare EmailNotificationChannel (SendGrid)
5. ✅ Implementare SMSNotificationChannel (Twilio)

### Sprint 2 - Reservation Validation
1. ✅ Implementare check ServiceVersionDay opening hours
2. ✅ Implementare check AvailabilityException
3. ✅ Implementare check Restaurant closure days
4. ✅ Implementare capacity rules

### Sprint 3 - Authentication & Security
1. ✅ Completare Agency authentication flow
2. ✅ Implementare WebSocket DB lookup validation
3. ✅ Definire JWT format per Agency

### Sprint 4 - Cleanup & Polish
1. ✅ Rimuovere file .OLD
2. ✅ Sistemare link email
3. ✅ Implementare notification cleanup
4. ✅ Social login (Facebook/Apple)

---

*Ultimo aggiornamento: 1 Dicembre 2025*
