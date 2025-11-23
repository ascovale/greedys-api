# Shared Read Strategy - Implementation Notes

## SharedReadStrategy Interface & Implementations

```java
public interface SharedReadStrategy {
    
    // Mark single notification as read (with scope)
    int markAsRead(SharedReadParams params);
    
    // Batch mark as read
    int markMultipleAsRead(List<SharedReadParams> paramsList);
    
    // Get supported scopes for this strategy
    List<SharedReadScope> getSupportedScopes();
}

@Service
public class RestaurantSharedReadStrategy implements SharedReadStrategy {
    
    private final RestaurantUserNotificationDAO dao;
    
    @Override
    public int markAsRead(SharedReadParams params) {
        switch (params.getScope()) {
            case NONE:
                return 0;  // No shared read
            
            case RESTAURANT:
                // All staff of same restaurant who read this event
                return dao.updateRestaurantSharedRead(
                    params.getEventId(),
                    params.getRestaurantId(),
                    params.getReadByUserId(),
                    LocalDateTime.now());
            
            case RESTAURANT_HUB:
                // All hub members
                return dao.updateRestaurantHubSharedRead(
                    params.getEventId(),
                    params.getRestaurantUserHubId(),
                    params.getReadByUserId(),
                    LocalDateTime.now());
            
            case RESTAURANT_HUB_ALL:
                // Broadcast: all hub members + other restaurants in hub
                return dao.updateRestaurantHubAllSharedRead(
                    params.getEventId(),
                    params.getRestaurantUserHubId(),
                    params.getReadByUserId(),
                    LocalDateTime.now());
            
            default:
                return 0;
        }
    }
    
    @Override
    public int markMultipleAsRead(List<SharedReadParams> paramsList) {
        return paramsList.stream()
            .map(this::markAsRead)
            .reduce(0, Integer::sum);
    }
    
    @Override
    public List<SharedReadScope> getSupportedScopes() {
        return Arrays.asList(
            SharedReadScope.NONE,
            SharedReadScope.RESTAURANT,
            SharedReadScope.RESTAURANT_HUB,
            SharedReadScope.RESTAURANT_HUB_ALL);
    }
}

@Service
public class AgencySharedReadStrategy implements SharedReadStrategy {
    
    private final AgencyUserNotificationDAO dao;
    
    @Override
    public int markAsRead(SharedReadParams params) {
        switch (params.getScope()) {
            case AGENCY:
                return dao.updateAgencySharedRead(
                    params.getEventId(),
                    params.getAgencyId(),
                    params.getReadByUserId(),
                    LocalDateTime.now());
            
            case AGENCY_HUB:
                return dao.updateAgencyHubSharedRead(
                    params.getEventId(),
                    params.getAgencyHubId(),
                    params.getReadByUserId(),
                    LocalDateTime.now());
            
            // ... etc
            
            default:
                return 0;
        }
    }
    
    @Override
    public List<SharedReadScope> getSupportedScopes() {
        return Arrays.asList(
            SharedReadScope.NONE,
            SharedReadScope.AGENCY,
            SharedReadScope.AGENCY_HUB,
            SharedReadScope.AGENCY_HUB_ALL);
    }
}
```

## SharedReadService

```java
@Service
public class SharedReadService {
    
    private final SharedReadStrategyFactory factory;
    
    public int markAsRead(Long notificationId, String entityType, 
                         SharedReadScope scope, SharedReadParams params) {
        
        if (!scope.requiresSharedRead()) {
            return 0;  // No propagation needed
        }
        
        SharedReadStrategy strategy = factory.getStrategy(entityType);
        if (strategy == null) {
            log.warn("No strategy for entity type: {}", entityType);
            return 0;
        }
        
        return strategy.markAsRead(params);
    }
}

@Component
public class SharedReadStrategyFactory {
    
    private final RestaurantSharedReadStrategy restaurantStrategy;
    private final AgencySharedReadStrategy agencyStrategy;
    
    public SharedReadStrategy getStrategy(String entityType) {
        switch (entityType) {
            case "RESTAURANT":
                return restaurantStrategy;
            case "AGENCY":
                return agencyStrategy;
            default:
                return null;
        }
    }
}
```

## SharedReadScope ENUM

```java
public enum SharedReadScope {
    
    NONE("No shared read"),
    RESTAURANT("Group: restaurant level"),
    RESTAURANT_HUB("Hub level"),
    RESTAURANT_HUB_ALL("Broadcast within hub"),
    AGENCY("Group: agency level"),
    AGENCY_HUB("Hub level"),
    AGENCY_HUB_ALL("Broadcast within hub");
    
    private final String description;
    
    SharedReadScope(String description) {
        this.description = description;
    }
    
    public boolean requiresSharedRead() {
        return this != NONE;
    }
}
```

## DAO Methods - SQL Patterns

```sql
-- RESTAURANT scope: All staff of same restaurant
UPDATE restaurant_user_notification
SET status = 'READ', read_at = ?, updated_at = NOW()
WHERE event_id LIKE ?  -- 'evt-123_%'
  AND restaurant_id = ?
  AND channel IN ('WEBSOCKET', 'EMAIL', 'SMS')
  AND read_by_all = true
  AND status = 'PENDING';

-- RESTAURANT_HUB scope: All hub members
UPDATE restaurant_user_notification r
SET status = 'READ', read_at = ?, updated_at = NOW()
WHERE r.event_id LIKE ?
  AND r.user_id IN (
    SELECT user_id FROM restaurant_user_hub_members 
    WHERE restaurant_user_hub_id = ?)
  AND r.read_by_all = true;

-- RESTAURANT_HUB_ALL scope: All restaurants in hub
UPDATE restaurant_user_notification
SET status = 'READ', read_at = ?, updated_at = NOW()
WHERE event_id LIKE ?
  AND restaurant_id IN (
    SELECT restaurant_id FROM restaurant_user_hubs
    WHERE restaurant_user_hub_id = ?)
  AND read_by_all = true;
```

---

**Document Version**: 1.0  
**Component**: Shared Read Strategy
