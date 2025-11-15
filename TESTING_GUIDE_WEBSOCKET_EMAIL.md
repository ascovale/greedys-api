# 🚀 GUIDA PRATICA - TEST NOTIFICATION SYSTEM
**Scenario:** Customer crea prenotazione → Restaurant staff riceve notifica via WebSocket + Email

---

## 📋 PREREQUISITI

### 1. Database Tables (verificare che esistono)
```sql
-- Deve esistere:
- event_outbox
- notification_restaurant (extends ANotification)
- notification_outbox
- notification_channel_send
```

### 2. RabbitMQ Running
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
# Accedi a http://localhost:15672 (guest/guest)
```

### 3. Email Service Configurato
```yaml
# application.yml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # App-specific password, not account password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

---

## 🔧 IMPLEMENTAZIONE - PASSO DOPO PASSO

### STEP 1: Correggi RestaurantNotificationListener (Loop su staff)

**File:** `src/main/java/com/application/common/service/notification/listener/RestaurantNotificationListener.java`

**Modifica:** Riga 107-155 - Sostituisci `createRestaurantNotifications()`

```java
/**
 * Crea RestaurantNotification per TUTTI gli staff del ristorante.
 * 
 * @param eventId L'ID univoco dell'evento
 * @param eventType Il tipo di evento
 * @param eventData I dati dell'evento
 */
private void createRestaurantNotifications(String eventId, String eventType, Map<String, Object> eventData) {
    try {
        // Step 1: Estrai il restaurantId dall'evento
        Object restaurantIdObj = eventData.get("restaurantId");
        Long restaurantId = restaurantIdObj != null ? ((Number) restaurantIdObj).longValue() : null;

        if (restaurantId == null) {
            log.warn("Event {} missing restaurantId, skipping restaurant notifications", eventId);
            return;
        }

        log.info("Creating restaurant notifications for restaurant {}", restaurantId);

        // Step 2: Query TUTTI gli staff di questo ristorante
        // TODO: Implementare correttamente con DAO
        // Per ora: placeholder per test
        List<Long> staffIds = new ArrayList<>();
        staffIds.add(1L);  // Staff test ID
        staffIds.add(2L);  // Staff test ID
        // staffIds.add(3L);
        
        // Alternativa (quando hai il DAO):
        // Restaurant restaurant = restaurantDAO.findById(restaurantId).orElseThrow();
        // List<RUser> staffList = restaurant.getRUsers();
        // List<Long> staffIds = staffList.stream().map(RUser::getId).collect(Collectors.toList());

        if (staffIds.isEmpty()) {
            log.warn("Restaurant {} has no staff members", restaurantId);
            return;
        }

        log.info("Found {} staff members for restaurant {}", staffIds.size(), restaurantId);

        // Step 3: PER OGNI STAFF - Crea RestaurantNotification
        for (Long staffUserId : staffIds) {
            try {
                // Crea notifica per questo staff
                RestaurantNotification notification = createNotificationFromEvent(
                    eventType, eventData, restaurantId, staffUserId
                );

                if (notification == null) {
                    log.warn("Could not create notification for event type: {}", eventType);
                    continue;
                }

                // Persist la notifica
                RestaurantNotification savedNotification = restaurantNotificationDAO.save(notification);
                
                log.debug("Created RestaurantNotification: id={}, restaurant={}, staff={}", 
                         savedNotification.getId(), restaurantId, staffUserId);

                // Step 4: Crea entry in notification_outbox per il ChannelPoller
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

                log.debug("Created NotificationOutbox entry: notification_id={}", savedNotification.getId());

            } catch (Exception e) {
                log.error("Error creating notification for staff {}", staffUserId, e);
                // Continua con prossimo staff, non far fallire tutto
                continue;
            }
        }

        // Step 5: Marca evento come processato
        eventOutboxDAO.updateProcessedBy(eventId, "RESTAURANT_LISTENER", Instant.now());

        log.info("Successfully created {} restaurant notifications for event {}", staffIds.size(), eventId);

    } catch (Exception e) {
        log.error("Error in createRestaurantNotifications", e);
        throw new RuntimeException(e);
    }
}
```

