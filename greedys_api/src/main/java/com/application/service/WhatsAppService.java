package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.notification.CustomerNotification;
import com.application.spring.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
@Transactional
public class WhatsAppService {

    @Autowired
    private TwilioConfig twilioConfig;

    public void sendWhatsAppMessage(String phoneNumber, String message) {
        Message.creator(
                new PhoneNumber("whatsapp:" + phoneNumber),
                new PhoneNumber("whatsapp:" + twilioConfig.getWhatsAppNumber()),
                message)
                .create();
        System.out.println("Messaggio inviato a " + phoneNumber);
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
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }
}
