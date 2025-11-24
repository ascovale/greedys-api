# WebSocket Token Refresh Integration - Complete Implementation Guide

## Architecture Overview

### Token Lifecycle for WebSocket Sessions

```
┌─────────────────────────────────────────────────────────────────┐
│ CLIENT BROWSER                                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. LOGIN (POST /api/v1/{type}/auth/login)                     │
│     ├─ Stores: accessToken (1 hour)                            │
│     └─ Stores: refreshToken (7 days)                           │
│                                                                  │
│  2. WEBSOCKET CONNECT with accessToken                          │
│     └─ WebSocketHandshakeInterceptor validates JWT              │
│                                                                  │
│  3. SUBSCRIBE to topics                                         │
│     └─ WebSocketChannelInterceptor validates destination        │
│                                                                  │
│  4. TOKEN EXPIRES in 1 hour                                     │
│     └─ Timer triggers: refreshAccessToken()                     │
│                                                                  │
│  5. REFRESH TOKEN (POST /api/v1/{type}/auth/refresh)           │
│     ├─ Sends: refreshToken                                      │
│     └─ Receives: newAccessToken + newRefreshToken              │
│                                                                  │
│  6. RECONNECT WEBSOCKET (Optional but recommended)              │
│     └─ New SockJS with fresh accessToken                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 1: Client-Side Implementation

### WebSocket Manager Class

**File**: `src/main/resources/static/js/websocket-manager.js` (NEW)

```javascript
/**
 * WebSocket Manager con token refresh automatico
 * Supporta Restaurant, Agency e Customer WebSocket connections
 */
class WebSocketManager {
    constructor(options = {}) {
        this.serverUrl = options.serverUrl || '/ws';
        this.accessToken = options.accessToken;
        this.refreshToken = options.refreshToken;
        this.userType = options.userType; // 'restaurant', 'agency', 'customer'
        this.stompClient = null;
        this.tokenRefreshInterval = null;
        this.currentSubscriptions = [];
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000; // 3 seconds
    }

    /**
     * Connetti a WebSocket e avvia il timer di refresh token
     */
    connect() {
        const socket = new SockJS(this.serverUrl + '?token=' + this.accessToken);
        this.stompClient = Stomp.over(socket);

        // Disabilita logging verbose di Stomp
        this.stompClient.debug = null;

        this.stompClient.connect({}, 
            (frame) => this.onConnectSuccess(frame),
            (error) => this.onConnectError(error)
        );
    }

    /**
     * Callback quando connessione WebSocket ha successo
     */
    onConnectSuccess(frame) {
        console.log('✅ [WebSocket] Connected successfully');
        this.isConnected = true;
        this.reconnectAttempts = 0;

        // Resubscribe a tutti i canali precedenti
        this.resubscribeToChannels();

        // Avvia il timer di refresh token
        this.startTokenRefreshTimer();
    }

    /**
     * Callback quando connessione WebSocket fallisce
     */
    onConnectError(error) {
        console.error('❌ [WebSocket] Connection error:', error);
        this.isConnected = false;

        // Tenta di riconnettersi con backoff exponenziale
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts);
            console.log(`🔄 [WebSocket] Retrying connection in ${delay}ms (attempt ${this.reconnectAttempts + 1}/${this.maxReconnectAttempts})`);
            
