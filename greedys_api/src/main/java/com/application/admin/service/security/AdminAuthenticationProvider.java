package com.application.admin.service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.application.admin.dao.AdminDAO;
import com.application.admin.model.Admin;

@Component
public class AdminAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    private AdminDAO userRepository;

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        final Admin user = userRepository.findByEmail(auth.getName());
        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Verifica esplicita della password
        if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
            System.out.println("\n\n\\n\n\nPassword non corrisponde: " + auth.getCredentials().toString() + " != " + user.getPassword());
            throw new BadCredentialsException("Invalid username or password");
        }

        try {
            // Procedi con l'autenticazione standard
            final Authentication result = super.authenticate(auth);
            return new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());
        } catch (AuthenticationException e) {
            // Cattura e stampa il motivo dell'errore
            System.err.println("Errore durante l'autenticazione: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

