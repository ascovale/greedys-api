package com.application.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import com.application.security.jwt.JwtRequestFilter;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.UserUserDetailsService;


@Configuration
public class JwtConfig {

    @Bean
    JwtRequestFilter jwtRequestFilter(UserUserDetailsService userDetailsService, JwtUtil passwordEncoder) {
        return new JwtRequestFilter(userDetailsService, passwordEncoder);
    }
}