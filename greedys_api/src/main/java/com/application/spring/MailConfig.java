package com.application.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    @Qualifier("reservationMailSender")
    @ConfigurationProperties(prefix = "mail.reservation")
    public JavaMailSender reservationMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    @Qualifier("supportMailSender")
    @ConfigurationProperties(prefix = "mail.support")
    public JavaMailSender supportMailSender() {
        return new JavaMailSenderImpl();
    }

    
}
