package com.application.restaurant;

import java.util.Arrays;

import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.service.security.RUserDetailsService;

/**
 * AuthenticationManager dedicato per l'autenticazione dei Restaurant Users.
 * Questo manager è specifico per le operazioni di login dei ristoratori.
 */
@Component("restaurantAuthenticationManager")
public class RestaurantAuthenticationManager extends ProviderManager {

    public RestaurantAuthenticationManager(
            RUserDAO rUserDAO,
            RUserDetailsService rUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        // Passa la lista di provider al ProviderManager
        super(Arrays.asList(createRestaurantProvider(rUserDAO, rUserDetailsService, passwordEncoder)));
    }

    private static RUserAuthenticationProvider createRestaurantProvider(
            RUserDAO rUserDAO,
            RUserDetailsService rUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        return new RUserAuthenticationProvider(rUserDAO, rUserDetailsService, passwordEncoder);
    }
}
