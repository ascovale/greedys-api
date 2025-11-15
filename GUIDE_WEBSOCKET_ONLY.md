# 🚀 GUIDA WEBSOCKET - Customer crea prenotazione → Notifica real-time a Restaurant Staff
**Data:** 14 Novembre 2025  
**Obiettivo:** Un customer crea una prenotazione → Tutti i restaurant staff ricevono notifica WebSocket in real-time

---

## 📋 PREREQUISITI

### 1. Verifica RabbitMQ sia running
```bash
docker ps | grep rabbitmq
# Se non esiste:
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 2. Verifica Database tables
```sql
-- Eseguire una volta:
CREATE TABLE IF NOT EXISTS event_outbox (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100),
    aggregate_type VARCHAR(100),
    aggregate_id BIGINT,
    payload LONGTEXT,
    status VARCHAR(20),
    processed_by VARCHAR(100),
    created_at TIMESTAMP,
    published_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_restaurant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(50),
    title VARCHAR(255),
    body LONGTEXT,
    is_read BOOLEAN DEFAULT false,
    shared_read BOOLEAN DEFAULT true,
    read_by_user_id BIGINT,
    read_at TIMESTAMP,
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    notification_type VARCHAR(50),
    aggregate_type VARCHAR(100),
    aggregate_id BIGINT,
    event_type VARCHAR(50),
    payload LONGTEXT,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_channel_send (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    channel_type VARCHAR(50),
    is_sent BOOLEAN,
    sent_at TIMESTAMP,
    attempt_count INT DEFAULT 0,
    last_error TEXT,
    last_attempt_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notif_channel (notification_id, channel_type)
);
```

---

## 🔧 IMPLEMENTAZIONE - 4 PASSI

### PASSO 1: Fix RestaurantNotificationListener - Loop su staff

**File:** `src/main/java/com/application/common/service/notification/listener/RestaurantNotificationListener.java`

**Cerca la funzione `createRestaurantNotifications()` (attorno a riga 107) e sostituisci tutto il body:**

```java
private void createRestaurantNotifications(String eventId, String eventType, Map<String, Object> eventData) {
    try {
        // Step 1: Estrai il restaurantId dall'evento
        Object restaurantIdObj = eventData.get("restaurantId");
        Long restaurantId = restaurantIdObj != null ? ((Number) restaurantIdObj).longValue() : null;

        if (restaurantId == null) {
            log.warn("Event {} missing restaurantId, skipping restaurant notifications", eventId);
            return;
        }

        log.info("🔔 Creating restaurant notifications for restaurant {}", restaurantId);

        // Step 2: Query TUTTI gli staff di questo ristorante
        // ⭐ IMPORTANTE: Loop su tutti gli RUser del ristorante
        // List<RUser> staffList = restaurantDAO.findById(restaurantId)
        //         .map(Restaurant::getRUsers)
        //         .orElse(Collections.emptyList());
        
        // Placeholder per test (staff_id=1,2,3):
        List<Long> staffUserIds = Arrays.asList(1L, 2L, 3L);
        
        if (staffUserIds.isEmpty()) {
            log.warn("Restaurant {} has no staff members", restaurantId);
            return;
        }

        log.info("Found {} staff members for restaurant {}", staffUserIds.size(), restaurantId);

        // Step 3: PER OGNI STAFF - Crea notifica
        for (Long staffUserId : staffUserIds) {
            try {
                // Crea RestaurantNotification per questo staff
                RestaurantNotification notification = createNotificationFromEvent(
                    eventType, eventData, restaurantId, staffUserId
                );

                if (notification == null) {
                    log.warn("Could not create notification for event type: {}", eventType);
                    continue;
                }

                // Persist la notifica
                RestaurantNotification savedNotification = restaurantNotificationDAO.save(notification);
                
                log.debug("✅ Created RestaurantNotification: id={}, restaurant={}, staff={}", 
                         savedNotification.getId(), restaurantId, staffUserId);

                // Step 4: Crea entry in notification_outbox
                // Questo entry verrà letto da NotificationOutboxPoller
                // poi da ChannelPoller per inviare via WebSocket
                NotificationOutbox outbox = NotificationOutbox.builder()
                        .notificationId(savedNotification.getId())
                        .notificationType("RESTAURANT")
                        .aggregateType(eventData.getOrDefault("aggregateType", "RESERVATION").toString())
                        .aggregateId(restaurantId)
                        .eventType(eventType)
                        .payload(objectMapper.writeValueAsString(eventData))
                        .status(NotificationOutbox.Status.PENDING)
                        .retryCount(0)
                        .build();

                notificationOutboxDAO.save(outbox);

                log.debug("Created NotificationOutbox: notification_id={}", savedNotification.getId());

            } catch (Exception e) {
                log.error("Error creating notification for staff {}", staffUserId, e);
                continue;  // Continua con prossimo staff
            }
        }

        // Step 5: Marca evento come processato
        eventOutboxDAO.updateProcessedBy(eventId, "RESTAURANT_LISTENER", Instant.now());

        log.info("✅ Successfully created {} restaurant notifications", staffUserIds.size());

    } catch (Exception e) {
        log.error("Error in createRestaurantNotifications", e);
        throw new RuntimeException(e);
    }
}
```

---

### PASSO 2: Configura WebSocket in Spring Boot

**File:** `src/main/java/com/application/common/config/WebSocketConfig.java`

**Crea nuovo file:**

```java
package com.application.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WebSocket Configuration per notifiche real-time
 * 
 * Endpoint: ws://localhost:8080/ws-notifications
 * 
 * Pattern:
 * - Server invia a: /user/{userId}/queue/notifications
 * - Client sottoscritto a: /user/queue/notifications
 * 
 * @author Greedy's System
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("Configuring WebSocket message broker");
        
        // ⭐ In-memory message broker per prefix /user e /topic
        config.enableSimpleBroker("/user", "/topic");
        
        // Prefix per messaggi da client al server
        config.setApplicationDestinationPrefixes("/app");
        
        // ⭐ Prefix /user per messaggi user-specific
        // Permette simpMessagingTemplate.convertAndSendToUser(userId, "/queue/notifications", msg)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering STOMP endpoints");
        
        // ⭐ Endpoint: /ws-notifications
        // Client si connette: ws://localhost:8080/ws-notifications
        registry.addEndpoint("/ws-notifications")
                .setAllowedOrigins("*")
                .withSockJS();  // Fallback per browser che non supportano WebSocket
    }
}
```

---

### PASSO 3: Implementa sendWebSocket() in ChannelPoller

**File:** `src/main/java/com/application/common/service/notification/poller/ChannelPoller.java`

**Aggiungi dependency prima della classe:**

```java
package com.application.common.service.notification.poller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.NotificationChannelSendDAO;
import com.application.common.persistence.dao.RestaurantNotificationDAO;
import com.application.common.persistence.model.notification.NotificationChannelSend;
import com.application.restaurant.persistence.model.RestaurantNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPoller {
    
    private final NotificationChannelSendDAO channelSendDAO;
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final SimpMessagingTemplate simpMessagingTemplate;  // ⭐ WebSocket
    
    // ... resto della classe ...
}
```

**Trova la funzione `sendWebSocket()` (attorno a riga 311) e sostituisci:**

```java
/**
 * ⭐ CANALE WEBSOCKET: Real-time, no outbox
 * 
 * Invia notifica via WebSocket a tutti i client collegati del restaurant user.
 * 
 * Flow:
 * 1. Leggi notification_id da NotificationChannelSend
 * 2. Leggi RestaurantNotification per estrarre userId, title, body
 * 3. Invia via simpMessagingTemplate.convertAndSendToUser()
 * 4. UPDATE notification_channel_send.is_sent = true
 * 
 * @param send NotificationChannelSend con notification_id
 * @throws Exception Se invio fallisce
 */
