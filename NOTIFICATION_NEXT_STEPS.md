# 🔜 PROSSIMI STEP - RabbitMQ & CHANNEL IMPLEMENTATION

**Ultimo Status:** Implementazione Core COMPLETATA (1500+ lines)  
**Prossima Fase:** Configurazione RabbitMQ + Channel Send Methods  
**Data:** November 12, 2025

---

## 📋 TODO Lista - Prossimi 12 ore

### ⏰ FASE 1: RabbitMQ Configuration (2-3 ore)

#### Step 1.1: Creare RabbitMQNotificationConfig.java
**Location:** `com/application/common/config/RabbitMQNotificationConfig.java`

```java
@Configuration
public class RabbitMQNotificationConfig {
    
    // ===== EXCHANGES =====
    @Bean
    public TopicExchange eventStreamExchange() {
        return new TopicExchange("event-stream", true, false);
    }
    
    @Bean
    public TopicExchange notificationChannelSendExchange() {
        return new TopicExchange("notification-channel-send", true, false);
    }
    
    // ===== QUEUES =====
    @Bean
    public Queue eventStreamQueue() {
        return new Queue("event-stream-queue", true);
    }
    
    @Bean
    public Queue notificationChannelSendQueue() {
        return new Queue("notification-channel-send-queue", true);
    }
    
    // ===== BINDINGS =====
    @Bean
    public Binding eventStreamBinding(Queue eventStreamQueue, TopicExchange eventStreamExchange) {
        return BindingBuilder.bind(eventStreamQueue)
            .to(eventStreamExchange)
            .with("event.*");
    }
    
    @Bean
    public Binding notificationChannelSendBinding(Queue notificationChannelSendQueue, TopicExchange notificationChannelSendExchange) {
        return BindingBuilder.bind(notificationChannelSendQueue)
            .to(notificationChannelSendExchange)
            .with("notification.*");
    }
}
```

#### Step 1.2: Add @RabbitListener to 4 Listeners

In `AdminNotificationListener.java`:
```java
@RabbitListener(queues = "event-stream-queue")
@Transactional
public void onEventReceived(String eventPayload) {
    // Existing implementation
}
```

**Repeat for:**
- RestaurantNotificationListener
- CustomerNotificationListener
- AgencyNotificationListener

#### Step 1.3: Update application.yml

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        max-concurrency: 10
        prefetch: 1
