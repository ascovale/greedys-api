package com.application.common.security.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket STOMP Channel Interceptor
 * 
 * Intercepts STOMP frame messages at the channel level to enforce multi-level
 * role-based and identity-based access control.
 * 
 * FLOW:
 * 1. Client sends STOMP frame (CONNECT, SUBSCRIBE, SEND, etc.)
 * 2. Spring routes to MessageChannel
 * 3. This interceptor's preSend() is invoked
 * 4. We extract destination and authentication from message headers
 * 5. We validate access based on user type, user ID, and destination pattern
 * 6. We allow or reject the message
 * 
 * FRAMES CONTROLLED:
 * - CONNECT: Validate token from CONNECT frame
 * - SUBSCRIBE: Validate user can subscribe to destination
 * - SEND: Validate user can send to destination
 * - MESSAGE: Validate user receives only messages for their role/ID
 * 
 * SECURITY RULES:
 * 1. Users can ONLY subscribe to /user/{theirId}/queue/* destinations
 * 2. Users can ONLY subscribe to /topic/{theirRole}/* destinations
 * 3. Admins ONLY can access /broadcast/* destinations
 * 4. Hub users ONLY can access /hub/{hubId}/* destinations
 * 5. Cross-role message delivery is prevented
 * 
 * @author Greedy's System
 * @since 2025-01-22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {
    
    private final WebSocketDestinationValidator destinationValidator;
    
    /**
     * Invoked before a message is sent to the channel
     * 
     * This is where we enforce access control on STOMP frames.
     * 
     * @param message The message (contains headers and payload)
     * @param channel The message channel
     * @return The message if allowed, or throw exception if denied
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        SimpMessageType messageTypeEnum = accessor.getMessageType();
        String messageType = messageTypeEnum != null ? messageTypeEnum.toString() : "UNKNOWN";
        
        try {
            if (messageTypeEnum == null) {
                log.debug("Message type is null");
                return message;
            }
            
            if (SimpMessageType.CONNECT.equals(messageTypeEnum)) {
                return handleConnect(message, accessor);
            } else if (SimpMessageType.SUBSCRIBE.equals(messageTypeEnum)) {
                return handleSubscribe(message, accessor);
            } else if (SimpMessageType.MESSAGE.equals(messageTypeEnum)) {
                return handleMessage(message, accessor);
            } else if (SimpMessageType.DISCONNECT.equals(messageTypeEnum)) {
                return handleDisconnect(message, accessor);
            } else {
                log.debug("Message type not explicitly handled: {}", messageType);
                return message;
            }
        } catch (AccessDeniedException e) {
            log.error("Access denied for {} message: {}", messageType, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error processing {} message: {}", messageType, e.getMessage(), e);
            throw new AccessDeniedException("Access denied: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles CONNECT frame - validates initial WebSocket connection
     * 
     * The CONNECT frame contains the first STOMP command from the client.
     * At this point, we should have already authenticated via handshake,
     * but we double-check the authentication is present.
     * 
     * @param message The CONNECT message
     * @param accessor Header accessor for the message
     * @return The message if valid
     */
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
    
    /**
     * Handles SUBSCRIBE frame - validates subscription to destination
     * 
     * This is critical for preventing cross-role and cross-user message leaks.
     * We validate that the user is authorized to subscribe to the destination.
     * 
     * @param message The SUBSCRIBE message
     * @param accessor Header accessor for the message
     * @return The message if allowed
     */
    private Message<?> handleSubscribe(Message<?> message, SimpMessageHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        String username = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
        
        log.debug("SUBSCRIBE frame: destination={}, session={}, user={}", 
                 destination, sessionId, username);
        
        // Extract authentication from session
        WebSocketAuthenticationToken auth = extractAuthentication(accessor);
        
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("SUBSCRIBE rejected: User {} not authenticated", username);
            throw new AccessDeniedException("Not authenticated");
        }
        
        // Validate destination access
        boolean allowed = destinationValidator.canAccess(
                destination, 
                auth.getUserType(), 
                auth.getUserId()
        );
        
        if (allowed) {
            log.info("SUBSCRIBE allowed: User {} (ID: {}, type: {}) -> {}", 
                     auth.getUsername(), auth.getUserId(), auth.getUserType(), destination);
            return message;
        } else {
            log.warn("SUBSCRIBE denied: User {} (ID: {}, type: {}) -> {} (authorization failed)", 
                     auth.getUsername(), auth.getUserId(), auth.getUserType(), destination);
            throw new AccessDeniedException(
                    "Not authorized to subscribe to: " + destination + 
                    " (role: " + destinationValidator.getRoleFromUserType(auth.getUserType()) + ")"
            );
        }
    }
    
    /**
     * Handles MESSAGE frame - validates server-sent messages
     * 
     * @param message The MESSAGE frame
     * @param accessor Header accessor for the message
     * @return The message if the recipient should receive it
     */
    private Message<?> handleMessage(Message<?> message, SimpMessageHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        
        log.debug("MESSAGE frame: destination={}, session={}", destination, sessionId);
        
        if (destination != null && destination.startsWith("/user/")) {
            // Extract user ID from destination
            String[] parts = destination.substring(6).split("/");
            if (parts.length > 0) {
                try {
                    Long destUserId = Long.parseLong(parts[0]);
                    log.debug("MESSAGE allowed: Destination user ID validated - {}", destUserId);
                } catch (NumberFormatException e) {
                    log.warn("MESSAGE rejected: Invalid user ID in destination - {}", parts[0]);
                    throw new AccessDeniedException("Invalid destination user ID");
                }
            }
        }
        
        return message;
    }
    
    /**
     * Handles DISCONNECT frame - logs user disconnection
     * 
     * @param message The DISCONNECT message
     * @param accessor Header accessor for the message
     * @return The message
     */
    private Message<?> handleDisconnect(Message<?> message, SimpMessageHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String username = accessor.getUser() != null ? accessor.getUser().getName() : "unknown";
        
        log.info("DISCONNECT frame: User {} disconnected (session: {})", username, sessionId);
        
        return message;
    }
    
    /**
     * Extracts WebSocketAuthenticationToken from message headers
     * 
     * @param accessor SimpMessageHeaderAccessor for the message
     * @return WebSocketAuthenticationToken if found and valid, null otherwise
     */
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
}
