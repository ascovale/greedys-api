package com.application.common.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import com.twilio.Twilio;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "true", matchIfMissing = false)
@Getter
@Slf4j
public class TwilioConfig {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.number}")
    private String whatsappNumber;

    @Value("${twilio.verify.service.sid}")
    private String verifyServiceSid;

    @Value("${twilio.verification.expiry.minutes:10}")
    private int verificationExpiryMinutes;

    @Value("${twilio.verification.max.attempts:3}")
    private int maxVerificationAttempts;

    @PostConstruct
    public void init() {
        log.info("🔧 TwilioConfig REALE attivato - Modalità servizi reali");
        Twilio.init(accountSid, authToken);
        log.info("✅ Twilio inizializzato con account: {}", accountSid);
    }
}


