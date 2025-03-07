package com.application.security.user.restaurant;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantUserPasswordResetTokenDAO;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserPasswordResetToken;
import com.application.security.user.ISecurityUserService;

@Service
@Transactional
@Qualifier("restaurantUserSecurityService")
public class RestaurantUserSecurityService implements ISecurityUserService {

    @Autowired
    private RestaurantUserPasswordResetTokenDAO passwordTokenRepository;

    // API
    @Override
    public String validatePasswordResetToken(long id, String token) {
        final RestaurantUserPasswordResetToken passToken = passwordTokenRepository.findByToken(token);
        if ((passToken == null) || (passToken.getRestaurantUser().getId() != id)) {
            return "invalidToken";
        }
        final LocalDateTime now = LocalDateTime.now();
        if (passToken.getExpiryDate().isBefore(now)) {
            return "expired";
        }

        final RestaurantUser user = passToken.getRestaurantUser();
        final Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Arrays.asList(new SimpleGrantedAuthority("CHANGE_PASSWORD_PRIVILEGE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return null;
    }

}