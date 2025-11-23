# Notification Disaggregation - Implementation Notes

## Class Hierarchy & Design Pattern

### Template Method Pattern

```java
public abstract class NotificationOrchestrator<T extends ANotification> {
    
    // Main entry point
    public abstract List<T> disaggregateAndProcess(Map<String, Object> message);
    
    // Abstract methods for subclasses to implement
    protected abstract List<Long> loadRecipients(Map<String, Object> message);
    protected abstract List<String> loadUserPreferences(Long userId);
    protected abstract Map<String, Object> loadGroupSettings(Map<String, Object> message);
    protected abstract Map<String, Object> loadEventTypeRules(String eventType);
    protected abstract T createNotificationRecord(String eventId, Long userId, String channel, Map<String, Object> msg);
    
    // Concrete helper methods (used by all subclasses)
    protected List<String> calculateFinalChannels(List<String> mandatory, List<String> optional, 
                                                   List<String> groupChannels, List<String> userChannels) {
        // Intersection logic: Group ∩ User ∩ (Mandatory + Optional)
        Set<String> candidates = new HashSet<>();
        candidates.addAll(mandatory);
        candidates.addAll(optional);
        
        Set<String> userSet = new HashSet<>(userChannels);
        Set<String> groupSet = new HashSet<>(groupChannels);
        
        candidates.retainAll(userSet);
        candidates.retainAll(groupSet);
        
        return new ArrayList<>(candidates);
    }
}
```

### Concrete Implementation Example

```java
@Service
public class RestaurantUserOrchestrator extends NotificationOrchestrator<RestaurantUserNotification> {
    
    private final RestaurantUserDAO staffDAO;
    private final RestaurantUserPreferencesDAO preferencesDAO;
    private final RestaurantSettingsDAO settingsDAO;
    
    @Override
    public List<RestaurantUserNotification> disaggregateAndProcess(Map<String, Object> message) {
        List<RestaurantUserNotification> result = new ArrayList<>();
        
        String eventId = (String) message.get("event_id");
        String eventType = (String) message.get("event_type");
        Long restaurantId = extractLong(message, "restaurant_id");
        
        // Step 1: Load recipients
        List<Long> staffIds = loadRecipients(message);
        log.info("Loaded {} staff for restaurant {}", staffIds.size(), restaurantId);
        
        // Step 2: Load event rules
        Map<String, Object> eventRules = loadEventTypeRules(eventType);
        @SuppressWarnings("unchecked")
        List<String> mandatory = (List<String>) eventRules.get("mandatory");
        @SuppressWarnings("unchecked")
        List<String> optional = (List<String>) eventRules.get("optional");
        
        // Step 3: Load group settings
        Map<String, Object> groupSettings = loadGroupSettings(message);
        @SuppressWarnings("unchecked")
        List<String> groupChannels = (List<String>) groupSettings.get("enabled_channels");
        
        // Step 4: For each recipient
        for (Long staffId : staffIds) {
            // Load user preferences
            List<String> userChannels = loadUserPreferences(staffId);
            
            // Calculate final channels
            List<String> finalChannels = calculateFinalChannels(mandatory, optional, groupChannels, userChannels);
            
            // Create record per channel
            for (String channel : finalChannels) {
                RestaurantUserNotification notif = createNotificationRecord(eventId, staffId, channel, message);
                notif.setReadByAll(isEventBroadcast(eventType)); // Set shared read flag
                result.add(notif);
            }
        }
        
        return result;
    }
    
    @Override
    protected List<Long> loadRecipients(Map<String, Object> message) {
        Long restaurantId = extractLong(message, "restaurant_id");
        return staffDAO.findActiveByRestaurantId(restaurantId)
                       .stream()
                       .map(Staff::getId)
                       .collect(Collectors.toList());
    }
    
    @Override
    protected List<String> loadUserPreferences(Long userId) {
        return preferencesDAO.findByUserId(userId)
                            .getEnabledChannels();  // [WEBSOCKET, EMAIL, SMS]
    }
    
    @Override
    protected Map<String, Object> loadGroupSettings(Map<String, Object> message) {
        Long restaurantId = extractLong(message, "restaurant_id");
        RestaurantSettings settings = settingsDAO.findByRestaurantId(restaurantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("enabled_channels", settings.getDefaultChannels());
        return result;
    }
    
    @Override
    protected Map<String, Object> loadEventTypeRules(String eventType) {
        // Hardcoded rules (could come from database)
        switch (eventType) {
            case "RESERVATION_REQUESTED":
                Map<String, Object> rules = new HashMap<>();
                rules.put("mandatory", List.of("WEBSOCKET"));
                rules.put("optional", List.of("EMAIL", "PUSH"));
                return rules;
            case "ORDER_CREATED":
                // Similar...
            default:
                // Default rules
        }
    }
    
    @Override
    protected RestaurantUserNotification createNotificationRecord(
        String eventId, Long userId, String channel, Map<String, Object> msg) {
        
        return RestaurantUserNotification.builder()
            .eventId(eventId + "_" + userId + "_" + channel)
            .userId(userId)
            .channel(NotificationChannel.valueOf(channel))
            .status(DeliveryStatus.PENDING)
            .title(extractTitle(msg))
            .body(extractBody(msg))
            .properties(extractProperties(msg))
            .createdAt(LocalDateTime.now())
            .build();
    }
}
```

## Factory Pattern Implementation

```java
@Service
public class NotificationOrchestratorFactory {
    
    @Autowired
    private RestaurantUserOrchestrator restaurantOrchestrator;
    
    @Autowired
    private CustomerOrchestrator customerOrchestrator;
    
    @Autowired
    private AgencyUserOrchestrator agencyOrchestrator;
    
    @Autowired
    private AdminOrchestrator adminOrchestrator;
    
    private Map<String, NotificationOrchestrator<?>> strategies = new HashMap<>();
    
    @PostConstruct
    public void init() {
        strategies.put("RESTAURANT", restaurantOrchestrator);
        strategies.put("CUSTOMER", customerOrchestrator);
        strategies.put("AGENCY", agencyOrchestrator);
        strategies.put("ADMIN", adminOrchestrator);
    }
    
    public NotificationOrchestrator<?> getOrchestrator(String aggregateType) {
        NotificationOrchestrator<?> orchestrator = strategies.get(aggregateType);
        if (orchestrator == null) {
            throw new IllegalArgumentException("Unknown aggregate type: " + aggregateType);
        }
        return orchestrator;
    }
}
```

## Transaction Management

```java
@Override
@Transactional
public List<RestaurantUserNotification> disaggregateAndProcess(Map<String, Object> message) {
    // All queries within single transaction
    // Ensures consistency if database changes during processing
    // Rolled back if exception thrown
}
```

## Performance Optimization Tips

1. **Batch loading**: Load all user preferences in one query, not per-user
2. **Caching**: Cache event rules (don't query every message)
3. **Connection pooling**: Ensure sufficient DB connections
4. **Index optimization**: Index on (user_id, enabled_channels)

---

**Document Version**: 1.0  
**Last Updated**: November 23, 2025  
**Component**: Notification Disaggregation
