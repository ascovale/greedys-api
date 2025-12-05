package com.application.admin;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.persistence.model.Admin;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminAuthenticationProvider extends DaoAuthenticationProvider {

    private final AdminDAO adminDAO;

    public AdminAuthenticationProvider(AdminDAO adminDAO, 
                                     UserDetailsService userDetailsService,
                                     PasswordEncoder passwordEncoder) {
        super(userDetailsService);
        this.adminDAO = adminDAO;
        super.setPasswordEncoder(passwordEncoder);
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        try {
            // Prima verifica se l'utente esiste
            final Admin user = adminDAO.findByEmail(auth.getName());
            if (user == null) {
                log.debug("Failed to find admin '{}'", auth.getName());
                throw new BadCredentialsException("Invalid username or password");
            }

            // Verifica lo stato dell'account prima dell'autenticazione
            if (user.getStatus() != Admin.Status.ENABLED) {
                String statusMessage = getStatusMessage(user.getStatus());
                log.warn("Account not enabled for admin {}: {}", auth.getName(), statusMessage);
                throw new BadCredentialsException(statusMessage);
            }

            // Verifica esplicita della password
            if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
                log.debug("Password non corrisponde per l'utente: {}", auth.getName());
                throw new BadCredentialsException("Invalid username or password");
            }

            // Procedi con l'autenticazione standard
            final Authentication result = super.authenticate(auth);
            return new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());
            
        } catch (AuthenticationException e) {
            log.error("Errore durante l'autenticazione per l'utente {}: {}", auth.getName(), e.getMessage());
            throw e;
        }
    }

    private String getStatusMessage(Admin.Status status) {
        switch (status) {
            case VERIFY_TOKEN:
                return "Admin account not verified. Please check your email and click the verification link.";
            case DISABLED:
                return "Admin account is disabled. Please contact support.";
            case BLOCKED:
                return "Admin account is blocked. Please contact support.";
            case DELETED:
                return "Admin account has been deleted.";
            default:
                return "Admin account is not active.";
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

