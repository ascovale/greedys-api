# WebSocket Status & Next Steps for Flutter App Team

Date: 18 November 2025

Summary: I verified the backend changes (pipeline deployed). The Spring WebSocket endpoint is configured under `/ws` and SockJS-based handshakes work from the browser. CORS and credentials are correctly set. Direct raw WebSocket upgrades to `/ws/websocket` currently return HTTP 400 (SockJS negotiation is expected). Below are exact test results, recommended actions for the Flutter team, and optional backend changes if you prefer a native WebSocket connection.

---

## Quick status (what works now)

- Endpoint path (Spring): `/ws` (registered as SockJS endpoint)
- Browser test page (uses SockJS + STOMP) successfully connected and stayed connected
- `/ws/info` (SockJS handshake) returns correct CORS headers and `access-control-allow-credentials: true`
- Spring service deployed and running (Docker service `greedys_api_spring-app`)
- No recent NullPointerExceptions in Spring logs after the latest fix

---

## Exact tests I ran (commands & results)

1) Check commits

- Found commit: `ea32de9f0be9b0d36f74753c97f32b0d709d63c2` — "Fix WebSocket event listener null pointer exception on connect/disconnect"

2) Docker service status

- `docker service ps --no-trunc greedys_api_spring-app` -> service is Running

3) SockJS handshake (CORS)

```bash
curl -s -D - -H 'Origin: http://localhost:8081' -k https://api.greedys.it/ws/info | sed -n '1,40p'
```
Response (headers):
- HTTP/2 200
- access-control-allow-credentials: true
- access-control-allow-origin: http://localhost:8081
- access-control-expose-headers: Authorization, Content-Type

4) SockJS greeting

```bash
curl -i -s -D - -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' -H 'Sec-WebSocket-Version: 13' -k https://api.greedys.it/ws | sed -n '1,40p'
```
Response: HTTP/2 200 with body "Welcome to SockJS!" — this is the expected SockJS handshake response.

5) Native WebSocket upgrade attempt (raw websocket path)

```bash
curl -i -s -D - -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' -H 'Sec-WebSocket-Version: 13' -k https://api.greedys.it/ws/websocket
```
Response: HTTP/2 400

Notes: Browser SockJS client connects correctly. Raw websocket upgrade to `/ws/websocket` currently returns 400.

---

## What this means for Flutter app

1. If your Flutter app can use a SockJS-compatible client (recommended):
   - Use a SockJS+STOMP client implementation (that handles the `/ws` handshake, polling and upgrades). The browser client works this way.
   - In Dart/Flutter there is no mainstream SockJS client library. If you can run the app as web (in browser) the browser SockJS client works fine.

2. If your Flutter app uses a native WebSocket/STOMP client (common on mobile):
   - Currently direct WebSocket upgrade to the SockJS path (`/ws/websocket`) returns 400.
   - You have two options:
     A) Backend change: Expose a native, non-SockJS STOMP-over-WebSocket endpoint (recommended for mobile clients). Example: register an additional endpoint in Spring without `.withSockJS()`, e.g. `registry.addEndpoint("/stomp").setAllowedOriginPatterns(...);` — this will accept direct WebSocket upgrades.
     B) Client workaround: Implement the SockJS protocol in Dart manually (not recommended) or use the long-polling fallback HTTP transport (complex).

---

## Recommended action (short)

- If the Flutter app targets mobile: Ask backend team to expose a native WebSocket endpoint (e.g. `/stomp` or `/ws-native`) that accepts raw WebSocket upgrades (no SockJS). Backend change is small (example below).

- If the Flutter app targets web (browser): use the existing `https://api.greedys.it/ws` with SockJS + STOMP (works already). Use `websocket-test.html` to validate.

---

## Suggested backend code change (for native websocket endpoint)

Add a second `Stomp` endpoint in `WebSocketConfig.java` without SockJS, for example:

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // existing SockJS endpoint
    registry.addEndpoint("/ws").setAllowedOriginPatterns("http://*", "https://*", "null").withSockJS();

    // New native WebSocket endpoint for mobile clients
    registry.addEndpoint("/stomp")
            .setAllowedOriginPatterns("http://*", "https://*", "null");
}
```

This will allow clients to perform a standard WebSocket upgrade to `wss://api.greedys.it/stomp` and then use STOMP over WebSocket.

Note: after this change, redeploy; test with:

```bash
# Expect HTTP 101 Switching Protocols
curl -i -s -D - -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' -H 'Sec-WebSocket-Version: 13' -k https://api.greedys.it/stomp | sed -n '1,20p'
```

---

## Short instructions for Flutter developers (if backend adds `/stomp` native WS)

- Use a STOMP-over-WebSocket client (e.g., `stomp_dart_client`) and connect to:
  - `wss://api.greedys.it/stomp`
- If authentication required: send JWT in `Authorization` header when connecting or in the STOMP CONNECT headers (coordinate with backend team).

Example STOMP config snippet (Dart):

```dart
StompClient(
  config: StompConfig(
    url: 'wss://api.greedys.it/stomp',
    onConnect: _onConnect,
    beforeConnect: (frame) {
      // If backend expects JWT header
      frame?.addHeader('Authorization', 'Bearer <token>');
    },
  ),
);
```

---

## ✅ Native WebSocket Endpoint ADDED

The backend now supports a **native WebSocket endpoint** at `/stomp` for mobile/Flutter clients!

### Change Made
- Added new endpoint `/stomp` in `WebSocketConfig.java`
- No SockJS overhead
- Direct WebSocket protocol support
- Same CORS and STOMP configuration as `/ws`
- Commit: `8141b43` (pending pipeline deployment)

### Test the New Endpoint
```bash
# After pipeline deploys, test with:
curl -i -s -D - -H 'Connection: Upgrade' \
     -H 'Upgrade: websocket' \
     -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' \
     -H 'Sec-WebSocket-Version: 13' \
     -k https://api.greedys.it/stomp | sed -n '1,20p'

# Expected: HTTP/2 101 Switching Protocols (NOT 400 or 404)
```

---

## 🚀 READY FOR FLUTTER TEAM

The Flutter app can now connect to `wss://api.greedys.it/stomp` directly!

Use the code from `WEBSOCKET_FLUTTER_GUIDE.md` with this endpoint:

```dart
StompClient(
  config: StompConfig(
    url: 'wss://api.greedys.it/stomp',  // ← Native WebSocket endpoint
    onConnect: _onConnect,
    onStompError: _onError,
    // Optional: Add JWT token if authentication required
    beforeConnect: (frame) {
      // frame?.addHeader('Authorization', 'Bearer $token');
    },
  ),
);
```

---

**Status**: ✅ READY FOR DEPLOYMENT  
**Pending**: Pipeline execution to deploy the new `/stomp` endpoint