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
    @Autowired
    private RestaurantUserDAO userRepository;

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        if (auth.getDetails() instanceof CustomAuthenticationDetails) {
            CustomAuthenticationDetails details = (CustomAuthenticationDetails) auth.getDetails();
            if (details.isBypassPasswordCheck()) {
                System.out.println("\n\n\n\n>>>>>>>>>>>> Entering RestaurantUserAuthenticationProvider.authenticate method");
                System.out.println("Authenticating user with email:restaurantId : " + auth.getName());
                System.out.println("Details: " + details);
                System.out.println("Credentials: " + auth.getCredentials());

                RestaurantUser user = userRepository.findByEmailAndRestaurantId(details.getEmail(), details.getRestaurantId());
                if (user == null) {
                    throw new BadCredentialsException("Invalid username or restaurant ID");
                }
                final Authentication result = super.authenticate(auth);
                UsernamePasswordAuthenticationToken u = new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());

                System.out.println("Andato!\n\n\n");
                return u;
            }
        }

        System.out.println("\n\n\n\n>>>>>>>>>>>> Entering RestaurantUserAuthenticationProvider.authenticate method");
        System.out.println("Authenticating user with email:restaurantId : " + auth.getName());

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

        final RestaurantUser user = userRepository.findByEmailAndRestaurantId(email, restaurantId);

        if (user == null) {
            System.out.println("User not found for email: " + email + " and restaurantId: " + restaurantId);
            throw new BadCredentialsException("Invalid username or password");
        }

        System.out.println("User found: " + user.getEmail());

        // Verifica esplicita della password
        if (!getPasswordEncoder().matches(auth.getCredentials().toString(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Procedi con l'autenticazione standard
        final Authentication result = super.authenticate(auth);
        System.out.println("\n\n\nAuthentication result: " + result);
        return new UsernamePasswordAuthenticationToken(user, result.getCredentials(), result.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

