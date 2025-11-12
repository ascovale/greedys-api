package com.application.common.persistence.model.notification.websocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestore delle sessioni WebSocket attive.
 * Traccia connessioni per username (email) e userType (customer/restaurant-user/admin)
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    @Data
    @AllArgsConstructor
    public static class SessionInfo {
        private String sessionId;
        private String userType;
    }

    // Map: username (email) → Set<SessionInfo>
    private final Map<String, Set<SessionInfo>> userSessions = new ConcurrentHashMap<>();

    /**
     * Registra una nuova sessione WebSocket per un utente
     * @param username Email dell'utente (da JWT)
     * @param userType Tipo utente: customer, restaurant-user, admin
     * @param sessionId ID sessione WebSocket
     */
    public void registerSession(String username, String userType, String sessionId) {
        userSessions.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>())
                    .add(new SessionInfo(sessionId, userType));
        log.debug("✅ Registered WebSocket session: username={}, userType={}, sessionId={}", 
                  username, userType, sessionId);
    }

    /**
     * Rimuove una sessione WebSocket quando l'utente si disconnette
     * @param username Email dell'utente
     * @param sessionId ID sessione da rimuovere
     */
    public void unregisterSession(String username, String sessionId) {
        Set<SessionInfo> sessions = userSessions.get(username);
        if (sessions != null) {
            sessions.removeIf(info -> info.getSessionId().equals(sessionId));
            if (sessions.isEmpty()) {
                userSessions.remove(username);
            }
            log.debug("❌ Unregistered WebSocket session: username={}, sessionId={}", username, sessionId);
        }
    }

    /**
     * Verifica se un utente ha almeno una sessione WebSocket attiva
     * @param username Email dell'utente
     * @return true se l'utente è connesso
     */
    public boolean isUserConnected(String username) {
        Set<SessionInfo> sessions = userSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Ottiene tutte le sessioni attive di un utente
     * @param username Email dell'utente
     * @return Set di SessionInfo
     */
    public Set<SessionInfo> getSessionInfos(String username) {
        return userSessions.getOrDefault(username, Set.of());
    }

    /**
     * Conta il numero totale di connessioni WebSocket attive
     * @return Numero totale di sessioni
     */
    public int getActiveConnectionCount() {
        return userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * Ottiene tutti gli username connessi
     * @return Set di username (email)
     */
    public Set<String> getConnectedUsernames() {
        return userSessions.keySet();
    }
}
