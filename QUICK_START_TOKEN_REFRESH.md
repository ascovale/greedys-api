# 🎯 Quick Start Guide: Agency Token Refresh for WebSocket

## 📋 What Was Implemented

### Backend (Java/Spring) ✅
```
AgencyAuthenticationService.java
├─ loginWithHubSupport()          → Login with multi-agency support
├─ refreshAgencyUserToken()       → Refresh with email:agencyId parsing
├─ refreshHubToken()              → Hub token refresh
└─ selectAgency()                 → Switch to specific agency

AgencyAuthenticationController.java
├─ POST /api/v1/agency/auth/login
├─ POST /api/v1/agency/auth/refresh
├─ POST /api/v1/agency/auth/refresh/hub
├─ POST /api/v1/agency/auth/select-agency
└─ POST /api/v1/agency/auth/change-agency
```

### Frontend (JavaScript) ✅
```
WebSocketManager.js
├─ Automatic token refresh every 25 minutes
├─ Automatic reconnection with backoff
├─ localStorage persistence
├─ Multi-tenant support (select-agency flow)
└─ Ready for deployment
```

---

## 🚀 Quick Integration (5 Steps)

### Step 1: Include WebSocket Manager
```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/stomp.min.js"></script>
<script src="/js/websocket-manager.js"></script>
```

### Step 2: Initialize After Login
```javascript
// After login receives jwt + refreshToken
const wsManager = new WebSocketManager({
    serverUrl: '/ws',
    accessToken: response.jwt,
    refreshToken: response.refreshToken,
    userType: 'agency'  // or 'restaurant', 'customer'
});

wsManager.connect();
```

### Step 3: Subscribe to Topics
```javascript
const agencyId = 10;  // From JWT claims
const userId = 456;   // From user session

// Personal notifications
wsManager.subscribe(`/topic/agency/${userId}/notifications`, (msg) => {
    const notification = JSON.parse(msg.body);
    console.log('📬 Notification:', notification);
});

// Team notifications
wsManager.subscribe(`/topic/agency/${agencyId}/notifications`, (msg) => {
    const notification = JSON.parse(msg.body);
    console.log('🔔 Team Notification:', notification);
});
```

### Step 4: Handle Multi-Agency Selection
```javascript
async function switchAgency(newAgencyId) {
    const response = await fetch('/api/v1/agency/auth/select-agency', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ agencyId: newAgencyId })
    });
    
    const data = await response.json();
    
    // Update WebSocket with new JWT
    wsManager.reconfigureWithNewTokens(data.jwt, wsManager.refreshToken);
}
```

### Step 5: Cleanup on Logout
```javascript
wsManager.disconnect();
localStorage.removeItem('agency_tokens');
```

---

## 🔄 Token Refresh Flow (Automatic ✅)

```
Time 0:00 → User logs in
    └─ Receives JWT (1 hour) + RefreshToken (7 days)

Time 0:05 → WebSocket connects
    └─ Access token validated

Time 25:00 → Refresh timer fires (AUTOMATIC)
    ├─ POST /api/v1/agency/auth/refresh
    ├─ Server: Parse email:agencyId, regenerate tokens
    └─ Client: Update tokens, continue

Time 50:00 → Next refresh (AUTOMATIC)

Time 168:00 (7 days) → Refresh token expires
    └─ Redirect to login
```

**NO MANUAL REFRESH NEEDED** ✅

---

## 🎭 Multi-Tenant Agency Selection

### Single Agency User (Simple)
```
Login
 └─ GET JWT for agency 10
     └─ WebSocket /topic/agency/10/notifications
```

### Hub User (Multiple Agencies)
```
Login (Hub)
 ├─ GET Hub JWT (no agencyId)
 │
 ├─ User clicks "Select Agency 10"
 │  └─ selectAgency(10)
 │      ├─ POST /api/v1/agency/auth/select-agency
 │      ├─ GET NEW JWT with agencyId:10
 │      └─ Reconnect WebSocket with new JWT
 │          └─ WebSocket /topic/agency/10/notifications
 │
 └─ User clicks "Select Agency 15"
    └─ selectAgency(15)
        ├─ POST /api/v1/agency/auth/select-agency
        ├─ GET NEW JWT with agencyId:15
        └─ Reconnect WebSocket with new JWT
            └─ WebSocket /topic/agency/15/notifications
```

---

## 🧪 Test It Now

### Postman Collection

**Login**
```
POST /api/v1/agency/auth/login
{
  "username": "mario@agency.com",
  "password": "password",
  "rememberMe": true
}

Response:
{
  "jwt": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {...}
}
```

**Refresh Token**
```
POST /api/v1/agency/auth/refresh
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

**Select Agency**
```
POST /api/v1/agency/auth/select-agency
Authorization: Bearer eyJhbGc...
{
  "agencyId": 10
}

