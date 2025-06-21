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

import com.application.persistence.dao.restaurant.RUserPasswordResetTokenDAO;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.persistence.model.restaurant.user.RUserPasswordResetToken;
import com.application.security.user.ISecurityUserService;

@Service
@Transactional
@Qualifier("RUserSecurityService")
public class RUserSecurityService implements ISecurityUserService {

    @Autowired
    private RUserPasswordResetTokenDAO passwordTokenRepository;

    // API
    @Override
    public String validatePasswordResetToken(String token) {
        final RUserPasswordResetToken passToken = passwordTokenRepository.findByToken(token);
        if ((passToken == null)) {
            return "invalidToken";
        }
        final LocalDateTime now = LocalDateTime.now();
        if (passToken.getExpiryDate().isBefore(now)) {
            return "expired";
        }

        final RUser user = passToken.getRUser();
        final Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Arrays.asList(new SimpleGrantedAuthority("CHANGE_PASSWORD_PRIVILEGE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return null;
    }

}