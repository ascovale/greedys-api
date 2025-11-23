package com.application.common.service.notification.strategy;

import java.util.List;
import java.util.Set;

/**
 * Strategy interface for marking notifications as read across different scopes
 * 
 * Implementations handle entity-specific logic (Restaurant, Agency, etc.)
 * with support for different propagation scopes (RESTAURANT, RESTAURANT_HUB, etc.)
 * 
 * Strategy Pattern allows extensibility:
 * - Add new implementation for new entity type
 * - No changes to service or factory needed
 * 
 * @author System
 */
public interface SharedReadStrategy {
    
    /**
     * Marks notification as read for specific scope
     * 
     * Implementation responsibilities:
     * - Validate params for this entity type
     * - Execute scope-specific SQL query
     * - Log operation for audit trail
     * - Handle transaction management
     * 
     * @param params Contains notificationId, userId, timestamps, entity IDs
     * 
     * @throws IllegalArgumentException if scope not supported by this strategy
     * @throws IllegalArgumentException if required params missing
     * 
     * Example (RESTAURANT scope):
     *   UPDATE restaurant_notification
     *   SET read = true, read_by_user_id = ?, read_at = ?
     *   WHERE restaurant_id = params.getRestaurantId()
     *   AND shared_read = true
     *   AND read = false
     */
    void markAsRead(SharedReadParams params);
    
    /**
     * Marks multiple notifications as read (batch operation)
     * 
     * Useful for:
     * - "Mark all as read" operations
     * - Admin broadcast to multiple users
     * - Cleanup operations
     * 
     * @param paramsList List of SharedReadParams, each with notification scope
     * 
     * Default implementation iterates and calls markAsRead for each
     * Can be overridden for optimized batch queries
     */
    void markMultipleAsRead(List<SharedReadParams> paramsList);
    
    /**
     * Returns set of scopes this strategy supports
     * 
     * Example:
     * - RestaurantStrategy: RESTAURANT, RESTAURANT_HUB, RESTAURANT_HUB_ALL
     * - AgencyStrategy: AGENCY, AGENCY_HUB, AGENCY_HUB_ALL
     * 
     * @return Immutable set of supported scopes
     */
    Set<SharedReadScope> getSupportedScopes();
    
    /**
     * Checks if this strategy supports given scope
     * 
     * @param scope Scope to check
     * @return true if strategy can handle this scope
     */
    default boolean supportsScope(SharedReadScope scope) {
        return getSupportedScopes().contains(scope);
    }
    
    /**
     * Returns human-readable name for this strategy
     * Used in logging and error messages
     * 
     * @return Strategy name (e.g., "RestaurantSharedReadStrategy")
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName();
    }
}
