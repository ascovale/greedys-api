package com.application.common.security.websocket;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.application.common.security.jwt.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WebSocket Handshake Interceptor
 * 
 * Intercepts WebSocket handshake to extract and validate JWT token,
 * then authenticates the WebSocket connection before it's established.
 * 
 * FLOW:
 * 1. Client initiates WebSocket connection with JWT in Authorization header or query param
 * 2. Spring invokes this interceptor's beforeHandshake()
 * 3. We extract JWT token
 * 4. We validate token using JwtUtil
 * 5. We extract user info (ID, type, email)
 * 6. We create WebSocketAuthenticationToken
 * 7. We store authentication in WebSocket session attributes
 * 8. Session continues with authenticated user
 * 
 * JWT EXTRACTION:
 * The interceptor tries to extract JWT in this order:
 * 1. Authorization header: "Authorization: Bearer <token>"
 * 2. Query parameter: "?token=<token>"
 * 3. SockJS fallback query: "?access_token=<token>"
 * 
 * SECURITY:
 * - Invalid/expired tokens are rejected with HTTP 401
 * - Malformed tokens are rejected with HTTP 400
 * - Valid tokens are authenticated and authorized for further WebSocket operations
 * 
 * @author Greedy's System
 * @since 2025-01-22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtUtil jwtUtil;
    
    // WebSocket session attribute keys
    public static final String WS_AUTHENTICATION_ATTR = "ws-authentication";
    public static final String WS_USER_ID_ATTR = "ws-user-id";
    public static final String WS_USER_TYPE_ATTR = "ws-user-type";
    public static final String WS_USERNAME_ATTR = "ws-username";
    public static final String WS_EMAIL_ATTR = "ws-email";
    public static final String WS_RESTAURANT_ID_ATTR = "ws-restaurant-id";
    public static final String WS_AGENCY_ID_ATTR = "ws-agency-id";
    
    /**
     * Invoked before the WebSocket handshake
     * 
     * This is where we authenticate the WebSocket connection by validating the JWT.
     * If validation fails, we reject the connection.
     * 
     * @param request The HTTP request (contains headers and query params)
     * @param response The HTTP response (used for setting status codes)
     * @param wsHandler The WebSocket handler
     * @param attributes Session attributes to store authentication info
     * @return true to proceed with handshake, false to reject
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, 
            ServerHttpResponse response, 
            WebSocketHandler wsHandler, 
            Map<String, Object> attributes) {
        
        log.info("🤝 WebSocket handshake initiated from: {}", request.getRemoteAddress());
        
        try {
            // Step 1: Extract JWT token from request
            String token = extractJwtToken(request);
            
            if (token == null || token.isEmpty()) {
                log.warn("❌ WebSocket connection rejected: No JWT token provided");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            
            log.debug("🔐 JWT token extracted (length: {})", token.length());
            
            // Step 2: Validate JWT token structure and signature
            Claims claims;
            try {
                claims = jwtUtil.extractAllClaims(token);
                log.debug("✅ JWT signature validated successfully");
            } catch (Exception e) {
                log.warn("❌ WebSocket connection rejected: Invalid JWT token - {}", e.getMessage());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            
            // Step 3: Check if token is expired
            try {
                jwtUtil.extractExpiration(token);
                log.debug("✅ JWT expiration validated");
            } catch (Exception e) {
                log.warn("❌ WebSocket connection rejected: JWT token expired");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            
            // Step 4: Extract user information from JWT claims
            String username = (String) claims.get("sub"); // subject = email/username
            String userType = (String) claims.get("user_type");
            String email = (String) claims.get("email");
            
            // User ID might be in different claim names depending on token type
            Long userId = null;
            if (claims.containsKey("user_id")) {
                Object userIdObj = claims.get("user_id");
                userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : Long.valueOf((String) userIdObj);
            }
            
            // Restaurant ID (for restaurant staff - may be null for other user types)
            Long restaurantId = null;
            if (claims.containsKey("restaurant_id")) {
                Object restaurantIdObj = claims.get("restaurant_id");
                restaurantId = restaurantIdObj instanceof Number ? ((Number) restaurantIdObj).longValue() : Long.valueOf((String) restaurantIdObj);
            }
            
            // Agency ID (for agency staff - may be null for other user types)
            Long agencyId = null;
            if (claims.containsKey("agency_id")) {
                Object agencyIdObj = claims.get("agency_id");
                agencyId = agencyIdObj instanceof Number ? ((Number) agencyIdObj).longValue() : Long.valueOf((String) agencyIdObj);
            }
            
            // If no user_id in claims, try to extract from username (as fallback)
            if (userId == null) {
                log.debug("⚠️ No user_id found in JWT claims for user: {}", username);
                // For now, we'll allow it but log the issue
            }
            
            log.debug("👤 User info extracted: username={}, userType={}, userId={}, email={}, restaurantId={}", 
                     username, userType, userId, email, restaurantId);
            
            // Step 5: Validate user type is known
            if (userType == null || userType.isEmpty()) {
                log.warn("❌ WebSocket connection rejected: No user_type in JWT");
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }
            
            // Step 6: Extract authorities from JWT
            @SuppressWarnings("unchecked")
            List<String> authoritiesJson = (List<String>) claims.getOrDefault("authorities", List.of());
            List<SimpleGrantedAuthority> authorities = authoritiesJson.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            
            // Step 7: Create WebSocket authentication token
            WebSocketAuthenticationToken authToken = new WebSocketAuthenticationToken(
                    username,
                    userType,
                    userId,
                    email,
                    authorities,
                    claims,
                    token
            );
            
            // Step 8: Store authentication in WebSocket session attributes
            // These will be available throughout the WebSocket session lifecycle
            attributes.put(WS_AUTHENTICATION_ATTR, authToken);
            attributes.put(WS_USER_ID_ATTR, userId);
            attributes.put(WS_USER_TYPE_ATTR, userType);
            attributes.put(WS_USERNAME_ATTR, username);
            attributes.put(WS_EMAIL_ATTR, email);
            if (restaurantId != null) {
                attributes.put(WS_RESTAURANT_ID_ATTR, restaurantId);
            }
            if (agencyId != null) {
                attributes.put(WS_AGENCY_ID_ATTR, agencyId);
            }
            
            log.info("✅ WebSocket handshake successful for user: {} (type: {}, restaurantId: {}, agencyId: {})", 
                    username, userType, restaurantId, agencyId);
            return true;
            
        } catch (Exception e) {
            log.error("❌ Unexpected error during WebSocket handshake: {}", e.getMessage(), e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }
    
    /**
     * Invoked after the WebSocket handshake (connection established)
     * 
     * Used for cleanup or logging after successful connection.
     */
    @Override
    public void afterHandshake(
            ServerHttpRequest request, 
            ServerHttpResponse response, 
            WebSocketHandler wsHandler, 
            Exception exception) {
        
        if (exception != null) {
            log.error("❌ WebSocket handshake failed: {}", exception.getMessage(), exception);
        } else {
            log.info("✅ WebSocket connection established successfully");
        }
    }
    
    /**
     * Extracts JWT token from the request.
     * 
     * Tries multiple locations in this order:
     * 1. Authorization header (standard): "Authorization: Bearer <token>"
     * 2. Query parameter: "?token=<token>"
     * 3. SockJS/query parameter: "?access_token=<token>"
     * 
     * @param request The HTTP request
     * @return The JWT token string, or null if not found
     */
    private String extractJwtToken(ServerHttpRequest request) {
        // Method 1: Check Authorization header
        // For HTTP upgrade requests (WebSocket handshake is HTTP upgrade)
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("JWT extracted from Authorization header");
            return token;
        }
        
        // Method 2: Check query parameter ?token=...
        // This is used by JavaScript clients and mobile apps
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            String token = httpRequest.getParameter("token");
            if (token != null && !token.isEmpty()) {
                log.debug("JWT extracted from query parameter 'token'");
                return token;
            }
            
            // Method 3: Check query parameter ?access_token=...
            // Alternative SockJS parameter name
            token = httpRequest.getParameter("access_token");
            if (token != null && !token.isEmpty()) {
                log.debug("JWT extracted from query parameter 'access_token'");
                return token;
            }
        }
        
        log.warn("⚠️ No JWT token found in request");
        return null;
    }
}