            this.reconnectAttempts++;
            setTimeout(() => this.connect(), delay);
        } else {
            console.error('❌ [WebSocket] Max reconnect attempts reached');
            this.handleConnectionFailed();
        }
    }

    /**
     * Gestore quando la connessione WebSocket fallisce completamente
     */
    handleConnectionFailed() {
        // Mostra messaggio d'errore all'utente
        console.error('❌ WebSocket connection failed. Please login again.');
        
        // Opzione 1: Mostra modal/alert (dipende dal framework UI)
        if (window.showErrorModal) {
            window.showErrorModal('WebSocket Connection Lost', 'Connection could not be established. Please refresh the page.');
        }
        
        // Opzione 2: Redirect a login se necessario
        // window.location.href = '/login';
    }

    /**
     * Avvia il timer di refresh token
     * Refresha ogni 25 minuti (token scade in 60 minuti)
     */
    startTokenRefreshTimer() {
        // Cancella il timer precedente se esiste
        if (this.tokenRefreshInterval) {
            clearInterval(this.tokenRefreshInterval);
        }

        // Configura intervallo: refresh ogni 25 minuti
        const refreshIntervalMs = 25 * 60 * 1000; // 25 minuti

        this.tokenRefreshInterval = setInterval(() => {
            console.log('🔄 [Token] Refreshing access token...');
            this.refreshAccessToken();
        }, refreshIntervalMs);

        // Log informativo
        console.log(`⏱️ [Token] Refresh timer started (every 25 minutes)`);
    }

    /**
     * Refresha il token di accesso
     * Chiama l'endpoint di refresh appropriato basato su userType
     */
    async refreshAccessToken() {
        try {
            // Seleziona endpoint in base al tipo di utente
            const endpoints = {
                'restaurant': '/api/v1/restaurant/auth/refresh',
                'agency': '/api/v1/agency/auth/refresh',
                'customer': '/api/v1/customer/auth/refresh'
            };

            const endpoint = endpoints[this.userType] || '/api/v1/restaurant/auth/refresh';

            console.log(`🔄 [Token] Calling ${endpoint}...`);

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + this.accessToken
                },
                body: JSON.stringify({
                    refreshToken: this.refreshToken
                })
            });

            if (!response.ok) {
                if (response.status === 401) {
                    console.error('❌ [Token] Refresh token expired or invalid');
                    this.handleTokenExpired();
                } else {
                    console.error('❌ [Token] Refresh failed with status:', response.status);
                    throw new Error(`Token refresh failed: ${response.status}`);
                }
                return;
            }

            const data = await response.json();
            
            // Aggiorna i token nel client
            this.accessToken = data.jwt;
            this.refreshToken = data.refreshToken;

            // Salva i nuovi token in localStorage/sessionStorage
            this.saveTokensToStorage(data.jwt, data.refreshToken);

            console.log('✅ [Token] Token refreshed successfully');

            // Opzione A: Disconnect e reconnect (interruzione minimale)
            // Opzione B: Solo update (nessuna interruzione)
            // Scegliamo Opzione B per minimizzare interruzioni
            
        } catch (error) {
            console.error('❌ [Token] Refresh error:', error);
            this.handleTokenRefreshError(error);
        }
    }

    /**
     * Salva i token in localStorage per persistenza
     */
    saveTokensToStorage(accessToken, refreshToken) {
        try {
            const now = new Date().getTime();
            const tokenData = {
                accessToken,
                refreshToken,
                savedAt: now,
                expiresAt: now + (60 * 60 * 1000) // 1 hour
            };
            
            localStorage.setItem(`${this.userType}_tokens`, JSON.stringify(tokenData));
            console.log('💾 [Token] Tokens saved to localStorage');
        } catch (error) {
            console.warn('⚠️ [Storage] Could not save tokens:', error);
        }
    }

    /**
     * Carica i token da localStorage
     */
    loadTokensFromStorage() {
        try {
            const stored = localStorage.getItem(`${this.userType}_tokens`);
            if (stored) {
                const data = JSON.parse(stored);
                // Verifica se il token non è scaduto
                if (data.expiresAt > new Date().getTime()) {
                    this.accessToken = data.accessToken;
                    this.refreshToken = data.refreshToken;
                    console.log('✅ [Storage] Tokens loaded from localStorage');
                    return true;
                }
            }
        } catch (error) {
            console.warn('⚠️ [Storage] Could not load tokens:', error);
        }
        return false;
    }

    /**
     * Gestore quando il token refresh fallisce
     */
    handleTokenRefreshError(error) {
        console.error('❌ Token refresh error:', error);
        
        // Se il refresh token è scaduto, richiedi login
        if (error.message.includes('Invalid refresh token') || error.message.includes('401')) {
            this.handleTokenExpired();
        } else {
            // Per altri errori, aspetta il prossimo tentativo
            console.warn('⚠️ Token refresh will be retried in 25 minutes');
        }
    }

    /**
     * Gestore quando il token è completamente scaduto
     */
    handleTokenExpired() {
        console.error('❌ Token expired - user needs to login again');
        
        // Cancella il timer
        clearInterval(this.tokenRefreshInterval);
        
        // Disconnetti WebSocket
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect();
        }
        
        // Cancella token da storage
        localStorage.removeItem(`${this.userType}_tokens`);
        
        // Redirect a login
        window.location.href = '/login';
    }

    /**
     * Subscribe a un canale WebSocket
     */
    subscribe(destination, callback) {
        if (!this.stompClient || !this.stompClient.connected) {
            console.error('❌ WebSocket not connected. Cannot subscribe to:', destination);
            return null;
        }

        console.log('📌 Subscribing to:', destination);
        
        const subscription = this.stompClient.subscribe(destination, callback);
        
        // Salva la subscription per risubscribe dopo reconnect
        this.currentSubscriptions.push({
            destination,
            callback
        });

        return subscription;
    }

    /**
     * Unsubscribe da un canale WebSocket
     */
    unsubscribe(destination) {
        const index = this.currentSubscriptions.findIndex(s => s.destination === destination);
        if (index > -1) {
            this.currentSubscriptions.splice(index, 1);
            console.log('📌 Unsubscribed from:', destination);
        }
    }

    /**
     * Resubscribe a tutti i canali precedenti
     */
    resubscribeToChannels() {
        console.log(`🔄 Resubscribing to ${this.currentSubscriptions.length} channels...`);
        
        this.currentSubscriptions.forEach(sub => {
            try {
                this.stompClient.subscribe(sub.destination, sub.callback);
            } catch (error) {
                console.error(`❌ Error resubscribing to ${sub.destination}:`, error);
            }
        });
    }

    /**
     * Send a message to a destination
     */
    send(destination, body) {
        if (!this.stompClient || !this.stompClient.connected) {
            console.error('❌ WebSocket not connected. Cannot send to:', destination);
            return;
        }

        this.stompClient.send(destination, {}, JSON.stringify(body));
    }

    /**
     * Disconnect WebSocket gracefully
     */
    disconnect() {
        console.log('🔌 Disconnecting WebSocket...');
        
        // Cancella timer di refresh
        if (this.tokenRefreshInterval) {
            clearInterval(this.tokenRefreshInterval);
        }

        // Disconnect Stomp
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect(() => {
                console.log('✅ WebSocket disconnected');
            });
        }

        this.isConnected = false;
    }

    /**
     * Riconfigura il manager con nuovi token (dopo cambio agenzia/ristorante)
     */
    reconfigureWithNewTokens(newAccessToken, newRefreshToken) {
        console.log('🔄 Reconfiguring WebSocket with new tokens...');
        
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
        
        this.saveTokensToStorage(newAccessToken, newRefreshToken);
        
        // Riconnetti con nuovi token
        this.disconnect();
        setTimeout(() => this.connect(), 1000);
    }
}

