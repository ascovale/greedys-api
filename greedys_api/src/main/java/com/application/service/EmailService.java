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

    //TODO il link per rimuovere ristorante è sbagliato da sistemare
    // Creare link per rimuovere ristorante lato flutterApp
    private SimpleMailMessage constructRestaurantAssociationConfirmationMessage(RestaurantUser restaurantUser) {
        final String recipientAddress = restaurantUser.getUser().getEmail();
        final String subject = "Conferma associazione con ristorante";
        final String message = "Ciao " + restaurantUser.getUser().getName() + ",\n\n" +
                "Sei stato associato al ristorante " + restaurantUser.getRestaurant().getName() + ".\n" +
                "Se non desideri essere associato a questo ristorante, clicca sul seguente link per rimuovere l'associazione:\n" +
                "http://example.com/removeAssociation?userId=" + restaurantUser.getUser().getId() + "&restaurantId=" + restaurantUser.getRestaurant().getId() + "\n\n" +
                "Grazie,\nIl team di supporto";
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        email.setFrom(env.getProperty("support.email"));
        return email;
    }

    @Async
    public void sendRestaurantAssociationConfirmationEmail(RestaurantUser restaurantUser) {
        try {
            final SimpleMailMessage email = constructRestaurantAssociationConfirmationMessage(restaurantUser);
            mailSender.send(email);
        } catch (Exception e) {
            // Log the exception and handle it accordingly
            System.err.println("Failed to send restaurant association confirmation email: " + e.getMessage());
        }
    }

}
