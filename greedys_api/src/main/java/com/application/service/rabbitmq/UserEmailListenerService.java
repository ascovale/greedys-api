package com.application.service.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.application.service.rabbitmq.email.UserEmail;

@Service
public class UserEmailListenerService {

    @Autowired
    @Qualifier("getUserMailSender")
    private JavaMailSender userEmailSender;

    @RabbitListener(queues = "userEmailQueue")
    public void receiveUserMessage(UserEmail userEmail) {
        sendEmail(userEmail);
    }

    private void sendEmail(UserEmail userEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail.getEmail());
        message.setSubject(userEmail.getSubject());
        message.setText(userEmail.getText());
        //message.setFrom("noreply@example.com"); Prendere mail da file di configurazione
        userEmailSender.send(message);
        System.out.println("Email inviata a: " + userEmail.getEmail());
    }
}