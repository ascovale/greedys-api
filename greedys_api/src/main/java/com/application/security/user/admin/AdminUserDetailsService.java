package com.application.security.user.admin;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.admin.AdminDAO;
import com.application.persistence.model.admin.Admin;
import com.application.security.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;

@Service("adminUserDetailsService")
@Transactional
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminDAO adminDAO;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public AdminUserDetailsService(AdminDAO adminDAO, LoginAttemptService loginAttemptService, HttpServletRequest request) {
        this.adminDAO = adminDAO;
        this.loginAttemptService = loginAttemptService;
        this.request = request;
    }

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException("blocked");
        }
        try {
            final Admin admin = adminDAO.findByEmail(email);
            if (admin == null) {
                throw new UsernameNotFoundException("No user found with username: " + email);
            }
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
