package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.spring.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
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
}
