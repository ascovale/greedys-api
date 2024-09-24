package com.application.security.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.user.Privilege;
import com.application.persistence.model.user.Role;
import com.application.persistence.model.user.User;
import com.application.security.LoginAttemptService;

@Service("userDetailsService")
@Transactional
public class UserUserDetailsService implements UserDetailsService {

    private final UserDAO userService;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public UserUserDetailsService(UserDAO userDAO, LoginAttemptService loginAttemptService, HttpServletRequest request) {
        this.userService = userDAO;
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
            final User user = userService.findByEmail(email);
            if (user == null) {
                throw new UsernameNotFoundException("No user found with username: " + email);
            }
            return new org.springframework.security.core.userdetails.User(user.getEmail(), 
                                                                          user.getPassword(), 
                                                                          user.isEnabled(), 
                                                                          true, 
                                                                          true, 
                                                                          true, 
                                                                          getAuthorities(user.getRoles()));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Collection<? extends GrantedAuthority> getAuthorities(final Collection<Role> roles) {
        return getGrantedAuthorities(getPrivileges(roles));
    }

    private final List<String> getPrivileges(final Collection<Role> roles) {
        final List<String> privileges = new ArrayList<String>();

        for (final Role role : roles) {
            privileges.add(role.getName());
            for (final Privilege item : role.getPrivileges()) {
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
