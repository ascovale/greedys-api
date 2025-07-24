package com.application.common.security.jwt.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminPasswordResetTokenDAO;
import com.application.admin.persistence.dao.AdminVerificationTokenDAO;
import com.application.customer.persistence.dao.PasswordResetTokenDAO;
import com.application.customer.persistence.dao.VerificationTokenDAO;
import com.application.restaurant.persistence.dao.RUserPasswordResetTokenDAO;
import com.application.restaurant.persistence.dao.RUserVerificationTokenDAO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TokensPurgeTask {
    
    // Customer tokens (nomi originali)
    private final VerificationTokenDAO tokenRepository;

    private final PasswordResetTokenDAO passwordTokenRepository;
    
    // Admin tokens
    private final AdminVerificationTokenDAO adminVerificationTokenRepository;
    
    private final AdminPasswordResetTokenDAO adminPasswordTokenRepository;
    
    // Restaurant tokens
    private final RUserVerificationTokenDAO restaurantVerificationTokenRepository;
    
    private final RUserPasswordResetTokenDAO restaurantPasswordTokenRepository;

    @Scheduled(cron = "${purge.cron.expression}")
    public void purgeExpired() {
        log.info("Starting token purge task");
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
        
        log.info("Token purge task completed successfully");
    }
}
