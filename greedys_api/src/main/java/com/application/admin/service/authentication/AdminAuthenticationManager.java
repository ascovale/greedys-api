package com.application.admin.service.authentication;

import java.util.Arrays;

import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.application.admin.AdminAuthenticationProvider;
import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.service.security.AdminUserDetailsService;

/**
 * AuthenticationManager dedicato per l'autenticazione degli Admin.
 * Questo manager è specifico per le operazioni di login degli amministratori.
 */
@Component("adminAuthenticationManager")
public class AdminAuthenticationManager extends ProviderManager {

    public AdminAuthenticationManager(
            AdminDAO adminDAO,
            AdminUserDetailsService adminUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        // Passa la lista di provider al ProviderManager
        super(Arrays.asList(createAdminProvider(adminDAO, adminUserDetailsService, passwordEncoder)));
    }

    private static AdminAuthenticationProvider createAdminProvider(
            AdminDAO adminDAO,
            AdminUserDetailsService adminUserDetailsService,
            PasswordEncoder passwordEncoder) {
        
        return new AdminAuthenticationProvider(adminDAO, adminUserDetailsService, passwordEncoder);
    }
}
