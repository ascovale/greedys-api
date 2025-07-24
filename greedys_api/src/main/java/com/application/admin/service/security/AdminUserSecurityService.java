package com.application.admin.service.security;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminPasswordResetTokenDAO;
import com.application.admin.persistence.model.Admin;
import com.application.admin.persistence.model.AdminPasswordResetToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminUserSecurityService {

    private final AdminPasswordResetTokenDAO passwordTokenRepository;

    public String validatePasswordResetToken(String token) {
        final AdminPasswordResetToken passToken = passwordTokenRepository.findByToken(token);
        if ((passToken == null) ) {
            return "invalidToken";
        }
        final LocalDateTime now = LocalDateTime.now();
        if (passToken.getExpiryDate().isBefore(now)) {
            return "expired";
        }

        final Admin user = passToken.getAdmin();
        final Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Arrays.asList(new SimpleGrantedAuthority("CHANGE_PASSWORD_PRIVILEGE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return null;
    }

}