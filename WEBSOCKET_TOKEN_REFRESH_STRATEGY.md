# WebSocket Token Refresh Integration Strategy

## Current State - Token Refresh Già Implementato ✅

### 1. RUser (Restaurant Staff) - Multi-Restaurant Support

**File**: `RestaurantAuthenticationService.java`

```java
// ✅ Hub User seleziona un ristorante
public AuthResponseDTO selectRestaurant(Long restaurantId) {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    
    // Trova il RUser specifico per questo ristorante
    RUser user = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
    if (user == null) throw exception;
    if (!user.isEnabled()) throw exception;
    
    // ✅ Genera JWT con restaurantId incluso
    String jwt = jwtUtil.generateToken(user);  // ← JWT contiene restaurantId
    return new AuthResponseDTO(jwt, rUserMapper.toDTO(user));
}

// ✅ Refresh Token per RUser
public AuthResponseDTO refreshRUserToken(String refreshToken) {
    // Valida che sia refresh token
    if (!jwtUtil.isRefreshToken(refreshToken)) throw exception;
    
    // Estrae email:restaurantId dal token
    String username = jwtUtil.extractUsername(refreshToken);  // "mario@email.com:5"
    String[] parts = username.split(":");
    String email = parts[0];
    Long restaurantId = Long.parseLong(parts[1]);
    
    // Trova il RUser specifico
    RUser rUser = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
    if (rUser == null) throw exception;
    
    // Valida refresh token
    if (!jwtUtil.validateToken(refreshToken, rUser)) throw exception;
    
    // ✅ Genera NUOVO JWT + refresh token
    String newJwt = jwtUtil.generateToken(rUser);
    String newRefreshToken = jwtUtil.generateRefreshToken(rUser);
    
    return AuthResponseDTO.builder()
            .jwt(newJwt)
            .refreshToken(newRefreshToken)
            .user(rUserMapper.toDTO(rUser))
            .build();
}
```

**Key Point**: JWT contiene `email:restaurantId` nel subject → quando fa refresh, sa per quale ristorante

---

### 2. JWT Structure

**Access Token per RUser**:
```json
{
  "sub": "mario@email.com:5",        // email:restaurantId
  "user_type": "restaurant-user",
  "user_id": 123,
  "restaurant_id": 5,
  "email": "mario@email.com",
  "authorities": ["ROLE_RESTAURANT"],
  "iat": 1700815200,
  "exp": 1700818800               // 1 ora
}
```

**Refresh Token per RUser**:
```json
{
  "sub": "mario@email.com:5",        // SAME formato
  "user_type": "restaurant-user",
  "user_id": 123,
  "restaurant_id": 5,
  "is_refresh_token": true,          // ← Marker
  "iat": 1700815200,
  "exp": 1700901600                  // 7 giorni
}
```

---

## Problem: WebSocket Non Usa Token Refresh

### Current WebSocket Flow

```
1. Client connette a /ws con JWT (access token)
   └─ WebSocketHandshakeInterceptor valida JWT

2. JWT salvato in WebSocket session attributes
   └─ Valido per 1 ora

3. Se JWT scade dopo 30 minuti:
   ├─ Client rimane connesso
   ├─ Ma se prova SUBSCRIBE a nuova destinazione
   └─ ❌ WebSocketChannelInterceptor rifiuta (JWT scaduto?)

4. ❌ PROBLEMA: Non c'è meccanismo di refresh token
   └─ Client deve disconnettere e riconnettere con nuovo JWT
```

---

## Solution: Implement WebSocket Token Refresh

### Approach 1: Client-Side Refresh (Recommended)

**Vantaggi**: 
- ✅ Simple, no server changes needed
- ✅ Client has full control
- ✅ Standard pattern