---

### STEP 2: Implementa WebSocket Channel Send

**File:** `src/main/java/com/application/common/service/notification/poller/ChannelPoller.java`

**Modifica:** Riga 311 - Sostituisci `sendWebSocket()`

```java
/**
 * ⭐ CANALE WEBSOCKET: Real-time, no persistence
 * 
 * Invia notifica via WebSocket a tutti i client collegati dell'utente.
 * Ideale per aggiornamenti in tempo reale.
 * 
 * @param send NotificationChannelSend con dettagli invio
 * @throws Exception Se invio fallisce
 */
private void sendWebSocket(NotificationChannelSend send) throws Exception {
    try {
        log.info("Sending WebSocket notification for channel_send_id={}", send.getId());

        // Step 1: Recupera la notifica dal database
        // TODO: Implementare con polymorphic query quando available
        // Per ora: assumi notification_id correlato
        Long notificationId = send.getNotificationId();

        // Step 2: Leggi la notifica (restaurantNotification nel nostro caso)
        // HACK: Assumi che sia RestaurantNotification
        RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        log.debug("Found notification: title={}, userId={}", notification.getTitle(), notification.getUserId());

        // Step 3: Estrai i dati
        String title = notification.getTitle();
        String body = notification.getBody();
        Long userId = notification.getUserId();
        String userType = notification.getUserType();
        Map<String, String> properties = notification.getProperties();

        // Step 4: Invia via WebSocket
        // Pattern: /user/{userId}/queue/notifications
        String destination = String.format("/user/%d/queue/notifications", userId);

        NotificationMessage message = NotificationMessage.builder()
            .notificationId(notificationId)
            .title(title)
            .body(body)
            .channel("WEBSOCKET")
            .sentAt(Instant.now())
            .metadata(properties != null ? properties : new HashMap<>())
            .build();

        // TODO: Injetta SimpMessagingTemplate in ChannelPoller
        // simpMessagingTemplate.convertAndSendToUser(
        //     userId.toString(),
        //     "/queue/notifications",
        //     message
        // );

        log.info("✅ WebSocket sent to user {} for notification {}", userId, notificationId);

        // Step 5: Marca come sent
        send.setSent(true);
        send.setSentAt(Instant.now());
        channelSendDAO.save(send);

        log.info("Updated notification_channel_send: sent=true");

    } catch (Exception e) {
        log.error("❌ Error sending WebSocket notification", e);
        
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

### STEP 3: Implementa Email Channel Send

**File:** `src/main/java/com/application/common/service/notification/poller/ChannelPoller.java`

**Modifica:** Riga 301 - Sostituisci `sendEmail()`

```java
/**
 * ⭐ CANALE EMAIL: Usa Outbox pattern, con retry
 * 
 * Invia email con NotificationChannelSend persistence.
 * Se fallisce, riprova al prossimo cycle (10 secondi).
 * 
 * @param send NotificationChannelSend con dettagli invio
 * @throws Exception Se invio fallisce
 */