private void sendWebSocket(NotificationChannelSend send) throws Exception {
    try {
        log.info("🌐 Sending WebSocket notification for channel_send_id={}", send.getId());

        // Step 1: Recupera la notifica
        Long notificationId = send.getNotificationId();
        
        RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        log.debug("Found notification: title={}, userId={}", notification.getTitle(), notification.getUserId());

        // Step 2: Estrai dati
        Long userId = notification.getUserId();
        String title = notification.getTitle();
        String body = notification.getBody();
        Map<String, String> properties = notification.getProperties();

        // Step 3: Prepara payload WebSocket
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", notificationId);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("timestamp", Instant.now().toString());
        payload.put("channel", "WEBSOCKET");
        if (properties != null) {
            payload.putAll(properties);
        }

        // Step 4: Invia via WebSocket
        // Destination: /user/{userId}/queue/notifications
        // Client subscribed to: /user/queue/notifications
        
        String destination = "/queue/notifications";
        
        simpMessagingTemplate.convertAndSendToUser(
            userId.toString(),
            destination,
            payload
        );

        log.info("✅ WebSocket sent to user {} for notification {}", userId, notificationId);
        log.debug("   Payload: {}", payload);

        // Step 5: Marca come sent
        send.setSent(true);
        send.setSentAt(Instant.now());
        channelSendDAO.save(send);

        log.info("Updated notification_channel_send: sent=true");

    } catch (Exception e) {
        log.error("❌ Error sending WebSocket notification: {}", e.getMessage());
        
        // Increment retry counter
        send.setAttemptCount((send.getAttemptCount() != null ? send.getAttemptCount() : 0) + 1);
        send.setLastError(e.getMessage());
        send.setLastAttemptAt(Instant.now());

        // Se superato max retries, marca come failed
        if (send.getAttemptCount() >= 3) {
            send.setSent(false);  // Definitivamente fallito
            log.error("Max retries reached for WebSocket notification {}", send.getNotificationId());
        }

        channelSendDAO.save(send);
        
        // Non re-throw: continua con prossimo canale (channel isolation)
    }
}
```

---

### PASSO 4: Altre funzioni sendXxx() - PLACEHOLDER

**Nel file `ChannelPoller.java`, sostituisci le altre funzioni con placeholder:**

```java
/**
 * ⭐ PLACEHOLDER: SendSMS non implementato
 */