**Flow**:
```javascript
// Client JavaScript
const stompClient = Stomp.over(socket);

stompClient.connect({'Authorization': 'Bearer ' + accessToken}, (frame) => {
    // Subscribe to WebSocket destinations
    stompClient.subscribe('/topic/restaurant/5/reservations', (msg) => {
        handleNotification(msg);
    });
});

// Periodically refresh token (e.g., every 30 minutes)
setInterval(() => {
    // Call REST endpoint to get new access token
    fetch('/api/refresh', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({refreshToken: storedRefreshToken})
    })
    .then(res => res.json())
    .then(data => {
        accessToken = data.jwt;  // Update local token
        refreshToken = data.refreshToken;  // Update refresh token
        
        // ⚠️ Option 1: Disconnect and reconnect (interrupts stream)
        stompClient.disconnect(() => {
            connectWebSocket(accessToken);
        });
        
        // ✅ Option 2: Update token in session (better)
        // Send UPDATE_TOKEN command to server
        stompClient.send('/app/update-token', {}, JSON.stringify({
            newToken: accessToken
        }));
    });
}, 30 * 60 * 1000);  // Every 30 minutes
```

---

### Approach 2: Server-Side Refresh with Heartbeat (Advanced)

**Server sends periodic PING with new token**

```java
// WebSocketConfig.java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/queue", "/topic")
        .setHeartbeatValue(new long[]{30000, 30000});  // 30 sec heartbeat
}

// New @MessageMapping to handle heartbeat
@Controller
public class WebSocketHeartbeatController {
    
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(WebSocketAuthenticationToken auth) {
        // Called when server needs to send heartbeat
        // Can include new token if current one is expiring
    }
}
```

---

### Approach 3: Token Validation + Refresh on SUBSCRIBE (Best for WebSocket)

**Validate and optionally refresh token on every SUBSCRIBE frame**

**File to modify**: `WebSocketChannelInterceptor.java`

```java
private Message<?> handleSubscribe(Message<?> message, SimpMessageHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    
    // Extract authentication from session
    WebSocketAuthenticationToken auth = extractAuthentication(accessor);
    
    if (auth == null || !auth.isAuthenticated()) {
        log.warn("SUBSCRIBE rejected: Not authenticated");
        throw new AccessDeniedException("Not authenticated");
    }
    
    // ✅ NEW: Check if JWT is expiring soon
    long expiresIn = jwtUtil.getTimeUntilExpiration(auth.getTokenString());
    if (expiresIn < 60 * 1000) {  // Less than 1 minute
        log.info("JWT expiring in {}ms, requesting client refresh", expiresIn);
        
        // ✅ Send signal to client to refresh
        accessor.setHeader("x-token-expiring", "true");
        accessor.setHeader("x-token-expires-in", String.valueOf(expiresIn));
        
        // In real implementation: could reject SUBSCRIBE and force refresh
        // For now: just warn and let proceed
    }
    
    // Continue with existing validation...
    boolean allowed = destinationValidator.canAccess(
            destination,
            auth.getUserType(),
            auth.getUserId(),
            auth.getRestaurantIdFromClaims(),
            auth.getAgencyIdFromClaims()
    );
    
    if (!allowed) {
        throw new AccessDeniedException("Not authorized");
    }
    
    return message;
}
```

---

## Recommended Implementation: Hybrid Approach

**Combine Client-Side Refresh + Server-Side Validation**

### Step 1: Client-Side - Periodic Token Refresh

**File**: `websocket.js` (client-side JavaScript)

