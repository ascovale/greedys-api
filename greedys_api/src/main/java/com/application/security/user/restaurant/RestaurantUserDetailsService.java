package com.application.security.user.restaurant;

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

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantRole;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.security.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;

@Service("restaurantUserDetailsService")
@Transactional
public class RestaurantUserDetailsService implements UserDetailsService {
    
    private final RestaurantUserDAO userService;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public RestaurantUserDetailsService(RestaurantUserDAO userDAO, LoginAttemptService loginAttemptService, HttpServletRequest request) {
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
            final RestaurantUser user = userService.findByEmail(email);
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

    private final Collection<? extends GrantedAuthority> getAuthorities(final Collection<RestaurantRole> roles) {
        return getGrantedAuthorities(getPrivileges(roles));
    }

    private final List<String> getPrivileges(final Collection<RestaurantRole> roles) {
        final List<String> privileges = new ArrayList<String>();

        for (final RestaurantRole role : roles) {
            privileges.add(role.getName());
            for (final RestaurantPrivilege item : role.getPrivileges()) {
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
