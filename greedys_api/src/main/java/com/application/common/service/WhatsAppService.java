package com.application.common.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.spring.TwilioConfig;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.CustomerNotification;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final TwilioConfig twilioConfig;

    public void sendWhatsAppMessage(String phoneNumber, String message) {
        Message.creator(
                new PhoneNumber("whatsapp:" + phoneNumber),
                new PhoneNumber("whatsapp:" + twilioConfig.getWhatsappNumber()),
                message)
                .create();
        log.info("Messaggio inviato a {}", phoneNumber);
    }

    public void sendWhatsAppMessage(CustomerNotification notification) {
        String phoneNumber = notification.getCustomer().getPhoneNumber();
        String message = notification.getBody();
        sendWhatsAppMessage(phoneNumber, message);
    }
  
    protected Customer getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Customer) principal);
        } else {
            log.warn("Questo non dovrebbe succedere");
            return null;
        }
    }
}
