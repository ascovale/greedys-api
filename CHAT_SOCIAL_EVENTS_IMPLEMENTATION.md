# Chat/Support/Social/Events System Implementation

## Overview
Implementazione completa del sistema Chat, Support, Social e Events per Greedy's API.

**Data**: Dicembre 2025  
**Versione**: 1.0

---

## 📁 Struttura File Creati

### 🗂️ Controllers (`com.application.common.controller`)

| File | Descrizione | Endpoints |
|------|-------------|-----------|
| `chat/ChatController.java` | REST API per chat | 15 endpoints |
| `support/SupportController.java` | REST API per supporto | 11 endpoints |
| `social/SocialController.java` | REST API per social | 25 endpoints |
| `event/RestaurantEventController.java` | REST API per eventi | 22 endpoints |

### 🔧 Services (`com.application.common.service`)

| File | Descrizione |
|------|-------------|
| `chat/ChatService.java` | Gestione conversazioni e messaggi |
| `support/SupportTicketService.java` | Gestione ticket + BOT integration |
| `social/SocialPostService.java` | Gestione post, commenti, reazioni, follow |
| `event/RestaurantEventService.java` | Gestione eventi e RSVP |

### 📬 RabbitMQ Queues (`RabbitMQConfig.java`)

```java
// Chat Queues
QUEUE_CHAT_DIRECT = "notification.chat.direct"
QUEUE_CHAT_GROUP = "notification.chat.group"
QUEUE_CHAT_SUPPORT = "notification.chat.support"
QUEUE_CHAT_RESERVATION = "notification.chat.reservation"

// Social Queues
QUEUE_SOCIAL_FEED = "notification.social.feed"
QUEUE_SOCIAL_EVENTS = "notification.social.events"
```

### 🔌 WebSocket Destinations (`WebSocketDestinationValidator.java`)

```
/topic/chat/conversation/{conversationId}  - Messaggi conversazione
/topic/chat/typing/{conversationId}        - Typing indicators
/topic/chat/user/{userId}                  - Notifiche chat utente

/topic/social/feed/{userId}                - Feed updates
/topic/social/post/{postId}                - Post interactions
/topic/social/user/{userId}/notifications  - Social notifications

/topic/event/{eventId}                     - Event updates
/topic/event/{eventId}/rsvp                - RSVP updates
/topic/event/restaurant/{restaurantId}     - Restaurant events

/topic/support/ticket/{ticketId}           - Ticket updates
/topic/support/user/{userId}               - Support notifications
/topic/support/staff                       - Staff notifications (admin only)
```

---

## 🎯 API Endpoints

### 💬 Chat API (`/api/chat`)

| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/conversations/direct` | Crea conversazione 1-to-1 |
| POST | `/conversations/group` | Crea gruppo chat |
| POST | `/conversations/reservation` | Crea chat prenotazione |
| GET | `/conversations` | Lista conversazioni utente |
| GET | `/conversations/{id}` | Dettaglio conversazione |
| GET | `/conversations/{id}/messages` | Lista messaggi |
| GET | `/conversations/{id}/messages/after/{afterId}` | Messaggi recenti (sync) |
| POST | `/conversations/{id}/messages` | Invia messaggio |
| POST | `/conversations/{id}/messages/{msgId}/reply` | Rispondi a messaggio |
| DELETE | `/conversations/{id}/messages/{msgId}` | Elimina messaggio |
| GET | `/conversations/{id}/messages/search` | Cerca messaggi |
| PUT | `/conversations/{id}/read` | Marca come letto |
| GET | `/conversations/{id}/unread-count` | Conta non letti |
| GET | `/conversations/{id}/participants` | Lista partecipanti |
| POST | `/conversations/{id}/participants` | Aggiungi partecipante |
| DELETE | `/conversations/{id}/participants/{userId}` | Rimuovi partecipante |
| DELETE | `/conversations/{id}/leave` | Lascia conversazione |
| POST | `/conversations/{id}/typing` | Typing indicator |

### 🎫 Support API (`/api/support`)

**Customer/Restaurant:**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/tickets` | Crea ticket |
| GET | `/tickets` | Lista ticket propri |
| GET | `/tickets/{idOrNumber}` | Dettaglio ticket |
| GET | `/tickets/{id}/messages` | Messaggi ticket |
| POST | `/tickets/{id}/messages` | Aggiungi messaggio |
| POST | `/tickets/{id}/bot-helpful` | Conferma BOT utile |
| POST | `/tickets/{id}/request-human` | Richiedi operatore |

**Staff:**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/staff/tickets/open` | Ticket aperti |
| GET | `/staff/tickets/assigned` | Ticket assegnati |
| PUT | `/staff/tickets/{id}/assign` | Assegna ticket |
| PUT | `/staff/tickets/{id}/status` | Cambia stato |
| PUT | `/staff/tickets/{id}/escalate` | Escalation |
| POST | `/staff/tickets/{id}/messages` | Risposta staff |
| GET | `/staff/tickets/{id}/messages/all` | Tutti i messaggi (inclusi interni) |

### 📱 Social API (`/api/social`)

**Posts:**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/posts` | Crea post |
| POST | `/posts/checkin` | Crea check-in |
| GET | `/posts/{id}` | Dettaglio post |
| PUT | `/posts/{id}` | Modifica post |
| DELETE | `/posts/{id}` | Elimina post |
| PUT | `/posts/{id}/pin` | Pin/Unpin |
| POST | `/posts/{id}/view` | Registra view |
| POST | `/posts/{id}/share` | Condividi |

