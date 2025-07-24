package com.application.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.model.Customer;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomerAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    private CustomerDAO customerDAO;

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        final Customer user = customerDAO.findByEmail(auth.getName());
        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Verifica esplicita della password
        if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
            log.debug("Password non corrisponde per l'utente: {}", auth.getName());
            throw new BadCredentialsException("Invalid username or password");
        }

        // Procedi con l'autenticazione standard
        final Authentication result = super.authenticate(auth);
        return new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

