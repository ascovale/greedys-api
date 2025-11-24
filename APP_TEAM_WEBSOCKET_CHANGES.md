# WebSocket Endpoint Changes - App Team Communication

## ⚠️ NUOVI ENDPOINT WEBSOCKET DISPONIBILI

Ecco i cambiamenti che il team frontend deve conoscere per le notifiche.

---

## 1. Endpoint di Connessione (INVARIATI)

### Per Browser (Web App)
```
ws://api.example.com/ws
wss://api.example.com/ws  (HTTPS)
```

**Supporta**: 
- SockJS fallback per browser vecchi
- Session cookies
- STOMP over WebSocket

### Per Mobile/Flutter (NUOVO)
```
ws://api.example.com/stomp
wss://api.example.com/stomp  (HTTPS)
```

**Supporta**: 
- WebSocket nativo (no SockJS)
- Più leggero, meno overhead

---

## 2. Topic per Subscribe (CAMBIAMENTI IMPORTANTI)

### A. Topic Personali (Per User)
**Destinazione**: `/topic/{userType}/{userId}/notifications`

**Esempi**:
```
/topic/restaurant/123/notifications
/topic/customer/456/notifications
/topic/admin/789/notifications
/topic/agency/999/notifications
```

**Uso**: Notifiche personali (messaggi individuali, badge, alert)

**Chi riceve**: Solo l'utente con quell'ID

**Implementazione**:
```javascript
// JavaScript Browser
var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe a notifiche personali
    const userId = 123;  // Da JWT
    const userType = 'restaurant';  // Da JWT
    
    stompClient.subscribe(`/topic/${userType}/${userId}/notifications`, (message) => {
        const notification = JSON.parse(message.body);
        console.log('Personal notification:', notification);
    });
});
```

---

### B. Topic di Ristorante per Prenotazioni (NUOVO)
**Destinazione**: `/topic/restaurant/{restaurantId}/reservations`

**Esempio**:
```
/topic/restaurant/5/reservations
/topic/restaurant/10/reservations
```

**Uso**: Notifiche di team per prenotazioni (TUTTI gli staff vedono la stessa notification)

**Chi riceve**: TUTTI gli staff di quel ristorante

**Quando**: Quando un CUSTOMER crea una prenotazione

**Implementazione**:
```javascript
// Tutti gli staff del ristorante 5 si iscrivono
const restaurantId = 5;  // Da JWT

stompClient.subscribe(`/topic/restaurant/${restaurantId}/reservations`, (message) => {
    const notification = JSON.parse(message.body);
    console.log('Team notification - New reservation:', notification);
    
    // Aggiorna lista prenotazioni in tempo reale
    updateReservationList(notification);
});
```

---

### C. Topic per Agency (NUOVO)
**Destinazione**: `/topic/agency/{agencyId}/notifications`

**Esempio**:
```
/topic/agency/15/notifications
```

**Uso**: Notifiche per team di agenzia

**Chi riceve**: TUTTI gli staff dell'agenzia

---

## 3. Formato del Messaggio (PAYLOAD)

### Notifiche Personali
```json
{
  "notificationId": 12345,
  "title": "Nuova prenotazione",
  "body": "Tavolo per 4 persone alle 20:00",
  "timestamp": "2025-11-24T14:30:00Z",
  "type": "RESERVATION_REQUESTED",
  "data": {
    "reservationId": 12345,
    "customerId": 789,
    "customerName": "Mario Rossi",
    "pax": 4,
    "kids": 0,
    "date": "2025-12-24",
    "notes": "Vegetarian dishes"
  }
}
```

### Notifiche di Team (Prenotazioni)
```json
{
  "notificationId": 54321,
  "title": "Nuova prenotazione dal cliente",
  "body": "Mario Rossi - 4 persone - 20:00",
  "timestamp": "2025-11-24T14:30:00Z",
  "type": "RESERVATION_REQUESTED",
  "readByAll": true,
  "priority": "HIGH",
  "data": {
    "reservationId": 12345,
    "customerId": 789,
    "customerName": "Mario Rossi",
    "pax": 4,
    "kids": 0,
    "date": "2025-12-24",
    "email": "mario@example.com",
    "notes": "Vegetarian dishes"
  }
}
```

---

## 4. Matrice di Subscribe per ogni Ruolo

