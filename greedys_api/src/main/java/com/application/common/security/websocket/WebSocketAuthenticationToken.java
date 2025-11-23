package com.application.common.security.websocket;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ WebSocket Authentication Token
 * 
 * Custom authentication token for WebSocket connections that carries JWT claims
 * and user information throughout the WebSocket session lifecycle.
 * 
 * FEATURES:
 * - Extracts JWT claims during handshake
 * - Stores user type, user ID, email
 * - Carries authorities from JWT
 * - Integrates with Spring Security Principal system
 * 
 * USAGE:
 * 1. Created during handshake interception when JWT is validated
 * 2. Stored in WebSocket session attributes
 * 3. Retrieved during STOMP frame processing
 * 4. Used for role-based destination authorization
 * 
 * @author Greedy's System
 * @since 2025-01-22
 */
@Getter
@ToString
@Slf4j
public class WebSocketAuthenticationToken extends AbstractAuthenticationToken {
    
    private static final long serialVersionUID = 1L;
    
    private final String username;
    private final String userType;
    private final Long userId;
    private final String email;
    private final Claims jwtClaims;
    private final String tokenString;
    
    /**
     * Constructor for authenticated token (used after successful JWT validation)
     * 
     * @param username Username/email from JWT subject
     * @param userType User type extracted from JWT (e.g., "restaurant-user", "customer", "admin", "agency-user")
     * @param userId User ID (extracted from claims)
     * @param email Email address from JWT
     * @param authorities Authorities granted by JWT
     * @param jwtClaims Full JWT claims for additional context
     * @param tokenString Original JWT token string
     */
    public WebSocketAuthenticationToken(
            String username,
            String userType,
            Long userId,
            String email,
            Collection<? extends GrantedAuthority> authorities,
            Claims jwtClaims,
            String tokenString) {
        
        super(authorities);
        this.username = username;
        this.userType = userType;
        this.userId = userId;
        this.email = email;
        this.jwtClaims = jwtClaims;
        this.tokenString = tokenString;
        
        // Mark as authenticated after all credentials are set
        setAuthenticated(true);
        
        log.debug("✅ WebSocket authentication token created for user: {}, type: {}, id: {}", 
                  username, userType, userId);
    }
    
    /**
     * Constructor for unauthenticated token (used before JWT validation)
     * 
     * @param tokenString JWT token string to validate
     */
    public WebSocketAuthenticationToken(String tokenString) {
        super(List.of());
        this.tokenString = tokenString;
        this.username = null;
        this.userType = null;
        this.userId = null;
        this.email = null;
        this.jwtClaims = null;
        setAuthenticated(false);
    }
    
    @Override
    public Object getCredentials() {
        return tokenString;
    }
    
    @Override
    public Object getPrincipal() {
        return username != null ? username : email;
    }
    
    /**
     * Check if this token is for a specific user type
     * 
     * @param type The user type to check (e.g., "restaurant-user", "customer")
     * @return true if the token's user type matches
     */
    public boolean isUserType(String type) {
        return type != null && type.equals(this.userType);
    }
    
    /**
     * Check if user is a restaurant user (either regular or hub)
     */
    public boolean isRestaurantUser() {
        return userType != null && userType.startsWith("restaurant-user");
    }
    
    /**
     * Check if user is a customer
     */
    public boolean isCustomer() {
        return "customer".equals(userType);
    }
    
    /**
     * Check if user is an admin
     */
    public boolean isAdmin() {
        return "admin".equals(userType);
    }
    
    /**
     * Check if user is an agency user (either regular or hub)
     */
    public boolean isAgencyUser() {
        return userType != null && userType.startsWith("agency-user");
    }
    
    /**
     * Check if user is a hub user (restaurant or agency hub)
     */
    public boolean isHubUser() {
        return (userType != null && userType.endsWith("-hub"));
    }
    
    /**
     * Get restaurant ID from JWT claims (if present)
     * 
     * @return restaurant ID if claim exists, null otherwise
     */
    public Long getRestaurantIdFromClaims() {
        if (jwtClaims == null || !jwtClaims.containsKey("restaurant_id")) {
            return null;
        }
        Object restaurantIdObj = jwtClaims.get("restaurant_id");
        return restaurantIdObj instanceof Number ? 
               ((Number) restaurantIdObj).longValue() : 
               Long.valueOf((String) restaurantIdObj);
    }
    
    /**
     * Get agency ID from JWT claims (if present)
     * 
     * @return agency ID if claim exists, null otherwise
     */
    public Long getAgencyIdFromClaims() {
        if (jwtClaims == null || !jwtClaims.containsKey("agency_id")) {
            return null;
        }
        Object agencyIdObj = jwtClaims.get("agency_id");
        return agencyIdObj instanceof Number ? 
               ((Number) agencyIdObj).longValue() : 
               Long.valueOf((String) agencyIdObj);
    }
}
