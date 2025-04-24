package com.application.security.user.restaurant;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.security.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;

@Service("restaurantUserDetailsService")
@Transactional
public class RestaurantUserDetailsService implements UserDetailsService {

    private final RestaurantUserDAO restaurantUserDAO;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public RestaurantUserDetailsService(RestaurantUserDAO restaurantUserDAO, LoginAttemptService loginAttemptService,
            HttpServletRequest request) {
        this.restaurantUserDAO = restaurantUserDAO;
        this.loginAttemptService = loginAttemptService;
        this.request = request;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) {
        final String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException("blocked");
        }

        // Split the username into email and restaurantId
        String[] parts = username.split(":");
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Invalid username format. Expected 'email:restaurantId'.");
        }

        String email = parts[0];
        Long restaurantId;
        try {
            restaurantId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Invalid restaurantId format.");
        }

        RestaurantUser user = restaurantUserDAO.findByEmailAndRestaurantId(email, restaurantId);
        if (user == null) {
            throw new UsernameNotFoundException("No user found with email: " + email + " and restaurant ID: " + restaurantId);
        }

        // Forza il caricamento lazy delle autorità
        user.getAuthorities().size();

        return user;
    }

    public UserDetails loadUserById(final Long restaurantUserId) throws UsernameNotFoundException {
        try {
            final RestaurantUser user = restaurantUserDAO.findById(restaurantUserId).orElse(null);
            if (user == null) {
                throw new UsernameNotFoundException("No user found with ID: " + restaurantUserId);
            }

            // Forza il caricamento lazy delle autorità
            user.getAuthorities().size();

            return user;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    

    private String getClientIP() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
