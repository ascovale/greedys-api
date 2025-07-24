package com.application.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.persistence.model.Admin;
import com.application.admin.service.security.AdminUserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AdminAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    private AdminDAO adminDAO;

    public AdminAuthenticationProvider(AdminUserDetailsService userDetailsService) {
        setUserDetailsService(userDetailsService);
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        final Admin user = adminDAO.findByEmail(auth.getName());
        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Verifica esplicita della password
        if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
            log.debug("Password non corrisponde per l'utente: {}", auth.getName());
            throw new BadCredentialsException("Invalid username or password");
        }

        try {
            // Procedi con l'autenticazione standard
            final Authentication result = super.authenticate(auth);
            return new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());
        } catch (AuthenticationException e) {
            // Cattura e stampa il motivo dell'errore
            log.error("Errore durante l'autenticazione per l'utente {}: {}", auth.getName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

