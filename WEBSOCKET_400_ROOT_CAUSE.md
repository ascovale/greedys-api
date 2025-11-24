# WebSocket 400 Bad Request - ROOT CAUSE FOUND ✅

**Data**: 24 Novembre 2025  
**Status**: 🔴 **PROBLEMA IDENTIFICATO - SockJS rifiuta il handshake**

---

## LOG BACKEND ANALYSIS

### Backend Logs (Docker Container)
```
2025-11-24T20:51:49.540Z DEBUG nio-8080-exec-5
GET /ws?token=eyJhbGciOiJIUzI1NiJ9...

2025-11-24T20:51:49.542Z DEBUG nio-8080-exec-5
Mapped to SockJsHttpRequestHandler

2025-11-24T20:51:49.542Z DEBUG nio-8080-exec-5
Processing transport request: GET http://api.greedys.it/ws?token=...

2025-11-24T20:51:49.543Z DEBUG nio-8080-exec-5
Completed 400 BAD_REQUEST ❌
```

### Timeline Analisi
1. ✅ Request arriva a `/ws` con token nei query parameters
2. ✅ Spring mappa il request a `SockJsHttpRequestHandler`
3. ✅ SockJS inizia a processare il transport request
4. ❌ **SockJS rifiuta con 400 PRIMA di invocare WebSocketHandshakeInterceptor**
5. ❌ **Handshake mai raggiunto = token mai validato**

---

## ROOT CAUSE: SESSIONE COOKIE

### Problema
SockJS richiede una **sessione valida** prima di permettere WebSocket. Guarda il log:

```java
registry.addEndpoint("/ws")
    .addInterceptors(handshakeInterceptor)
    .withSockJS()
    .setSessionCookieNeeded(true)  // ← QUI!
```

**setSessionCookieNeeded(true)** significa:
- SockJS **richiede** un Session Cookie valido dalla richiesta
- Il client invia il token nei query parameters
- Ma **SockJS non riceve un Session Cookie valido**
- SockJS rifiuta la connessione con 400 **PRIMA** di arrivare all'interceptor

### Perché 400?
SockJS implementa il protocollo SockJS che richiede:
1. ✅ Request HTTP GET a `/ws`
2. ✅ Header `Upgrade: websocket` (WebSocket upgrade request)
3. ✅ **Session ID valido da cookie o header** ← **MANCA QUESTO**
4. ❌ Se session manca → 400 Bad Request

---

## SOLUZIONE

### Opzione 1: Disabilita Requirement del Session Cookie (SCONSIGLIATO)

```java
registry.addEndpoint("/ws")
    .addInterceptors(handshakeInterceptor)
    .withSockJS()
    .setSessionCookieNeeded(false);  // ❌ Disabilita session check
```

**Problema**: Riduce la sicurezza, permette connessioni anonime.

### Opzione 2: Client Invia Session Cookie (RECOMMENDED)

Il client JavaScript/Flutter deve:

```javascript
// JavaScript - Invia il token E ricevi la sessione
fetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({username, password}),
    credentials: 'include'  // ← IMPORTANTE: Accetta e invia cookies
})
.then(res => res.json())
.then(data => {
    // Ora il client ha un session cookie dalla risposta
    // Invia websocket con token nei query params
    const socket = new SockJS('/ws?token=' + data.jwt, null, {
        sessionId: generateSessionId  // ← Passa session ID esplicito
    });
})
```

### Opzione 3: Usa Custom Endpoint Senza SockJS (BEST)

Il file `WebSocketConfig.java` ha già configurato **due endpoint**:

```java
// Endpoint 1: /ws - SockJS (ha il problema)
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("*")
    .addInterceptors(handshakeInterceptor)
    .withSockJS()  // ← SockJS richiede session
    .setSessionCookieNeeded(true);

// Endpoint 2: /stomp - Native WebSocket PURO (no SockJS)
registry.addEndpoint("/stomp")
    .setAllowedOriginPatterns("http://*", "https://*", "null")
    .addInterceptors(handshakeInterceptor);
    // NO withSockJS() = WebSocket puro, senza session requirement
```

**Soluzione**: Il client deve usare `/stomp` invece di `/ws`!

---

## WHAT CLIENT SHOULD DO

### Current (BROKEN)
```javascript
// Flutter - stomp_dart_client
var socket = new SockJS('/ws?token=<jwt>');  // ❌ 400 Bad Request
```

