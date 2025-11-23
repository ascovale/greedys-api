package com.application.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.application.common.security.websocket.WebSocketChannelInterceptor;
import com.application.common.security.websocket.WebSocketHandshakeInterceptor;

import lombok.RequiredArgsConstructor;
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
 * 2. Client si iscrive a topic: /topic/{userType}/{userId}/notifications
 *    (userType = "restaurant" | "customer" | "agency" | "admin")
 * 3. Server invia via SimpMessagingTemplate.convertAndSend()
 * 4. Client riceve messaggi in tempo reale
 * 
 * ⭐ DESIGN: Destination include userType per evitare collisioni ID tra tabelle diverse
 *    (RUser, Customer, AgencyUser, Admin hanno ID independenti con auto-increment)
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
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final WebSocketChannelInterceptor channelInterceptor;

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
     * ⭐ REGISTRA GLI INTERCEPTOR SUGLI INBOUND/OUTBOUND CHANNEL
     * 
     * Questo metodo configura gli interceptor che validano OGNI STOMP FRAME:
     * - CONNECT: Valida che l'utente sia autenticato
     * - SUBSCRIBE: Valida che l'utente possa accedere alla destinazione
     * - SEND: Valida che l'utente possa inviare al destinazione
     * - MESSAGE: Valida che il messaggio sia del ruolo/utente corretto
     * 
     * FLUSSO SICUREZZA:
     * Client STOMP → WebSocketChannelInterceptor.preSend() → Valida accesso → Passa/Blocca
     * 
     * @param registration Registrazione del channel
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Registering WebSocket channel security interceptors");
        
        // Registra l'interceptor per validare OGNI STOMP frame in ingresso
        registration.interceptors(channelInterceptor);
        
        log.info("✅ WebSocket channel interceptor registered for inbound messages");
    }
    
    /**
     * ⭐ REGISTRA GLI INTERCEPTOR SUI MESSAGGI IN USCITA
     * 
     * Valida i messaggi che il server invia ai client per prevenire
     * la perdita di messaggi cross-role.
     * 
     * @param registration Registrazione del channel
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        log.info("Registering WebSocket outbound channel security");
        
        // Registra l'interceptor anche sui messaggi in uscita
        registration.interceptors(channelInterceptor);
        
        log.info("✅ WebSocket channel interceptor registered for outbound messages");
    }

    /**
     * Registra gli endpoint WebSocket.
     * 
     * ⭐ ENDPOINT REGISTRATION:
     * - /ws: Endpoint principale per connessioni WebSocket
     * - withSockJS(): Fallback per browser senza WebSocket
     * - setAllowedOrigins("*"): Permetti accesso da qualsiasi origine
     * 
     * ⭐ JWT AUTHENTICATION:
     * La sicurezza è implementata in WebSocketHandshakeInterceptor che:
     * 1. Estrae JWT da Authorization header o query parameter
     * 2. Valida il token usando JwtUtil
     * 3. Estrae user ID, type, email da JWT claims
     * 4. Crea WebSocketAuthenticationToken
     * 5. Salva in WebSocket session attributes
     * 6. Rifiuta connessione se JWT non valido (401)
     * 
     * ⭐ JWT EXTRACTION LOCATIONS:
     * Client JavaScript (SockJS):
     *     var socket = new SockJS('/ws?token=<jwt>');
     * Client JavaScript (Native WebSocket):
     *     var socket = new WebSocket('ws://host/stomp?token=<jwt>');
     * Client HTTP Header:
     *     GET /ws HTTP/1.1
     *     Authorization: Bearer <jwt>
     * 
     * ⭐ CLIENT CONNECTION:
     * var socket = new SockJS('/ws?token=' + jwtToken);
     * var stompClient = Stomp.over(socket);
     * stompClient.connect({}, function(frame) {
     *     // Subscribe to user's personal notification queue
     *     // Pattern: /topic/{userType}/{userId}/notifications
     *     // Examples: /topic/restaurant/50/notifications, /topic/customer/50/notifications
     *     stompClient.subscribe('/topic/restaurant/123/notifications', function(message) {
     *         console.log('Received: ' + message.body);
     *     });
     * });
     * 
     * @param registry Il registro degli endpoint STOMP
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering WebSocket STOMP endpoints: /ws (SockJS) and /stomp (native)");
        
        // ⭐ ENDPOINT 1: /ws - SockJS + STOMP (per browser, web app, fallback)
        // SockJS fornisce fallback per browser senza WebSocket nativo (es. IE9, network proxy)
        registry.addEndpoint("/ws")
            // ⭐ IMPORTANTE: Disabilita CORS validation per WebSocket
            // WebSocket non supporta credentials come REST, quindi non validare CORS qui
            // La validazione avviene in SecurityConfig solo per REST endpoints
            .setAllowedOriginPatterns("*")
            // ⭐ REGISTRA HANDSHAKE INTERCEPTOR
            // Questo interceptor estrae JWT dal request e autentica la connessione
            .addInterceptors(handshakeInterceptor)
            // Abilita SockJS fallback per browser vecchi
            .withSockJS()
            // ⭐ CRITICO: Permetti session cookies per SockJS handshake
            // SockJS ha bisogno di cookies per identificare la sessione
            .setSessionCookieNeeded(true);
        
        // ⭐ ENDPOINT 2: /stomp - Native WebSocket + STOMP (per client mobili, Flutter, etc)
        // Questo endpoint accetta connessioni WebSocket native senza SockJS fallback
        registry.addEndpoint("/stomp")
            .setAllowedOriginPatterns("http://*", "https://*", "null")
            // ⭐ REGISTRA HANDSHAKE INTERCEPTOR (anche per native WebSocket)
            .addInterceptors(handshakeInterceptor);
            // NO withSockJS() - questo è per WebSocket puro
        
        log.info("WebSocket STOMP endpoints registered successfully");
        log.info("  - /ws: SockJS endpoint for browsers (HTTP fallback available)");
        log.info("  - /stomp: Native WebSocket endpoint for mobile/Flutter clients");
        log.info("✅ JWT authentication enabled via WebSocketHandshakeInterceptor");
        log.info("✅ Role-based access control enabled via WebSocketChannelInterceptor");
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
     *        // Pattern: /topic/{userType}/{userId}/notifications
     *        stompClient.subscribe('/topic/restaurant/123/notifications', (message) => {
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
    *           Long staffId = staff.getId();  // e.g., 123
    *           String userType = "restaurant";  // from recipientType
    *           Map<String, Object> payload = {
    *               "notificationId": 123,
    *               "title": "Nuova prenotazione",
    *               "body": "Tavolo per 4 persone alle 20:00",
    *               "timestamp": "2025-01-20T14:30:00Z"
    *           };
    *           
    *           // NEW: Direct destination pattern with userType for personal notifications
    *           // Personal notifications (badge, generic alerts) use per-user topic:
    *           //    /topic/{userType}/{userId}/notifications
    *           String destination = String.format("/topic/%s/%d/notifications", 
    *                                               userType, staffId);
    *           simpMessagingTemplate.convertAndSend(destination, payload);
    *       }
    *
    *       // NOTE: Reservation list updates are restaurant-scoped and use restaurantId
    *       // ChannelPoller / ReservationPublisher should publish list updates to:
    *       //    /topic/restaurant/{restaurantId}/reservations
    *       // This destination is NOT per-user; all staff subscribe to the restaurant
    *       // reservations topic to receive list updates (create/accept/reject).
     * 
     * 3. MESSAGE DELIVERY
     *    a. SimpMessagingTemplate serializza il payload a JSON
     *    b. Invia a /topic/restaurant/123/notifications (con userType prefisso)
     *    c. Client browser riceve il messaggio dalla destinazione corretta
     *    d. JavaScript callback elabora il payload
     *    e. UI update in REAL-TIME (nessun refresh!)
     *    
     *    ⭐ WHY userType IN PATH?
     *       RUser, Customer, AgencyUser, Admin hanno auto-increment ID indipendenti
     *       Quindi userId=123 potrebbe essere sia RUser che Customer
     *       Includere userType nella destinazione previene routing a utente sbagliato
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
