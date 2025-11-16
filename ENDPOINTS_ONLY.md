# Endpoints Aggiunti

## WebSocket Connection

**Endpoint Backend**: `/ws`

**URL Connessione**:
- **Dev locale**: `ws://localhost:8080/ws` (plain WebSocket, no SSL)
- **Produzione**: Dipende da SSL

**⚠️ IMPORTANTE - wss:// vs ws://**

| URL HTTPS | WebSocket | SSL Certificate |
|-----------|-----------|------------------|
| `https://api.greedys.it` | `wss://api.greedys.it/ws` | ✅ Valido |
| `http://localhost:8080` | `ws://localhost:8080/ws` | ❌ No SSL |

**Se stai su `https://api.greedys.it` con certificato invalido → Usa `ws://` (plain)**
**Se il certificato è valido → Usa `wss://` (secure)**

**La app Flutter converte automaticamente**:
- `https://api.greedys.it` + cert valido → `wss://api.greedys.it/ws` ✅
- `https://api.greedys.it` + cert invalido → Errore (cambio in `ws://`) ⚠️
- `http://localhost:8080` → `ws://localhost:8080/ws` ✅

---

## Notification API

### GET /restaurant/notifications/badge
Conta le notifiche non lette
```
Authorization: Bearer {token}
Response: { "count": 3 }
```

### POST /restaurant/notifications/menu-open
Resetta il contatore (segna come lette)
```
Authorization: Bearer {token}
Response: { "success": true }
```

### GET /restaurant/notifications?page=0&size=20
Lista paginate delle notifiche
```
Authorization: Bearer {token}
Response: { 
  "content": [ {...}, {...} ],
  "totalElements": 45,
  "totalPages": 3,
  "number": 0
}
```

## Reservation API

### PUT /restaurant/reservation/{id}/accept
Accetta una prenotazione
```
Request Body:
{
  "tableNumber": 5,
  "notes": "Vicino finestra"
}
Response: { "id": 123, "status": "ACCEPTED" }
```

### PUT /restaurant/reservation/{id}/reject
Rifiuta una prenotazione
```
Request Body:
{
  "reason": "Non disponibili a quell'ora"
}
Response: { "id": 123, "status": "REJECTED" }
```

### PUT /restaurant/reservation/{id}/seated
Marca come seduto
```
Response: { "id": 123, "status": "SEATED" }
```

### PUT /restaurant/reservation/{id}/no_show
Marca come non presentato
```
Response: { "id": 123, "status": "NO_SHOW" }
```

### GET /restaurant/reservation/pending/get
Lista prenotazioni in sospeso
```
Response: { 
  "content": [ {...}, {...} ],
  "totalElements": 5
}
```

## WebSocket Events (RESTAURANT APP)

**Connect**: `STOMP to /ws`

**Subscribe**: `/topic/restaurants/{restaurantId}/reservations`

**Events ricevuti**:
```json
{
  "type": "RESERVATION_CREATED|RESERVATION_ACCEPTED|RESERVATION_REJECTED",
  "reservation": { "id": 1, "customerName": "Marco", "pax": 4, ... },
  "timestamp": "2025-11-15T10:30:00Z"
}
```

---

## WebSocket Data (CUSTOMER APP)

**Connect**: `STOMP to /ws`

**Subscribe**: `/topic/customers/{customerId}/reservations`

**Events ricevuti**:
```json
{
  "type": "RESERVATION_CREATED|RESERVATION_ACCEPTED|RESERVATION_REJECTED",
  "reservation": {
    "id": 1,
    "restaurantName": "La Pizzeria",
    "date": "2025-11-20",
    "time": "20:00",
    "pax": 4,
    "kids": 1,
    "status": "ACCEPTED",
    "tableNumber": 5,
    "notes": "Vicino finestra"
  },
  "timestamp": "2025-11-15T10:30:00Z"
}
```

**Quando arrivano**:
- `RESERVATION_CREATED` - Subito dopo creazione (conferma lato app)
- `RESERVATION_ACCEPTED` - Quando ristorante accetta
- `RESERVATION_REJECTED` - Quando ristorante rifiuta (con motivo)
