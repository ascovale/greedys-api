# WebSocket CONNECT Frame Rejected - ROOT CAUSE #2 ✅

**Data**: 24 Novembre 2025  
**Status**: 🔴 **SECONDO PROBLEMA IDENTIFICATO - Authentication headers non propagati**

---

## LOG ANALYSIS

```
✅ WebSocket handshake successful
✅ WebSocket connection established successfully

❌ CONNECT frame rejected: No valid authentication
❌ Access denied for CONNECT message: Not authenticated
❌ WebSocket closed with CloseStatus[code=1002]
```

---

## ROOT CAUSE: Authentication Headers Not Propagated

### Handshake vs STOMP CONNECT

**HANDSHAKE (WebSocketHandshakeInterceptor.java:line 145-150)**
```java
attributes.put(WS_AUTHENTICATION_ATTR, authToken);  // ✅ Salva qui
attributes.put(WS_USER_ID_ATTR, userId);
attributes.put(WS_USERNAME_ATTR, username);
```

**CONNECT Frame (WebSocketChannelInterceptor.java:line 110-120)**
```java
Object auth = accessor.getHeader("ws-authentication");  // ❌ Cerca qui (diverso!)

if (auth instanceof WebSocketAuthenticationToken) {
    // OK
} else {
    throw new AccessDeniedException("Not authenticated");  // ❌ Lancia qui
}
```

### Il Problema

1. **Handshake interceptor**: Salva autenticazione negli **session attributes**
2. **STOMP CONNECT frame**: Cerca autenticazione negli **message headers**
3. ❌ **Mismatch**: I dati sono in un posto, ma il code li cerca in un altro
4. ❌ **Result**: "Not authenticated" anche se il token è valido

---

## SOLUZIONE: Propaga WebSocketAuthenticationToken dai Session Attributes ai Message Headers

### File: `WebSocketChannelInterceptor.java`

**Modifica il metodo `handleConnect()`** (righe 109-125):

```java
private Message<?> handleConnect(Message<?> message, SimpMessageHeaderAccessor accessor) {
    String sessionId = accessor.getSessionId();
    
    // PRIMA (BROKEN):
    // Object auth = accessor.getHeader("ws-authentication");
    
    // DOPO (FIXED):
    // Prova prima nei message headers
    Object auth = accessor.getHeader("ws-authentication");
    
    // Se non trovato nei headers, cerca negli session attributes
    if (!(auth instanceof WebSocketAuthenticationToken)) {
        // Spring popola sessionAttributes nei headers - cerca lì
        java.util.Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            auth = sessionAttributes.get(WebSocketHandshakeInterceptor.WS_AUTHENTICATION_ATTR);
            log.debug("Retrieved authentication from session attributes");
        }
    }
    
    if (auth instanceof WebSocketAuthenticationToken) {
        WebSocketAuthenticationToken token = (WebSocketAuthenticationToken) auth;
        log.info("✅ CONNECT frame: User {} (type: {}) connected", 
                 token.getUsername(), token.getUserType());
        return message;
    } else {
        log.warn("❌ CONNECT frame rejected: No valid authentication for session {}", sessionId);
        throw new AccessDeniedException("Not authenticated");
    }
}
```

**Modifica il metodo `extractAuthentication()`** (righe 217-232):

```java
private WebSocketAuthenticationToken extractAuthentication(SimpMessageHeaderAccessor accessor) {
    // Try custom header first
    Object auth = accessor.getHeader("ws-authentication");
    if (auth instanceof WebSocketAuthenticationToken) {
        return (WebSocketAuthenticationToken) auth;
    }
    
    // AGGIUNTO: Try session attributes (set by handshake interceptor)
    java.util.Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    if (sessionAttributes != null) {
        auth = sessionAttributes.get(WebSocketHandshakeInterceptor.WS_AUTHENTICATION_ATTR);
        if (auth instanceof WebSocketAuthenticationToken) {
            log.debug("Retrieved authentication from session attributes");
            return (WebSocketAuthenticationToken) auth;
        }
    }
    
    // Try Spring Security user principal
    if (accessor.getUser() != null) {
        log.debug("Using Spring Security principal instead of custom header");
        return null;
    }
    
    return null;
}
```

---

## ALTERNATIVA: Propaga negli Headers durante Handshake

**File**: `WebSocketHandshakeInterceptor.java` (righe 140-155)

**Aggiunta**: Registra l'autenticazione in modo che Spring la propag hi automaticamente

```java
// Oltre ad aggiungere negli attributes:
attributes.put(WS_AUTHENTICATION_ATTR, authToken);
attributes.put(WS_USER_ID_ATTR, userId);

// AGGIUNTA: Registra anche nel STOMP headers per accesso nei CONNECT frame
// Spring automaticamente propaga session attributes nei STOMP message headers
// Ma per sicurezza, forziamo anche qui:
attributes.put("simpUser", new org.springframework.security.core.userdetails.User(
    username,
    "",
    java.util.Collections.emptyList()
));
```

---

## TIMELINE FIX

### ✅ Option 1: Quick Fix (Raccomandato)

**Modifica**: `WebSocketChannelInterceptor.java`

**Metodi interessati**:
- `handleConnect()` - Add fallback to session attributes
- `extractAuthentication()` - Check session attributes

**Time**: 5 minuti di coding + 2 minuti di test

**Rischio**: Basso - solo aggiunge fallback, non rimuove logica esistente

