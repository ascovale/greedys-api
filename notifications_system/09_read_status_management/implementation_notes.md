# Read Status Management - Implementation Notes

## ReadStatusService Implementation

```java
@Service
public class ReadStatusService {
    
    private final RestaurantUserNotificationDAO restaurantDAO;
    private final CustomerNotificationDAO customerDAO;
    private final AgencyUserNotificationDAO agencyDAO;
    private final AdminNotificationDAO adminDAO;
    
    private final SharedReadService sharedReadService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public int markNotificationAsRead(Long notificationId, Long userId) {
        ANotification notif = loadNotification(notificationId);
        
        if (notif == null) {
            throw new NotificationNotFoundException("Notification not found");
        }
        
        // Check authorization (user can only read their own)
        if (!notif.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cannot read others' notifications");
        }
        
        // If already read: skip
        if (notif.getStatus() == DeliveryStatus.READ) {
            return 0;
        }
        
        // Mark as read
        notif.setStatus(DeliveryStatus.READ);
        notif.setReadAt(LocalDateTime.now());
        
        int updated = 1;
        
        // If shared read needed: propagate to group
        if (notif.isReadByAll()) {
            SharedReadParams params = SharedReadParams.builder()
                .notificationId(notificationId)
                .eventId(notif.getEventId())
                .readByUserId(userId)
                .readAt(LocalDateTime.now())
                .restaurantId(notif.getRestaurantId())
                .channel(notif.getChannel())
                .scope(calculateScope(notif))
                .build();
            
            // Calculate scope and call strategy
            int sharedUpdated = sharedReadService.markAsRead(
                notificationId,
                notif.getRecipientType(),
                params.getScope(),
                params);
            
            updated = sharedUpdated > 0 ? sharedUpdated : 1;
        } else {
            // Just update this notification
            persistNotification(notif);
        }
        
        // Broadcast status to connected users
        broadcastReadStatus(notif, userId);
        
        return updated;
    }
    
    private SharedReadScope calculateScope(ANotification notif) {
        // Load entity to determine scope
        if (notif.getRecipientType().equals("RESTAURANT")) {
            // Load restaurant context
            RestaurantUserNotification rn = (RestaurantUserNotification) notif;
            // Determine scope from restaurant_user_hub or settings
            return determineRestaurantScope(rn);
        } else if (notif.getRecipientType().equals("AGENCY")) {
            return determineAgencyScope((AgencyUserNotification) notif);
        }
        return SharedReadScope.NONE;
    }
    
    private SharedReadScope determineRestaurantScope(RestaurantUserNotification notif) {
        // If hub_id is set: use hub scope
        // Otherwise: use restaurant scope
        Long hubId = notif.getRestaurantUserHubId();
        if (hubId != null && hubId > 0) {
            return SharedReadScope.RESTAURANT_HUB;
        }
        return SharedReadScope.RESTAURANT;
    }
    
    private void broadcastReadStatus(ANotification notif, Long readByUserId) {
        String destination = "/topic/notifications/" + notif.getRestaurantId() 
            + "/" + notif.getRecipientType();
        
        messagingTemplate.convertAndSend(destination, 
            ReadStatusUpdate.builder()
                .eventId(notif.getEventId())
                .notificationId(notif.getId())
                .status(DeliveryStatus.READ.name())
                .readByUserId(readByUserId)
                .timestamp(System.currentTimeMillis())
                .build());
    }
    
    private ANotification loadNotification(Long notificationId) {
        // Try all 4 tables
        RestaurantUserNotification r = restaurantDAO.findById(notificationId).orElse(null);
        if (r != null) return r;
        
        CustomerNotification c = customerDAO.findById(notificationId).orElse(null);
        if (c != null) return c;
        
        AgencyUserNotification a = agencyDAO.findById(notificationId).orElse(null);
        if (a != null) return a;
        
        AdminNotification ad = adminDAO.findById(notificationId).orElse(null);
        if (ad != null) return ad;
        
        return null;
    }
    
    private void persistNotification(ANotification notif) {
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

// DTO
@Data
@Builder
public class ReadStatusUpdate {
    private String eventId;
    private Long notificationId;
    private String status;
    private Long readByUserId;
    private Long timestamp;
}

@Data
@Builder
public class SharedReadParams {
    private Long notificationId;
    private String eventId;
    private Long readByUserId;
    private LocalDateTime readAt;
    private Long restaurantId;
    private Long agencyId;
    private Long restaurantUserHubId;
    private Long agencyHubId;
    private NotificationChannel channel;
    private SharedReadScope scope;
}
```

## REST Controller

```java
@RestController
@RequestMapping("/api/notifications")
public class NotificationReadController {
    
    private final ReadStatusService readStatusService;
    
    @PostMapping("/{notificationId}/mark-read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal WebSocketAuthenticationToken auth) {
        
        int updated = readStatusService.markNotificationAsRead(
            notificationId, auth.getUserId());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "updatedCount", updated));
    }
    
    @PostMapping("/mark-read-bulk")
    public ResponseEntity<?> markMultipleAsRead(
            @RequestBody List<Long> notificationIds,
            @AuthenticationPrincipal WebSocketAuthenticationToken auth) {
        
        int total = 0;
        for (Long id : notificationIds) {
            total += readStatusService.markNotificationAsRead(id, auth.getUserId());
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "updatedCount", total));
    }
}
```

---

**Document Version**: 1.0  
**Component**: Read Status Management
