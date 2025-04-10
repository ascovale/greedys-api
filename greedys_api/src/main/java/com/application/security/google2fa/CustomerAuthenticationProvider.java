package com.application.security.google2fa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.model.customer.Customer;

@Component
public class CustomerAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    private CustomerDAO userRepository;

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        final Customer user = userRepository.findByEmail(auth.getName());
        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Verifica esplicita della password
        if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
            System.out.println("\n\n\\n\n\nPassword non corrisponde: " + auth.getCredentials().toString() + " != " + user.getPassword());
            // throw new BadCredentialsException("Invalid username or password");   
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

