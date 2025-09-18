package com.application.common.spring.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    /**
     * Configurazione rate limiting per prenotazioni
     * Limite: 10 prenotazioni per utente ogni 10 minuti
     */
    @Bean
    public Duration reservationRateLimit() {
        return Duration.ofMinutes(10);
    }

    /**
     * Limite per richieste di autenticazione
     * Limite: 5 tentativi per IP ogni 5 minuti
     */
    @Bean 
    public Duration authRateLimit() {
        return Duration.ofMinutes(5);
    }
}