// Export per uso globale
window.WebSocketManager = WebSocketManager;
```

---

## Part 2: Configuration & Initialization

### HTML Template per Restaurant WebSocket

**File**: `src/main/resources/templates/restaurant-dashboard.html`

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/stomp.min.js"></script>
    <script src="/js/websocket-manager.js"></script>
</head>
<body>

<div id="notifications-container"></div>

<script>
    // Recupera i token dal localStorage (salvati al login)
    const tokenData = JSON.parse(localStorage.getItem('restaurant_tokens') || '{}');
    
    // Inizializza il WebSocket Manager
    const wsManager = new WebSocketManager({
        serverUrl: '/ws',
        accessToken: tokenData.accessToken || '[TOKEN_FROM_LOGIN]',
        refreshToken: tokenData.refreshToken || '[REFRESH_TOKEN]',
        userType: 'restaurant' // 'restaurant', 'agency', o 'customer'
    });

    // Connetti a WebSocket
    wsManager.connect();

    // Subscribe a notifiche per questo ristorante
    const restaurantId = 5; // Dalla sessione utente
    const userId = 123;      // Dalla sessione utente

    // Canale di notifiche personali
    wsManager.subscribe(`/topic/restaurant/${userId}/notifications`, (message) => {
        const notification = JSON.parse(message.body);
        console.log('📬 Personal Notification:', notification);
        displayNotification(notification);
    });

    // Canale di notifiche del team (prenotazioni)
    wsManager.subscribe(`/topic/restaurant/${restaurantId}/reservations`, (message) => {
        const reservation = JSON.parse(message.body);
        console.log('🔔 Team Reservation:', reservation);
        displayReservation(reservation);
    });

    // Funzione helper per visualizzare notifiche
    function displayNotification(notification) {
        const container = document.getElementById('notifications-container');
        const notifDiv = document.createElement('div');
        notifDiv.className = 'notification';
        notifDiv.innerHTML = `
            <p>${notification.message}</p>
            <small>${new Date(notification.timestamp).toLocaleString()}</small>
        `;
        container.appendChild(notifDiv);
    }

    function displayReservation(reservation) {
        const container = document.getElementById('notifications-container');
        const reservDiv = document.createElement('div');
        reservDiv.className = 'reservation';
        reservDiv.innerHTML = `
            <p>New Reservation from ${reservation.customerName}</p>
            <p>Date: ${reservation.date}</p>
            <p>Party Size: ${reservation.partySize}</p>
        `;
        container.appendChild(reservDiv);
    }

    // Cleanup on page unload
    window.addEventListener('beforeunload', () => {
        wsManager.disconnect();
    });
</script>

</body>
</html>
```

