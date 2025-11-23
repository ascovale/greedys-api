# Channel Delivery - Implementation Notes

## ChannelPoller Service

Orchestrates polling for each of 4 channels on distinct schedules.

```java
@Service
public class ChannelPoller {
    
    private final RestaurantUserNotificationDAO restaurantDAO;
    private final CustomerNotificationDAO customerDAO;
    private final AgencyUserNotificationDAO agencyDAO;
    private final AdminNotificationDAO adminDAO;
    
    private final INotificationChannel webSocketChannel;
    private final INotificationChannel emailChannel;
    private final INotificationChannel pushChannel;
    private final INotificationChannel smsChannel;
    
    // WebSocket: 5s (real-time)
    @Scheduled(fixedDelay = 5000)
    public void pollWebSocketChannel() {
        pollAndDeliver(NotificationChannel.WEBSOCKET, webSocketChannel);
    }
    
    // Email: 30s (batched)
    @Scheduled(fixedDelay = 30000)
    public void pollEmailChannel() {
        pollAndDeliver(NotificationChannel.EMAIL, emailChannel);
    }
    
    // Push: 60s (batched)
    @Scheduled(fixedDelay = 60000)
    public void pollPushChannel() {
        pollAndDeliver(NotificationChannel.PUSH, pushChannel);
    }
    
    // SMS: 60s (rate-limited)
    @Scheduled(fixedDelay = 60000)
    public void pollSmsChannel() {
        pollAndDeliver(NotificationChannel.SMS, smsChannel);
    }
    
    private void pollAndDeliver(NotificationChannel channel, INotificationChannel impl) {
        List<ANotification> pending = queryAllTablesByChannel(channel, DeliveryStatus.PENDING);
        
        for (ANotification notif : pending) {
            if (!impl.isEnabled()) {
                continue;
            }
            
            try {
                boolean sent = impl.send(
                    notif.getTitle(),
                    notif.getBody(),
                    notif.getUserId(),
                    notif.getRecipientType(),
                    notif.getProperties());
                
                if (sent) {
                    updateStatus(notif, DeliveryStatus.DELIVERED);
                } else {
                    // Retry next cycle (stays PENDING)
                    log.debug("Send failed, will retry: {}", notif.getId());
                }
            } catch (Exception e) {
                log.error("Error sending notification {}: {}", notif.getId(), e.getMessage());
                // Error: stays PENDING for next cycle
            }
        }
    }
    
    private List<ANotification> queryAllTablesByChannel(NotificationChannel channel, 
                                                       DeliveryStatus status) {
        List<ANotification> results = new ArrayList<>();
        
        // Query all 4 tables
        results.addAll(restaurantDAO.findByChannelAndStatusLimit(channel, status, 100));
        results.addAll(customerDAO.findByChannelAndStatusLimit(channel, status, 100));
        results.addAll(agencyDAO.findByChannelAndStatusLimit(channel, status, 100));
        results.addAll(adminDAO.findByChannelAndStatusLimit(channel, status, 100));
        
        // Sort by created_at (FIFO)
        return results.stream()
            .sorted(Comparator.comparing(ANotification::getCreatedAt))
            .collect(Collectors.toList());
    }
    
    private void updateStatus(ANotification notif, DeliveryStatus status) {
        notif.setStatus(status);
        notif.setUpdatedAt(LocalDateTime.now());
        
        switch (notif.getRecipientType()) {
            case RESTAURANT:
                restaurantDAO.save((RestaurantUserNotification) notif);
                break;
            case CUSTOMER:
                customerDAO.save((CustomerNotification) notif);
                break;
            case AGENCY:
                agencyDAO.save((AgencyUserNotification) notif);
                break;
            case ADMIN:
                adminDAO.save((AdminNotification) notif);
                break;
        }
    }
}
```

## INotificationChannel Interface

```java
public interface INotificationChannel {
    
    // Get channel type
    NotificationChannel getChannelType();
    
    // Send notification (called by poller)
    boolean send(String title, String body, Long recipientId, 
                 String recipientType, Map<String, String> properties);
    
    // Is this channel enabled?
    boolean isEnabled();
    
    // Maximum retry attempts before marking FAILED
    int getMaxRetries();
    
    // Backoff delay between retries (milliseconds)
    long getRetryDelayMs();
    
    // Request timeout (milliseconds)
    long getTimeoutMs();
}
```

## Channel Implementations

### WebSocketNotificationChannel
- Best-effort delivery
- Max retries: 1 (no retry)
- Timeout: 500ms
- Failure: Silent (no error logging)

### EmailNotificationChannel
- Retryable via SMTP
- Max retries: 3
- Backoff: 5s exponential
- Timeout: 10s per message
- Batching: 100 per request

### PushNotificationChannel
- Firebase Cloud Messaging
- Max retries: 3
- Backoff: 5s exponential
- Timeout: 10s
- Topic-based routing

### SMSNotificationChannel
- Twilio integration
- Max retries: 3
- Backoff: 10s exponential (rate-limited)
- Timeout: 15s
- Per-country routing

## Query Patterns

```sql
-- Poll by channel (RESTAURANT table example)
SELECT * FROM restaurant_user_notification
WHERE channel = 'WEBSOCKET' 
  AND status = 'PENDING'
ORDER BY created_at ASC
LIMIT 100;

-- Mark as delivered
UPDATE restaurant_user_notification
SET status = 'DELIVERED', updated_at = NOW()
WHERE id = ?;

-- Mark as failed (after max retries)
UPDATE restaurant_user_notification
SET status = 'FAILED', updated_at = NOW()
WHERE id = ? AND retry_count >= max_retries;
```

## Error Handling

- **SMTP failure**: Retry next cycle, increment retry_count
- **FCM failure**: Retry if retryable error, else mark FAILED
- **Twilio failure**: Retry with backoff, phone format validation
- **WebSocket offline**: Mark FAILED immediately (no retry)

## Concurrency & Scaling

- **Thread Safety**: Each poller runs on single thread (@Scheduled default)
- **Scale**: Use @Scheduled(cron) with thread pool for multiple instances
- **Database**: Use row-level SELECT FOR UPDATE to prevent duplicate processing
- **Distributed**: Use distributed lock (Redis) for multi-instance deployments

---

**Document Version**: 1.0  
**Component**: Channel Delivery
