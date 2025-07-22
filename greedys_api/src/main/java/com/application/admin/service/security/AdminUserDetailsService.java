package com.application.admin.service.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.dao.AdminDAO;
import com.application.admin.model.Admin;
import com.application.common.security.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("adminUserDetailsService")
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminDAO adminDAO;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException("blocked");
        }
        try {
            final Admin admin = adminDAO.findByEmail(email);
            if (admin == null) {
                throw new UsernameNotFoundException("No admin found with username: " + email);
            }

            // Forza il caricamento lazy delle autorità
            admin.getAuthorities().size();

            return admin;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String getClientIP() {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

}
