package com.application.spring;

import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {

    @Value("${TWILIO_ACCOUNT_SID}")
    private String accountSid;

    @Value("${TWILIO_AUTH_TOKEN}")
    private String authToken;

    @Value("${TWILIO_WHATSAPP_NUMBER}")
    private String whatsappNumber;

    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public String getWhatsAppNumber() {
        return whatsappNumber;
    }
}


