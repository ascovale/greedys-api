package com.application.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
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
        log.info("Reservation Mail Sender Properties:");
        log.info("Host: {}", reservationHost);
        log.info("Port: {}", reservationPort);
        log.info("Username: {}", reservationUsername);
        log.info("Password: [PROTECTED]");
        log.info("Using STARTTLS on port 587: {}", (reservationPort == 587));
    }

    @Bean
    @Qualifier("reservationMailSender")
    @ConfigurationProperties(prefix = "mail.reservation")
    JavaMailSender reservationMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        return mailSender;
    }

    @Bean
    @Qualifier("supportMailSender")
    @ConfigurationProperties(prefix = "mail.support")
    JavaMailSender supportMailSender() {
        return new JavaMailSenderImpl();
    }

}
