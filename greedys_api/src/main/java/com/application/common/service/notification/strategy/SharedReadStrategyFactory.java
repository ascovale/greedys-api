package com.application.common.service.notification.strategy;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting appropriate SharedReadStrategy
 * 
 * Maps entity types to implementations:
 * - "RESTAURANT" → RestaurantSharedReadStrategy
 * - "RESTAURANT_USER" → RestaurantSharedReadStrategy
 * - "RESTAURANT_USER_HUB" → RestaurantSharedReadStrategy
 * - "AGENCY" → AgencySharedReadStrategy
 * - "AGENCY_USER" → AgencySharedReadStrategy
 * - "AGENCY_USER_HUB" → AgencySharedReadStrategy
 * 
 * Easily extensible:
 * - Add new strategy: Implement SharedReadStrategy
 * - Register: strategies.put("NEW_ENTITY", newStrategy)
 * - Use: factory.getStrategy("NEW_ENTITY")
 * 
 * @author System
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SharedReadStrategyFactory {
    
    private final RestaurantSharedReadStrategy restaurantStrategy;
    private final AgencySharedReadStrategy agencyStrategy;
    
    private final Map<String, SharedReadStrategy> strategies = new HashMap<>();
    
    /**
     * Initialize strategy mappings
     * Called automatically on component construction
     */
    @PostConstruct
    public void init() {
        // Restaurant entity types
        strategies.put("RESTAURANT", restaurantStrategy);
        strategies.put("RESTAURANT_USER", restaurantStrategy);
        strategies.put("RESTAURANT_USER_HUB", restaurantStrategy);
        strategies.put("RUSER", restaurantStrategy);
        strategies.put("RUSERHUB", restaurantStrategy);
        
        // Agency entity types
        strategies.put("AGENCY", agencyStrategy);
        strategies.put("AGENCY_USER", agencyStrategy);
        strategies.put("AGENCY_USER_HUB", agencyStrategy);
        strategies.put("AGENCYUSER", agencyStrategy);
        strategies.put("AGENCYUSERHUB", agencyStrategy);
        
        log.debug("SharedReadStrategyFactory initialized with {} entity type mappings", 
            strategies.size());
    }
    
    /**
     * Get strategy for entity type
     * 
     * @param entityType Entity type string (e.g., "RESTAURANT", "AGENCY")
     * @return Strategy for this entity type
     * @throws IllegalArgumentException if entity type not found and no default available
     */
    public SharedReadStrategy getStrategy(String entityType) {
        
        if (entityType == null || entityType.trim().isEmpty()) {
            log.warn("Entity type is null/empty. Defaulting to RESTAURANT strategy");
            return restaurantStrategy;
        }
        
        String normalized = entityType.toUpperCase().trim();
        
        SharedReadStrategy strategy = strategies.get(normalized);
        
        if (strategy == null) {
            log.warn(
                "No strategy found for entity type: {}. Available: {}. Defaulting to RESTAURANT",
                entityType,
                strategies.keySet()
            );
            return restaurantStrategy;
        }
        
        log.debug("Selected {} strategy for entity type: {}", 
            strategy.getStrategyName(), 
            entityType);
        
        return strategy;
    }
    
    /**
     * Check if factory supports entity type
     * 
     * @param entityType Entity type to check
     * @return true if factory can provide strategy for this type
     */
    public boolean supportsEntityType(String entityType) {
        if (entityType == null) return false;
        return strategies.containsKey(entityType.toUpperCase());
    }
    
    /**
     * Register custom strategy for entity type
     * 
     * Allows runtime registration of new strategies
     * 
     * @param entityType Entity type name
     * @param strategy Strategy implementation
     */
    public void registerStrategy(String entityType, SharedReadStrategy strategy) {
        if (entityType == null || strategy == null) {
            throw new IllegalArgumentException("Entity type and strategy cannot be null");
        }
        
        String normalized = entityType.toUpperCase();
        strategies.put(normalized, strategy);
        
        log.info("Registered strategy {} for entity type {}", 
            strategy.getStrategyName(), 
            entityType);
    }
    
    /**
     * Get all registered entity types
     * 
     * Useful for debugging and validation
     * 
     * @return Set of supported entity type strings
     */
    public java.util.Set<String> getRegisteredEntityTypes() {
        return java.util.Collections.unmodifiableSet(strategies.keySet());
    }
    
    /**
     * Get all registered strategies
     * 
     * @return Map of entity type to strategy
     */
    public Map<String, SharedReadStrategy> getAllStrategies() {
        return java.util.Collections.unmodifiableMap(strategies);
    }
}
