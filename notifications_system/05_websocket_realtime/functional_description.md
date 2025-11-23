# WebSocket Real-Time - Functional & Implementation

## Functional Description

Delivers notifications in real-time to connected clients via WebSocket/STOMP.

**Purpose**: Immediate notification delivery (100-500ms latency)

**Delivery**: Best-effort, no retry if client offline

**Topics** (with userType prefix to prevent ID collisions):
- `/topic/restaurant/{userId}/notifications`
- `/topic/customer/{userId}/notifications`
- `/topic/agency/{userId}/notifications`
- `/topic/admin/{userId}/notifications`

**Reservations (restaurant-scoped)**:
- `/topic/restaurant/{restaurantId}/reservations`  ← used for reservation list updates (create/accept/reject)

⭐ **WHY restaurantId for reservations?** The reservations list is a restaurant-level resource; all staff subscribe to the restaurant reservations topic to see list changes. Badge counts (campanella) can still be delivered per-user via the `/notifications` topic.

⭐ **WHY userType in path?** Different user tables (RUser, Customer, AgencyUser, Admin) have independent auto-increment IDs.
userId=50 could be both a restaurant user AND a customer user. Including userType prevents routing to wrong user.

**Timing**: Immediately after DB persist in listener (synchronous)

---

## Implementation

```java
// WebSocketNotificationSender
@Service
public class NotificationWebSocketSender {
    
    private final SimpMessagingTemplate template;
    
    public boolean sendRestaurantNotification(RestaurantUserNotification n) {
        return sendNotificationInternal(n.getId(), n.getEventId(), n.getUserId(),
            "restaurant", n.getTitle(), n.getBody(), n.getProperties());
    }
    
    private boolean sendNotificationInternal(Long notifId, String eventId, Long userId,
                                             String userType, String title, String body, Map props) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", notifId);
            payload.put("eventId", eventId);
            payload.put("title", title);
            payload.put("body", body);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("properties", props);
            
            // NEW: Use /topic/{userType}/{userId}/notifications pattern
            String destination = String.format("/topic/%s/%d/notifications", userType, userId);
            template.convertAndSend(destination, payload);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket send failed: {}", e.getMessage());
            return false;
        }
    }
}

// In BaseNotificationListener (abstract)
protected void attemptWebSocketSend(T notification) {
    // Implement in subclasses
}

// RestaurantNotificationListener
@Override
protected void attemptWebSocketSend(RestaurantUserNotification notification) {
    webSocketSender.sendRestaurantNotification(notification);
}
```

**Security (WebSocketChannelInterceptor)**:
```java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    
    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
        String destination = accessor.getDestination();
        String userId = getUserId(accessor);
        String userType = getUserType(accessor);
        
        // Validate destination ownership
        if (!WebSocketDestinationValidator.canAccess(destination, userId, userType)) {
            throw new AccessDeniedException("Cannot subscribe to: " + destination);
        }
    }
    
    return message;
}
```

---

**Document Version**: 1.0  
**Component**: WebSocket Real-Time Delivery