**Feed:**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/feed` | Feed personalizzato |
| GET | `/feed/trending` | Post trending |
| GET | `/feed/user/{userId}` | Post utente |
| GET | `/feed/restaurant/{id}` | Post ristorante |
| GET | `/search` | Cerca post |

**Reactions:**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/posts/{id}/reactions` | Aggiungi reazione |
| DELETE | `/posts/{id}/reactions` | Rimuovi reazione |
| GET | `/posts/{id}/reactions/check` | Verifica reazione |

**Comments:**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/posts/{id}/comments` | Lista commenti |
| GET | `/comments/{id}/replies` | Risposte a commento |
| POST | `/posts/{id}/comments` | Aggiungi commento |
| PUT | `/comments/{id}` | Modifica commento |
| DELETE | `/comments/{id}` | Elimina commento |

**Follow:**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/follow` | Segui |
| DELETE | `/follow` | Smetti di seguire |
| PUT | `/follow/{id}/accept` | Accetta richiesta |
| DELETE | `/follow/{id}/reject` | Rifiuta richiesta |
| POST | `/block` | Blocca utente |
| GET | `/follow/check` | Verifica follow |
| GET | `/followers` | Lista followers |
| GET | `/following` | Lista following |
| GET | `/follow/counts` | Contatori |

### 🎉 Events API (`/api/events`)

**Restaurant (Gestione):**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/` | Crea evento |
| PUT | `/{id}` | Modifica evento |
| PUT | `/{id}/publish` | Pubblica evento |
| PUT | `/{id}/cancel` | Cancella evento |
| DELETE | `/{id}` | Elimina evento |
| POST | `/{id}/reminders` | Invia reminder |
| GET | `/{id}/attendees` | Lista partecipanti |
| GET | `/{id}/attendees/checked-in` | Partecipanti check-in |
| POST | `/{id}/attendees/{rsvpId}/checkin` | Check-in |
| POST | `/{id}/attendees/checkin-user/{userId}` | Check-in per user |
| PUT | `/rsvp/{rsvpId}/confirm` | Conferma RSVP |

**Customer (Scoperta):**
| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/{id}` | Dettaglio evento |
| GET | `/restaurant/{id}` | Eventi ristorante |
| GET | `/restaurant/{id}/upcoming` | Eventi futuri |
| GET | `/date/{date}` | Eventi per data |
| GET | `/range` | Eventi in range |
| GET | `/type/{type}` | Eventi per tipo |
| GET | `/featured` | Eventi in evidenza |
| GET | `/available` | Eventi con posti |
| GET | `/search` | Cerca eventi |
| POST | `/{id}/rsvp` | Registrati (RSVP) |
| DELETE | `/{id}/rsvp` | Cancella RSVP |
| GET | `/{id}/rsvp` | RSVP utente |
| GET | `/my-events` | Miei eventi |

---

## 🔒 Security

### WebSocket Destination Validation

Il `WebSocketDestinationValidator` è stato aggiornato per validare i nuovi topic:

1. **Chat Topics**: Verifica che l'utente sia autenticato e, per topic user-specific, che l'ID corrisponda
2. **Social Topics**: Verifica autenticazione, per user notifications verifica ID match
3. **Event Topics**: Verifica autenticazione, per restaurant topics verifica staff access
4. **Support Topics**: Verifica autenticazione, staff topics solo per admin

---

## 📊 Event Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                        SERVICE LAYER                              │
│  ChatService / SupportTicketService / SocialPostService /        │
│  RestaurantEventService                                          │
│                           │                                       │
│                           ▼                                       │
│               ┌───────────────────────┐                          │
│               │   EventOutboxDAO.save │                          │
│               └───────────────────────┘                          │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    RABBIT MQ QUEUES                              │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐     │
│  │ chat.direct    │  │ social.feed    │  │ social.events  │     │
│  │ chat.group     │  └────────────────┘  └────────────────┘     │
│  │ chat.support   │                                              │
│  │ chat.reservation│                                             │
│  └────────────────┘                                              │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                       LISTENERS                                   │
│  ChatNotificationListener / SocialNotificationListener           │
│                           │                                       │
│                           ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │           ORCHESTRATORS                                     │  │
│  │   ChatOrchestrator / SocialOrchestrator                    │  │
│  │   - Disaggregazione per destinatari                        │  │
│  │   - Creazione notifiche individuali                        │  │
│  │   - Invio su canali (WebSocket, Push, Email)              │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                      WebSocket                                    │
│  /topic/chat/conversation/{id}                                   │
│  /topic/social/user/{id}/notifications                           │
│  /topic/event/{id}                                               │
│  /topic/support/ticket/{id}                                      │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📝 Next Steps (Suggeriti)

1. **Unit Tests**: Creare test per i nuovi controller e service
2. **Integration Tests**: Test end-to-end con RabbitMQ e WebSocket
3. **Security**: Implementare `@PreAuthorize` per validazione permessi specifici
4. **DTO Mapping**: Creare DTO response separate dalle entity per API pubbliche
5. **Rate Limiting**: Aggiungere rate limiting per le API social
6. **Caching**: Implementare cache per feed e trending posts
7. **Notifications**: Collegare i nuovi orchestrator al sistema push esistente

---

## ✅ Completato

- [x] RabbitMQConfig con 6 nuove code
- [x] ChatController (15 endpoints)
- [x] SupportController (11 endpoints)
- [x] SocialController (25 endpoints)
- [x] RestaurantEventController (22 endpoints)
- [x] WebSocketDestinationValidator aggiornato
- [x] Servizi: ChatService, SupportTicketService, SocialPostService, RestaurantEventService
- [x] Orchestrator: ChatOrchestrator, SocialOrchestrator
- [x] Listeners: ChatNotificationListener, SocialNotificationListener
- [x] Entities: ChatNotification, SocialNotification
- [x] DAOs: ChatNotificationDAO, SocialNotificationDAO