```

#### Step 1.4: Test RabbitMQ Connection
```bash
# Start RabbitMQ (Docker)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# Check management UI: http://localhost:15672 (guest/guest)
```

---

### ⏰ FASE 2: Implementa Channel Send Methods (4-6 ore)

Location: `com/application/common/service/notification/poller/ChannelPoller.java`

Replace the 5 TODO methods:

#### Step 2.1: sendSMS()

**Option A: AWS SNS**
```java
private void sendSMS(NotificationChannelSend send) {
    Notification notif = notificationRepo.findById(send.getNotificationId()).orElseThrow();
    User user = userRepo.findById(notif.getUserId()).orElseThrow();
    
    if (user.getPhoneNumber() == null) {
        throw new ChannelException("User non ha telefono registrato");
    }
    
    try {
        PublishRequest request = PublishRequest.builder()
            .topicArn(snsTopicArn)
            .phoneNumber(user.getPhoneNumber())
            .message(notif.getTitle() + ": " + notif.getBody())
            .build();
        
        snsClient.publish(request);
        
        log.info("SMS sent to {} for notification {}", user.getPhoneNumber(), notif.getId());
    } catch (SnsException e) {
        throw new ChannelException("SMS send failed: " + e.getMessage());
    }
}
```

**Option B: Twilio**
```java
private void sendSMS(NotificationChannelSend send) {
    Notification notif = notificationRepo.findById(send.getNotificationId()).orElseThrow();
    User user = userRepo.findById(notif.getUserId()).orElseThrow();
    
    if (user.getPhoneNumber() == null) {
        throw new ChannelException("User non ha telefono registrato");
    }
    
    try {
        Message message = Message.creator(
            new PhoneNumber("+1" + user.getPhoneNumber()),  // To number
            new PhoneNumber(twilioPhoneNumber),  // From number
            notif.getTitle() + ": " + notif.getBody()
        ).create();
        
        log.info("SMS sent via Twilio: {}", message.getSid());
    } catch (TwilioException e) {
        throw new ChannelException("SMS send failed: " + e.getMessage());
    }
}
```

#### Step 2.2: sendEmail()

**Option A: JavaMailSender**
```java
private void sendEmail(NotificationChannelSend send) {
    Notification notif = notificationRepo.findById(send.getNotificationId()).orElseThrow();
    User user = userRepo.findById(notif.getUserId()).orElseThrow();
    
    if (user.getEmail() == null) {
        throw new ChannelException("User non ha email registrato");
    }
    
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(notif.getTitle());
        message.setText(notif.getBody());
        message.setFrom("noreply@greedys.com");
        
        mailSender.send(message);
        
        log.info("Email sent to {} for notification {}", user.getEmail(), notif.getId());
    } catch (Exception e) {
        throw new ChannelException("Email send failed: " + e.getMessage());
    }
}
```

**Option B: SendGrid**
```java
private void sendEmail(NotificationChannelSend send) {
    Notification notif = notificationRepo.findById(send.getNotificationId()).orElseThrow();
    User user = userRepo.findById(notif.getUserId()).orElseThrow();
    
    if (user.getEmail() == null) {
        throw new ChannelException("User non ha email registrato");
    }
    
    try {
        Mail mail = new Mail(
            new Email("noreply@greedys.com"),
            notif.getTitle(),
            new Email(user.getEmail()),
            new Content("text/html", notif.getBody())
        );
        
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);
        
        log.info("Email sent via SendGrid: {}", response.getStatusCode());
    } catch (IOException e) {
        throw new ChannelException("Email send failed: " + e.getMessage());
    }
}
```

#### Step 2.3: sendPush()

**Firebase Cloud Messaging**
```java
private void sendPush(NotificationChannelSend send) {
    Notification notif = notificationRepo.findById(send.getNotificationId()).orElseThrow();
    User user = userRepo.findById(notif.getUserId()).orElseThrow();
    
    List<String> deviceTokens = userDeviceTokenRepo.findTokensByUserId(user.getId());
    
    if (deviceTokens.isEmpty()) {
        throw new ChannelException("User non ha device token registrato");
    }
    
    try {
        for (String token : deviceTokens) {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(new com.google.firebase.messaging.Notification(
                    notif.getTitle(),
                    notif.getBody()
                ))
                .putData("notificationId", notif.getId().toString())
                .build();
            
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push sent to {} via Firebase: {}", token, response);
        }
    } catch (FirebaseMessagingException e) {
        throw new ChannelException("Push send failed: " + e.getMessage());
    }
}
```

#### Step 2.4: sendWebSocket()

**Spring WebSocket**
```java
private void sendWebSocket(NotificationChannelSend send) {
    Notification notif = notificationRepo.findById(send.getNotificationId()).orElseThrow();
    User user = userRepo.findById(notif.getUserId()).orElseThrow();
    
    try {
        NotificationMessage message = NotificationMessage.builder()
            .notificationId(notif.getId())
            .title(notif.getTitle())
            .body(notif.getBody())
            .timestamp(Instant.now())
            .build();
        
        // Broadcast to user's WebSocket connection
        webSocketService.sendToUser(user.getId(), message);
        
        log.info("WebSocket message sent to user {}", user.getId());
    } catch (Exception e) {
        throw new ChannelException("WebSocket send failed: " + e.getMessage());
    }
}
```

#### Step 2.5: sendSlack()

**Slack API**
```java
private void sendSlack(NotificationChannelSend send) {
    Notification notif = notificationRepo.findById(send.getNotificationId()).orElseThrow();
    
    // Solo per ADMIN_USER
    if (!notif.getUserType().equals("ADMIN_USER")) {
        throw new ChannelException("Slack is only for admin notifications");
    }
    
    try {
        SlackMessage slackMessage = SlackMessage.builder()
            .channel("#notifications")
            .username("Greedy's Bot")
            .text(notif.getTitle() + ": " + notif.getBody())
            .iconEmoji(":bell:")
            .build();
        
        slackService.send(slackMessage);
        
        log.info("Slack message sent for notification {}", notif.getId());
    } catch (Exception e) {
        throw new ChannelException("Slack send failed: " + e.getMessage());
    }
}
```

---

### ⏰ FASE 3: Integration Testing (3-4 ore)

#### Step 3.1: Create NotificationIntegrationTest.java

```java
@SpringBootTest
@ActiveProfiles("test")
class NotificationIntegrationTest {
    
    @Autowired EventOutboxDAO eventOutboxDAO;
    @Autowired NotificationOutboxDAO notificationOutboxDAO;
    @Autowired AdminNotificationDAO adminNotificationDAO;
    @Autowired RabbitTemplate rabbitTemplate;
    
