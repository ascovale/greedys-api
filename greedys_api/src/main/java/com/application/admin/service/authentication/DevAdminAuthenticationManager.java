package com.application.admin.service.authentication;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock AdminAuthenticationManager for dev-minimal profile.
 * Extends AdminAuthenticationManager but bypasses actual authentication.
 */
@Component("adminAuthenticationManager")
@Profile("dev-minimal")
@Slf4j
public class DevAdminAuthenticationManager extends AdminAuthenticationManager {

    public DevAdminAuthenticationManager() {
        // Call protected parent constructor with dev provider
        super(List.of(new DevAuthenticationProvider()));
        log.info("🔧 DEV-MINIMAL: Admin authentication manager initialized (mock)");
    }

    /**
     * Dev-only provider that accepts all authentication requests.
     */
    private static class DevAuthenticationProvider implements AuthenticationProvider {
        
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            String email = authentication.getName();
            // Return authenticated token for any admin
            return new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return true;
        }
    }
}
