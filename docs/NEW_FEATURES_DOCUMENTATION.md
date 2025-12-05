# 📚 Documentazione Nuove Funzionalità - Greedy's API

> **Data**: 1 Dicembre 2025  
> **Versione**: 1.0.0  
> **Autore**: Greedy's System

---

## 📋 Indice

1. [Sistema Chat](#1-sistema-chat)
2. [Sistema Supporto Ticket](#2-sistema-supporto-ticket)
3. [Sistema Social Feed](#3-sistema-social-feed)
4. [Sistema Eventi Ristorante](#4-sistema-eventi-ristorante)
5. [Struttura Controller per Tipo Utente](#5-struttura-controller-per-tipo-utente)
6. [Sistema Audit](#6-sistema-audit)

---

## 1. Sistema Chat

### 1.1 Panoramica

Sistema di messaggistica in tempo reale per conversazioni tra utenti del sistema.

**Entità:**
- `ChatConversation` - Conversazione (diretta, gruppo, supporto, prenotazione)
- `ChatMessage` - Singolo messaggio
- `ChatParticipant` - Partecipante alla conversazione

**Tipi di Conversazione:**
- `DIRECT` - Chat 1:1 tra due utenti
- `GROUP` - Chat di gruppo con più membri
- `SUPPORT` - Chat collegata a ticket supporto
- `RESERVATION` - Chat collegata a prenotazione

### 1.2 Flusso: Creazione Conversazione Diretta

```
CustomerChatController     ChatService           ConversationDAO        ParticipantDAO
        |                      |                       |                      |
        |-- createDirect(u1,u2)-->                     |                      |
        |                      |                       |                      |
        |                      |-- findDirect(u1,u2)-->|                      |
        |                      |<-- null/existing -----|                      |
        |                      |                       |                      |
        |                      |-- save(conversation)->|                      |
        |                      |<-- saved -------------|                      |
        |                      |                       |                      |
        |                      |-- save(participant1)------------------------>|
        |                      |-- save(participant2)------------------------>|
        |                      |                       |                      |
        |<-- conversation -----|                       |                      |
        |                      |                       |                      |
```

### 1.3 Flusso: Invio Messaggio

```
CustomerChatController     ChatService           MessageDAO       EventOutboxDAO     WebSocket
        |                      |                    |                   |               |
        |-- sendMessage(cId,txt)-->                 |                   |               |
        |                      |                    |                   |               |
        |                      |-- save(message)--->|                   |               |
        |                      |<-- saved ----------|                   |               |
        |                      |                    |                   |               |
        |                      |-- updateConversation lastMessageAt     |               |
        |                      |                    |                   |               |
        |                      |-- save(event)------------------------>|               |
        |                      |                    |                   |               |
        |                      |                    |       (async)     |-- notify ---->|
        |                      |                    |                   |               |
        |<-- message ----------|                    |                   |               |
        |                      |                    |                   |               |
```

### 1.4 Endpoints per Tipo Utente

| Endpoint | Customer | Restaurant | Admin | Agency |
|----------|----------|------------|-------|--------|
| POST /conversations/direct | ✅ | ✅ | ✅ | ✅ |
| POST /conversations/group | ✅ | ✅ | ✅ | ✅ |
| GET /conversations | ✅ | ✅ | ✅ | ✅ |
| POST /messages | ✅ | ✅ | ✅ | ✅ |
| GET /messages/{id} | ✅ | ✅ | ✅ | ✅ |
| PUT /messages/{id}/read | ✅ | ✅ | ✅ | ✅ |

---

## 2. Sistema Supporto Ticket

### 2.1 Panoramica

Sistema di ticketing con supporto BOT automatico e escalation a staff umano.

**Entità:**
- `SupportTicket` - Ticket di supporto
- `SupportTicketMessage` - Messaggio nel ticket
- `SupportFAQ` - FAQ per risposte automatiche BOT

**Stati Ticket:**
- `OPEN` - Appena creato
- `IN_PROGRESS` - In lavorazione
- `WAITING_CUSTOMER` - In attesa risposta cliente
- `WAITING_STAFF` - In attesa risposta staff
- `RESOLVED` - Risolto
- `CLOSED` - Chiuso

### 2.2 Flusso: Creazione Ticket con BOT

```
CustomerSupportController    SupportTicketService      TicketDAO        FAQDAO        EventOutboxDAO
        |                           |                     |               |               |
        |-- createTicket(data) ---->|                     |               |               |
        |                           |                     |               |               |
        |                           |-- save(ticket) ---->|               |               |
        |                           |<-- ticket ----------|               |               |
        |                           |                     |               |               |
        |                           |-- addMessage(desc)----------------->|               |
        |                           |                     |               |               |
        |                           |-- tryBotResponse -->|               |               |
        |                           |                     |               |               |
        |                           |-- searchFAQ(keywords)-------------->|               |
        |                           |<-- faqList -------------------------|               |
        |                           |                     |               |               |
        |                           | [if FAQ found]      |               |               |
        |                           |-- addBotMessage --->|               |               |
        |                           |                     |               |               |
        |                           |-- save(event) -------------------------------->|
        |                           |                     |               |               |
        |<-- ticket ----------------|                     |               |               |
        |                           |                     |               |               |
```

### 2.3 Flusso: Escalation a Staff

```
SupportTicketService         TicketDAO           EventOutboxDAO        AdminNotification
        |                       |                       |                      |
        |-- escalateTicket ---->|                       |                      |
        |                       |                       |                      |
        |-- update(status=IN_PROGRESS)----------------->|                      |
        |                       |                       |                      |
        |-- save(ESCALATION_EVENT) -------------------->|                      |
        |                       |                       |                      |
        |                       |        (async)        |-- notify admins ---->|
        |                       |                       |                      |
```

### 2.4 Endpoints per Tipo Utente

| Endpoint | Customer | Restaurant | Admin | Agency |
|----------|----------|------------|-------|--------|
| POST /tickets | ✅ | ✅ | ✅ | ✅ |
| GET /tickets | ✅ (own) | ✅ (own) | ✅ (all) | ✅ (own) |
| GET /tickets/{id} | ✅ | ✅ | ✅ | ✅ |
| POST /tickets/{id}/messages | ✅ | ✅ | ✅ | ✅ |
| PUT /tickets/{id}/status | ❌ | ❌ | ✅ | ❌ |
| PUT /tickets/{id}/assign | ❌ | ❌ | ✅ | ❌ |
| PUT /tickets/{id}/escalate | ❌ | ❌ | ✅ | ❌ |
| PUT /tickets/{id}/resolve | ❌ | ❌ | ✅ | ❌ |

---

## 3. Sistema Social Feed

### 3.1 Panoramica

Feed social per ristoranti con post, commenti, reazioni e sistema follow.

**Entità:**
- `SocialPost` - Post nel feed
- `SocialComment` - Commento su post
- `SocialReaction` - Reazione (like, love, etc.)
- `SocialFollow` - Relazione follower/following

**Tipi Post:**
- `REGULAR` - Post normale
- `PROMOTION` - Promozione
- `EVENT` - Evento
- `MENU_UPDATE` - Aggiornamento menu
- `NEWS` - Notizia

### 3.2 Flusso: Creazione Post con Notifica Followers

```
RestaurantSocialController    SocialPostService       PostDAO        FollowDAO       EventOutboxDAO
        |                           |                    |               |                |
        |-- createPost(data) ------>|                    |               |                |
        |                           |                    |               |                |
        |                           |-- save(post) ----->|               |                |
        |                           |<-- post -----------|               |                |
        |                           |                    |               |                |
        |                           |-- getFollowers(restaurantId)----->|                |
        |                           |<-- followerList ------------------|                |
        |                           |                    |               |                |
        |                           | [for each follower]|               |                |
        |                           |-- save(event) ---------------------------------------->|
        |                           |                    |               |                |
        |<-- post ------------------|                    |               |                |
        |                           |                    |               |                |
```

### 3.3 Flusso: Reazione e Commento

```
CustomerSocialController     SocialPostService       ReactionDAO      CommentDAO     EventOutboxDAO
        |                          |                     |                |               |
        |-- reactToPost(like) ---->|                     |                |               |
        |                          |                     |                |               |
        |                          |-- findExisting ---->|                |               |
        |                          |<-- null/existing ---|                |               |
        |                          |                     |                |               |
        |                          |-- save(reaction)--->|                |               |
        |                          |                     |                |               |
        |                          |-- updateCounts ---->|                |               |
        |                          |                     |                |               |
        |                          |-- save(event) ----------------------------------->|
        |                          |                     |                |               |
        |<-- reaction -------------|                     |                |               |
        |                          |                     |                |               |
```

### 3.4 Endpoints per Tipo Utente

| Endpoint | Customer | Restaurant | Admin | Agency |
|----------|----------|------------|-------|--------|
| POST /posts | ❌ | ✅ | ✅ | ❌ |
| GET /feed | ✅ | ✅ | ✅ | ✅ |
| GET /posts/{id} | ✅ | ✅ | ✅ | ✅ |
| POST /posts/{id}/react | ✅ | ✅ | ✅ | ✅ |
| POST /posts/{id}/comment | ✅ | ✅ | ✅ | ✅ |
| POST /restaurants/{id}/follow | ✅ | ❌ | ❌ | ✅ |
| DELETE /restaurants/{id}/follow | ✅ | ❌ | ❌ | ✅ |
| DELETE /posts/{id} | ❌ | ✅ (own) | ✅ (any) | ❌ |
| DELETE /comments/{id} | ✅ (own) | ✅ | ✅ (any) | ✅ (own) |

---

## 4. Sistema Eventi Ristorante

### 4.1 Panoramica

Sistema per la gestione degli eventi dei ristoranti con RSVP e check-in.

**Entità:**
- `RestaurantEvent` - Evento del ristorante
- `EventRSVP` - Risposta RSVP di un utente

**Stati Evento:**
- `DRAFT` - Bozza
- `PUBLISHED` - Pubblicato
- `CANCELLED` - Cancellato
- `COMPLETED` - Completato

**Stati RSVP:**
- `CONFIRMED` - Confermato
- `CANCELLED` - Cancellato
- `WAITLIST` - In lista d'attesa
- `CHECKED_IN` - Check-in effettuato

### 4.2 Flusso: Creazione Evento e Pubblicazione

```
RestaurantEventController   RestaurantEventService    EventDAO       FollowDAO      EventOutboxDAO
        |                           |                    |               |               |
        |-- createEvent(data) ----->|                    |               |               |
        |                           |                    |               |               |
        |                           |-- save(event) ---->|               |               |
        |                           |<-- event (DRAFT)---|               |               |
        |<-- event -----------------|                    |               |               |
        |                           |                    |               |               |
        |-- publishEvent(id) ------>|                    |               |               |
        |                           |                    |               |               |
        |                           |-- update(PUBLISHED)->              |               |
        |                           |                    |               |               |
        |                           |-- getFollowers ------------------->|               |
        |                           |<-- followers ----------------------|               |
        |                           |                    |               |               |
        |                           | [for each follower]|               |               |
        |                           |-- save(notification event) ----------------------->|
        |                           |                    |               |               |
        |<-- event -----------------|                    |               |               |
        |                           |                    |               |               |
```

### 4.3 Flusso: RSVP con Gestione Capacità

```
CustomerEventController    RestaurantEventService      EventDAO        RSVPDAO       EventOutboxDAO
        |                          |                      |               |               |
        |-- rsvp(eventId) -------->|                      |               |               |
        |                          |                      |               |               |
        |                          |-- getEvent(id) ----->|               |               |
        |                          |<-- event ------------|               |               |
        |                          |                      |               |               |
        |                          |-- checkCapacity ---->|               |               |
        |                          |                      |               |               |
        |                          | [if capacity ok]     |               |               |
        |                          |-- save(CONFIRMED)------------------->|               |
        |                          |                      |               |               |
        |                          | [if capacity full]   |               |               |
        |                          |-- save(WAITLIST)-------------------->|               |
        |                          |                      |               |               |
        |                          |-- save(event) -------------------------------------->|
        |                          |                      |               |               |
        |<-- rsvp -----------------|                      |               |               |
        |                          |                      |               |               |
```

### 4.4 Flusso: Check-in all'Evento

```
RestaurantEventController   RestaurantEventService      RSVPDAO         EventOutboxDAO
        |                          |                       |                   |
        |-- checkIn(eventId,userId)->                      |                   |
        |                          |                       |                   |
        |                          |-- findRSVP(event,user)->                  |
        |                          |<-- rsvp --------------|                   |
        |                          |                       |                   |
        |                          |-- update(CHECKED_IN)->|                   |
        |                          |                       |                   |
        |                          |-- save(checkIn event) --------------->|
        |                          |                       |                   |
        |<-- rsvp -----------------|                       |                   |
        |                          |                       |                   |
```

### 4.5 Endpoints per Tipo Utente

| Endpoint | Customer | Restaurant | Admin | Agency |
|----------|----------|------------|-------|--------|
| POST /events | ❌ | ✅ | ✅ | ❌ |
| GET /events | ✅ | ✅ | ✅ | ✅ |
| GET /events/{id} | ✅ | ✅ | ✅ | ✅ |
| PUT /events/{id} | ❌ | ✅ | ✅ | ❌ |
| POST /events/{id}/publish | ❌ | ✅ | ✅ | ❌ |
| POST /events/{id}/cancel | ❌ | ✅ | ✅ | ❌ |
| POST /events/{id}/rsvp | ✅ | ❌ | ✅ | ✅ |
| DELETE /events/{id}/rsvp | ✅ | ❌ | ✅ | ✅ |
| POST /events/{id}/checkin/{userId} | ❌ | ✅ | ✅ | ❌ |
| GET /events/{id}/rsvps | ❌ | ✅ | ✅ | ❌ |

---

## 5. Struttura Controller per Tipo Utente

### 5.1 Organizzazione Package

```
com.application
├── customer/controller/
│   ├── CustomerChatController.java
│   ├── CustomerSupportController.java
│   ├── CustomerSocialController.java
│   └── CustomerEventController.java
│
├── restaurant/controller/
│   ├── RestaurantChatController.java
│   ├── RestaurantSupportController.java
│   ├── RestaurantSocialController.java
│   └── RestaurantEventController.java
│
├── admin/controller/
│   ├── AdminChatController.java
│   ├── AdminSupportController.java
│   ├── AdminSocialController.java
│   └── AdminEventController.java
│
└── agency/controller/
    ├── AgencyChatController.java
    ├── AgencySupportController.java
    ├── AgencySocialController.java
    └── AgencyEventController.java
```

### 5.2 Pattern di Autenticazione

```java
// Customer
@AuthenticationPrincipal Customer customer
customer.getId()

// Restaurant User (RUser)
@AuthenticationPrincipal RUser rUser
rUser.getId()
rUser.getRestaurant().getId()

// Admin
@AuthenticationPrincipal Admin admin
admin.getId()

// Agency User
@AuthenticationPrincipal AgencyUser agencyUser
agencyUser.getId()
agencyUser.getAgency().getId()
```

---

## 6. Sistema Audit

### 6.1 Panoramica

Due sistemi di audit complementari:

1. **AuditService** (generico) - Per tutti i tipi di operazioni
2. **ReservationAuditService** - Specializzato per prenotazioni

### 6.2 Flusso Audit Prenotazione

```
AdminReservationController   AdminReservationService      AuditService        AuditRepository
        |                           |                          |                     |
        |-- updateStatus(id,NEW)--->|                          |                     |
        |                           |                          |                     |
        |                           |-- findById(id) --------->|                     |
        |                           |<-- reservation (OLD_STATUS)                    |
        |                           |                          |                     |
        |                           |-- saveOldStatus          |                     |
        |                           |-- setStatus(NEW)         |                     |
        |                           |-- save() --------------->|                     |
        |                           |                          |                     |
        |                           |-- auditStatusChanged(id,OLD,NEW,adminId) ----->|
        |                           |                          |                     |
        |                           |                          |-- save(auditLog)-->|
        |                           |                          |<-- saved ----------|
        |                           |                          |                     |
        |<-- reservation (NEW) -----|                          |                     |
        |                           |                          |                     |
```

### 6.3 Dati Audit Registrati

| Campo | Descrizione |
|-------|-------------|
| `reservationId` | ID prenotazione |
| `restaurantId` | ID ristorante |
| `userId` | ID utente che ha effettuato l'operazione |
| `userType` | Tipo utente (CUSTOMER, RESTAURANT_USER, ADMIN, AGENCY) |
| `action` | Tipo operazione (CREATED, STATUS_CHANGED, UPDATED, etc.) |
| `oldValue` | Valore precedente |
| `newValue` | Nuovo valore |
| `reason` | Motivo del cambiamento |
| `timestamp` | Data/ora operazione |

---

## 📝 Note Tecniche

### Dipendenze Condivise

Tutti i service utilizzano:
- `EventOutboxDAO` per eventi asincroni
- `ObjectMapper` per serializzazione JSON

### Pattern EventOutbox

Gli eventi vengono salvati in `event_outbox` e processati da orchestrator:
- `CustomerOrchestrator`
- `RestaurantUserOrchestrator`
- `AdminOrchestrator`
- `AgencyUserOrchestrator`

### WebSocket Integration

I messaggi real-time sono gestiti tramite:
- STOMP over WebSocket
- Message broker (RabbitMQ)
- Subscription per conversazione/canale

---

## 🔧 Configurazione

### Application Properties

```properties
# Chat
chat.max-message-length=5000
chat.max-group-members=50

# Support
support.bot.enabled=true
support.auto-escalate-hours=24

# Social
social.feed.max-posts-per-page=20
social.max-comment-length=1000

# Events
events.default-reminder-hours=24
events.max-capacity-default=100
```

---

> **Documento generato automaticamente**  
> Per aggiornamenti e contributi, contattare il team di sviluppo.
