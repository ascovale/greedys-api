package com.application.service.rabbitMQ;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.application.service.rabbitMQ.email.ReservationEmail;
import com.application.service.rabbitMQ.email.UserEmail;

@Service
public class ReservationEmailListenerService {

    @Autowired
    @Qualifier("getReservationMailSender")
    private JavaMailSender reservationEmailSender;

    @RabbitListener(queues = "reservationEmailQueue")
    public void receiveUserMessage(ReservationEmail reservationEmail) {
        sendEmail(reservationEmail);
    }

    private void sendEmail(ReservationEmail reservationEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(reservationEmail.getEmail());
        message.setSubject(reservationEmail.getSubject());
        message.setText(reservationEmail.getText());
        // message.setFrom(reservationEmailSender.
        //message.setFrom("your_email@example.com"); Prendere mail da file di configurazione
        reservationEmailSender.send(message);
        System.out.println("Email inviata a: " + reservationEmail.getEmail());
    }
}