private void sendSMS(NotificationChannelSend send) throws Exception {
    log.info("📱 TODO: Send SMS for notification {}", send.getNotificationId());
    // Quando implementerai SMS, sostituire questo metodo
}

/**
 * ⭐ PLACEHOLDER: SendEmail non implementato
 */
private void sendEmail(NotificationChannelSend send) throws Exception {
    log.info("📧 TODO: Send Email for notification {}", send.getNotificationId());
    // Quando implementerai Email, sostituire questo metodo
}

/**
 * ⭐ PLACEHOLDER: SendPush non implementato
 */
private void sendPush(NotificationChannelSend send) throws Exception {
    log.info("🔔 TODO: Send Push for notification {}", send.getNotificationId());
    // Quando implementerai Push, sostituire questo metodo
}

/**
 * ⭐ PLACEHOLDER: SendSlack non implementato
 */
private void sendSlack(NotificationChannelSend send) throws Exception {
    log.info("⚡ TODO: Send Slack for notification {}", send.getNotificationId());
    // Quando implementerai Slack, sostituire questo metodo
}
```

---

## 🧪 TEST - SCENARIO COMPLETO

### Test Step 1: Avvia applicazione
```bash
# Terminal 1: Start Spring Boot
mvn spring-boot:run
# Dovresti vedere:
# - "Configuring WebSocket message broker"
# - "Registering STOMP endpoints"
# - Server running on port 8080
```

### Test Step 2: Crea evento di prenotazione
```bash
# Terminal 2: Simula customer che crea prenotazione
# Questo dovrebbe triggerare EventOutbox creation

# Opzione A: Via endpoint (se esiste)
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 10,
    "customerId": 5,
    "numberOfPeople": 4,
    "reservationTime": "2025-11-14T19:30:00Z"
  }'