```javascript
class WebSocketManager {
    constructor(token, refreshToken) {
        this.accessToken = token;
        this.refreshToken = refreshToken;
        this.tokenRefreshInterval = null;
    }
    
    connect() {
        const socket = new SockJS('/ws?token=' + this.accessToken);
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('✅ Connected to WebSocket');
            this.subscribeToChannels();
            this.startTokenRefreshTimer();  // ✅ Start refresh
        });
    }
    
    startTokenRefreshTimer() {
        // Refresh token every 25 minutes (token expires in 60 min)
        this.tokenRefreshInterval = setInterval(() => {
            this.refreshAccessToken();
        }, 25 * 60 * 1000);
    }
    
    async refreshAccessToken() {
        console.log('🔄 Refreshing access token...');
        
        try {
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    refreshToken: this.refreshToken
                })
            });
            
            if (!response.ok) {
                console.error('❌ Token refresh failed');
                this.handleTokenExpired();
                return;
            }
            
            const data = await response.json();
            this.accessToken = data.jwt;
            this.refreshToken = data.refreshToken;
            
            console.log('✅ Token refreshed successfully');
            
            // ✅ Optionally: Disconnect and reconnect with new token
            // This ensures WebSocket uses fresh token
            this.reconnectWithNewToken();
            
        } catch (error) {
            console.error('❌ Token refresh error:', error);
            this.handleTokenExpired();
        }
    }
    
    reconnectWithNewToken() {
        console.log('🔄 Reconnecting WebSocket with new token...');
        
        // Save subscriptions
        const subscriptions = this.currentSubscriptions;
        
        // Disconnect
        this.stompClient.disconnect(() => {
            // Reconnect with new token
            const socket = new SockJS('/ws?token=' + this.accessToken);
            this.stompClient = Stomp.over(socket);
            
            this.stompClient.connect({}, (frame) => {
                console.log('✅ Reconnected with new token');
                
                // Re-subscribe to channels
                subscriptions.forEach(sub => {
                    this.stompClient.subscribe(sub.destination, sub.callback);
                });
            });
        });
    }
    
    handleTokenExpired() {
        console.error('❌ Token expired, user needs to login again');
        clearInterval(this.tokenRefreshInterval);
        this.stompClient.disconnect();
        window.location.href = '/login';
    }
    
    subscribeToChannels() {
        const restaurantId = this.getRestaurantIdFromContext();
        
        // Personal notifications
        this.subscribeAndTrack(
            `/topic/restaurant/${this.getUserId()}/notifications`,
            (msg) => this.handlePersonalNotification(msg)
        );
        
        // Team reservations
        this.subscribeAndTrack(
            `/topic/restaurant/${restaurantId}/reservations`,
            (msg) => this.handleTeamNotification(msg)
        );
    }
    
    subscribeAndTrack(destination, callback) {
        if (!this.currentSubscriptions) {
            this.currentSubscriptions = [];
        }
        
        this.currentSubscriptions.push({
            destination,
            callback
        });
        
        this.stompClient.subscribe(destination, callback);
    }
    
    disconnect() {
        clearInterval(this.tokenRefreshInterval);
        this.stompClient.disconnect();
    }
}

// Usage
const wsManager = new WebSocketManager(accessToken, refreshToken);
wsManager.connect();
```

---

### Step 2: Server-Side - Enhanced Validation

**File**: `WebSocketHandshakeInterceptor.java`

Aggiungi metodo per verificare scadenza:

```java
// Aggiungi a WebSocketHandshakeInterceptor
private boolean isTokenExpiringVeryQuickly(Claims claims) {
    Long expirationTime = claims.getExpiration().getTime();
    Long currentTime = System.currentTimeMillis();
    long timeUntilExpiration = expirationTime - currentTime;
    
    // If token expires in less than 1 minute, it's suspect
    return timeUntilExpiration < 60 * 1000;
}

// In beforeHandshake(), after validation:
if (isTokenExpiringVeryQuickly(claims)) {
    log.warn("⚠️ JWT token expiring very soon, consider refresh");
    response.setHeader("X-Token-Status", "EXPIRING_SOON");
}
```

---

### Step 3: Backend Refresh Endpoints (Already Implemented)

**Use existing endpoints**:

```bash
# For RUser (Restaurant Staff)
POST /api/restaurant/auth/refresh
Content-Type: application/json
Body: {"refreshToken": "eyJhbGc..."}

Response:
{
  "jwt": "eyJhbGc...",  // New access token
  "refreshToken": "eyJhbGc...",  // New refresh token
  "user": {...}
}

# For Customer
POST /api/customer/auth/refresh
Content-Type: application/json
Body: {"refreshToken": "eyJhbGc..."}

# For Admin
POST /api/admin/auth/refresh
Content-Type: application/json
Body: {"refreshToken": "eyJhbGc..."}
```