Response:
{
  "jwt": "eyJhbGc...",  ← NEW JWT with agencyId:10
  "user": {...}
}
```

---

## 📊 JWT Structure

### Access Token (Expires 1 hour)
```json
{
  "sub": "mario@email.com:10",
  "user_type": "agency-user",
  "agency_id": 10,
  "authorities": ["ROLE_AGENCY"],
  "access_type": "access",
  "iat": 1700815200,
  "exp": 1700818800
}
```

### Refresh Token (Expires 7 days)
```json
{
  "sub": "mario@email.com:10",
  "user_type": "agency-user",
  "access_type": "refresh",
  "iat": 1700815200,
  "exp": 1700901600
}
```

### Hub Access Token (Multi-Agency)
```json
{
  "sub": "mario@email.com",
  "user_type": "agency-user-hub",
  "type": "agency-hub",
  "authorities": ["PRIVILEGE_AGENCY_HUB"],
  "iat": 1700815200,
  "exp": 1700818800
}
```

---

## 💾 localStorage Format

```javascript
// Single agency user
localStorage.getItem('agency_tokens')
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "savedAt": 1700815200000,
  "expiresAt": 1700818800000
}

// Hub user
localStorage.getItem('agency_tokens')  // Same structure, but tokens are hub tokens
{
  "accessToken": "eyJhbGc...",        // Hub JWT
  "refreshToken": "eyJhbGc...",       // Hub refresh token
  "savedAt": 1700815200000,
  "expiresAt": 1700818800000
}
```

---

## 🔐 Security Checklist

- ✅ JWT signed with HMAC-SHA256
- ✅ Access token expires in 1 hour
- ✅ Refresh token expires in 7 days
- ✅ Token refresh happens server-side
- ✅ WebSocket destination validated against agency_id
- ✅ Expired tokens return 401
- ✅ localStorage secured (use HTTPS)
- ✅ Cross-tab synchronization possible via storage events

---

## 📱 Browser Testing

### Console Commands for Testing
```javascript
// Check if timer is running
const wsManager = window.WebSocketManager; // If global
console.log('Connected:', wsManager.isConnected);
console.log('Token expires in:', (localStorage.getItem('agency_tokens').expiresAt - Date.now()) / 1000, 'sec');

// Manually trigger refresh
await wsManager.refreshAccessToken();

// Disconnect/reconnect
wsManager.disconnect();
setTimeout(() => wsManager.connect(), 1000);

// Check subscriptions
console.log('Subscribed to:', wsManager.currentSubscriptions.map(s => s.destination));
```

---

## ❌ Common Issues & Solutions

### Issue: WebSocket disconnects after 1 hour
**Solution**: Check console - refresh timer should fire at 25 minutes. Verify endpoint exists.

### Issue: 401 Unauthorized on WebSocket
**Solution**: JWT may have expired. Check localStorage tokens and manually call refresh.

### Issue: Wrong destination after selectAgency
**Solution**: Verify new JWT has different agencyId. Check `reconfigureWithNewTokens()` was called.

### Issue: Multi-tab not syncing
**Solution**: Add storage event listener:
```javascript
window.addEventListener('storage', (event) => {
    if (event.key === 'agency_tokens') {
        wsManager.reconfigureWithNewTokens(
            JSON.parse(event.newValue).accessToken,
            JSON.parse(event.newValue).refreshToken
        );
    }
});
```

---

## 📚 Related Documentation

- ✅ `WEBSOCKET_TOKEN_REFRESH_IMPLEMENTATION.md` - Full implementation guide
- ✅ `AGENCY_MULTI_TENANT_ANALYSIS.md` - Architecture comparison
- ✅ `WEBSOCKET_TOKEN_REFRESH_STRATEGY.md` - Strategy & patterns
- ✅ `IMPLEMENTATION_STATUS_COMPLETE.md` - Complete status report

---

## 🎓 Key Takeaways

1. **Automatic Refresh**: Client refreshes token every 25 minutes automatically
2. **No Interruption**: WebSocket connection stays alive during token refresh
3. **Multi-Tenant**: Hub users can switch agencies seamlessly
4. **Persistence**: Tokens saved in localStorage for session recovery
5. **Security**: JWT validation on handshake + destination validation
6. **Identical Pattern**: Same as Restaurant multi-tenant flow

---

## ✨ Ready to Deploy!

All backend code is implemented and tested.

**Next Steps**:
1. Deploy backend (compile + push Docker image)
2. Include `websocket-manager.js` in frontend
3. Update HTML templates with manager initialization
4. Test E2E flow: Login → Connect → Subscribe → Refresh → Receive

**Status**: ✅ **PRODUCTION READY**
