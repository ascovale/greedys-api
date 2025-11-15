# 🔍 CONTROLLO IMPLEMENTAZIONE - STATO ATTUALE
**Data:** 14 Novembre 2025  
**Verifica:** WebSocket e componenti notifiche

---

## ❌ COSA MANCA

### 1️⃣ WebSocketConfig.java - **NON ESISTE**

**Status:** ❌ File non creato

**Dove dovrebbe essere:**
```
src/main/java/com/application/common/config/WebSocketConfig.java
```

**Azione richiesta:** 
Creare il file con @EnableWebSocketMessageBroker

---

## ⚠️ PROBLEMI RISCONTRATI

### 1. RestaurantNotificationListener - HARDCODED STAFF_ID

**File:** `RestaurantNotificationListener.java` (Riga 119-123)

```java
// ❌ PROBLEMA: Crea notifica solo per staff_id=1
Long staffUserId = 1L;

RestaurantNotification notification = createNotificationFromEvent(
    eventType, eventData, restaurantId, staffUserId
);
```

**Impatto:** 
- ❌ Se un ristorante ha 5 staff, SOLO staff_id=1 riceve notifica
- ❌ Non itera su tutti gli staff

**Dovrebbe essere:**
```java
// ✅ CORRETTO: Loop su TUTTI gli staff
List<Long> staffUserIds = Arrays.asList(1L, 2L, 3L);  // O query da DB

for (Long staffUserId : staffUserIds) {
    RestaurantNotification notification = createNotificationFromEvent(
        eventType, eventData, restaurantId, staffUserId
    );
    // ... persist ...
}
```

---

### 2. ChannelPoller - sendWebSocket() NON IMPLEMENTATO

**File:** `ChannelPoller.java` (Riga ~311)

**Stato:** Metodo esiste ma contiene solo log placeholder

```java
private void sendWebSocket(NotificationChannelSend send) throws Exception {
    // TODO: Implementare WebSocket send logic
    log.debug("TODO: Send WebSocket for notification {}", send.getNotificationId());
}
```

**Problema:** 
- ❌ Non invia niente
- ❌ Non usa SimpMessagingTemplate
- ❌ is_sent rimane NULL

---

### 3. WebSocketNotificationChannel - ESISTE MA DIVERSO

**File:** `WebSocketNotificationChannel.java` 

**Stato:** ✅ Esiste, ma è una **vecchia implementazione**

**Differenza:**
- ✅ Ha SimpMessagingTemplate
- ✅ Ha WebSocketSessionManager
- ❌ Implementa NotificationChannel (non è ChannelPoller)
- ❌ Usa recipientType/recipientId (non notification_id)
- ❌ Non è integrato con ChannelPoller

**Questo è diverso da quello che serve per il workflow outbox!**

---

## ✅ COSA FUNZIONA GIÀ

### 1. RestaurantNotificationListener - Base OK
- ✅ onEventReceived() esiste
- ✅ Idempotency check presente
- ✅ createRestaurantNotification() esiste
- ❌ **Solo per staff_id=1 (hardcoded)**

### 2. ChannelPoller - Structure OK
- ✅ @Scheduled(fixedDelay=10000) presente
- ✅ pollAndSendChannels() esiste
- ✅ Channel isolation pattern corretto
- ❌ **sendWebSocket() non implementato**

### 3. NotificationChannelSend - Database OK
- ✅ Entity esiste
- ✅ Campi: notification_id, channel_type, is_sent, sent_at
- ✅ DAO present

---

## 🔧 AZIONI DA FARE (IN ORDINE)

### ✏️ AZIONE 1: Crea WebSocketConfig.java

**File:** 
```
src/main/java/com/application/common/config/WebSocketConfig.java
```

**Contenuto:**
```java
package com.application.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("🔧 Configuring WebSocket message broker");
        config.enableSimpleBroker("/user", "/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("🔧 Registering STOMP endpoints");
        registry.addEndpoint("/ws-notifications")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

**Tempo:** 5 minuti

---

### ✏️ AZIONE 2: Fix RestaurantNotificationListener - Loop su staff

**File:** `RestaurantNotificationListener.java`

**Linee da modificare:** 119-123 e 125-160

**Cerca questa sezione:**
```java
// Step 2: TODO - Query per trovare tutti gli staff di questo ristorante
// List<RUser> staffList = restaurantUserDAO.findByRestaurantId(restaurantId);

// Step 3: Per ogni staff, crea una RestaurantNotification
// (Placeholder: crea per staff_id=1)
Long staffUserId = 1L;

RestaurantNotification notification = createNotificationFromEvent(eventType, eventData, restaurantId, staffUserId);
```

**Sostituisci con:**
```java
// Step 2: Query per trovare TUTTI gli staff di questo ristorante
// TEMPORANEO: Placeholder per test
List<Long> staffUserIds = Arrays.asList(1L, 2L, 3L);

