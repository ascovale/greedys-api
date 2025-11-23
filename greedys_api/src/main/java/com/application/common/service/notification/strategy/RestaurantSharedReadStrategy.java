package com.application.common.service.notification.strategy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.RestaurantNotificationDAO;

/**
 * Strategy for restaurant-scoped shared read operations
 * 
 * Supports scopes:
 * - RESTAURANT: Mark all notifications in restaurant as read
 * - RESTAURANT_HUB: Mark all notifications across hub's restaurants as read
 * - RESTAURANT_HUB_ALL: Admin broadcast - mark ALL as read
 * 
 * Each scope triggers different SQL query:
 * - RESTAURANT: WHERE restaurant_id = ? AND shared_read = true
 * - RESTAURANT_HUB: WHERE restaurant_user_hub_id = ? AND shared_read = true
 * - RESTAURANT_HUB_ALL: WHERE restaurant_user_hub_id = ? (no conditions)
 * 
 * @author System
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantSharedReadStrategy implements SharedReadStrategy {
    
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    
    private static final Set<SharedReadScope> SUPPORTED_SCOPES = 
        EnumSet.of(
            SharedReadScope.RESTAURANT,
            SharedReadScope.RESTAURANT_HUB,
            SharedReadScope.RESTAURANT_HUB_ALL
        );
    
    // ========== MAIN INTERFACE METHOD ==========
    
    /**
     * Dispatches to scope-specific handler
     */
    @Override
    @Transactional
    public void markAsRead(SharedReadParams params) {
        
        params.validateRestaurant();
        
        String scopeStr = params.getScope();
        if (scopeStr == null || scopeStr.isEmpty()) {
            scopeStr = SharedReadScope.NONE.name();
        }
        
        try {
            SharedReadScope scope = SharedReadScope.valueOf(scopeStr);
            
            if (!SUPPORTED_SCOPES.contains(scope)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Scope %s not supported by RestaurantSharedReadStrategy. Supported: %s",
                        scope,
                        SUPPORTED_SCOPES
                    )
                );
            }
            
            log.debug(
                "Restaurant marking notification {} as read with scope {}",
                params.getNotificationId(),
                scope
            );
            
            switch (scope) {
                case RESTAURANT:
                    markAsReadRestaurant(params);
                    break;
                    
                case RESTAURANT_HUB:
                    markAsReadRestaurantHub(params);
                    break;
                    
                case RESTAURANT_HUB_ALL:
                    markAsReadRestaurantHubAll(params);
                    break;
                    
                default:
                    log.warn("Unexpected scope in switch: {}", scope);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid scope: {}", scopeStr, e);
            throw e;
        }
    }
    
    // ========== SCOPE-SPECIFIC HANDLERS ==========
    
    /**
     * RESTAURANT scope: Mark all unread notifications for restaurant as read
     * 
     * Use case: Reservation alert sent to all staff in restaurant#5
     *   - Staff#1 reads
     *   - All staff in restaurant#5 see it as read
     *   - Staff in other restaurants: unaffected
     * 
     * Query:
     *   UPDATE restaurant_notification
     *   SET read = true, read_by_user_id = ?, read_at = ?
     *   WHERE restaurant_id = ?
     *   AND shared_read = true
     *   AND read = false
     */
    private void markAsReadRestaurant(SharedReadParams params) {
        
        if (params.getRestaurantId() == null) {
            throw new IllegalArgumentException(
                "restaurantId required for RESTAURANT scope"
            );
        }
        
        int updated = restaurantNotificationDAO.markAsReadRestaurant(
            params.getRestaurantId(),
            params.getReadByUserId(),
            params.getReadAt()
        );
        
        log.info(
            "Marked {} restaurant notifications as read | "
            + "scope=RESTAURANT, restaurant_id={}, read_by_user={}",
            updated,
            params.getRestaurantId(),
            params.getReadByUserId()
        );
    }
    
    /**
     * RESTAURANT_HUB scope: Mark all notifications across hub as read
     * 
     * Use case: Hub manager sends notification to all staff across hub's restaurants
     *   - Hub#10 manages restaurant#1, restaurant#2, restaurant#3
     *   - Hub staff reads
     *   - ALL staff in hub#10 (across all 3 restaurants) see it as read
     * 
     * Query:
     *   UPDATE restaurant_notification
     *   SET read = true, read_by_user_id = ?, read_at = ?
     *   WHERE restaurant_user_hub_id = ?
     *   AND shared_read = true
     *   AND read = false
     */
    private void markAsReadRestaurantHub(SharedReadParams params) {
        
        if (params.getRestaurantUserHubId() == null) {
            throw new IllegalArgumentException(
                "restaurantUserHubId required for RESTAURANT_HUB scope"
            );
        }
        
        int updated = restaurantNotificationDAO.markAsReadRestaurantHub(
            params.getRestaurantUserHubId(),
            params.getReadByUserId(),
            params.getReadAt()
        );
        
        log.info(
            "Marked {} restaurant notifications as read | "
            + "scope=RESTAURANT_HUB, hub_id={}, read_by_user={}",
            updated,
            params.getRestaurantUserHubId(),
            params.getReadByUserId()
        );
    }
    
    /**
     * RESTAURANT_HUB_ALL scope: Admin broadcast - mark ALL as read immediately
     * 
     * Use case: Critical system announcement to entire hub
     *   - Admin sends "Server Maintenance Notification"
     *   - Admin marks hub#10 as read
     *   - ALL users in hub#10 see notification as "read"
     *   - Even if some users haven't actually read it yet
     *   - No "unread" badge appears anywhere
     * 
     * Query:
     *   UPDATE restaurant_notification
     *   SET read = true, read_by_user_id = ?, read_at = ?
     *   WHERE restaurant_user_hub_id = ?
     *   (all rows, regardless of current read state)
     */
    private void markAsReadRestaurantHubAll(SharedReadParams params) {
        
        if (params.getRestaurantUserHubId() == null) {
            throw new IllegalArgumentException(
                "restaurantUserHubId required for RESTAURANT_HUB_ALL scope"
            );
        }
        
        int updated = restaurantNotificationDAO.markAsReadRestaurantHubAll(
            params.getRestaurantUserHubId(),
            params.getReadByUserId(),
            params.getReadAt()
        );
        
        log.info(
            "Marked {} restaurant notifications as read (BROADCAST) | "
            + "scope=RESTAURANT_HUB_ALL, hub_id={}, read_by_user={}",
            updated,
            params.getRestaurantUserHubId(),
            params.getReadByUserId()
        );
    }
    
    // ========== BATCH OPERATIONS ==========
    
    @Override
    @Transactional
    public void markMultipleAsRead(List<SharedReadParams> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            log.debug("No params to process");
            return;
        }
        
        log.debug("Processing batch of {} shared read operations", paramsList.size());
        
        paramsList.forEach(this::markAsRead);
        
        log.info("Completed batch processing of {} notifications", paramsList.size());
    }
    
    // ========== METADATA ==========
    
    @Override
    public Set<SharedReadScope> getSupportedScopes() {
        return SUPPORTED_SCOPES;
    }
}
