package com.application.common.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.dao.AdminPasswordResetTokenDAO;
import com.application.admin.dao.AdminVerificationTokenDAO;
import com.application.customer.dao.PasswordResetTokenDAO;
import com.application.customer.dao.VerificationTokenDAO;
import com.application.restaurant.dao.RUserPasswordResetTokenDAO;
import com.application.restaurant.dao.RUserVerificationTokenDAO;

@Service
@Transactional
public class TokensPurgeTask {
    
    // Customer tokens (nomi originali)
    @Autowired
    VerificationTokenDAO tokenRepository;

    @Autowired
    PasswordResetTokenDAO passwordTokenRepository;
    
    // Admin tokens
    @Autowired
    AdminVerificationTokenDAO adminVerificationTokenRepository;
    
    @Autowired
    AdminPasswordResetTokenDAO adminPasswordTokenRepository;
    
    // Restaurant tokens
    @Autowired
    RUserVerificationTokenDAO restaurantVerificationTokenRepository;
    
    @Autowired
    RUserPasswordResetTokenDAO restaurantPasswordTokenRepository;

    @Scheduled(cron = "${purge.cron.expression}")
    public void purgeExpired() {
        LocalDate nowDate = LocalDate.from(Instant.now());
        LocalDateTime nowDateTime = LocalDateTime.now();
        
        // Purge customer tokens (LocalDate)
        passwordTokenRepository.deleteAllExpiredSince(nowDate);
        tokenRepository.deleteAllExpiredSince(nowDate);
        
        // Purge admin tokens (LocalDate)
        adminPasswordTokenRepository.deleteAllExpiredSince(nowDate);
        adminVerificationTokenRepository.deleteAllExpiredSince(nowDate);
        
        // Purge restaurant tokens (LocalDateTime)
        restaurantPasswordTokenRepository.deleteAllExpiredSince(nowDateTime);
        restaurantVerificationTokenRepository.deleteAllExpiredSince(nowDateTime);
    }
}
