package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.application.persistence.model.restaurant.RestaurantNotification;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.persistence.model.user.Notification;
import com.application.service.utils.NotificatioUtils;

@Service
public class EmailService {

    @Autowired
    @Qualifier("getUserMailSender")
    private JavaMailSender mailSender;

    @Autowired
    private Environment env;

    private SimpleMailMessage constructNotificationMessage(Notification notification) {
        final String recipientAddress = notification.getClientUser().getEmail();

        notification.getType().toString();
        final String subject = NotificatioUtils.getUserTemplates().get(notification.getType()).getTitle() + "   "
                + notification.getId().toString();
        final String message = NotificatioUtils.getUserTemplates().get(notification.getType()).getMessage();
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        email.setFrom(env.getProperty("support.email"));
        return email;
    }

    @Async
    public void sendEmailNotification(Notification notification) {
        try {
            final SimpleMailMessage email = constructNotificationMessage(notification);
            mailSender.send(email);
        } catch (Exception e) {
            // Log the exception and handle it accordingly
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    private SimpleMailMessage constructRestaurantNotificationMessage(RestaurantNotification notification) {
        RestaurantUser user = notification.getRestaurantUser();
        final String recipientAddress = user.getUser().getEmail();
        final String subject = NotificatioUtils.getRestaurantTemplates().get(notification.getType()).getTitle()
                + "   "
                + notification.getId().toString();
        final String message = NotificatioUtils.getRestaurantTemplates().get(notification.getType())
                .getMessage();
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        email.setFrom(env.getProperty("support.email"));
        return email;

    }

    @Async
    public void sendEmailNotification(RestaurantNotification notification) {
        try {
            RestaurantUser user = notification.getRestaurantUser();
            if (user.getUserOptions().getNotificationPreferences().get(notification.getType())) {
                final SimpleMailMessage email = constructRestaurantNotificationMessage(notification);
                mailSender.send(email);
            } else {
                System.out.println("User " + user.getUser().getEmail() + " has disabled notifications for " + notification.getType());
            }
        } catch (Exception e) {
            // Log the exception and handle it accordingly
            System.err.println("Failed to send restaurant email: " + e.getMessage());
        }
    }

}