### HTML Template per Agency WebSocket

**File**: `src/main/resources/templates/agency-dashboard.html`

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/stomp.min.js"></script>
    <script src="/js/websocket-manager.js"></script>
</head>
<body>

<div id="notifications-container"></div>

<script>
    // Recupera i token da localStorage
    const tokenData = JSON.parse(localStorage.getItem('agency_tokens') || '{}');
    
    // Inizializza il WebSocket Manager per Agency
    const wsManager = new WebSocketManager({
        serverUrl: '/ws',
        accessToken: tokenData.accessToken,
        refreshToken: tokenData.refreshToken,
        userType: 'agency'
    });

    wsManager.connect();

    // Subscribe a notifiche per questa agenzia
    const agencyId = 10;  // Dalla sessione utente
    const userId = 456;   // Dalla sessione utente

    // Canale di notifiche personali
    wsManager.subscribe(`/topic/agency/${userId}/notifications`, (message) => {
        const notification = JSON.parse(message.body);
        console.log('📬 Agency Personal Notification:', notification);
        displayNotification(notification);
    });

    // Canale di notifiche dell'agenzia
    wsManager.subscribe(`/topic/agency/${agencyId}/notifications`, (message) => {
        const notification = JSON.parse(message.body);
        console.log('🔔 Agency Team Notification:', notification);
        displayAgencyNotification(notification);
    });

    // Cleanup on page unload
    window.addEventListener('beforeunload', () => {
        wsManager.disconnect();
    });
</script>

</body>
</html>
```

---

## Part 3: Server-Side Validation Enhancements

### Enhanced WebSocketChannelInterceptor

**File**: Already implemented in `WebSocketChannelInterceptor.java`

Key features:
- ✅ Validates JWT on SUBSCRIBE frame
- ✅ Checks token expiration before rejecting
- ✅ Logs token expiration warnings
- ✅ Returns error headers if needed

Current implementation is sufficient.

---

## Part 4: Token Refresh Endpoints

### Restaurant Authentication Endpoints

```bash
# Login
POST /api/v1/restaurant/auth/login
Content-Type: application/json
{
  "username": "mario@restaurant.com",
  "password": "password123",
  "rememberMe": true
}

Response:
{
  "jwt": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {...}
}

# Refresh Token
POST /api/v1/restaurant/auth/refresh
Content-Type: application/json
{
  "refreshToken": "eyJhbGc..."
}

Response:
{
  "jwt": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {...}
}

# Select Restaurant (for hub users)
POST /api/v1/restaurant/auth/select-restaurant
Content-Type: application/json
{
  "restaurantId": 5
}

