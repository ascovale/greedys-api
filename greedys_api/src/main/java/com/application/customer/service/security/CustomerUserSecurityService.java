package com.application.customer.service.security;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.customer.persistence.dao.PasswordResetTokenDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.PasswordResetToken;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerUserSecurityService {

    private final PasswordResetTokenDAO passwordTokenRepository;

    public String validatePasswordResetToken(String token) {
        final PasswordResetToken passToken = passwordTokenRepository.findByToken(token);
        if ((passToken == null) ) {
            return "invalidToken";
        }
        final LocalDateTime now = LocalDateTime.now();
        if (passToken.getExpiryDate().isBefore(now)) {
            return "expired";
        }

        final Customer user = passToken.getCustomer();
        final Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Arrays.asList(new SimpleGrantedAuthority("CHANGE_PASSWORD_PRIVILEGE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return null;
    }

}