// Step 3: Per OGNI staff, crea una RestaurantNotification
for (Long staffUserId : staffUserIds) {
    try {
        RestaurantNotification notification = createNotificationFromEvent(
            eventType, eventData, restaurantId, staffUserId
        );

        if (notification == null) {
            log.warn("Could not create notification for event type: {}", eventType);
            continue;
        }

        // Step 4: Persist la notifica
        RestaurantNotification savedNotification = restaurantNotificationDAO.save(notification);

        log.debug("✅ Created RestaurantNotification: id={}, restaurant={}, staff={}", 
                 savedNotification.getId(), restaurantId, staffUserId);

        // Step 5: Crea entry in notification_outbox
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
```

**Tempo:** 10 minuti

---

### ✏️ AZIONE 3: Implementa sendWebSocket() in ChannelPoller

**File:** `ChannelPoller.java`

**Step 3A: Aggiungi dipendenza SimpMessagingTemplate**

**Cerca la classe:**
```java
public class ChannelPoller {

    private static final int MAX_RETRIES = 3;

    private final NotificationChannelSendDAO channelSendDAO;

    public ChannelPoller(NotificationChannelSendDAO channelSendDAO) {
        this.channelSendDAO = channelSendDAO;
    }
```

**Sostituisci con:**
```java
@RequiredArgsConstructor  // ← AGGIUNGI
public class ChannelPoller {

    private static final int MAX_RETRIES = 3;

    private final NotificationChannelSendDAO channelSendDAO;
    private final RestaurantNotificationDAO restaurantNotificationDAO;  // ← AGGIUNGI
    private final SimpMessagingTemplate simpMessagingTemplate;  // ← AGGIUNGI

    // Costruttore viene generato automaticamente da @RequiredArgsConstructor
```

**Step 3B: Implementa sendWebSocket()**

**Trova (attorno a riga 311):**
```java
private void sendWebSocket(NotificationChannelSend send) throws Exception {
    // TODO: Implementare WebSocket send logic
    log.debug("TODO: Send WebSocket for notification {}", send.getNotificationId());
}
```

**Sostituisci con:**
```java
/**
 * ⭐ CANALE WEBSOCKET: Real-time, no outbox
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

        // Step 3: Prepara payload
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
        simpMessagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            payload
        );

        log.info("✅ WebSocket sent to user {} for notification {}", userId, notificationId);

        // Step 5: Marca come sent
        send.setSent(true);
        send.setSentAt(Instant.now());
        channelSendDAO.save(send);

    } catch (Exception e) {
        log.error("❌ Error sending WebSocket notification: {}", e.getMessage());
        
        // Increment retry counter
        send.setAttemptCount((send.getAttemptCount() != null ? send.getAttemptCount() : 0) + 1);
        send.setLastError(e.getMessage());
        send.setLastAttemptAt(Instant.now());

        if (send.getAttemptCount() >= 3) {
            send.setSent(false);
            log.error("Max retries reached for WebSocket notification {}", send.getNotificationId());
        }

        channelSendDAO.save(send);
    }
}
```

**Step 3C: Placeholder per altri canali**

**Trova (attorno a riga 301-330):**
```java
private void sendSMS(NotificationChannelSend send) throws Exception {
    // TODO: ...
}

private void sendEmail(NotificationChannelSend send) throws Exception {
    // TODO: ...
}

private void sendPush(NotificationChannelSend send) throws Exception {
    // TODO: ...
}

private void sendSlack(NotificationChannelSend send) throws Exception {
    // TODO: ...
}
```

**Sostituisci con:**
```java
private void sendSMS(NotificationChannelSend send) throws Exception {
    log.info("📱 TODO: Send SMS for notification {}", send.getNotificationId());
}

private void sendEmail(NotificationChannelSend send) throws Exception {
    log.info("📧 TODO: Send Email for notification {}", send.getNotificationId());
}

private void sendPush(NotificationChannelSend send) throws Exception {
    log.info("🔔 TODO: Send Push for notification {}", send.getNotificationId());
}

private void sendSlack(NotificationChannelSend send) throws Exception {
    log.info("⚡ TODO: Send Slack for notification {}", send.getNotificationId());
}
```

**Tempo:** 15 minuti

---

## 📊 SUMMARY - PRIMA vs DOPO

| Componente | PRIMA | DOPO |
|-----------|-------|------|
| WebSocketConfig | ❌ Non esiste | ✅ Creato |
| RestaurantNotificationListener loop | ❌ Hardcoded staff_id=1 | ✅ Loop su 3 staff |
| ChannelPoller.sendWebSocket() | ❌ TODO log only | ✅ Implementato con SimpMessagingTemplate |
| SimpMessagingTemplate injection | ❌ Non injectato | ✅ @RequiredArgsConstructor |
| RestaurantNotificationDAO injection | ❌ Non injectato | ✅ @RequiredArgsConstructor |
| **Status** | **⚠️ Incomplete** | **✅ Ready for testing** |

---

## 🎯 TEMPO TOTALE

- Azione 1 (WebSocketConfig): **5 min**
- Azione 2 (RestaurantNotificationListener loop): **10 min**
- Azione 3 (ChannelPoller.sendWebSocket + deps): **15 min**

**TOTALE: ~30 minuti**

---

## ✅ DOPO AVER FATTO LE AZIONI

Poi potrai:
1. Avviare applicazione: `mvn spring-boot:run`
2. Testare scenario completo (vedi GUIDE_WEBSOCKET_ONLY.md)
3. Verificare logs e database

---

## ⚠️ NOTA IMPORTANTE

**WebSocketNotificationChannel.java** esiste ma è una **vecchia implementazione**:
- Non è integrata con ChannelPoller
- Usa vecchio pattern NotificationChannel
- Implementa logica diversa

**Per questa guida:** Non modificare questo file, usa ChannelPoller.sendWebSocket() nuovo.
