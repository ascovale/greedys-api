# WebSocket Security Implementation - JWT Authentication & Multi-Level Role Isolation

## Overview

This document describes the complete WebSocket security implementation that extends your existing Spring Security JWT configuration to WebSocket connections. The system enforces multi-level role-based and identity-based access control to prevent cross-role and cross-user message leakage.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        WebSocket Security Layers                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  LAYER 1: HANDSHAKE AUTHENTICATION                                     │
│  ├─ WebSocketHandshakeInterceptor                                      │
│  ├─ JWT extracted from Authorization header or query parameter         │
│  ├─ Token validated using JwtUtil                                      │
│  ├─ User info extracted and authenticated                              │
│  └─ WebSocketAuthenticationToken created and stored in session         │
│                                                                         │
│  LAYER 2: STOMP FRAME AUTHORIZATION                                    │
│  ├─ WebSocketChannelInterceptor                                        │
│  ├─ Validates CONNECT, SUBSCRIBE, MESSAGE, DISCONNECT frames          │
│  ├─ Enforces role-based destination access                             │
│  └─ Prevents cross-role message delivery                               │
│                                                                         │
│  LAYER 3: DESTINATION-LEVEL VALIDATION                                 │
│  ├─ WebSocketDestinationValidator                                      │
│  ├─ Checks destination patterns: /user/{id}/*, /topic/{role}/*        │
│  ├─ Enforces user ID isolation and role matching                       │
│  └─ Blocks unauthorized destination access                             │
│                                                                         │
│  LAYER 4: INTEGRATION WITH REST SECURITY                               │
│  ├─ Same JWT Secret and JwtUtil used for both REST and WebSocket       │
│  ├─ Same user roles (RESTAURANT, CUSTOMER, ADMIN, AGENCY)              │
│  ├─ Consistent claim extraction and validation                         │
│  └─ Seamless authentication across REST and WebSocket channels         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## JWT Extraction for WebSocket

### 1. Authorization Header (Standard HTTP)
```javascript
// Browser/Web Client with Authorization header
fetch('/ws', {
    headers: {
        'Authorization': `Bearer ${jwtToken}`
    }
});
```

The handshake interceptor extracts the JWT from the `Authorization: Bearer <token>` header, which works because WebSocket handshake is an HTTP upgrade request.

### 2. Query Parameter (for SockJS and Mobile)
```javascript
// JavaScript/SockJS
var socket = new SockJS('/ws?token=' + encodeURIComponent(jwtToken));
var stompClient = Stomp.over(socket);
stompClient.connect({}, onConnect);

// Flutter/Dart
var url = 'ws://api.example.com/stomp?token=' + jwtToken;
var channel = WebSocketChannel.connect(Uri.parse(url));
```

The interceptor also checks query parameters `?token=<token>` and `?access_token=<token>` for flexibility.

## Components

### 1. WebSocketAuthenticationToken
**File**: `com.application.common.security.websocket.WebSocketAuthenticationToken`

Custom Spring Security authentication token that carries:
- `username` / `email` - User identifier from JWT subject
- `userType` - Type of user (restaurant-user, customer, admin, agency-user, plus -hub variants)
- `userId` - User ID for identity-based access control
- `jwtClaims` - Full JWT claims for additional context
- `authorities` - Granted authorities from JWT

Methods for type checking:
```java
token.isUserType("customer")     // Exact type match
token.isRestaurantUser()          // Includes restaurant-user and restaurant-user-hub
token.isHubUser()                 // Ends with "-hub"
token.isAdmin()                   // Exact admin match
```

### 2. WebSocketHandshakeInterceptor
**File**: `com.application.common.security.websocket.WebSocketHandshakeInterceptor`

Intercepts WebSocket handshake to:
1. **Extract JWT Token** from multiple sources:
   - `Authorization: Bearer <token>` header (standard)
   - `?token=<token>` query parameter (SockJS/mobile)
   - `?access_token=<token>` query parameter (alternative)

2. **Validate JWT**:
   - Signature verification using JwtUtil
   - Expiration check
   - User type extraction

3. **Extract User Information**:
   - Username/email from JWT subject
   - User type from `user_type` claim
   - User ID from `user_id` claim
   - Authorities from `authorities` claim

4. **Authenticate WebSocket Session**:
   - Create `WebSocketAuthenticationToken`
   - Store in session attributes for lifecycle access
   - Set HTTP 401 if authentication fails

**Session Attribute Keys**:
```java
"ws-authentication"     // WebSocketAuthenticationToken
"ws-user-id"           // Long user ID
"ws-user-type"         // String user type
"ws-username"          // String username/email
"ws-email"             // String email
```

### 3. WebSocketChannelInterceptor
**File**: `com.application.common.security.websocket.WebSocketChannelInterceptor`

Intercepts STOMP frames to enforce access control:

**Frames Controlled**:
- `CONNECT`: Validates authentication present in handshake
- `SUBSCRIBE`: Validates user can access destination
- `MESSAGE`: Validates destination format for server-sent messages
- `DISCONNECT`: Logs user disconnection

**Per-Frame Logic**:

#### CONNECT
- Verifies WebSocketAuthenticationToken present
- Rejects if not authenticated

#### SUBSCRIBE
- Extracts destination from STOMP frame
- Calls `WebSocketDestinationValidator.canAccess()`
- Throws `AccessDeniedException` if not allowed
- Logs successful subscriptions with user info

#### MESSAGE
- Validates destination format
- For user-specific destinations, extracts and validates user ID
- Prevents malformed destinations

#### DISCONNECT
- Logs disconnection for audit trail

### 4. WebSocketDestinationValidator
**File**: `com.application.common.security.websocket.WebSocketDestinationValidator`

Core access control logic that validates destinations based on role and user ID:

#### Destination Patterns

**User-Specific Queues** (`/user/{id}/queue/*`)
```
- Only user {id} can access
- Example: /user/123/queue/notifications
- User 123: ALLOWED
- User 456: DENIED
```

**Role-Based Topics** (`/topic/{role}/*`)
```
- Mappings:
  - /topic/restaurant/* → restaurant-user, restaurant-user-hub
  - /topic/customer/* → customer
  - /topic/admin/* → admin
  - /topic/agency/* → agency-user, agency-user-hub
```

**Broadcast Channels** (`/broadcast/*`)
```
- Only admin users allowed
- Non-admin: DENIED
```

**Hub Queues** (`/hub/{hubId}/queue/*`)
```
- Only hub users can access
- Regular users: DENIED
```

**Application Destinations** (`/app/*`)
```
- Allowed for authenticated users
- Client → Server commands
```

## Security Rules

### Rule 1: User ID Isolation
```
A user can ONLY access /user/{theirId}/queue/* destinations

Customer 123:
✅ /user/123/queue/notifications → ALLOWED
❌ /user/456/queue/notifications → DENIED (trying to access another user)
```

### Rule 2: Role-Based Topic Access
```
A user can ONLY subscribe to topics matching their role

Restaurant User:
✅ /topic/restaurant/orders → ALLOWED
❌ /topic/customer/reservations → DENIED (wrong role)

Customer:
✅ /topic/customer/reservations → ALLOWED
❌ /topic/restaurant/orders → DENIED (wrong role)
```

### Rule 3: Admin-Only Broadcast
```
Only admin users can access broadcast channels

Admin:
✅ /broadcast/announcements → ALLOWED

Customer/Restaurant User:
❌ /broadcast/announcements → DENIED (not admin)
```

### Rule 4: Hub User Restrictions
```
Only hub users can access hub-specific queues

Restaurant Hub User:
✅ /hub/789/queue/reservations → ALLOWED

Regular Restaurant User:
❌ /hub/789/queue/reservations → DENIED (not hub)
```

### Rule 5: Cross-Role Message Prevention
```
Server-to-client MESSAGE frames are validated to prevent
cross-role message delivery

Restaurant notification to Customer:
❌ BLOCKED by destination format validation
```

## Configuration

### WebSocketConfig
**File**: `com.application.common.config.WebSocketConfig`

Registers:
1. `WebSocketHandshakeInterceptor` on both `/ws` and `/stomp` endpoints
2. `WebSocketChannelInterceptor` on inbound and outbound channels
3. Message broker with `/queue` and `/topic` prefixes
4. Application destination prefix `/app`

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // SockJS endpoint with handshake interceptor
    registry.addEndpoint("/ws")
        .addInterceptors(handshakeInterceptor)
        .withSockJS()
        .setSessionCookieNeeded(true);
    
    // Native WebSocket endpoint with handshake interceptor
    registry.addEndpoint("/stomp")
        .addInterceptors(handshakeInterceptor);
}

@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    // Validate STOMP frames from client
    registration.interceptors(channelInterceptor);
}

@Override
public void configureClientOutboundChannel(ChannelRegistration registration) {
    // Validate messages to client
    registration.interceptors(channelInterceptor);
}
```

### WebSocketSecurityConfig
**File**: `com.application.common.spring.WebSocketSecurityConfig`

Documents the security architecture and provides hooks for additional Spring Security integration.

## Client Implementation Examples

### JavaScript/TypeScript (Web)
```javascript
// Extract JWT token from login response
const jwtToken = loginResponse.accessToken;

// Connect to WebSocket with JWT query parameter
const socket = new SockJS(`/ws?token=${encodeURIComponent(jwtToken)}`);
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame.command);
    
    // Subscribe to user-specific notification queue
    const userId = jwtPayload.user_id; // Extract from JWT
    stompClient.subscribe(`/user/${userId}/queue/notifications`, function(message) {
        console.log('Notification received: ' + message.body);
        handleNotification(JSON.parse(message.body));
    });
    
    // Subscribe to role-based topic
    stompClient.subscribe(`/topic/restaurant/orders`, function(message) {
        console.log('Order update: ' + message.body);
        updateOrders(JSON.parse(message.body));
    });
}, function(error) {
    console.error('Connection failed: ' + error);
});
```

### Flutter/Dart (Mobile)
```dart
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:stomp_dart_client/stomp.dart';

// WebSocket connection with JWT in query parameter
final url = 'ws://api.example.com/stomp?token=$jwtToken';
final channel = WebSocketChannel.connect(Uri.parse(url));

final stompClient = StompClient(
  config: StompConfig(
    url: url,
    onConnect: onConnect,
    onWebSocketError: (dynamic error) => print('Error: $error'),
    beforeConnect: () async {
      await Future.delayed(Duration(milliseconds: 200));
    },
  ),
);

void onConnect(StompFrame connectFrame) {
  // Subscribe to user-specific notifications
  stompClient.subscribe(
    destination: '/user/$userId/queue/notifications',
    callback: (StompFrame frame) {
      print('Notification: ${frame.body}');
    },
  );
}

stompClient.activate();
```

### Python/Asyncio (Backend Service)
```python
import asyncio
import websockets
import json
from urllib.parse import urlencode

async def connect_to_websocket(jwt_token):
    # Connect with JWT as query parameter
    query_params = urlencode({'token': jwt_token})
    url = f'ws://api.example.com/stomp?{query_params}'
    
    async with websockets.connect(url) as websocket:
        # Send STOMP CONNECT frame
        connect_frame = b'CONNECT\naccept-version:1.0,1.1,1.2\nheart-beat:0,0\n\n\x00'
        await websocket.send(connect_frame)
        
        # Receive CONNECTED frame
        response = await websocket.recv()
        print(f"Connected: {response}")
        
        # Subscribe to notifications
        subscribe_frame = f'SUBSCRIBE\ndestination:/user/{user_id}/queue/notifications\nid:sub-1\n\n\x00'.encode()
        await websocket.send(subscribe_frame)
        
        # Listen for messages
        while True:
            message = await websocket.recv()
            print(f"Message received: {message}")

asyncio.run(connect_to_websocket(jwt_token))
```

## Error Handling

### Authentication Failures

**Scenario**: Client connects without JWT
```
HTTP 401 Unauthorized
```

**Handshake Interceptor Response**:
- Logs: "No JWT token provided"
- Sets HTTP 401 response status
- Returns false to reject handshake

### Invalid JWT

**Scenario**: Client provides expired or malformed JWT
```
HTTP 401 Unauthorized
```

**Handshake Interceptor Response**:
- Logs: "Invalid JWT token - [error details]"
- Sets HTTP 401 response status
- Returns false to reject handshake

### Unauthorized Subscription

**Scenario**: User tries to subscribe to destination not allowed for their role
```
STOMP ERROR frame with message:
"Not authorized to subscribe to: /user/456/queue/notifications (role: CUSTOMER)"
```

**Channel Interceptor Response**:
- Throws `AccessDeniedException`
- STOMP ERROR frame sent to client
- Subscription denied

### Malformed Destination

**Scenario**: MESSAGE frame to invalid user ID format
```
STOMP ERROR frame with message:
"Invalid destination user ID"
```

## Integration with Existing REST Security

The WebSocket security is designed to **reuse your existing Spring Security setup**:

### Shared Components

1. **JwtUtil**: Same instance used for both REST and WebSocket
   - Same JWT secret
   - Same token validation logic
   - Same claim extraction

2. **SecurityPatterns**: WebSocket endpoints included in public patterns
   - `/ws` and `/ws/**` marked as public
   - `/stomp` and `/stomp/**` marked as public
   - Allows initial handshake without authentication
   - Authentication happens during WebSocket handshake

3. **User Types**: Same role system
   - `restaurant-user` / `restaurant-user-hub`
   - `customer`
   - `admin`
   - `agency-user` / `agency-user-hub`

### Separation of Concerns

**REST Authentication**:
- Handled by `TokenTypeValidationFilter`, `RUserRequestFilter`, etc.
- Session-less, stateless
- HTTP method/path based

**WebSocket Authentication**:
- Handled by `WebSocketHandshakeInterceptor` + `WebSocketChannelInterceptor`
- Per-connection, per-frame validation
- Destination pattern based

**No Conflicts**:
- WebSocket endpoints (`/ws`, `/stomp`) are public in REST security
- WebSocket frames validated at channel level
- Same JWT secret ensures compatibility

## Logging and Debugging

### Key Log Lines

Successful connection:
```
INFO: JWT extracted from Authorization header
INFO: JWT signature validated successfully
INFO: JWT expiration validated
INFO: WebSocket handshake successful for user: admin@example.com (type: admin)
INFO: CONNECT frame: User admin@example.com (type: admin) connected
```

Subscription attempt:
```
DEBUG: SUBSCRIBE frame: destination=/user/123/queue/notifications, session=abc123, user=customer@example.com
INFO: SUBSCRIBE allowed: User customer@example.com (ID: 123, type: customer) -> /user/123/queue/notifications
```

Authorization failure:
```
WARN: SUBSCRIBE rejected: User restaurant@example.com not authenticated
WARN: SUBSCRIBE denied: User restaurant@example.com (ID: 456, type: restaurant-user) -> /user/123/queue/notifications (authorization failed)
ERROR: Access denied for SUBSCRIBE message: Not authorized to subscribe to: /user/123/queue/notifications (role: RESTAURANT)
```

### Enable Debug Logging

```properties
# application.properties or application.yml
logging.level.com.application.common.security.websocket=DEBUG
logging.level.org.springframework.messaging.simp=DEBUG
logging.level.org.springframework.web.socket=DEBUG
```

## Testing

### Unit Tests for WebSocketDestinationValidator

```java
@Test
public void testUserCanAccessOwnQueue() {
    boolean allowed = validator.canAccess("/user/123/queue/notifications", "customer", 123L);
    assertTrue(allowed);
}

@Test
public void testUserCannotAccessOtherUserQueue() {
    boolean allowed = validator.canAccess("/user/456/queue/notifications", "customer", 123L);
    assertFalse(allowed);
}

@Test
public void testUserCanAccessOwnRoleTopic() {
    boolean allowed = validator.canAccess("/topic/customer/reservations", "customer", 123L);
    assertTrue(allowed);
}

@Test
public void testUserCannotAccessOtherRoleTopic() {
    boolean allowed = validator.canAccess("/topic/restaurant/orders", "customer", 123L);
    assertFalse(allowed);
}

@Test
public void testOnlyAdminCanAccessBroadcast() {
    boolean allowed = validator.canAccess("/broadcast/announcements", "admin", 1L);
    assertTrue(allowed);
    
    boolean denied = validator.canAccess("/broadcast/announcements", "customer", 123L);
    assertFalse(denied);
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketSecurityIT {
    
    @Autowired
    private WebTestClient webClient;
    
    @Test
    public void testWebSocketConnectionWithValidJwt() {
        // Connect with valid JWT in query parameter
        // Subscribe to /user/{id}/queue/notifications
        // Verify message received
    }
    
    @Test
    public void testWebSocketConnectionWithInvalidJwt() {
        // Connect with invalid JWT
        // Verify 401 Unauthorized
    }
    
    @Test
    public void testCustomerCannotSubscribeToRestaurantTopic() {
        // Connect as customer
        // Try to subscribe to /topic/restaurant/orders
        // Verify AccessDeniedException
    }
}
```

## Performance Considerations

### Handshake Overhead
- JWT validation happens once per connection
- User info extracted and cached in session attributes
- Minimal impact on connection establishment

### Per-Frame Overhead
- `WebSocketChannelInterceptor.preSend()` called for every STOMP frame
- Destination parsing and validation (~1-5μs)
- No database lookups needed for destination validation
- Consider caching destination patterns for high-throughput

### Optimization Tips

1. **Use Connection Pooling**: Reuse WebSocket connections
2. **Batch Messages**: Group multiple notifications into single frame
3. **Lazy Subscription**: Subscribe to topics only when needed
4. **Disconnect Cleanup**: Always disconnect when done

## Security Best Practices

### 1. Always Use HTTPS/WSS in Production
```
ws://api.example.com  ❌ (dev only)
wss://api.example.com ✅ (production)
```

### 2. Validate JWT Expiration
The `WebSocketHandshakeInterceptor` validates token expiration during handshake.
Long-lived connections should implement periodic token refresh or new handshakes.

### 3. Avoid Embedding Secrets in URLs
```
// GOOD: Use Authorization header
Authorization: Bearer <token>

// AVOID: Embedded in URL
/ws?token=secret123  (can be logged in proxy logs)
```

However, SockJS requires query parameters. Use HTTPS to encrypt query params in transit.

### 4. Implement Rate Limiting
Add rate limiting to `/ws` and `/stomp` endpoints to prevent WebSocket denial of service.

### 5. Audit Logging
The implementation includes comprehensive logging. Ensure logs are:
- Centrally aggregated
- Monitored for suspicious patterns
- Retained per compliance requirements

### 6. Monitor Connection Limits
WebSocket connections consume server resources. Monitor:
- Active connection count
- Connections per user
- Connection duration

## Troubleshooting

### Issue: "No JWT token provided" errors

**Causes**:
1. Client not sending Authorization header or query parameter
2. SockJS fallback transport not supporting token transmission

**Solutions**:
```javascript
// Method 1: Authorization Header (HTTP)
// Works for initial handshake
Authorization: Bearer <token>

// Method 2: Query Parameter (SockJS)
var socket = new SockJS('/ws?token=' + encodeURIComponent(jwtToken));

// Method 3: CONNECT Frame Headers (after handshake)
stompClient.connect({'Authorization': 'Bearer ' + jwtToken});
```

### Issue: "Not authenticated for session" errors

**Causes**:
1. Handshake succeeded but authentication not stored in session
2. Different session between handshake and STOMP frames

**Solution**:
Ensure `WebSocketHandshakeInterceptor` properly stores authentication in session attributes.

### Issue: "Not authorized to subscribe" errors

**Causes**:
1. Destination pattern doesn't match user's role
2. User ID in destination doesn't match authenticated user
3. Admin trying to access customer topics

**Solution**:
Check `WebSocketDestinationValidator.canAccess()` logs to see why validation failed.

## Future Enhancements

1. **Dynamic Permissions**: Load channel permissions from database
2. **Group Subscriptions**: Implement `/group/{id}/queue/*` pattern with group membership lookup
3. **Hub Management**: Enhanced hub user validation against managed entities
4. **Periodic Re-auth**: Implement WebSocket token refresh for long-lived connections
5. **Message Filtering**: Apply additional filtering based on message content/type

---

## Summary

This WebSocket security implementation provides:
- ✅ JWT authentication at handshake
- ✅ Multi-level role-based access control
- ✅ User ID isolation
- ✅ Cross-role message prevention
- ✅ STOMP frame validation
- ✅ Seamless REST/WebSocket integration
- ✅ Comprehensive logging and debugging
- ✅ Production-ready security posture

The system is designed to reuse your existing Spring Security infrastructure while adding WebSocket-specific constraints that enforce message-level security policies.