### Fixed (WORKS)
```javascript
// Flutter - stomp_dart_client
// Usa l'endpoint /stomp che NON richiede sessione
final client = StompClient(
    config: StompConfig(
        url: 'wss://api.greedys.it/stomp?token=' + jwtToken,  // ← /stomp!
        onConnect: (StompFrame frame) {
            print("✅ Connected!");
        }
    )
);
```

---

## ALTERNATIVA: Configura SockJS Correttamente

Se il client INSISTE nell'usare `/ws` con SockJS, devi:

### 1️⃣ Disabilita Session Requirement

```java
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("*")
    .addInterceptors(handshakeInterceptor)
    .withSockJS()
    .setSessionCookieNeeded(false);  // ← Non richiede session
```

### 2️⃣ Oppure Invia il Cookie dalla Login Response

Il server login response deve includere:
```json
{
    "jwt": "eyJhbGciOiJIUzI1NiJ9...",
    "sessionId": "ABC123DEF456"
}
```

E il client lo include nella richiesta WebSocket:
```javascript
const socket = new SockJS('/ws?token=' + jwt + '&sessionId=' + sessionId);
```

---

## RACCOMANDAZIONE FINALE

### ✅ Usa /stomp Endpoint (Native WebSocket)

**Vantaggi**:
- ✅ No SockJS overhead
- ✅ No Session Cookie requirement
- ✅ Direct WebSocket (più veloce)
- ✅ Support iOS, Android, Flutter nativamente
- ✅ Token nei query parameters è sufficiente

**Svantaggio**:
- ❌ Browser vecchi (IE9, IE10) non supportano WebSocket nativo
  - Ma SockJS fallback è comunque disponibile in `/ws` per browser

### Modifica nel Client (Flutter)

**File**: `lib/services/websocket_manager.dart` (o simile)

```dart
// PRIMA (Broken)
String wsUrl = 'wss://api.greedys.it/ws?token=$jwtToken';

// DOPO (Fixed)
String wsUrl = 'wss://api.greedys.it/stomp?token=$jwtToken';
```

---

## DEBUG BACKEND (Optional)

Per verificare che l'interceptor sia richiamato, aggiungi logs dettagliati:

**File**: `src/main/resources/application.properties`

```properties
logging.level.com.application.common.security.websocket.WebSocketHandshakeInterceptor=DEBUG
logging.level.org.springframework.web.socket.sockjs=DEBUG
logging.level.org.springframework.web.socket.server.support=DEBUG
```

Poi prova di nuovo. Dovresti vedere:
```
DEBUG WebSocketHandshakeInterceptor: 🤝 WebSocket handshake initiated
DEBUG WebSocketHandshakeInterceptor: 🔐 JWT token extracted
DEBUG WebSocketHandshakeInterceptor: ✅ JWT signature validated
DEBUG WebSocketHandshakeInterceptor: ✅ WebSocket handshake successful
```

---

## CHECKLIST PER FIX

- [ ] **Client deve usare `/stomp` endpoint** (non `/ws`)
  - Change URL: `wss://api.greedys.it/stomp?token=JWT`
  - No SockJS needed
  - Native WebSocket only

- [ ] **Oppure: Disabilita session requirement** se devi usare `/ws`
  ```java
  .setSessionCookieNeeded(false)
  ```

- [ ] **Test di connessione**:
  - Fai login → ricevi JWT
  - Apri DevTools → Network tab
  - Fai connessione WebSocket
  - Verifica: 101 Switching Protocols (non 400)

- [ ] **Verify Backend Logs**:
  ```
  ✅ WebSocket handshake successful
  ✅ WebSocket connection established successfully
  ```

---

## TIMELINE NEXT STEPS

1. **Immediately** (5 min): Change client URL to `/stomp`
2. **Test** (2 min): Verify 101 Switching Protocols response
3. **Verify** (3 min): Check backend logs for "handshake successful"
4. **Subscribe** (2 min): Test STOMP subscription to `/topic/restaurant/ID/notifications`
5. **Monitor** (ongoing): Watch for real-time messages

---

## CONTACTS

**Issue**: Client WebSocket → 400 Bad Request  
**Root Cause**: SockJS session cookie requirement mismatch  
**Fix**: Use `/stomp` endpoint (native WebSocket, no session needed)  
**Impact**: Full real-time notification delivery working in < 15 minutes
