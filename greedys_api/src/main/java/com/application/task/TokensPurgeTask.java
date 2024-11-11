package com.application.task;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.user.PasswordResetTokenDAO;
import com.application.persistence.dao.user.VerificationTokenDAO;

@Service
@Transactional
public class TokensPurgeTask {
    @Autowired
    VerificationTokenDAO tokenRepository;

    @Autowired
    PasswordResetTokenDAO passwordTokenRepository;

    //@Scheduled(cron = "${purge.cron.expression}")
    public void purgeExpired() {

        LocalDate now = LocalDate.from(Instant.now());
        passwordTokenRepository.deleteAllExpiredSince(now);
        tokenRepository.deleteAllExpiredSince(now);
    }
}
