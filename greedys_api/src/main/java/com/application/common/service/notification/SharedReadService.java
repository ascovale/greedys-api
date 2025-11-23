package com.application.common.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.service.notification.strategy.SharedReadParams;
import com.application.common.service.notification.strategy.SharedReadScope;
import com.application.common.service.notification.strategy.SharedReadStrategy;
import com.application.common.service.notification.strategy.SharedReadStrategyFactory;

/**
 * Orchestrator service for shared read operations
 * 
 * Responsibilities:
 * 1. Validates inputs (scope, entity type)
 * 2. Selects appropriate strategy via factory
 * 3. Delegates to strategy for scope-specific logic
 * 4. Handles transaction management
 * 5. Provides logging/audit trail
 * 
 * Usage:
 * 
 *   sharedReadService.markAsRead(
 *     notificationId,
 *     "RESTAURANT",
 *     SharedReadScope.RESTAURANT_HUB,
 *     params
 *   );
 * 
 * When scope = NONE, method returns immediately (no shared read processing)
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SharedReadService {
    
    private final SharedReadStrategyFactory strategyFactory;
    
    // ========== PUBLIC API ==========
    
    /**
     * Mark notification as read with potential shared propagation
     * 
     * Main entry point for shared read operations
     * 
     * @param notificationId ID of notification being marked as read
     * @param entityType Entity type ("RESTAURANT", "AGENCY", etc.)
     * @param scope Shared read scope (determines propagation behavior)
     * @param params Additional context (IDs, timestamps, user info)
     * 
     * @throws IllegalArgumentException if inputs invalid
     * 
     * Process:
     * 1. Check if scope requires processing (NONE scope → return early)
     * 2. Get strategy for entity type
     * 3. Set scope on params
     * 4. Delegate to strategy
     */
    @Transactional
    public void markAsRead(
        Long notificationId,
        String entityType,
        SharedReadScope scope,
        SharedReadParams params
    ) {
        
        // === VALIDATION ===
        if (notificationId == null) {
            throw new IllegalArgumentException("notificationId cannot be null");
        }
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("entityType cannot be null/empty");
        }
        if (scope == null) {
            scope = SharedReadScope.NONE;  // Default
        }
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        
        log.debug(
            "Request: markAsRead(notification={}, entity={}, scope={})",
            notificationId,
            entityType,
            scope
        );
        
        // === EARLY EXIT: No shared read needed ===
        if (!requiresSharedRead(scope)) {
            log.debug(
                "Scope {} does not require shared read processing - returning",
                scope
            );
            return;
        }
        
        // === GET STRATEGY ===
        SharedReadStrategy strategy = strategyFactory.getStrategy(entityType);
        
        if (strategy == null) {
            log.error(
                "Factory returned null strategy for entity type: {}",
                entityType
            );
            throw new IllegalStateException(
                "No strategy available for entity type: " + entityType
            );
        }
        
        // === VALIDATE SCOPE SUPPORT ===
        if (!strategy.supportsScope(scope)) {
            throw new IllegalArgumentException(
                String.format(
                    "Strategy %s does not support scope %s",
                    strategy.getStrategyName(),
                    scope
                )
            );
        }
        
        // === PREPARE PARAMS ===
        params.setNotificationId(notificationId);
        params.setScope(scope.name());
        
        log.info(
            "Executing shared read | strategy={}, entity={}, scope={}, notification={}",
            strategy.getStrategyName(),
            entityType,
            scope,
            notificationId
        );
        
        try {
            // === DELEGATE TO STRATEGY ===
            strategy.markAsRead(params);
            
            log.info(
                "Successfully marked notification {} as read | "
                + "strategy={}, scope={}",
                notificationId,
                strategy.getStrategyName(),
                scope
            );
            
        } catch (Exception e) {
            log.error(
                "Error in shared read strategy: {}",
                strategy.getStrategyName(),
                e
            );
            throw e;
        }
    }
    
    /**
     * Convenience method: Mark single notification as read
     * with basic parameters
     * 
     * @param notificationId Notification ID
     * @param entityType Entity type
     * @param scope Shared read scope
     * @param readByUserId User who read
     */
    @Transactional
    public void markAsRead(
        Long notificationId,
        String entityType,
        SharedReadScope scope,
        Long readByUserId
    ) {
        SharedReadParams params = SharedReadParams.builder()
            .readByUserId(readByUserId)
            .readAt(java.time.Instant.now())
            .build();
        
        markAsRead(notificationId, entityType, scope, params);
    }
    
    /**
     * Mark multiple notifications as read
     * 
     * @param params List of SharedReadParams, each with different notification
     * @param entityType Entity type (same for all in this batch)
     * @param scope Shared read scope (same for all in this batch)
     */
    @Transactional
    public void markMultipleAsRead(
        java.util.List<SharedReadParams> params,
        String entityType,
        SharedReadScope scope
    ) {
        
        if (params == null || params.isEmpty()) {
            log.debug("No params to process in batch");
            return;
        }
        
        log.info("Processing batch of {} notifications", params.size());
        
        if (!requiresSharedRead(scope)) {
            log.debug("Scope {} doesn't require shared read - returning", scope);
            return;
        }
        
        SharedReadStrategy strategy = strategyFactory.getStrategy(entityType);
        
        for (SharedReadParams param : params) {
            param.setScope(scope.name());
            try {
                strategy.markAsRead(param);
            } catch (Exception e) {
                log.error(
                    "Error marking notification {} as read",
                    param.getNotificationId(),
                    e
                );
                // Continue with remaining notifications instead of failing entire batch
            }
        }
        
        log.info("Batch processing completed for {} notifications", params.size());
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Check if scope requires shared read processing
     * 
     * @param scope Scope to check
     * @return true if scope != NONE
     */
    public boolean requiresSharedRead(SharedReadScope scope) {
        return scope != null && scope.requiresSharedRead();
    }
    
    /**
     * Check if entity type is supported
     * 
     * @param entityType Entity type
     * @return true if factory has strategy for this type
     */
    public boolean supportsEntityType(String entityType) {
        return strategyFactory.supportsEntityType(entityType);
    }
    
    /**
     * Get factory (for advanced usage)
     * 
     * @return The strategy factory
     */
    protected SharedReadStrategyFactory getFactory() {
        return strategyFactory;
    }
}
