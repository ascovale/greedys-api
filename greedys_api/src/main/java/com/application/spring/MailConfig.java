package com.application.spring;

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
        // Optionally print TLS/SSL info for debugging
        System.out.println("Using STARTTLS on port 587: " + (reservationPort == 587));
    }

    @Bean
    @Qualifier("reservationMailSender")
    @ConfigurationProperties(prefix = "mail.reservation")
    public JavaMailSender reservationMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        return mailSender;
    }

    @Bean
    @Qualifier("supportMailSender")
    @ConfigurationProperties(prefix = "mail.support")
    public JavaMailSender supportMailSender() {
        return new JavaMailSenderImpl();
    }

}
