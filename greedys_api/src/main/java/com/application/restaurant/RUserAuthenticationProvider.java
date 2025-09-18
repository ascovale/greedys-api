package com.application.restaurant;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.security.RUserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RUserAuthenticationProvider extends DaoAuthenticationProvider {
    
    private final RUserDAO rUserDAO;

    public RUserAuthenticationProvider(RUserDAO rUserDAO, 
                                     RUserDetailsService userDetailsService,
                                     PasswordEncoder passwordEncoder) {
        super(userDetailsService);
        this.rUserDAO = rUserDAO;
        super.setPasswordEncoder(passwordEncoder);
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        try {
            // Gestione del bypass per autenticazione speciale
            if (auth.getDetails() instanceof RUserAuthenticationDetails) {
                RUserAuthenticationDetails details = (RUserAuthenticationDetails) auth.getDetails();
                if (details.isBypassPasswordCheck()) {
                    log.debug("Entering RUserAuthenticationProvider.authenticate method with bypass");
                    log.debug("Authenticating user with email:restaurantId: {}", auth.getName());
                    log.debug("Details: {}", details);

                    RUser user = rUserDAO.findByEmailAndRestaurantId(details.getEmail(), details.getRestaurantId());
                    if (user == null) {
                        log.debug("User not found for email: {} and restaurantId: {}", details.getEmail(), details.getRestaurantId());
                        throw new BadCredentialsException("Invalid username or restaurant ID");
                    }

                    // Check account status even for bypass authentication
                    if (!user.isAccountNonLocked()) {
                        String statusMessage = getStatusMessage(user.getStatus());
                        log.debug("Account not accessible for user: {} - Status: {}", user.getEmail(), user.getStatus());
                        throw new BadCredentialsException(statusMessage);
                    }
                    final Authentication result = super.authenticate(auth);
                    UsernamePasswordAuthenticationToken u = new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());

                    log.debug("Authentication successful with bypass");
                    return u;
                }
            }

            log.debug("Entering RUserAuthenticationProvider.authenticate method");
            log.debug("Authenticating user with email:restaurantId: {}", auth.getName());

            // Split the username into email and restaurantId
            String[] parts = auth.getName().split(":");
            if (parts.length != 2) {
                throw new BadCredentialsException("Invalid username format. Expected 'email:restaurantId'.");
            }

            String email = parts[0];
            Long restaurantId;
            try {
                restaurantId = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                throw new BadCredentialsException("Invalid restaurantId format.");
            }

            // Prima verifica se l'utente esiste
            final RUser user = rUserDAO.findByEmailAndRestaurantId(email, restaurantId);
            if (user == null) {
                log.debug("User not found for email: {} and restaurantId: {}", email, restaurantId);
                throw new BadCredentialsException("Invalid username or password");
            }

            log.debug("User found: {}", user.getEmail());

            // Verifica lo stato dell'account prima dell'autenticazione
            if (!user.isAccountNonLocked()) {
                String statusMessage = getStatusMessage(user.getStatus());
                log.debug("Account not accessible for user: {} - Status: {}", user.getEmail(), user.getStatus());
                throw new BadCredentialsException(statusMessage);
            }

            // Verifica esplicita della password
            if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
                log.debug("Password mismatch for user: {}", email);
                throw new BadCredentialsException("Invalid username or password");
            }

            // Procedi con l'autenticazione standard
            final Authentication result = super.authenticate(auth);
            log.debug("Authentication successful for user: {}", user.getEmail());
            return new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());
            
        } catch (AuthenticationException e) {
            log.error("Errore durante l'autenticazione per l'utente {}: {}", auth.getName(), e.getMessage());
            throw e;
        }
    }

    private String getStatusMessage(RUser.Status status) {
        switch (status) {
            case VERIFY_TOKEN:
                return "Please verify your email address before logging in.";
            case DISABLED:
                return "Your account has been disabled. Please contact support.";
            case BLOCKED:
                return "Your account has been blocked. Please contact support.";
            case DELETED:
                return "This account no longer exists.";
            default:
                return "Your account is not accessible at this time.";
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

