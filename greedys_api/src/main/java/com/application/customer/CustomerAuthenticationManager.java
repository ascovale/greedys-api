package com.application.customer;

import java.util.Arrays;

import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.service.security.CustomerUserDetailsService;

/**
 * AuthenticationManager dedicato per l'autenticazione dei Customer.
 * Questo manager è specifico per le operazioni di login dei clienti.
 */
@Component("customerAuthenticationManager")
public class CustomerAuthenticationManager extends ProviderManager {

    public CustomerAuthenticationManager(
            CustomerDAO customerDAO,
            CustomerUserDetailsService customerUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        // Passa la lista di provider al ProviderManager
        super(Arrays.asList(createCustomerProvider(customerDAO, customerUserDetailsService, passwordEncoder)));
    }

    private static CustomerAuthenticationProvider createCustomerProvider(
            CustomerDAO customerDAO,
            CustomerUserDetailsService customerUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        return new CustomerAuthenticationProvider(customerDAO, customerUserDetailsService, passwordEncoder);
    }
}
