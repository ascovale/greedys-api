package com.application.security.user.admin;

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

import com.application.persistence.dao.admin.AdminPasswordResetTokenDAO;
import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.admin.AdminPasswordResetToken;
import com.application.security.user.ISecurityUserService;

@Service
@Transactional
@Qualifier("adminSecurityService")
public class AdminUserSecurityService implements ISecurityUserService {

    @Autowired
    private AdminPasswordResetTokenDAO passwordTokenRepository;

    // API

    @Override
    public String validatePasswordResetToken(long id, String token) {
        final AdminPasswordResetToken passToken = passwordTokenRepository.findByToken(token);
        if ((passToken == null) || (passToken.getAdmin().getId() != id)) {
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