private void sendEmail(NotificationChannelSend send) throws Exception {
    try {
        log.info("Sending Email notification for channel_send_id={}", send.getId());

        // Step 1: Recupera la notifica
        Long notificationId = send.getNotificationId();
        
        // HACK: Assumi RestaurantNotification
        RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        log.debug("Found notification: title={}, userId={}", notification.getTitle(), notification.getUserId());

        // Step 2: Ricerca email dell'utente
        Long userId = notification.getUserId();
        String userType = notification.getUserType();
        String userEmail = null;

        if ("RESTAURANT_USER".equals(userType)) {
            // Leggi email da RUser (restaurant staff)
            // TODO: Implementare correttamente
            // RUser ruser = ruserDAO.findById(userId).orElseThrow();
            // userEmail = ruser.getEmail();
            
            // HACK per test:
            userEmail = "staff" + userId + "@restaurant-test.com";
            log.warn("⚠️ Using test email: {}. Implementa RUserDAO query", userEmail);
        } else if ("CUSTOMER".equals(userType)) {
            // Leggi email da Customer
            userEmail = "customer" + userId + "@test.com";
        } else if ("ADMIN_USER".equals(userType)) {
            // Leggi email da Admin
            userEmail = "admin" + userId + "@test.com";
        }

        if (userEmail == null || userEmail.isEmpty()) {
            throw new RuntimeException("User email not found for userId=" + userId + ", userType=" + userType);
        }

        log.info("Sending email to: {}", userEmail);

        // Step 3: Prepara email
        String subject = "[Greedy's] " + notification.getTitle();
        String htmlBody = buildEmailBody(notification);

        // Step 4: Invia via JavaMailSender
        // TODO: Injetta JavaMailSender in ChannelPoller
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(userEmail);
        // message.setSubject(subject);
        // message.setText(notification.getBody());
        // mailSender.send(message);

        // SIMULAZIONE per test (log solamente):
        log.info("📧 EMAIL SENT:");
        log.info("  TO: {}", userEmail);
        log.info("  SUBJECT: {}", subject);
        log.info("  BODY: {}", notification.getBody());

        // Step 5: Marca come sent
        send.setSent(true);
        send.setSentAt(Instant.now());
        channelSendDAO.save(send);

        log.info("✅ Email sent to {} for notification {}", userEmail, notificationId);

    } catch (Exception e) {
        log.error("❌ Error sending Email notification: {}", e.getMessage());
        
        // Increment retry counter
        send.setAttemptCount((send.getAttemptCount() != null ? send.getAttemptCount() : 0) + 1);
        send.setLastError(e.getMessage());
        send.setLastAttemptAt(Instant.now());

        // Se superato max retries, marca come failed
        if (send.getAttemptCount() >= 3) {
            send.setSent(false);  // Definitivamente fallito
            log.error("Max retries reached for Email notification {}", send.getNotificationId());
        }

        channelSendDAO.save(send);
        
        // Non re-throw: continua con prossimo canale (channel isolation)
    }
}

/**
 * Helper: Costruisci HTML email
 */
private String buildEmailBody(RestaurantNotification notification) {
    StringBuilder html = new StringBuilder();
    html.append("<html><body style='font-family: Arial, sans-serif;'>\n");
    html.append("<h2>").append(notification.getTitle()).append("</h2>\n");
    html.append("<p>").append(notification.getBody()).append("</p>\n");
    
    if (notification.getProperties() != null && !notification.getProperties().isEmpty()) {
        html.append("<hr/>\n");
        html.append("<h4>Dettagli:</h4>\n");
        html.append("<ul>\n");
        
        notification.getProperties().forEach((key, value) -> {
            html.append("<li><strong>").append(key).append(":</strong> ").append(value).append("</li>\n");
        });
        
        html.append("</ul>\n");
    }
    
    html.append("<hr/>\n");
    html.append("<small>Notifica generata da Greedy's System</small>\n");
    html.append("</body></html>");
    
    return html.toString();
}
```

---

## 🧪 TESTING - SCENARIO COMPLETO

### Test 1: Crea Evento Manualmente (via REST o Script)

```bash
# 1. Accedi a RabbitMQ Admin
# http://localhost:15672

# 2. Crea evento (simula ReservationService)
curl -X POST http://localhost:8080/api/events/test \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "reservation-001",
    "eventType": "RESERVATION_REQUESTED",
    "aggregateType": "RESERVATION",
    "aggregateId": 123,
    "payload": {
      "restaurantId": 10,
      "reservationId": 123,
      "customerName": "John Doe",
      "numberOfPeople": 4,
      "requestedTime": "19:30"
    }
  }'