    @Test
    void testEndToEndFlow() throws InterruptedException {
        // 1. Create domain event
        EventOutbox event = EventOutbox.builder()
            .eventId("test-123")
            .eventType("RESERVATION_REQUESTED")
            .aggregateType("RESERVATION")
            .aggregateId(1L)
            .payload("{...}")
            .status(Status.PENDING)
            .build();
        eventOutboxDAO.save(event);
        
        // 2. Wait for poller
        Thread.sleep(6000);
        
        // 3. Verify event published
        assertThat(eventOutboxDAO.findById("test-123").get().getStatus())
            .isEqualTo(Status.PROCESSED);
        
        // 4. Wait for listener
        Thread.sleep(1000);
        
        // 5. Verify notification created
        List<AdminNotification> notifications = adminNotificationDAO.findAll();
        assertThat(notifications).isNotEmpty();
        
        // 6. Wait for channel poller
        Thread.sleep(11000);
        
        // 7. Verify channels sent
        // Check SMS, Email, Push, WebSocket, Slack channels
    }
    
    @Test
    void testChannelIsolation() {
        // Create notification
        // Set SMS to fail
        // Verify Email/Push/WS/Slack still send
    }
    
    @Test
    void testIdempotency() {
        // Send same event twice
        // Verify notification created only once
    }
}
```

---

## 📊 Checklist Implementazione

### FASE 1: RabbitMQ
- [ ] Create RabbitMQNotificationConfig.java
- [ ] Add @RabbitListener to all 4 listeners
- [ ] Update application.yml with RabbitMQ config
- [ ] Test RabbitMQ connection (docker start)
- [ ] Verify exchanges & queues created

### FASE 2: Channel Implementations
- [ ] Implement sendSMS() (choose Twilio/AWS SNS)
- [ ] Implement sendEmail() (choose JavaMailSender/SendGrid)
- [ ] Implement sendPush() (Firebase)
- [ ] Implement sendWebSocket() (Spring WebSocket)
- [ ] Implement sendSlack() (Slack API)
- [ ] Add @Autowired per each service

### FASE 3: Testing
- [ ] Create integration test for end-to-end flow
- [ ] Test channel isolation (SMS fails, others continue)
- [ ] Test idempotency (duplicate events)
- [ ] Test retry logic (max retries = 3)
- [ ] Load test: send 1000 notifications

### FASE 4: Monitoring (BONUS)
- [ ] Add actuator endpoints
- [ ] Expose metrics (pending count, failed count, latency)
- [ ] Create dashboard (Grafana)
- [ ] Setup alerts (failed channels, high retry)

---

## ⚙️ Configuration Properties Needed

**application.yml** (add to existing):
```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    listener:
      simple:
        max-concurrency: 10
        prefetch: 1

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

notification:
  channels:
    sms:
      provider: twilio  # or 'aws-sns'
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      phone-number: ${TWILIO_PHONE_NUMBER}
    
    push:
      firebase:
        project-id: ${FIREBASE_PROJECT_ID}
        credentials-path: ${FIREBASE_CREDENTIALS_PATH}
    
    slack:
      webhook-url: ${SLACK_WEBHOOK_URL}
      enabled: true
```

---

## 🎯 Estimated Timeline

| Fase | Task | Duration | Cumulative |
|------|------|----------|-----------|
| 1.1 | RabbitMQ Config | 1 hour | 1h |
| 1.2 | Add @RabbitListener | 30 min | 1.5h |
| 1.3 | Update YAML | 30 min | 2h |
| 1.4 | Test Connection | 1 hour | 3h |
| 2.1 | sendSMS() | 1 hour | 4h |
| 2.2 | sendEmail() | 1 hour | 5h |
| 2.3 | sendPush() | 1 hour | 6h |
| 2.4 | sendWebSocket() | 1 hour | 7h |
| 2.5 | sendSlack() | 1 hour | 8h |
| 3.1 | Integration Tests | 2 hours | 10h |
| 3.2 | Load Testing | 1 hour | 11h |
| 3.3 | Documentation | 1 hour | 12h |

**Total: ~12 hours**

---

## 📝 Notes

1. **RabbitMQ Docker:**
   ```bash
   docker run -d --name rabbitmq \
     -p 5672:5672 \
     -p 15672:15672 \
     -e RABBITMQ_DEFAULT_USER=guest \
     -e RABBITMQ_DEFAULT_PASS=guest \
     rabbitmq:3-management
   ```

2. **Choose Channel Providers:**
   - SMS: Twilio (easy) or AWS SNS (integrated)
   - Email: JavaMailSender (simple) or SendGrid (reliable)
   - Push: Firebase (free tier)
   - WebSocket: Spring built-in
   - Slack: Free API

3. **Error Handling:**
   - All methods throw `ChannelException`
   - ChannelPoller catches and increments attempt_count
   - After 3 retries, marks as failed (is_sent=false)

4. **Testing Strategy:**
   - Use TestContainers for RabbitMQ
   - Mock external services (SMS, Email, Push)
   - Test real RabbitMQ flow
   - Verify isolation: SMS fail, others continue

---

**Ready to proceed? Start with FASE 1: RabbitMQ Configuration**