### ✅ Option 2: Better Fix

**Modifica**: `WebSocketHandshakeInterceptor.java`

**Aggiunta**: Propaga l'autenticazione nei STOMP headers durante handshake

**Time**: 10 minuti di coding + 3 minuti di test

**Rischio**: Molto basso - segue Spring best practices

---

## IMPLEMENTAZIONE RAPIDA

### File da Modificare
1. `/greedys_api/src/main/java/com/application/common/security/websocket/WebSocketChannelInterceptor.java`

### Cambiamenti Specifici

#### Change 1: handleConnect() method (Lines 109-125)

```java
// BEFORE
private Message<?> handleConnect(Message<?> message, SimpMessageHeaderAccessor accessor) {
    String sessionId = accessor.getSessionId();
    
    // Get authentication from message headers (set by handshake interceptor)
    Object auth = accessor.getHeader("ws-authentication");
    
    if (auth instanceof WebSocketAuthenticationToken) {
        WebSocketAuthenticationToken token = (WebSocketAuthenticationToken) auth;
        log.info("CONNECT frame: User {} (type: {}) connected", 
                 token.getUsername(), token.getUserType());
        return message;
    } else {
        log.warn("CONNECT frame rejected: No valid authentication for session {}", sessionId);
        throw new AccessDeniedException("Not authenticated");
    }
}

// AFTER
private Message<?> handleConnect(Message<?> message, SimpMessageHeaderAccessor accessor) {
    String sessionId = accessor.getSessionId();
    
    // Get authentication from message headers (set by handshake interceptor)
    Object auth = accessor.getHeader("ws-authentication");
    
    // Fallback: check session attributes (handshake sets them here)
    if (!(auth instanceof WebSocketAuthenticationToken)) {
        java.util.Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            auth = sessionAttributes.get(WebSocketHandshakeInterceptor.WS_AUTHENTICATION_ATTR);
        }
    }
    
    if (auth instanceof WebSocketAuthenticationToken) {
        WebSocketAuthenticationToken token = (WebSocketAuthenticationToken) auth;
        log.info("✅ CONNECT frame: User {} (type: {}) connected", 
                 token.getUsername(), token.getUserType());
        return message;
    } else {
        log.warn("❌ CONNECT frame rejected: No valid authentication for session {}", sessionId);
        throw new AccessDeniedException("Not authenticated");
    }
}
```

#### Change 2: extractAuthentication() method (Lines 217-232)

```java
// BEFORE
private WebSocketAuthenticationToken extractAuthentication(SimpMessageHeaderAccessor accessor) {
    // Try custom header first
    Object auth = accessor.getHeader("ws-authentication");
    if (auth instanceof WebSocketAuthenticationToken) {
        return (WebSocketAuthenticationToken) auth;
    }
    
    // Try Spring Security user principal
    if (accessor.getUser() != null) {
        log.debug("Using Spring Security principal instead of custom header");
        return null;
    }
    
    return null;
}

// AFTER
private WebSocketAuthenticationToken extractAuthentication(SimpMessageHeaderAccessor accessor) {
    // Try custom header first
    Object auth = accessor.getHeader("ws-authentication");
    if (auth instanceof WebSocketAuthenticationToken) {
        return (WebSocketAuthenticationToken) auth;
    }
    
    // Fallback: try session attributes (set by handshake interceptor)
    java.util.Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    if (sessionAttributes != null) {
        auth = sessionAttributes.get(WebSocketHandshakeInterceptor.WS_AUTHENTICATION_ATTR);
        if (auth instanceof WebSocketAuthenticationToken) {
            log.debug("Retrieved authentication from session attributes");
            return (WebSocketAuthenticationToken) auth;
        }
    }
    
    // Try Spring Security user principal
    if (accessor.getUser() != null) {
        log.debug("Using Spring Security principal instead of custom header");
        return null;
    }
    
    return null;
}
```

---

## TESTING AFTER FIX

### Test 1: WebSocket Connection
```
curl -i -N -H "Connection: Upgrade" \
     -H "Upgrade: websocket" \
     "ws://api.greedys.it/stomp?token=JWT"
```

**Expected**: 101 Switching Protocols ✅

### Test 2: STOMP CONNECT
```javascript
const client = Stomp.over(socket);
client.connect(
    {},
    (frame) => console.log("✅ Connected"),
    (error) => console.log("❌ Error:", error)
);
```

**Expected**: `✅ Connected` (no error)

### Test 3: Backend Logs
```
✅ CONNECT frame: User test@test.it:3 (type: restaurant-user) connected
✅ SUBSCRIBE allowed: User test@test.it:3 -> /topic/restaurant/123/notifications
```

---

## VERIFICATION CHECKLIST

- [ ] File `WebSocketChannelInterceptor.java` modificato con fallback
- [ ] `handleConnect()` legge da session attributes
- [ ] `extractAuthentication()` legge da session attributes
- [ ] Codice compilato senza errori
- [ ] Test di connessione WebSocket passa
- [ ] Backend logs mostrano "✅ CONNECT frame" (non "❌ rejected")
- [ ] SUBSCRIBE allowed per /topic/...
- [ ] Notifiche ricevute sul client

---

## NEXT STEPS

1. **Applica il fix** a `WebSocketChannelInterceptor.java`
2. **Compila** il progetto
3. **Testa** la connessione WebSocket
4. **Verifica** i logs nel backend
5. **Conferma** che le notifiche sono ricevute
