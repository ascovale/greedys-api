package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.application.persistence.model.user.Notification;
import com.application.persistence.model.user.User;
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

    public void sendWhatsAppMessage(Notification notification) {
        String phoneNumber = notification.getClientUser().getNumero_di_telefono();
        String message = notification.getText();
        sendWhatsAppMessage(phoneNumber, message);
    }
    /* TODO modificare tutti i getCurrentUser in questo modo
    
    else if (principal instanceof UserDetails) {
            return userRepository.findByUsername(((UserDetails) principal).getUsername());
        } else {
            throw new IllegalStateException("Utente non autenticato");
        }*/
    protected User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }
}