Response:
{
  "jwt": "eyJhbGc...",  // New JWT with restaurantId:5
  "user": {...}
}
```

### Agency Authentication Endpoints (NEWLY IMPLEMENTED)

```bash
# Login
POST /api/v1/agency/auth/login
Content-Type: application/json
{
  "username": "mario@agency.com",
  "password": "password123",
  "rememberMe": true
}

Response:
{
  "jwt": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {...}
}

# Refresh Token
POST /api/v1/agency/auth/refresh
Content-Type: application/json
{
  "refreshToken": "eyJhbGc..."
}

Response:
{
  "jwt": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {...}
}

# Select Agency (for hub users)
POST /api/v1/agency/auth/select-agency
Content-Type: application/json
{
  "agencyId": 10
}

Response:
{
  "jwt": "eyJhbGc...",  // New JWT with agencyId:10
  "user": {...}
}

# Refresh Hub Token (for multi-agency hub)
POST /api/v1/agency/auth/refresh/hub
Content-Type: application/json
{
  "refreshToken": "eyJhbGc..."
}

Response:
{
  "jwt": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {...}
}
```

---

## Part 5: Login Flow Integration

### Login Controller Example

**File**: Update your login endpoint to return tokens

```javascript
// After login succeeds on backend, frontend receives:
{
  "jwt": "eyJhbGc...",           // Access token (1 hour)
  "refreshToken": "eyJhbGc...",  // Refresh token (7 days)
  "user": {
    "id": 123,
    "username": "mario@restaurant.com",
    "restaurantId": 5,
    "status": "ENABLED"
  }
}

// Frontend saves both tokens:
localStorage.setItem('restaurant_tokens', JSON.stringify({
  accessToken: response.jwt,
  refreshToken: response.refreshToken,
  savedAt: new Date().getTime(),
  expiresAt: new Date().getTime() + (60 * 60 * 1000)
}));

// Then initializes WebSocket with access token
const wsManager = new WebSocketManager({
  serverUrl: '/ws',
  accessToken: response.jwt,
  refreshToken: response.refreshToken,
  userType: 'restaurant'
});
wsManager.connect();
```

---

## Part 6: Multi-Tenant Restaurant Selection

### Select Restaurant Flow (Hub User)

```javascript
// User selects a different restaurant
async function selectRestaurant(newRestaurantId) {
  try {
    const response = await fetch('/api/v1/restaurant/auth/select-restaurant', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ restaurantId: newRestaurantId })
    });
    
    const data = await response.json();
    
    // Update tokens
    const newTokens = {
      accessToken: data.jwt,
      refreshToken: wsManager.refreshToken,  // Keep same refresh token
      savedAt: new Date().getTime(),
      expiresAt: new Date().getTime() + (60 * 60 * 1000)
    };
    
    localStorage.setItem('restaurant_tokens', JSON.stringify(newTokens));
    
    // Reconfigure WebSocket with new tokens
    wsManager.reconfigureWithNewTokens(data.jwt, wsManager.refreshToken);
    
    // Update UI with new restaurant info
    updateRestaurantUI(data.user);
    
  } catch (error) {
    console.error('Failed to select restaurant:', error);
  }
}
```

### Select Agency Flow (Hub User)

```javascript
// User selects a different agency
async function selectAgency(newAgencyId) {
  try {
    const response = await fetch('/api/v1/agency/auth/select-agency', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ agencyId: newAgencyId })
    });
    
    const data = await response.json();
    
    // Update tokens
    const newTokens = {
      accessToken: data.jwt,
      refreshToken: wsManager.refreshToken,  // Keep same refresh token
      savedAt: new Date().getTime(),
      expiresAt: new Date().getTime() + (60 * 60 * 1000)
    };
    
    localStorage.setItem('agency_tokens', JSON.stringify(newTokens));
    
    // Reconfigure WebSocket with new tokens
    wsManager.reconfigureWithNewTokens(data.jwt, wsManager.refreshToken);
    
    // Update UI with new agency info
    updateAgencyUI(data.user);
    
  } catch (error) {
    console.error('Failed to select agency:', error);
  }
}
```

---

## Part 7: Testing Checklist

### Unit Tests

- [ ] Test `AgencyAuthenticationService.loginWithHubSupport()` with single agency
- [ ] Test `AgencyAuthenticationService.loginWithHubSupport()` with multiple agencies
- [ ] Test `AgencyAuthenticationService.refreshAgencyUserToken()` with valid refresh token
- [ ] Test `AgencyAuthenticationService.refreshAgencyUserToken()` with expired refresh token
- [ ] Test `AgencyAuthenticationService.selectAgency()` with valid agencyId
- [ ] Test `AgencyAuthenticationService.selectAgency()` with invalid agencyId

### Integration Tests

- [ ] JWT format contains `email:agencyId` for AgencyUser
- [ ] JWT format contains `email` for AgencyUserHub
- [ ] `findByEmailAndAgencyId()` DAO method returns correct user
- [ ] WebSocket accepts JWT with agency_id claim

### End-to-End Tests

- [ ] Login → Receive tokens → Store in localStorage
- [ ] Connect WebSocket with access token
- [ ] Subscribe to /topic/agency/{agencyId}/notifications
- [ ] Wait 30+ minutes → Token refresh fires
- [ ] Verify new tokens received and stored
- [ ] WebSocket still connected and receiving messages
- [ ] Hub user: Select different agency → New JWT generated
- [ ] Verify new JWT has different agencyId

### Browser Testing

- [ ] Multi-tab: Login in tab 1 → Open WebSocket in tab 2
- [ ] Both tabs should receive token refresh notifications
- [ ] Close tab → Reconnect tab → Should still work
- [ ] Long session (8+ hours) → Multiple token refreshes
- [ ] Token expires → Redirect to login

---

## Part 8: Troubleshooting

### Issue: WebSocket disconnects after 1 hour

**Cause**: Access token expiration without refresh

**Solution**: Ensure token refresh timer fires every 25 minutes

```javascript
// Check in browser console:
// 1. Is timer running?
setInterval(() => {
  console.log('Token refresh timer is running');
}, 1000);

