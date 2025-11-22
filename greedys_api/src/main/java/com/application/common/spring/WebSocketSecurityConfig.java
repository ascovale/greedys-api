package com.application.common.spring;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WebSocket Spring Security Configuration
 * 
 * This configuration integrates JWT authentication with WebSocket/STOMP messaging.
 * 
 * ARCHITECTURE:
 * ┌───────────────────────────────────────────────────────────────────────┐
 * │ WebSocket Security Layer - Multi-Level Role Isolation               │
 * ├───────────────────────────────────────────────────────────────────────┤
 * │                                                                       │
 * │ LEVEL 1: HANDSHAKE AUTHENTICATION                                    │
 * │ ├─ Location: WebSocketHandshakeInterceptor                           │
 * │ ├─ JWT extracted from: Authorization header or query param           │
 * │ ├─ Validation: Token signature, expiration, user type               │
 * │ └─ Result: WebSocketAuthenticationToken created                     │
 * │                                                                       │
 * │ LEVEL 2: STOMP FRAME AUTHORIZATION                                   │
 * │ ├─ Location: WebSocketChannelInterceptor                             │
 * │ ├─ Frames checked: CONNECT, SUBSCRIBE, SEND, MESSAGE                │
 * │ ├─ Rules: Role-based destination access, user ID isolation          │
 * │ └─ Result: Access allowed/denied per frame                          │
 * │                                                                       │
 * │ LEVEL 3: DESTINATION VALIDATION                                      │
 * │ ├─ Location: WebSocketDestinationValidator                           │
 * │ ├─ Patterns: /user/{id}/queue/*, /topic/{role}/*, /broadcast/*     │
 * │ ├─ Rules: User can only access their ID, matching role topics       │
 * │ └─ Result: Cross-role/cross-user message leakage prevented          │
 * │                                                                       │
 * └───────────────────────────────────────────────────────────────────────┘
 * 
 * SECURITY ENFORCEMENT:
 * 
 * 1. CUSTOMER trying to access RESTAURANT topic
 *    ❌ Denied by WebSocketDestinationValidator.validateTopicAccess()
 * 
 * 2. User 123 trying to subscribe to /user/456/queue/notifications
 *    ❌ Denied by WebSocketDestinationValidator.validateUserSpecificQueue()
 * 
 * 3. Admin trying to access /broadcast/announcements
 *    ✅ Allowed by WebSocketDestinationValidator.validateBroadcastAccess()
 * 
 * 4. Non-hub user trying to access /hub/789/queue/orders
 *    ❌ Denied by WebSocketDestinationValidator.validateHubAccess()
 * 
 * CSRF DISABLED: JWT token-based authentication is stateless and immune to CSRF.
 * 
 * @author Greedy's System
 * @since 2025-01-22
 */
@Slf4j
@Configuration
public class WebSocketSecurityConfig {
    
    // Configuration is managed by:
    // 1. WebSocketConfig - Registers interceptors for WebSocket endpoints
    // 2. WebSocketHandshakeInterceptor - Authenticates JWT during handshake
    // 3. WebSocketChannelInterceptor - Validates STOMP frames
    // 4. WebSocketDestinationValidator - Enforces role-based access control
    
    public WebSocketSecurityConfig() {
        log.info("⭐ WebSocket Security Configuration initialized");
        log.info("   - JWT authentication: WebSocketHandshakeInterceptor");
        log.info("   - STOMP frame validation: WebSocketChannelInterceptor");
        log.info("   - Destination access control: WebSocketDestinationValidator");
        log.info("   - Multi-level role isolation: ENABLED");
    }
}
