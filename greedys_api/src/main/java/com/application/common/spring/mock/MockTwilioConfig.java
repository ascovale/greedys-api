package com.application.common.spring.mock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.application.common.spring.TwilioConfig;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock Twilio Configuration per sviluppo minimal
 * Si attiva solo quando twilio.enabled=false
 */
@Configuration
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "false", matchIfMissing = true)
@Getter
@Primary
@Slf4j
public class MockTwilioConfig extends TwilioConfig {

    public MockTwilioConfig() {
        log.warn("🔧 MOCK: TwilioConfig attivato - modalità sviluppo minimal");
    }

    @Override
    public String getAccountSid() {
        return "MOCK_ACCOUNT_SID";
    }

    @Override
    public String getAuthToken() {
        return "MOCK_AUTH_TOKEN";
    }

    @Override
    public String getWhatsappNumber() {
        return "+1234567890";
    }

    @Override
    public String getVerifyServiceSid() {
        return "MOCK_VERIFY_SERVICE_SID";
    }

    @Override
    public int getVerificationExpiryMinutes() {
        return 10;
    }

    @Override
    public int getMaxVerificationAttempts() {
        return 3;
    }
}