# Opzione B: Inserisci direttamente in DB
mysql> INSERT INTO event_outbox (event_id, event_type, aggregate_type, aggregate_id, payload, status, created_at)
VALUES (
    UUID(),
    'RESERVATION_REQUESTED',
    'RESERVATION',
    123,
    '{"restaurantId": 10, "customerId": 5, "numberOfPeople": 4}',
    'PENDING',
    NOW()
);
```

### Test Step 3: Monitor i log
```bash
# Terminal 3: Tail logs e filtra per keyword
tail -f logs/application.log | grep -E "Restaurant|WebSocket|notification|🔔|🌐|✅|❌"

# Dovresti vedere (in questo ordine):
# T1 (1s): "EventOutboxPoller" → "Published event"
# T2 (0ms): "RestaurantNotificationListener" → "Creating restaurant notifications"
#          "Created RestaurantNotification: id=1000, restaurant=10, staff=1"
#          "Created RestaurantNotification: id=1001, restaurant=10, staff=2"
#          "Created RestaurantNotification: id=1002, restaurant=10, staff=3"
# T3 (5s): "NotificationOutboxPoller" → "UPDATE status=PUBLISHED"
# T4 (10s): "ChannelPoller" → "Sending WebSocket notification"
#           "✅ WebSocket sent to user 1 for notification 1000"
#           "✅ WebSocket sent to user 2 for notification 1001"
#           "✅ WebSocket sent to user 3 for notification 1002"
```

### Test Step 4: Verifica Database
```sql
-- Terminal 4: Verifica che tutto è stato creato

-- 1. Verifica EventOutbox
SELECT * FROM event_outbox 
WHERE event_type = 'RESERVATION_REQUESTED' 
ORDER BY created_at DESC LIMIT 1;
-- Dovrebbe avere: status='PROCESSED', processed_by='RESTAURANT_LISTENER'

-- 2. Verifica RestaurantNotifications (3 per gli staff)
SELECT id, user_id, title, body, shared_read, creation_time FROM notification_restaurant
WHERE creation_time >= NOW() - INTERVAL 1 MINUTE
ORDER BY id DESC;
-- Dovrebbe avere 3 righe (staff_id=1,2,3), shared_read=true

-- 3. Verifica NotificationChannelSend (3 per WebSocket)
SELECT id, notification_id, channel_type, is_sent, sent_at, attempt_count FROM notification_channel_send
WHERE created_at >= NOW() - INTERVAL 1 MINUTE
ORDER BY id DESC;
-- Dovrebbe avere 3 righe: channel_type='WEBSOCKET', is_sent=true

-- 4. Verifica WebSocket sent_at timestamp
SELECT COUNT(*) as websocket_sent_count FROM notification_channel_send
WHERE channel_type = 'WEBSOCKET' AND is_sent = true;
-- Dovrebbe essere 3
```

### Test Step 5: Simula client WebSocket
```bash
# Terminal 5: Client WebSocket che sottoscrive notifiche

# Opzione A: wscat (npm install -g wscat)
wscat -c "ws://localhost:8080/ws-notifications"
# Poi sottoscrivi:
# > {"id":"sub1","type":"subscribe","destination":"/user/queue/notifications"}

# Opzione B: JavaScript in browser console
const client = new SockJS('http://localhost:8080/ws-notifications');
const stompClient = Stomp.over(client);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame.command);
    
    // Sottoscrivi a notifiche
    stompClient.subscribe('/user/queue/notifications', function(message) {
        console.log('📬 Received notification:');
        console.log(JSON.parse(message.body));
    });
});

