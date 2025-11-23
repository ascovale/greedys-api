# Channel Delivery - Functional & Implementation

## Functional Description

Polls notification tables and delivers via channel-specific implementations with retry logic.

**4 Channels**:
- **WebSocket**: Real-time push (best-effort)
- **Email**: Scheduled batch (retryable via SMTP)
- **Push**: Mobile notifications (retryable via FCM)
- **SMS**: Text messages (retryable via Twilio)

**Polling**:
- WebSocket: 5s (immediate)
- Email: 30s (batched)
- Push: 60s (batched)
- SMS: 60s (rate-limited)

**Per-Channel Flow**:
1. Poll DB: SELECT WHERE channel=? AND status='PENDING'
2. For each: Attempt send
3. If success: UPDATE status='DELIVERED'
4. If fail: Retry next cycle (up to max)
5. After max retries: UPDATE status='FAILED'

**Delivery Guarantees**:
- WebSocket: Best-effort (1 attempt, no retry if offline)
- Others: At-least-once (retryable)

---

## Implementation Notes

**ChannelPoller Service**:
```java
@Service
public class ChannelPoller {
    
    @Scheduled(fixedDelay = 5000)  // WebSocket
    public void pollWebSocketChannel() {
        pollAndDeliver(NotificationChannel.WEBSOCKET);
    }
    
    @Scheduled(fixedDelay = 30000)  // Email
    public void pollEmailChannel() {
        pollAndDeliver(NotificationChannel.EMAIL);
    }
    
    private void pollAndDeliver(NotificationChannel channel) {
        // Query all 4 tables
        List<ANotification> pending = queryByChannelAndStatus(channel, PENDING);
        
        INotificationChannel impl = getChannelImplementation(channel);
        
        for (ANotification notif : pending) {
            try {
                boolean sent = impl.send(...);
                if (sent) {
                    updateStatus(notif, DELIVERED);
                }
            } catch (Exception e) {
                log.error("Delivery failed", e);
                // Stay PENDING for retry
            }
        }
    }
}
```

**INotificationChannel Interface**:
```java
public interface INotificationChannel {
    NotificationChannel getChannelType();
    boolean send(String title, String body, Long recipient, 
                 String recipientType, Map<String, String> properties);
    boolean isEnabled();
    int getMaxRetries();
    long getRetryDelayMs();
    long getTimeoutMs();
}
```

**Implementations**:
- WebSocketNotificationChannel: 1 attempt, no retries
- EmailNotificationChannel: 3 attempts, 5s backoff
- PushNotificationChannel: 3 attempts, FCM specific
- SMSNotificationChannel: 3 attempts, Twilio specific

**Retry Strategy**: Exponential backoff with max attempts

---

**Document Version**: 1.0  
**Component**: Channel Delivery
