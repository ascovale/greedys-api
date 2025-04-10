package com.application.security.google2fa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.restaurant.user.RestaurantUser;

@Component
public class RestaurantUserAuthenticationProvider extends DaoAuthenticationProvider {
    //TODO forse non serve questa classe perchè non è nel security config
    //TODO DA ELIOMINARE CLASSE
    @Autowired
    private RestaurantUserDAO userRepository;

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        System.out.println("\n\n\n\n>>>>>>>>>>>> Entering RestaurantUserAuthenticationProvider.authenticate method");
        System.out.println("Authenticating user with email: " + auth.getName());

        final RestaurantUser user = userRepository.findByEmail(auth.getName());

        if (user == null) {
            System.out.println("User not found for email: " + auth.getName());
        } else {
            System.out.println("User found: " + user.getEmail());
        }
        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Verifica esplicita della password
        if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
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

