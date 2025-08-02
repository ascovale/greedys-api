package com.application.common.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MailConfig {

    @Bean
    @Qualifier("reservationMailSender")
    @ConfigurationProperties(prefix = "mail.reservation")
    JavaMailSender reservationMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        log.info("Configured reservation mail sender with properties from mail.reservation.*");
        return mailSender;
    }

}
