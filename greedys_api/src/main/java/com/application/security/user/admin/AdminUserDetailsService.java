package com.application.security.user.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.admin.AdminDAO;
import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.admin.AdminPrivilege;
import com.application.persistence.model.admin.AdminRole;
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
            return new org.springframework.security.core.userdetails.User(admin.getEmail(), 
            admin.getPassword(), 
                                                                          admin.isEnabled(), 
                                                                          true, 
                                                                          true, 
                                                                          true, 
                                                                          getAuthorities(admin.getAdminRoles()));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Collection<? extends GrantedAuthority> getAuthorities(final Collection<AdminRole> roles) {
        return getGrantedAuthorities(getPrivileges(roles));
    }

    private final List<String> getPrivileges(final Collection<AdminRole> roles) {
        final List<String> privileges = new ArrayList<String>();

        for (final AdminRole role : roles) {
            privileges.add(role.getName());
            for (final AdminPrivilege item : role.getAdminPrivileges()) {
                privileges.add(item.getName());
            }
        }
        
        return privileges;
    }

    private final List<GrantedAuthority> getGrantedAuthorities(final List<String> privileges) {
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }

    private final String getClientIP() {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

}