---

## Implementation Checklist

- [ ] **Client-Side**
  - [ ] Implement `WebSocketManager` with token refresh timer
  - [ ] Start refresh timer on WebSocket connect
  - [ ] Call `/api/*/auth/refresh` every 25 minutes
  - [ ] Handle token refresh response
  - [ ] Store new tokens in localStorage/sessionStorage
  - [ ] Implement reconnect with new token logic
  - [ ] Test: Connect → Wait 30 min → Token refresh → Still connected ✅

- [ ] **Server-Side (Optional Enhancements)**
  - [ ] Add `getTimeUntilExpiration()` method to `JwtUtil`
  - [ ] Add `isTokenExpiringVeryQuickly()` check in handshake
  - [ ] Add header `X-Token-Status: EXPIRING_SOON` if needed
  - [ ] Log token refresh events for audit

- [ ] **Testing**
  - [ ] Test with token expiring in 1 second (simulate short-lived token)
  - [ ] Test refresh token endpoint returns valid new token
  - [ ] Test WebSocket survives token refresh
  - [ ] Test multiple tabs/browsers with same user
  - [ ] Test RUser with multi-restaurant (restaurantId preserved)

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ CLIENT BROWSER                                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  localStorage:                                                   │
│  ├─ accessToken: eyJhbGc... (1 hour)                            │
│  └─ refreshToken: eyJhbGc... (7 days)                           │
│                                                                  │
│  WebSocketManager:                                              │
│  ├─ Connect to /ws?token={accessToken}                         │
│  ├─ Subscribe to /topic/restaurant/{id}/reservations           │
│  └─ Start timer: refresh every 25 minutes                       │
│                                                                  │
│  Timer Callback (every 25 min):                                 │
│  ├─ POST /api/restaurant/auth/refresh                          │
│  │  └─ Body: {refreshToken: ...}                               │
│  ├─ Response: {jwt: ..., refreshToken: ...}                    │
│  ├─ Update localStorage                                         │
│  └─ Optionally: Reconnect WebSocket with new token             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ HTTP POST
                            │
        ┌───────────────────┴────────────────────┐
        │                                         │
        ▼                                         ▼
   /api/restaurant/auth/refresh           /api/customer/auth/refresh
   (for RUser - multi-restaurant)         (for Customer)
        │                                         │
        │ Validate refreshToken                   │ Validate refreshToken
        │ Extract: email:restaurantId             │ Extract: email
        │ Find RUser by email+restaurantId        │ Find Customer by email
        │ Verify refreshToken signature           │ Verify refreshToken signature
        │ Generate new JWT + refreshToken         │ Generate new JWT + refreshToken
        │                                         │
        └───────────────┬───────────────────────┘
                        │
                        ▼
        Response: {jwt, refreshToken, user}
                        │
                        ▼ (Client stores)
        Update localStorage with new tokens
                        │
                        ▼
        Optional: Reconnect WebSocket
        └─ Disconnect old connection
        └─ New SockJS('/ws?token={newJWT}')
        └─ Re-subscribe to channels
```

---

## Summary

**Current State**: ✅ Token refresh è **già implementato** per:
- RUser (restaurant staff) - preserva restaurantId
- Customer
- Admin

**What's Missing**: WebSocket non usa il refresh token

**Best Solution**: 
1. ✅ Client periodicamente chiama `/api/*/auth/refresh`
2. ✅ Client riceve nuovo access token
3. ✅ Client opzionalmente disconnette e riconnette con nuovo token
4. ✅ WebSocket rimane protetto con JWT sempre fresco

**Effort**: ~3-4 ore implementazione client-side, 0 ore server-side (già fatto!)

**Benefit**: WebSocket sessioni possono durare giorni/settimane senza interruzione!