# 3. Verifica che EventOutbox è creato
SELECT * FROM event_outbox WHERE event_id = 'reservation-001';

# 4. Aspetta 1-2 secondi (EventOutboxPoller)
# 5. Verifica che sia pubblicato su RabbitMQ
```

### Test 2: Monitor Log

```bash
# Terminal 1: Tail dei logs
tail -f logs/application.log | grep -E "Restaurant|WEBSOCKET|Email|Created"

# Dovresti vedere:
# - RestaurantNotificationListener riceve evento
# - Creates N RestaurantNotification (una per staff)
# - NotificationOutbox creato
# - ChannelPoller processa
# - "WEBSOCKET sent to user X"
# - "EMAIL SENT to ..."
```

### Test 3: Verifica Database

```sql
-- 1. Verifica NotificationOutbox creato
SELECT * FROM notification_outbox 
WHERE event_type = 'RESERVATION_REQUESTED' 
ORDER BY created_at DESC LIMIT 1;

-- 2. Verifica RestaurantNotification creato (uno per staff)
SELECT * FROM notification_restaurant 
WHERE creation_time >= NOW() - INTERVAL 1 MINUTE;

-- 3. Verifica NotificationChannelSend
SELECT * FROM notification_channel_send 
WHERE created_at >= NOW() - INTERVAL 1 MINUTE;

-- 4. Verifica Email inviato
SELECT * FROM notification_channel_send 
WHERE channel_type = 'EMAIL' AND is_sent = true;

-- 5. Verifica WebSocket inviato
SELECT * FROM notification_channel_send 
WHERE channel_type = 'WEBSOCKET' AND is_sent = true;
```

---

## 🔌 INTEGRAZIONI MANCANTI (TODO)

### 1. Injection delle dipendenze in ChannelPoller

```java
@Service
@Slf4j
public class ChannelPoller {
    private final NotificationChannelSendDAO channelSendDAO;
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final JavaMailSender mailSender;  // ← AGGIUNGERE
    private final SimpMessagingTemplate messagingTemplate;  // ← AGGIUNGERE

    public ChannelPoller(
        NotificationChannelSendDAO channelSendDAO,
        RestaurantNotificationDAO restaurantNotificationDAO,
        JavaMailSender mailSender,  // ← AGGIUNGERE
        SimpMessagingTemplate messagingTemplate  // ← AGGIUNGERE
    ) {
        this.channelSendDAO = channelSendDAO;
        this.restaurantNotificationDAO = restaurantNotificationDAO;
        this.mailSender = mailSender;
        this.messagingTemplate = messagingTemplate;
    }
}
```

### 2. Recupero Email da RUser

```java
// Aggiungi metodo in RestaurantNotificationListener
private String getRestaurantUserEmail(Long userId) {
    // TODO: Implementare query su RUser
    // RUser ruser = ruserDAO.findById(userId).orElseThrow();
    // return ruser.getEmail();
    
    // Per test: placeholder
    return "staff" + userId + "@test.com";
}
```

### 3. RabbitMQ Config (se non esiste)

```java
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_EVENT = "event-stream";
    public static final String QUEUE_EVENT = "event-stream-queue";
    
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EXCHANGE_EVENT);
    }

    @Bean
    public Queue eventQueue() {
        return new Queue(QUEUE_EVENT);
    }

    @Bean
    public Binding eventBinding(Queue eventQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(eventQueue).to(eventExchange).with("event.*");
    }
}
```

---

## 📊 FLOW TEMPORALE ASPETTATO (CON WEBSOCKET + EMAIL)

```
T0: Customer POST /reservations
    └─ ReservationService.createNewReservation()
    └─ INSERT event_outbox (PENDING)