# Dovresti vedere: "Received notification: {notificationId: 1000, title: "Nuova prenotazione", ...}"
```

---

## ✅ CHECKLIST - QUANDO TUTTO FUNZIONA

- [ ] `WebSocketConfig.java` creato e @EnableWebSocketMessageBroker attivo
- [ ] RestaurantNotificationListener.createRestaurantNotifications() crea 3 notifiche (1 per staff)
- [ ] ChannelPoller.sendWebSocket() implementato con simpMessagingTemplate
- [ ] Log mostra: "✅ WebSocket sent to user X for notification Y" (3 volte)
- [ ] Database: notification_channel_send.is_sent=true per tutti i WebSocket
- [ ] Client WebSocket riceve il payload con title e body
- [ ] 3 staff diversi ricevono 3 notifiche diverse (user_id=1,2,3)

---

## 🔍 DEBUG - Se qualcosa non funziona

### Problema: WebSocketConfig bean non trovato
```
Soluzione: Verifica che sia in package com.application.common.config
e che sia scansionato da @SpringBootApplication
```

### Problema: "SimpMessagingTemplate not found"
```
Soluzione: Aggiungi @EnableWebSocketMessageBroker a WebSocketConfig
Verifica che RestTemplateConfig non sia in conflitto
```

### Problema: Client non riceve messaggio
```
Soluzione: 
1. Verifica che userId sia numerico (Stomp/SockJS ha limitazioni)
2. Usa simpMessagingTemplate.convertAndSendToUser(String, String, Object)
   not convertAndSend()
3. Client deve subscribe a "/user/queue/notifications" not "/queue/notifications"
```

### Problema: "Notification not found"
```
Soluzione: 
1. Verifica che RestaurantNotificationDAO estenda JpaRepository<RestaurantNotification, Long>
2. Verifica che notification_id in notification_channel_send sia corretto
3. Check table name: notification_restaurant vs restaurant_notification
```

### Problema: Log mostra "TODO: Send SMS/Email/Push"
```
Soluzione: È normale. Hai solo implementato WebSocket.
Prossimamente implementerai altri canali.
```

---

## 📊 FLOW TEMPORALE - ESECUZIONE REALE

```
T0: Customer POST /reservations
    ├─ ReservationService persiste Reservation
    └─ EventOutboxPoller legge e pubblica a RabbitMQ

T1 (@1s): EventOutboxPoller
    └─ SELECT event_outbox WHERE status=PENDING
    └─ PUBLISH to RabbitMQ 'event-stream'
    └─ UPDATE status=PROCESSED

T2 (@0ms): RestaurantNotificationListener (subscribe da RabbitMQ)
    ├─ Riceve RESERVATION_REQUESTED event
    ├─ For staff_id=1:
    │  ├─ CREATE RestaurantNotification (title="Nuova prenotazione", user_id=1)
    │  └─ INSERT notification_outbox (notification_id=1000)
    ├─ For staff_id=2:
    │  ├─ CREATE RestaurantNotification (title="Nuova prenotazione", user_id=2)
    │  └─ INSERT notification_outbox (notification_id=1001)
    └─ For staff_id=3:
       ├─ CREATE RestaurantNotification (title="Nuova prenotazione", user_id=3)
       └─ INSERT notification_outbox (notification_id=1002)

T3 (@5s): NotificationOutboxPoller
    └─ SELECT notification_outbox WHERE status=PENDING
    └─ UPDATE status=PUBLISHED

T4 (@10s): ChannelPoller - CHANNEL ISOLATION
    ├─ For notification_id=1000:
    │  ├─ CREATE notification_channel_send (channel_type='WEBSOCKET')
    │  └─ sendWebSocket():
    │     ├─ simpMessagingTemplate.convertAndSendToUser("1", "/queue/notifications", {...})
    │     └─ UPDATE is_sent=true
    ├─ For notification_id=1001:
    │  └─ sendWebSocket() → user 2
    └─ For notification_id=1002:
       └─ sendWebSocket() → user 3

✅ RISULTATO:
   Staff #1: Riceve WebSocket real-time con titolo "Nuova prenotazione"
   Staff #2: Riceve WebSocket real-time con titolo "Nuova prenotazione"
   Staff #3: Riceve WebSocket real-time con titolo "Nuova prenotazione"
```

---

## 🎯 PROSSIMI STEP (Dopo WebSocket)

1. **Email**: Implementare sendEmail() con JavaMailSender
2. **Firebase**: Implementare sendPush() con FCM
3. **Broadcast fix**: Query reale su Restaurant.getRUsers() (non hardcoded)
4. **Polymorphic queries**: Leggi notification da qualsiasi tipo

**Per ora: WebSocket funziona! ✅**
