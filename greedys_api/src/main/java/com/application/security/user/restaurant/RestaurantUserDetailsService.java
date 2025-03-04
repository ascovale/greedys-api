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
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.security.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;

@Service("restaurantUserDetailsService")
@Transactional
public class RestaurantUserDetailsService implements UserDetailsService {

    private final RestaurantUserDAO userService;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public RestaurantUserDetailsService(RestaurantUserDAO userDAO, LoginAttemptService loginAttemptService,
            HttpServletRequest request) {
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
            Collection<? extends GrantedAuthority> authorities;
            final RestaurantUser user = userService.findByEmail(email);
            if (user == null) {
                throw new UsernameNotFoundException("No user found with username: " + email);
            } else if (isMultiRestaurantUser(email)) {
                authorities = getSwitchUserAuthorities();
                new org.springframework.security.core.userdetails.User(user.getEmail(),
                    user.getPassword(),
                    user.isEnabled(),
                    true,
                    true,
                    true,
                    authorities);
            } 
            return user;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UserDetails loadUserById(final Long restaurantUserId) throws UsernameNotFoundException {
        try {
            final RestaurantUser user = userService.findById(restaurantUserId).get();
            if (user == null) {
                throw new UsernameNotFoundException("No user found with ID: " + restaurantUserId);
            }

            return user;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
    public UserDetails loadSwitchUserById(final Long id) throws UsernameNotFoundException {
        try {
            final RestaurantUser user = userService.findById(id).get();
            if (user == null) {
                throw new UsernameNotFoundException("No user found with id: " + id);
            }
            if (!isMultiRestaurantUser(user.getEmail())) {
                throw new UsernameNotFoundException("User is not a multi-restaurant user with id: " + id);
            }
            Collection<? extends GrantedAuthority> authorities = getSwitchUserAuthorities();

            return new org.springframework.security.core.userdetails.User(user.getEmail(),
                    user.getPassword(),
                    user.isEnabled(),
                    true,
                    true,
                    true,
                    authorities);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMultiRestaurantUser(String email) {
        return userService.isMultiRestaurantUser(email);
    }

    private Collection<? extends GrantedAuthority> getSwitchUserAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("PRIVILEGE_SWITCH_TO_RESTAURANT_USER"));
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
