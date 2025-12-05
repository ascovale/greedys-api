package com.application.admin.service.authentication;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.application.admin.AdminAuthenticationProvider;
import com.application.admin.persistence.dao.AdminDAO;

/**
 * AuthenticationManager dedicato per l'autenticazione degli Admin.
 * Questo manager è specifico per le operazioni di login degli amministratori.
 */
@Component("adminAuthenticationManager")
@Profile("!dev-minimal")
public class AdminAuthenticationManager extends ProviderManager {

    /**
     * Protected constructor for subclasses that need custom provider setup.
     * Used by DevAdminAuthenticationManager.
     */
    protected AdminAuthenticationManager(List<AuthenticationProvider> providers) {
        super(providers);
    }

    public AdminAuthenticationManager(
            AdminDAO adminDAO,
            UserDetailsService adminUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        // Passa la lista di provider al ProviderManager
        super(Arrays.asList(createAdminProvider(adminDAO, adminUserDetailsService, passwordEncoder)));
    }

    private static AdminAuthenticationProvider createAdminProvider(
            AdminDAO adminDAO,
            UserDetailsService adminUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        return new AdminAuthenticationProvider(adminDAO, adminUserDetailsService, passwordEncoder);
    }
}
