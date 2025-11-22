package com.application.common.security.websocket;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WebSocket Destination Validator
 * 
 * Validates whether a user is allowed to subscribe to or send messages to
 * a specific WebSocket destination based on their role and identity.
 * 
 * SECURITY RULES:
 * 
 * 1. USER-SPECIFIC QUEUES (/user/{id}/queue/*)
 *    - User can ONLY access /user/{theirId}/queue/*
 *    - Pattern: /user/123/queue/notifications
 *    - User ID 123 can subscribe, but user 456 cannot
 * 
 * 2. ROLE-BASED TOPICS (/topic/{role}/*)
 *    - User can ONLY subscribe to topics matching their role
 *    - RESTAURANT users: /topic/restaurant/*
 *    - CUSTOMER users: /topic/customer/*
 *    - ADMIN users: /topic/admin/*
 *    - AGENCY users: /topic/agency/*
 * 
 * 3. GROUP CHANNELS (/group/{groupId}/*)
 *    - User can subscribe if they're member of the group (future enhancement)
 *    - Would require database lookup to verify membership
 * 
 * 4. BROADCAST TOPICS (/broadcast/*)
 *    - Restricted to admin users only
 * 
 * 5. HUB-SPECIFIC QUEUES (/hub/{hubId}/queue/*)
 *    - Hub users can access queues for restaurants/agencies they manage
 * 
 * ARCHITECTURE:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ WebSocket Client Request                                        │
 * │ SUBSCRIBE /user/123/queue/notifications                         │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ WebSocketChannelInterceptor.preSend()                           │
 * │ ↓ Extracts destination & authentication                         │
 * │ ↓ Calls WebSocketDestinationValidator.canAccess()              │
 * │ ↓                                                                 │
 * │ ├─ Destination Pattern: /user/123/queue/...                    │
 * │ ├─ User Type: RESTAURANT (matches allowed role)                │
 * │ ├─ User ID: 123 (matches destination ID)                       │
 * │ └─ Result: ✅ ALLOWED                                           │
 * └─────────────────────────────────────────────────────────────────┘
 * 
 * @author Greedy's System
 * @since 2025-01-22
 */
@Slf4j
@Service
public class WebSocketDestinationValidator {
    
    private static final String USER_PREFIX = "/user/";
    private static final String TOPIC_PREFIX = "/topic/";
    private static final String GROUP_PREFIX = "/group/";
    private static final String BROADCAST_PREFIX = "/broadcast/";
    private static final String HUB_PREFIX = "/hub/";
    
    /**
     * Validates if a user can access a specific WebSocket destination
     * 
     * @param destination The STOMP destination (e.g., /user/123/queue/notifications)
     * @param userType The user's type (e.g., "restaurant-user", "customer", "admin")
     * @param userId The user's ID
     * @return true if access is allowed, false otherwise
     */
    public boolean canAccess(String destination, String userType, Long userId) {
        if (destination == null || destination.isEmpty() || userType == null || userId == null) {
            log.warn("❌ Invalid validation parameters: destination={}, userType={}, userId={}", 
                     destination, userType, userId);
            return false;
        }
        
        log.debug("🔍 Validating WebSocket access: destination={}, userType={}, userId={}", 
                  destination, userType, userId);
        
        // Parse destination type
        if (destination.startsWith(USER_PREFIX)) {
            return validateUserSpecificQueue(destination, userId);
        } else if (destination.startsWith(TOPIC_PREFIX)) {
            return validateTopicAccess(destination, userType);
        } else if (destination.startsWith(GROUP_PREFIX)) {
            return validateGroupAccess(destination, userId);
        } else if (destination.startsWith(BROADCAST_PREFIX)) {
            return validateBroadcastAccess(userType);
        } else if (destination.startsWith(HUB_PREFIX)) {
            return validateHubAccess(destination, userType);
        } else if (destination.startsWith("/app/")) {
            // Application destinations (client → server commands)
            // More permissive - allow if authenticated
            log.debug("✅ /app destination allowed for authenticated user");
            return true;
        } else {
            log.warn("❌ Unknown destination pattern: {}", destination);
            return false;
        }
    }
    
    /**
     * Validates access to user-specific queues (/user/{id}/queue/...)
     * 
     * Security: User can ONLY access their own user ID
     * 
     * @param destination e.g., /user/123/queue/notifications
     * @param userId The authenticated user's ID
     * @return true only if the ID in the destination matches userId
     */
    private boolean validateUserSpecificQueue(String destination, Long userId) {
        // Extract ID from /user/{id}/queue/...
        // Format: /user/123/queue/notifications
        String[] parts = destination.substring(USER_PREFIX.length()).split("/");
        
        if (parts.length < 2) {
            log.warn("❌ Invalid user queue format: {}", destination);
            return false;
        }
        
        try {
            Long destinationUserId = Long.parseLong(parts[0]);
            
            if (destinationUserId.equals(userId)) {
                log.debug("✅ User {} allowed to access /user/{}/queue", userId, destinationUserId);
                return true;
            } else {
                log.warn("❌ User {} denied access to /user/{}/queue (trying to access another user)", 
                         userId, destinationUserId);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("❌ Invalid user ID in destination: {}", parts[0]);
            return false;
        }
    }
    
    /**
     * Validates access to role-based topics (/topic/{role}/...)
     * 
     * Security: User can ONLY subscribe to topics matching their role
     * 
     * @param destination e.g., /topic/restaurant/orders
     * @param userType The user's type (e.g., "restaurant-user", "customer")
     * @return true if user type matches the topic role
     */
    private boolean validateTopicAccess(String destination, String userType) {
        // Extract role from /topic/{role}/...
        // Format: /topic/restaurant/orders
        String[] parts = destination.substring(TOPIC_PREFIX.length()).split("/");
        
        if (parts.length < 1) {
            log.warn("❌ Invalid topic format: {}", destination);
            return false;
        }
        
        String topicRole = parts[0];
        
        // Map user types to topic roles
        boolean allowed = false;
        switch (topicRole.toLowerCase()) {
            case "restaurant":
                allowed = userType.startsWith("restaurant-user");
                break;
            case "customer":
                allowed = "customer".equals(userType);
                break;
            case "admin":
                allowed = "admin".equals(userType);
                break;
            case "agency":
                allowed = userType.startsWith("agency-user");
                break;
            default:
                log.warn("❌ Unknown topic role: {}", topicRole);
                return false;
        }
        
        if (allowed) {
            log.debug("✅ User type {} allowed to access /topic/{}", userType, topicRole);
        } else {
            log.warn("❌ User type {} denied access to /topic/{} (role mismatch)", userType, topicRole);
        }
        
        return allowed;
    }
    
    /**
     * Validates access to group channels (/group/{groupId}/...)
     * 
     * Security: User can ONLY subscribe if they're a member of the group
     * (This is a placeholder for future group membership verification)
     * 
     * @param destination e.g., /group/456/notifications
     * @param userId The user's ID
     * @return true if user is a group member (currently always false pending DB lookup)
     */
    private boolean validateGroupAccess(String destination, Long userId) {
        // Extract group ID from /group/{groupId}/...
        String[] parts = destination.substring(GROUP_PREFIX.length()).split("/");
        
        if (parts.length < 1) {
            log.warn("❌ Invalid group format: {}", destination);
            return false;
        }
        
        try {
            Long groupId = Long.parseLong(parts[0]);
            
            // TODO: Implement database lookup to verify user is member of groupId
            // For now, deny all group access (not yet implemented)
            log.warn("⏳ Group channel access not yet implemented: /group/{}", groupId);
            return false;
            
        } catch (NumberFormatException e) {
            log.warn("❌ Invalid group ID in destination: {}", parts[0]);
            return false;
        }
    }
    
    /**
     * Validates access to broadcast topics (/broadcast/...)
     * 
     * Security: ONLY admin users can subscribe to broadcast topics
     * 
     * @param userType The user's type
     * @return true only if user is admin
     */
    private boolean validateBroadcastAccess(String userType) {
        boolean allowed = "admin".equals(userType);
        
        if (allowed) {
            log.debug("✅ Admin user allowed to access /broadcast/");
        } else {
            log.warn("❌ Non-admin user type {} denied access to /broadcast/", userType);
        }
        
        return allowed;
    }
    
    /**
     * Validates access to hub-specific queues (/hub/{hubId}/queue/...)
     * 
     * Security: Hub users can access queues for restaurants/agencies they manage
     * (Placeholder for hub management verification)
     * 
     * @param destination e.g., /hub/789/queue/reservations
     * @param userType The user's type (must be hub user)
     * @return true if user is a hub user (currently allows all hub users)
     */
    private boolean validateHubAccess(String destination, String userType) {
        // Hub access requires hub user type
        boolean isHubUser = userType != null && userType.endsWith("-hub");
        
        if (isHubUser) {
            log.debug("✅ Hub user allowed to access hub queue: {}", destination);
        } else {
            log.warn("❌ Non-hub user type {} denied access to hub queue: {}", userType, destination);
        }
        
        // TODO: Verify hub user actually manages the hub ID in destination
        // Extract hub ID and check user's managed hubs
        
        return isHubUser;
    }
    
    /**
     * Gets the role name from a user type for logging/display
     */
    public String getRoleFromUserType(String userType) {
        if (userType == null) return "UNKNOWN";
        
        if (userType.startsWith("restaurant-user")) {
            return userType.endsWith("-hub") ? "RESTAURANT_HUB" : "RESTAURANT";
        } else if ("customer".equals(userType)) {
            return "CUSTOMER";
        } else if ("admin".equals(userType)) {
            return "ADMIN";
        } else if (userType.startsWith("agency-user")) {
            return userType.endsWith("-hub") ? "AGENCY_HUB" : "AGENCY";
        }
        return "UNKNOWN";
    }
}
