# WebSocket Real-Time - Implementation Notes

## NotificationWebSocketSender Implementation

```java
@Service
public class NotificationWebSocketSender implements NotificationChannelSender {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public boolean sendRestaurantNotification(RestaurantUserNotification notif) {
        return sendInternal(
            notif.getId(),
            notif.getEventId(),
            notif.getUserId(),
            "restaurant",
            notif.getTitle(),
            notif.getBody(),
            notif.getProperties());
    }
    
    public boolean sendCustomerNotification(CustomerNotification notif) {
        return sendInternal(
            notif.getId(),
            notif.getEventId(),
            notif.getUserId(),
            "customer",
            notif.getTitle(),
            notif.getBody(),
            notif.getProperties());
    }
    
    private boolean sendInternal(Long notifId, String eventId, Long userId, String userType,
                                 String title, String body, Map<String, String> properties) {
        try {
            // NEW: Use /topic/{userType}/{userId}/notifications pattern to prevent ID collisions
            String destination = String.format("/topic/%s/%d/notifications", userType, userId);
            
            WebSocketMessage payload = WebSocketMessage.builder()
                .notificationId(notifId)
                .eventId(eventId)
                .title(title)
                .body(body)
                .timestamp(System.currentTimeMillis())
                .properties(properties)
                .build();
            
            messagingTemplate.convertAndSend(destination, payload);
            return true;
        } catch (Exception e) {
            log.debug("WebSocket send failed (client likely offline): {}", e.getMessage());
            return false;  // No retry
        }
    }
}

// DTO
@Data
@Builder
public class WebSocketMessage {
    private Long notificationId;
    private String eventId;
    private String title;
    private String body;
    private Long timestamp;
    private Map<String, String> properties;
}
```

## Integration with BaseNotificationListener

```java
@Service
public abstract class BaseNotificationListener {
    
    protected abstract List<? extends ANotification> disaggregate(String message);
    protected abstract void attemptWebSocketSend(ANotification notification);
    
    @RabbitListener(queues = "notification.restaurant")
    public void onNotificationMessage(Message message, Channel channel) throws Exception {
        String messageBody = new String(message.getBody());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            List<? extends ANotification> notifications = disaggregate(messageBody);
            
            for (ANotification notif : notifications) {
                // Persist to database
                persistNotification(notif);
                
                // Attempt WebSocket send (synchronous, best-effort)
                attemptWebSocketSend(notif);
            }
            
            // ACK after all persistence complete
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // NACK and retry
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

// Concrete implementation
@Service
public class RestaurantNotificationListener extends BaseNotificationListener {
    
    private final NotificationWebSocketSender webSocketSender;
    
    @Override
    protected void attemptWebSocketSend(ANotification notification) {
        RestaurantUserNotification rnm = (RestaurantUserNotification) notification;
        webSocketSender.sendRestaurantNotification(rnm);
    }
}
```

## WebSocket Topic Structure

```
/topic/{userType}/{userId}/notifications

⭐ userType values: "restaurant" | "customer" | "agency" | "admin"

Examples:
- /topic/restaurant/50/notifications
- /topic/customer/50/notifications  (different user, same ID=50)
- /topic/agency/78/notifications
- /topic/admin/1/notifications

Security: WebSocketChannelInterceptor validates destination ownership
         Prevents routing to wrong user even if userIds collide across tables
```

## Reservations Topic (restaurant-scoped)

```
/topic/restaurant/{restaurantId}/reservations

⭐ This topic is restaurant-scoped (uses restaurantId, NOT userId). All staff subscribe to the restaurant reservations topic to receive full list updates (reservation created, accepted, rejected). For badge counts (campanella) you can still send per-user small notifications on `/topic/{userType}/{userId}/notifications`.
```

## Connection Lifecycle

```
1. Client connects: WebSocket upgrade + JWT handshake
2. Client subscribes: STOMP SUBSCRIBE to /topic/notifications/{id}/{type}
3. Listener validates via WebSocketChannelInterceptor
4. Messages flow to /topic/... → delivered to subscribed client
5. Client disconnects: STOMP DISCONNECT
6. Spring STOMP broker cleans up session
```

## Transaction Boundaries

```
@Transactional
protected void processNotificationMessage(...) {
    // 1. Disaggregate (stays inside transaction)
    
    // 2. Persist each notification (INSERT, inside transaction)
    persistNotification(notification);
    
    // 3. WebSocket send (OUTSIDE transaction - best-effort)
    attemptWebSocketSend(notification);
    
    // If WebSocket fails: notification already in DB ✅ (correct)
    // If DB fails: WebSocket send skipped ✅ (correct)
}
```

## Error Handling

- **Client offline**: send() fails, logs at DEBUG level, continues
- **Invalid JWT**: Handshake fails, HTTP 401
- **Wrong destination**: Subscribe fails, STOMP ERROR frame
- **Network error**: Socket close handled by Spring STOMP broker

## Monitoring & Metrics

```java
// Track WebSocket metrics:
- Active connections (WebSocketSessionManager)
- Messages sent per minute
- Failed send attempts
- Connection duration histogram
```

---

**Document Version**: 1.0  
**Component**: WebSocket Real-Time Delivery
