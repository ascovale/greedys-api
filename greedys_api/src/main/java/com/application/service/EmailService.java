package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.customer.Notification;
import com.application.persistence.model.restaurant.user.RestaurantNotification;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.utils.NotificatioUtils;

@Service
@Transactional
public class EmailService {

    @Autowired
    @Qualifier("reservationMailSender")
    private JavaMailSender mailSender;

    private SimpleMailMessage constructNotificationMessage(Notification notification) {
        final String recipientAddress = notification.getCustomer().getEmail();

        notification.getType().toString();
        final String subject = NotificatioUtils.getUserTemplates().get(notification.getType()).getTitle() + "   "
                + notification.getId().toString();
        final String message = NotificatioUtils.getUserTemplates().get(notification.getType()).getMessage();
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        email.setFrom("reservation@greedys.it");
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
        final String recipientAddress = user.getEmail();
        final String subject = NotificatioUtils.getRestaurantTemplates().get(notification.getType()).getTitle()
                + "   "
                + notification.getId().toString();
        final String message = NotificatioUtils.getRestaurantTemplates().get(notification.getType())
                .getMessage();
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        // TODO: Impostare le diverse email per le varie tipologie di notifiche
        //email.setFrom("reservation@greedys.it");
        email.setFrom("reservation@greedys.it");
        return email;

    }

    @Async
    public void sendEmailNotification(RestaurantNotification notification) {

        /* 
        try {
            RestaurantUser user = notification.getRestaurantUser();
            if (user.getUserOptions().getNotificationPreferences().get(notification.getType())) {
                final SimpleMailMessage email = constructRestaurantNotificationMessage(notification);
                mailSender.send(email);
            } else {
                System.out.println("User " + user.getEmail() + " has disabled notifications for " + notification.getType());
            }
        } catch (Exception e) {
            // Log the exception and handle it accordingly
            System.err.println("Failed to send restaurant email: " + e.getMessage());
        }*/
    }

    // TODO: il link per rimuovere ristorante è sbagliato da sistemare
    // Creare link per rimuovere ristorante lato flutterApp
    private SimpleMailMessage constructRestaurantAssociationConfirmationMessage(RestaurantUser restaurantUser) {
        if (restaurantUser == null || restaurantUser.getRestaurant() == null || restaurantUser.getEmail() == null) {
            throw new IllegalArgumentException("Invalid restaurant user or restaurant details");
        }
        final String recipientAddress = restaurantUser.getEmail();
        final String subject = "Conferma associazione con ristorante";
        final String message = "Ciao " + ",\n\n" +
                "Sei stato associato al ristorante " + restaurantUser.getRestaurant().getName() + ".\n" +
                "Se non desideri essere associato a questo ristorante, clicca sul seguente link per rimuovere l'associazione:\n" +
                "http://example.com/removeAssociation?userId=" + restaurantUser.getId() + "&restaurantId=" + restaurantUser.getRestaurant().getId() + "\n\n" +
                "Grazie,\nIl team di supporto";
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message);
        email.setFrom("reservation@greedys.it");
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

    public void sendEmail(String email, String subject, String content) {
        try {
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Recipient email address cannot be null or empty");
            }
            System.out.println("Preparing to send email to: " + email); // Log per debug
            final SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(content);
            message.setFrom("reservation@greedys.it");
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + email); // Log per conferma
        } catch (Exception e) {
            System.err.println("Error while sending email: " + e.getMessage()); // Log dettagliato
            e.printStackTrace();
        }
    }

    public void sendEmail(SimpleMailMessage constructResendVerificationTokenEmail) {
        if (constructResendVerificationTokenEmail.getTo() == null || constructResendVerificationTokenEmail.getTo().length == 0) {
            throw new IllegalArgumentException("Recipient email address cannot be null or empty");
        }
        mailSender.send(constructResendVerificationTokenEmail);
    }   

    public void sendTestEmail(String to, String subject, String text) {
        try {
            System.out.println("Trying to send Test email to: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("Message: " + text);

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(to);
            email.setSubject(subject);
            email.setText(text);
            email.setFrom("reservation@greedys.it");

            System.out.println("Sending email...");
            mailSender.send(email);
            System.out.println("Email sent successfully to: " + to);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid email address: " + e.getMessage());
        } catch (org.springframework.mail.MailAuthenticationException e) {
            System.err.println("Authentication failed: " + e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public JavaMailSenderImpl getMailSender() {
        return (JavaMailSenderImpl) mailSender;
    }
}