### Restaurant User (Staff)
```
✅ SUBSCRIBE a: /topic/restaurant/{restaurantId}/notifications
✅ SUBSCRIBE a: /topic/restaurant/{restaurantId}/reservations  ← NUOVO
✅ SUBSCRIBE a: /topic/ruser/{userId}/notifications           ← Personal
❌ CANNOT subscribe a: /topic/customer/* (wrong role)
```

### Customer
```
✅ SUBSCRIBE a: /topic/customer/{customerId}/notifications
✅ SUBSCRIBE a: /topic/customer/{customerId}/orders           ← Future
❌ CANNOT subscribe a: /topic/restaurant/* (wrong role)
```

### Admin
```
✅ SUBSCRIBE a: /topic/admin/{adminId}/notifications
✅ SUBSCRIBE a: /broadcast/*                                  ← Broadcast per tutti
```

### Agency User
```
✅ SUBSCRIBE a: /topic/agency/{agencyId}/notifications         ← NUOVO
✅ SUBSCRIBE a: /topic/agency/{agencyId}/users               ← Future
❌ CANNOT subscribe a: /topic/restaurant/* (wrong role)
```

---

## 5. Sicurezza WebSocket

### JWT Validation
- ✅ JWT estratto da header `Authorization: Bearer <token>`
- ✅ JWT validato in `WebSocketHandshakeInterceptor`
- ✅ Se JWT scaduto → connessione rifiutata

### Authorization Check
- ✅ Destination validator verifica che restaurantId in URL == restaurantId in JWT
- ✅ Se non corrisponde → `AccessDeniedException`
- ✅ Esempio: Staff di ristorante 5 NON può accedere a `/topic/restaurant/10/reservations`

```java
// Server-side validation (automatic)
// Se userId=123 tenta di subscribe a /topic/restaurant/10/reservations
// ma JWT contiene restaurantId=5, viene rifiutato con:
// "Restaurant user 123 (restaurantId: 5) denied access to /topic/restaurant/10/reservations (MISMATCH)"
```

---

## 6. Implementazione Completa (JavaScript Example)

```javascript
class WebSocketClient {
    constructor(token, userType, userId, restaurantId) {
        this.token = token;
        this.userType = userType;      // 'restaurant', 'customer', 'admin', 'agency'
        this.userId = userId;          // e.g., 123
        this.restaurantId = restaurantId; // For restaurant users, e.g., 5
        this.stompClient = null;
    }
    
    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        // Header con JWT
        const headers = {
            'Authorization': `Bearer ${this.token}`
        };
        
        this.stompClient.connect(headers, (frame) => {
            console.log('✅ WebSocket Connected');
            this.subscribeToPersonalNotifications();
            this.subscribeToTeamChannels();
        });
    }
    
    subscribeToPersonalNotifications() {
        // Subscribe a notifiche personali
        const personalTopic = `/topic/${this.userType}/${this.userId}/notifications`;
        
        this.stompClient.subscribe(personalTopic, (message) => {
            const notification = JSON.parse(message.body);
            console.log('📬 Personal notification:', notification);
            this.handlePersonalNotification(notification);
        });
        
        console.log(`✅ Subscribed to: ${personalTopic}`);
    }
    
    subscribeToTeamChannels() {
        if (this.userType === 'restaurant') {
            // Subscribe a notifiche di team (prenotazioni)
            const teamTopic = `/topic/restaurant/${this.restaurantId}/reservations`;
            
            this.stompClient.subscribe(teamTopic, (message) => {
                const notification = JSON.parse(message.body);
                console.log('🏢 Team notification - New reservation:', notification);
                this.handleTeamNotification(notification);
            });
            
            console.log(`✅ Subscribed to: ${teamTopic}`);
        } else if (this.userType === 'agency') {
            // Agency users subscribe to agency channel
            const agencyTopic = `/topic/agency/${this.agencyId}/notifications`;
            
            this.stompClient.subscribe(agencyTopic, (message) => {
                const notification = JSON.parse(message.body);
                console.log('🏢 Agency team notification:', notification);
                this.handleTeamNotification(notification);
            });
            
            console.log(`✅ Subscribed to: ${agencyTopic}`);
        }
    }
    
    handlePersonalNotification(notification) {
        // Mostra notifiche personali: badge, popup, etc
        this.showToast(notification.title);
        this.updateBadge(1);
    }
    
    handleTeamNotification(notification) {
        // Aggiorna lista prenotazioni in tempo reale
        // readByAll=true significa che è una notifica di team
        if (notification.readByAll) {
            this.updateSharedReservationList(notification);
        }
    }
    
    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            console.log('❌ WebSocket Disconnected');
        }
    }
}

// Utilizzo
const client = new WebSocketClient(
    'eyJhbGc...',           // JWT token
    'restaurant',            // userType
    123,                     // userId
    5                        // restaurantId
);

client.connect();
```