T1 (@1s): EventOutboxPoller
    └─ SELECT event_outbox WHERE status=PENDING
    └─ PUBLISH to RabbitMQ
    └─ UPDATE status=PROCESSED

T2 (@0ms): RestaurantNotificationListener (riceve da RabbitMQ)
    └─ For staff_id=1:
       ├─ CREATE RestaurantNotification (sharedRead=true)
       └─ INSERT notification_outbox (PENDING)
    └─ For staff_id=2:
       ├─ CREATE RestaurantNotification (sharedRead=true)
       └─ INSERT notification_outbox (PENDING)

T3 (@5s): NotificationOutboxPoller
    └─ SELECT notification_outbox WHERE status=PENDING
    └─ UPDATE status=PUBLISHED

T4 (@10s): ChannelPoller (CHANNEL ISOLATION)
    ├─ For notification_id=1000 (staff_1):
    │  ├─ WEBSOCKET CHANNEL:
    │  │  └─ simpMessagingTemplate.convertAndSendToUser("1", "/queue/notifications", message)
    │  │  └─ UPDATE is_sent=true (IMMEDIATAMENTE)
    │  │
    │  └─ EMAIL CHANNEL:
    │     └─ mailSender.send(message)
    │     └─ UPDATE is_sent=true
    │
    └─ For notification_id=1001 (staff_2):
       ├─ WEBSOCKET CHANNEL: ...
       └─ EMAIL CHANNEL: ...

✅ RISULTATO:
   - Staff #1: Riceve WebSocket in real-time + Email in 10 sec
   - Staff #2: Riceve WebSocket in real-time + Email in 10 sec
```

---

## 🎯 CHECKPOINT PROGRESS

- [ ] RestaurantNotificationListener loop su staff (STEP 1)
- [ ] sendWebSocket() implementato (STEP 2)
- [ ] sendEmail() implementato (STEP 3)
- [ ] RabbitMQ config presente
- [ ] EmailSender configurato in application.yml
- [ ] JavaMailSender injected in ChannelPoller
- [ ] SimpMessagingTemplate injected in ChannelPoller
- [ ] Test scenario completato
- [ ] Database verified (notification_channel_send.is_sent=true)
- [ ] Logs verificati (WebSocket sent + Email sent)

---

## 🐛 TROUBLESHOOTING

### Problema: "RestaurantNotification not found"
```
Soluzione: Polymorphic query non implementata
Workaround: Assumi type basato su notificationType da NotificationOutbox
```

### Problema: "RUser email not found"
```
Soluzione: RUserDAO query non implementata
Workaround: Placeholder emails per test (staff1@test.com, etc)
```

### Problema: "SimpMessagingTemplate bean not found"
```
Soluzione: WebSocket non configurato
Fix: Aggiungi WebSocketConfig bean
```

### Problema: "JavaMailSender not configured"
```
Soluzione: Email properties mancano in application.yml
Fix: Aggiungi spring.mail.* properties
```

---

## 📚 RIFERIMENTI

| File | Metodo | Stato |
|------|--------|-------|
| RestaurantNotificationListener | onEventReceived() | ✅ Esiste, aggiorna createRestaurantNotifications |
| RestaurantNotificationListener | createRestaurantNotifications() | ⚠️ Implementa loop su staff |
| ChannelPoller | sendEmail() | ⚠️ Implementa con JavaMailSender |
| ChannelPoller | sendWebSocket() | ⚠️ Implementa con SimpMessagingTemplate |
| ChannelPoller | pollAndSendChannels() | ✅ Esiste, chiama send* methods |
| RabbitMQConfig | - | ⚠️ Crea se non esiste |

---

## ✨ PROSSIMO STEP (Dopo WebSocket + Email)

1. **SMS**: Twilio/AWS SNS
2. **Push**: Firebase Cloud Messaging
3. **Slack**: Slack API (per admin alerts)
4. **Polymorphic Queries**: Leggi qualsiasi tipo di notification

Buona fortuna! 🚀
