package com.application.restaurant.service.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.security.LoginAttemptService;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.model.user.RUser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RUserDetailsService implements UserDetailsService {

    private final RUserDAO RUserDAO;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        final String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException("blocked");
        }

        try {
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
        } catch (UsernameNotFoundException e) {
            // Re-lancia l'eccezione di autenticazione senza wrapping
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
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
        } catch (UsernameNotFoundException e) {
            // Re-lancia l'eccezione di autenticazione senza wrapping
            throw e;
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
