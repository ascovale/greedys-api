package com.application.security.user.restaurant;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RUserDAO;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.security.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;

@Service("RUserDetailsService")
@Transactional
public class RUserDetailsService implements UserDetailsService {

    private final RUserDAO RUserDAO;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public RUserDetailsService(RUserDAO RUserDAO, LoginAttemptService loginAttemptService,
            HttpServletRequest request) {
        this.RUserDAO = RUserDAO;
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

        RUser user = RUserDAO.findByEmailAndRestaurantId(email, restaurantId);
        if (user == null) {
            throw new UsernameNotFoundException("No user found with email: " + email + " and restaurant ID: " + restaurantId);
        }

        // Forza il caricamento lazy delle autorità
        user.getAuthorities().size();

        return user;
    }

    public UserDetails loadUserById(final Long RUserId) throws UsernameNotFoundException {
        try {
            final RUser user = RUserDAO.findById(RUserId).orElse(null);
            if (user == null) {
                throw new UsernameNotFoundException("No user found with ID: " + RUserId);
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