// 2. Check token expiration
const tokenData = JSON.parse(localStorage.getItem('restaurant_tokens'));
console.log('Token expires in:', (tokenData.expiresAt - Date.now()) / 1000, 'seconds');
```

### Issue: Multi-restaurant users see wrong organization ID

**Cause**: JWT not updated after `selectRestaurant()` / `selectAgency()`

**Solution**: Verify `reconfigureWithNewTokens()` is called

```javascript
// Check:
const newJwt = response.jwt;
const claims = jwtDecode(newJwt);
console.log('New JWT contains restaurantId:', claims.restaurant_id);
```

### Issue: Refresh token endpoint returns 401

**Cause**: Refresh token expired or invalid format

**Solution**: 
1. Verify refresh token is 7 days old max
2. Verify token format: `email:restaurantId` or `email:agencyId`

```bash
# Test refresh endpoint:
curl -X POST http://localhost:8080/api/v1/restaurant/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "eyJ..."}'
```

---

## Summary: What's Implemented

✅ **Agency Authentication Service**
- `loginWithHubSupport()` - Login with hub support for multiple agencies
- `refreshAgencyUserToken()` - Refresh token with email:agencyId parsing
- `refreshHubToken()` - Hub token refresh
- `selectAgency()` - Switch to specific agency with new JWT

✅ **Agency Controller**
- POST `/api/v1/agency/auth/login` - Login endpoint
- POST `/api/v1/agency/auth/refresh` - Refresh token endpoint
- POST `/api/v1/agency/auth/refresh/hub` - Hub refresh endpoint
- POST `/api/v1/agency/auth/select-agency` - Select agency endpoint
- POST `/api/v1/agency/auth/change-agency` - Alias for select-agency

✅ **JWT Configuration**
- Auto-extracted `agency_id` in JWT via reflection
- Token format: `email:agencyId` (same as Restaurant pattern)
- JwtUtil already supports AgencyUser

✅ **Client-Side WebSocket Manager**
- Automatic token refresh every 25 minutes
- Automatic reconnect with exponential backoff
- Persist tokens in localStorage
- Resubscribe to channels after reconnect
- Support for multi-tenant selection flow

✅ **Restaurant & Agency Support**
- Both use identical pattern for token refresh
- Both support hub and single-tenant flows
- Both use email:organizationId token format

**Next Step**: Deploy and test!
