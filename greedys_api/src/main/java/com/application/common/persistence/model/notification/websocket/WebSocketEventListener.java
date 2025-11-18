package com.application.common.persistence.model.notification.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener per eventi WebSocket (connessione, disconnessione, sottoscrizione)
 * Gestisce il tracking delle sessioni attive per tipo utente (customer/restaurant/admin)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // ⭐ FIX: Null-check per evitare NullPointerException
        var sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.debug("📡 WebSocket CONNECTED: sessionId={} (no session attributes)", sessionId);
            return;
        }
        
        String username = (String) sessionAttributes.get("username");
        String userType = (String) sessionAttributes.get("userType");
        
        if (username != null && userType != null) {
            sessionManager.registerSession(username, userType, sessionId);
            log.info("🟢 WebSocket CONNECTED: sessionId={}, username={}, userType={}", 
                     sessionId, username, userType);
        } else {
            log.debug("📡 WebSocket CONNECTED: sessionId={} (no user info)", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // ⭐ FIX: Null-check per evitare NullPointerException
        var sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.debug("📡 WebSocket DISCONNECTED: sessionId={} (no session attributes)", sessionId);
            return;
        }
        
        String username = (String) sessionAttributes.get("username");
        
        if (username != null) {
            sessionManager.unregisterSession(username, sessionId);
            log.info("🔴 WebSocket DISCONNECTED: sessionId={}, username={}", sessionId, username);
        } else {
            log.debug("📡 WebSocket DISCONNECTED: sessionId={} (no user info)", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String userType = (String) headerAccessor.getSessionAttributes().get("userType");
        
        log.debug("📨 WebSocket SUBSCRIBE: destination={}, username={}, userType={}", 
                  destination, username, userType);
    }
}
