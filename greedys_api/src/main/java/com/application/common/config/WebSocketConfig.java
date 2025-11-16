package com.application.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WEBSOCKET CONFIGURATION
 * 
 * Configurazione di WebSocket per il sistema di notifiche real-time.
 * 
 * Responsabilità:
 * 1. Abilitare il message broker STOMP
 * 2. Registrare l'endpoint WebSocket /ws
 * 3. Configurare il prefisso di destinazione /app
 * 4. Abilitare SockJS fallback per browser senza WebSocket
 * 
 * ⭐ FLOW:
 * 1. Client connette a WebSocket endpoint: /ws
 * 2. Client si iscrive a topic: /user/{userId}/queue/notifications
 * 3. Server invia via SimpMessagingTemplate.convertAndSendToUser()
 * 4. Client riceve messaggi in tempo reale
 * 
 * ⭐ DIFFERENZA DA EMAIL/SMS:
 * - Email/SMS: Persistono in notification_channel_send [L2], hanno retry
 * - WebSocket: DIRECT pattern, nessuna persistenza, best-effort only
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura il message broker in-memory.
     * 
     * ⭐ NOTA IMPORTANTE:
     * - enableSimpleBroker(): Message broker in-memory (non persistente)
     * - Prefissi: /queue, /topic per messaggi
     * - setApplicationDestinationPrefix(): /app per comandi client→server
     * 
     * Per produzione con RabbitMQ:
     * config.enableStompBrokerRelay("localhost")
     *   .setRelayHost("localhost")
     *   .setRelayPort(61613)
     * 
     * @param config Il registro del message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("Configuring WebSocket message broker");
        
        // Abilita in-memory message broker per /queue e /topic
        config.enableSimpleBroker("/queue", "/topic");
        
        // Prefisso per destinazioni da client
        config.setApplicationDestinationPrefixes("/app");
        
        log.info("WebSocket message broker configured");
    }

    /**
     * Registra gli endpoint WebSocket.
     * 
     * ⭐ ENDPOINT REGISTRATION:
     * - /ws: Endpoint principale per connessioni WebSocket
     * - withSockJS(): Fallback per browser senza WebSocket
     * - setAllowedOrigins("*"): Permetti accesso da qualsiasi origine
     * 
     * ⭐ CLIENT CONNECTION:
     * var socket = new SockJS('/ws');
     * var stompClient = Stomp.over(socket);
     * stompClient.connect({}, function(frame) {
     *     stompClient.subscribe('/user/queue/notifications', function(message) {
     *         console.log('Received: ' + message.body);
     *     });
     * });
     * 
     * @param registry Il registro degli endpoint STOMP
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering WebSocket STOMP endpoint: /ws");
        
        // Registra endpoint WebSocket
        registry.addEndpoint("/ws")
            // ⭐ IMPORTANTE: Disabilita CORS validation per WebSocket
            // WebSocket non supporta credentials come REST, quindi non validare CORS qui
            // La validazione avviene in SecurityConfig solo per REST endpoints
            .setAllowedOriginPatterns("*")
            // Abilita SockJS fallback per browser vecchi
            .withSockJS()
            // Timeout di sessione
            .setSessionCookieNeeded(false);
        
        log.info("WebSocket STOMP endpoint registered successfully");
    }

    /**
     * ⭐ SPIEGAZIONE DEL FLOW WEBSOCKET
     * 
     * SCENARIO: Staff di ristorante riceve notifica di nuova prenotazione in REAL-TIME
     * 
     * 1. CLIENT SIDE (Browser dello staff)
     *    var socket = new SockJS('/ws');
     *    var stompClient = Stomp.over(socket);
     *    stompClient.connect({}, function(frame) {
     *        // Si iscrive a topic personale
     *        stompClient.subscribe('/user/queue/notifications', (message) => {
     *            JSON payload = message.body;
     *            console.log('New reservation: ' + payload.title);
     *        });
     *    });
     * 
     * 2. SERVER SIDE (quando arriva evento RESERVATION_REQUESTED)
     *    a. Service crea event_outbox [L0]
     *    b. EventOutboxPoller legge da DB, pubblica a RabbitMQ ogni 1s
     *    c. RestaurantNotificationListener riceve da RabbitMQ
     *    d. Crea RestaurantNotification + notification_outbox [L1]
     *    e. NotificationOutboxPoller legge L1, crea L2
     *    f. ChannelPoller legge L2, trova WEBSOCKET channel:
     *       
     *       for (staff : staffList) {
     *           Long staffId = staff.getId();  // e.g., 1
     *           Map<String, Object> payload = {
     *               "notificationId": 123,
     *               "title": "Nuova prenotazione",
     *               "body": "Tavolo per 4 persone alle 20:00",
     *               "timestamp": "2025-01-20T14:30:00Z"
     *           };
     *           
     *           simpMessagingTemplate.convertAndSendToUser(
     *               "1",  // userId come String
     *               "/queue/notifications",
     *               payload
     *           );
     *       }
     * 
     * 3. MESSAGE DELIVERY
     *    a. SimpMessagingTemplate serializza il payload a JSON
     *    b. Invia a /user/1/queue/notifications
     *    c. Client browser riceve il messaggio
     *    d. JavaScript callback elabora il payload
     *    e. UI update in REAL-TIME (nessun refresh!)
     * 
     * 4. TIMING:
     *    - T+0ms: Service crea event_outbox [L0]
     *    - T+1000ms: EventOutboxPoller pubblica a RabbitMQ
     *    - T+1100ms: Listener riceve, crea L1
     *    - T+5000ms: NotificationOutboxPoller legge L1, crea L2
     *    - T+10000ms: ChannelPoller legge L2, invia via WebSocket
     *    - T+10010ms: Client riceve messaggio REAL-TIME
     * 
     * ⭐ DIFFERENZA DA EMAIL:
     * - Email: T+5-10 min per invio effettivo
     * - WebSocket: T+10 sec ma REAL-TIME per staff online
     * - Se staff offline: Messaggio perso (best-effort, non persistente)
     */
}
