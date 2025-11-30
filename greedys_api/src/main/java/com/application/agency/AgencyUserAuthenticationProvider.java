package com.application.agency;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.application.agency.service.security.AgencyUserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgencyUserAuthenticationProvider extends DaoAuthenticationProvider {

    public AgencyUserAuthenticationProvider(AgencyUserDetailsService userDetailsService,
                                           PasswordEncoder passwordEncoder) {
        super(userDetailsService);
        super.setPasswordEncoder(passwordEncoder);
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        try {
            // Per ora solo autenticazione normale tramite password
            // TODO: Implementare bypass authentication con AgencyUserAuthenticationDetails se necessario
            return super.authenticate(auth);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}