---

## 7. Checklist per il Team App

- [ ] Aggiornare endpoint WebSocket da `/ws` a supportare sia `/ws` (browsers) che `/stomp` (mobile)
- [ ] Implementare subscribe a `/topic/restaurant/{restaurantId}/reservations` per staff
- [ ] Implementare subscribe a `/topic/agency/{agencyId}/notifications` per agency users
- [ ] Verificare che JWT sia passato in Authorization header durante handshake
- [ ] Implementare handler per `readByAll=true` (notifiche di team)
- [ ] Testare che utenti NON autorizzati ricevano `AccessDeniedException` quando tentano di subscribe a topic sbagliato
- [ ] Aggiornare UI per mostrare notifiche di team in tempo reale (non solo personali)
- [ ] Verificare fallback SockJS per browser senza WebSocket nativo

---

## 8. Testing WebSocket

### Test 1: Connect e Subscribe Personale
```bash
# Terminal 1: Connect via WebSocket
# Endpoint: ws://localhost:8080/ws (or /stomp)
# Headers: Authorization: Bearer <JWT_TOKEN>

# Expected: CONNECTED frame
# Then subscribe to: /topic/restaurant/123/notifications
# Expected: Should receive subscription confirmation
```

### Test 2: Receive Team Notification
```bash
# Terminal 2: Create reservation via REST API
POST /api/customer/reservations
Authorization: Bearer <CUSTOMER_JWT>
Body: {pax: 4, kids: 0, date: "2025-12-24", ...}

# Terminal 1 WebSocket: Should receive message on
# /topic/restaurant/5/reservations
# with readByAll=true
```

### Test 3: Security - Unauthorized Access
```bash
# Try to subscribe to wrong restaurant
# /topic/restaurant/10/reservations (user's restaurantId=5 in JWT)
# Expected: AccessDeniedException
```

---

## 9. Breaking Changes

**⚠️ Se aggiornate il frontend, notate:**

| Vecchio | Nuovo | Status |
|--------|-------|--------|
| N/A | `/topic/restaurant/{id}/reservations` | ✅ NUOVO |
| N/A | `/topic/agency/{id}/notifications` | ✅ NUOVO |
| `/topic/{userType}/{id}/notifications` | SAME | ✅ Invariato |
| `/ws` endpoint | SAME (SockJS) | ✅ Invariato |
| `/stomp` endpoint | ✅ NUOVO per mobile | ✅ NUOVO |

---

## 10. Timing per il Ricevimento

**Customer crea prenotazione** → T+0ms
↓
**EventOutbox salvato in DB** → T+0ms
↓
**EventOutboxOrchestrator poll** → T+0-1000ms (ogni 1 secondo)
↓
**Messaggio pubblicato a RabbitMQ** → T+1000ms
↓
**RestaurantTeamNotificationListener riceve** → T+1000-100ms
↓
**WebSocket invia a team channel** → T+1100ms (max)
↓
**✅ Staff riceve notification** → T+1100ms (TOTAL LATENCY ~1 secondo)

---

## Domande Comuni

**Q: Perché userType nella destinazione?**
A: Perché userId=123 potrebbe essere sia RUser che Customer. Includere userType previene routing a utente sbagliato.

**Q: Cosa succede se JWT scade durante sessione WebSocket?**
A: La connessione rimane aperta, ma nuovo subscribe fallirà con 401. Cliente deve riconnettersi con nuovo JWT.

**Q: Qual è la differenza tra /topic/{userType}/{id}/notifications e /topic/restaurant/{id}/reservations?**
A: Primo è personale (solo quell'utente), secondo è di team (TUTTI gli staff del ristorante).

**Q: Posso usare SockJS per mobile?**
A: Sì, `/ws` endpoint supporta SockJS. Ma `/stomp` è più leggero (no fallback).

---

## Contatti per Domande

Per domande sugli endpoint WebSocket contattare il team backend!
