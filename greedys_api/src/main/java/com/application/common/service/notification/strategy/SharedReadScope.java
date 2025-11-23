package com.application.common.service.notification.strategy;

/**
 * Defines scope of shared read state propagation
 * 
 * NOT all notifications are "shared" - this enum only applies when
 * a notification targets multiple recipient types (e.g., RUser + RUserHub)
 * 
 * Default: NONE (95% of notifications - each recipient has independent read status)
 * 
 * @author System
 */
public enum SharedReadScope {
    
    /**
     * No shared read logic - each recipient has independent read status
     * DEFAULT SCOPE (95% of notifications)
     * 
     * Example: Email notification to single RUser
     *   - RUser#1 reads notification
     *   - Only RUser#1's badge updates
     *   - Other RUsers don't see it marked as read
     * 
     * Action when set:
     *   - markAsReadShared() is NOT called
     *   - Standard notification.setRead(true) is executed
     */
    NONE("None", false, false),
    
    /**
     * Share read status within same restaurant
     * 
     * Example: Reservation notification sent to all RUsers in restaurant#5
     *   - RUser#1 (restaurant#5) reads notification
     *   - RUser#2 (restaurant#5) sees it marked as read
     *   - RUser#3 (restaurant#5) sees it marked as read
     *   - RUsers from other restaurants: NOT affected
     * 
     * Query:
     *   UPDATE restaurant_notification
     *   SET read = true, read_by_user_id = ?, read_at = ?
     *   WHERE restaurant_id = ?
     *   AND shared_read = true
     *   AND read = false
     */
    RESTAURANT("Restaurant Group", true, false),
    
    /**
     * Share read status across restaurant hub (multiple restaurants)
     * Hub typically manages 2-5 restaurants with shared staff
     * 
     * Example: Hub manager sends notification to all RUsers managed by hub#10
     *   - Hub#10 manages restaurant#1, restaurant#2, restaurant#3
     *   - RUser#1 (rest#1) reads notification
     *   - RUser#2 (rest#2) sees it as read
     *   - RUser#3 (rest#3) sees it as read
     *   - RUserHub#10 sees it as read
     * 
     * Query:
     *   UPDATE restaurant_notification
     *   SET read = true, read_by_user_id = ?, read_at = ?
     *   WHERE restaurant_user_hub_id = ?
     *   AND shared_read = true
     *   AND read = false
     */
    RESTAURANT_HUB("Restaurant Hub", true, true),
    
    /**
     * Admin broadcast: Mark ALL as read immediately across hub
     * System-level decision to mark entire hub as read
     * 
     * Example: Critical system announcement to hub#10
     *   - Admin sends "System Maintenance" notification
     *   - Admin marks hub#10 as read
     *   - ALL RUsers in hub#10 see notification as "read"
     *   - ALL RUserHubs see it as "read"
     *   - Even if some users haven't actually read it yet
     * 
     * Query:
     *   UPDATE restaurant_notification
     *   SET read = true, read_by_user_id = ?, read_at = ?
     *   WHERE restaurant_user_hub_id = ?
     *   (no condition on current read status)
     */
    RESTAURANT_HUB_ALL("Restaurant Hub - Broadcast", true, true);
    
    private final String displayName;
    private final boolean isShared;
    private final boolean isHubLevel;
    
    SharedReadScope(String displayName, boolean isShared, boolean isHubLevel) {
        this.displayName = displayName;
        this.isShared = isShared;
        this.isHubLevel = isHubLevel;
    }
    
    /**
     * Checks if this scope requires shared read propagation
     * @return true if scope != NONE
     */
    public boolean requiresSharedRead() {
        return this != NONE && isShared;
    }
    
    /**
     * Checks if this scope operates at hub level
     * @return true if scope is RESTAURANT_HUB or RESTAURANT_HUB_ALL
     */
    public boolean isHubLevel() {
        return isHubLevel;
    }
    
    /**
     * Checks if this scope is broadcast-all (immediate mark)
     * @return true if scope is RESTAURANT_HUB_ALL
     */
    public boolean isBroadcastAll() {
        return this == RESTAURANT_HUB_ALL;
    }
    
    /**
     * Human-readable display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @return true if this is a restaurant-level operation
     */
    public boolean isRestaurantLevel() {
        return this == RESTAURANT;
    }
}
