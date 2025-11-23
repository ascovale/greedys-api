# Security Validation - Implementation Notes

## WebSocketHandshakeInterceptor

```java
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtUtil jwtUtil;
    
    @Override
    public boolean beforeHandshake(ServletUpgradeRequest request, 
                                  ServletUpgradeResponse response,
                                  WebSocketHandler wsHandler, 
                                  Map<String, Object> attributes) {
        try {
            // 1. Extract JWT from Authorization header
            String token = extractTokenFromHeader(request);
            if (token == null) {
                // 2. Try query parameter
                token = extractTokenFromQuery(request);
            }
            
            if (token == null) {
                log.warn("No JWT token provided");
                return false;  // HTTP 401
            }
            
            // 3. Validate signature & expiration
            Claims claims = jwtUtil.validateAndGetClaims(token);
            
            // 4. Extract user information
            Long userId = claims.get("user_id", Long.class);
            String userType = claims.get("user_type", String.class);
            String username = claims.getSubject();
            
            if (userId == null || userType == null) {
                log.warn("Invalid JWT claims");
                return false;  // HTTP 401
            }
            
            // 5. Store in session attributes for later STOMP validation
            attributes.put("userId", userId);
            attributes.put("userType", userType);
            attributes.put("username", username);
            attributes.put("authenticated", true);
            
            log.info("WebSocket handshake successful: user_id={}, type={}", userId, userType);
            return true;  // HTTP 101 Switching Protocols
            
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;  // HTTP 401
        } catch (Exception e) {
            log.error("Handshake error", e);
            return false;  // HTTP 500
        }
    }
    
    private String extractTokenFromHeader(ServletUpgradeRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private String extractTokenFromQuery(ServletUpgradeRequest request) {
        String token = request.getParameter("token");
        return token != null ? token : null;
    }
}
```

## WebSocketChannelInterceptor

```java
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {
    
    private final WebSocketDestinationValidator validator;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String command = accessor.getCommand().toString();
        
        // Check authentication first
        if (!isAuthenticated(accessor)) {
            throw new AccessDeniedException("Not authenticated");
        }
        
        if ("SUBSCRIBE".equals(command)) {
            String destination = accessor.getDestination();
            Long userId = (Long) accessor.getSessionAttributes().get("userId");
            String userType = (String) accessor.getSessionAttributes().get("userType");
            
            // Validate destination ownership
            if (!validator.canAccess(destination, userId, userType)) {
                throw new AccessDeniedException("Cannot subscribe to: " + destination);
            }
            
            log.info("SUBSCRIBE allowed: user={}, destination={}", userId, destination);
        }
        
        if ("SEND".equals(command)) {
            String destination = accessor.getDestination();
            if (!destination.startsWith("/app/")) {
                throw new AccessDeniedException("Cannot send to: " + destination);
            }
        }
        
        return message;
    }
    
    private boolean isAuthenticated(StompHeaderAccessor accessor) {
        Map<String, Object> session = accessor.getSessionAttributes();
        return session != null && session.get("authenticated") != null;
    }
}
```

## WebSocketDestinationValidator

```java
@Component
public class WebSocketDestinationValidator {
    
    public static boolean canAccess(String destination, Long userId, String userType) {
        // Rule 1: User-specific queues
        if (destination.startsWith("/user/" + userId + "/")) {
            return true;
        }
        
        // Rule 2: Role-based topics
        if (destination.startsWith("/topic/notifications/" + userId + "/")) {
            // Check if userType matches
            if (destination.endsWith("/" + userType)) {
                return true;
            }
            if (destination.endsWith("/RESTAURANT") && isRestaurantUser(userType)) {
                return true;
            }
            if (destination.endsWith("/CUSTOMER") && userType.equals("CUSTOMER")) {
                return true;
            }
            if (destination.endsWith("/AGENCY") && isAgencyUser(userType)) {
                return true;
            }
            if (destination.endsWith("/ADMIN") && userType.equals("ADMIN")) {
                return true;
            }
        }
        
        // Rule 3: Broadcast (admin only)
        if (destination.startsWith("/topic/broadcast/")) {
            return userType.equals("ADMIN");
        }
        
        // Default: deny
        return false;
    }
    
    private static boolean isRestaurantUser(String userType) {
        return userType.startsWith("RESTAURANT");
    }
    
    private static boolean isAgencyUser(String userType) {
        return userType.startsWith("AGENCY");
    }
}
```

## WebSocketAuthenticationToken

```java
@Data
public class WebSocketAuthenticationToken implements Authentication {
    
    private final String username;
    private final Long userId;
    private final String userType;
    private final List<GrantedAuthority> authorities;
    private boolean authenticated;
    
    public boolean isUserType(String type) {
        return userType.equals(type);
    }
    
    public boolean isHubUser() {
        return userType.contains("-hub");
    }
    
    public String getRecipientType() {
        // Extract role from userType
        if (userType.startsWith("RESTAURANT")) {
            return "RESTAURANT";
        } else if (userType.startsWith("AGENCY")) {
            return "AGENCY";
        } else if (userType.equals("CUSTOMER")) {
            return "CUSTOMER";
        } else if (userType.equals("ADMIN")) {
            return "ADMIN";
        }
        return null;
    }
}
```

## Integration with Spring Security

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .addInterceptors(webSocketHandshakeInterceptor())
            .withSockJS();
        
        registry.addEndpoint("/stomp")
            .setAllowedOrigins("*")
            .addInterceptors(webSocketHandshakeInterceptor());
    }
    
    @Bean
    public WebSocketHandshakeInterceptor webSocketHandshakeInterceptor() {
        return new WebSocketHandshakeInterceptor(jwtUtil);
    }
}

@Configuration
public class WebSocketSecurityConfig {
    
    @Bean
    public ChannelInterceptor webSocketChannelInterceptor() {
        return new WebSocketChannelInterceptor(webSocketDestinationValidator());
    }
}
```

---

**Document Version**: 1.0  
**Component**: Security Validation (WebSocket Authentication & Authorization)
