# Read Status Management - Implementation

ReadStatusService orchestrates read/unread operations:

```java
@Service
public class ReadStatusService {
    
    @Transactional
    public int markNotificationAsRead(Long notifId, Long userId) {
        ANotification notif = loadNotification(notifId);
        
        int updated = 0;
        
        if (notif.isReadByAll()) {
            // Shared read: propagate to group
            SharedReadParams params = SharedReadParams.builder()
                .notificationId(notifId)
                .readByUserId(userId)
                .readAt(LocalDateTime.now())
                .restaurantId(notif.getRestaurantId())  // For RESTAURANT scope
                .scope(SharedReadScope.RESTAURANT)
                .build();
            
            updated = sharedReadService.markAsRead(
                notifId, "RESTAURANT", 
                SharedReadScope.RESTAURANT, params);
        } else {
            // Individual read
            notif.setStatus(DeliveryStatus.READ);
            notif.setReadAt(LocalDateTime.now());
            dao.save(notif);
            updated = 1;
        }
        
        // Broadcast to connected users
        broadcastReadStatus(notif, userId);
        
        return updated;
    }
    
    private void broadcastReadStatus(ANotification notif, Long readByUserId) {
        messagingTemplate.convertAndSend(
            "/topic/notifications/" + notif.getRestaurantId() + "/" + notif.getRecipientType(),
            Map.of(
                "eventId", notif.getEventId(),
                "notificationId", notif.getId(),
                "status", "READ",
                "readByUserId", readByUserId
            )
        );
    }
}
```

**DAO Operations**:
- Single UPDATE: `UPDATE WHERE id = ? AND user_id = ?`
- Batch UPDATE: `UPDATE WHERE event_id LIKE ? AND restaurant_id = ? AND read_by_all = true`

---

**Document Version**: 1.0  
**Component**: Read Status Management
