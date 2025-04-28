package com.application.spring;

import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Value("${mail.reservation.host}")
    private String reservationHost;

    @Value("${mail.reservation.port}")
    private int reservationPort;

    @Value("${mail.reservation.username}")
    private String reservationUsername;

    @Value("${mail.reservation.password}")
    private String reservationPassword;

    @PostConstruct
    public void logReservationMailSenderProperties() {
        System.out.println("Reservation Mail Sender Properties:");
        System.out.println("Host: " + reservationHost);
        System.out.println("Port: " + reservationPort);
        System.out.println("Username: " + reservationUsername);
        System.out.println("Password: [PROTECTED]");

    }

    @Bean
    @Qualifier("reservationMailSender")
    @ConfigurationProperties(prefix = "mail.reservation")
    public JavaMailSender reservationMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        try {
            System.out.println("Testing connection to SMTP server...");
            mailSender.testConnection();
            System.out.println("Connection successful.");
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("MessagingException: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("Caused by: " + cause.getMessage());
                cause = cause.getCause();
            }
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        return mailSender;
    }

    @Bean
    @Qualifier("supportMailSender")
    @ConfigurationProperties(prefix = "mail.support")
    public JavaMailSender supportMailSender() {
        return new JavaMailSenderImpl();
    }

}
