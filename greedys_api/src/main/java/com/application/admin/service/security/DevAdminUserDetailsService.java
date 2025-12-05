package com.application.admin.service.security;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.persistence.model.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock AdminUserDetailsService for dev-minimal profile.
 * 
 * This class provides the same functionality as AdminUserDetailsService but without
 * the LoginAttemptService and HttpServletRequest dependencies which may not be 
 * available in dev-minimal profile.
 * 
 * The real AdminUserDetailsService has @Profile("!dev-minimal") so it's excluded
 * when running with dev-minimal profile. This class fills that gap.
 */
@Service("adminUserDetailsService")
@Transactional
@RequiredArgsConstructor
@Slf4j
@Profile("dev-minimal")
@Primary
public class DevAdminUserDetailsService implements UserDetailsService {

    private final AdminDAO adminDAO;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("🚀 DevAdminUserDetailsService.loadUserByUsername called for: {}", email);
        
        try {
            Admin admin = adminDAO.findByEmail(email);
            if (admin == null) {
                throw new UsernameNotFoundException("No admin found with email: " + email);
            }
            // Force lazy loading of authorities
            admin.getAuthorities().size();
            return admin;
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading admin user: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
