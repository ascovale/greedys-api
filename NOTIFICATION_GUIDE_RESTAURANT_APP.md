# WebSocket Configuration - Restaurant Notifications

## Endpoints

```
ws://localhost:8080/ws
ws://localhost:8080/stomp
```

---

## Topic Format

```
/topic/notifications/{userId}/RESTAURANT
```

---

## Notification Payload

```json
{
  "notificationId": 456789,
  "eventId": "evt-3-order-12345",
  "title": "Nuova Prenotazione",
  "body": "Tavolo per 4 persone alle 20:00",
  "recipientType": "RESTAURANT",
  "timestamp": 1700000000,
  "properties": {
    "reservation_id": "12345",
    "guest_count": "4",
    "time_slot": "20:00"
  }
}
```

---

## Message Broker

- **Prefixes**: `/topic/`, `/queue/`
- **App prefix**: `/app`

---

## STOMP Configuration

**File:** `com.application.common.config.WebSocketConfig`

```
Endpoint /ws:
  - SockJS with fallback
  - AllowedOrigins: *
  - SessionCookieNeeded: true

Endpoint /stomp:
  - Native WebSocket
  - AllowedOrigins: http://*, https://*, null
```

---

## CORS Headers

```
Allow-Credentials: true
Allowed-Origins: http://*, https://*, null
Allowed-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Exposed-Headers: Authorization, Content-Type
```

---

## RabbitMQ Queue

```
notification.restaurant → RestaurantNotificationListener
```

---

## WebSocket Sender

**File:** `com.application.common.notification.service.NotificationWebSocketSender`

Sends via `SimpMessagingTemplate` to destination: `/topic/notifications/{userId}/{recipientType}`

---

## Authentication

**Header required:**
```
Authorization: Bearer {accessToken}
```

**Token must contain:**
- `userId`
- `userType` (RESTAURANT, CUSTOMER, AGENCY, ADMIN)
- `type` = "ACCESS_TOKEN"

---

## Security Chain

**File:** `com.application.common.spring.SecurityConfig`

```
1. TokenTypeValidationFilter
2. HubValidationFilter
3. UserTypeJwtRequestFilter
```

---

## Delivery Model

- **Latency**: ~1-2 seconds
- **Guarantee**: Best-effort (no retry if offline)
- **Idempotency**: UNIQUE(eventId, userId, userType) in DB
- **Persistence**: No (messages not stored if offline)
- **Mode**: Synchronous
