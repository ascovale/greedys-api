package com.application.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.application.security.jwt.JwtRequestFilter;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.admin.AdminUserDetailsService;
import com.application.security.user.customer.CustomerUserDetailsService;
import com.application.security.user.restaurant.RestaurantUserDetailsService;


@Configuration
public class JwtConfig {

    @Bean
    @Qualifier("customerJwtRequestFilter")
    JwtRequestFilter jwtRequestFilter(CustomerUserDetailsService userDetailsService, JwtUtil passwordEncoder) {
        return new JwtRequestFilter(userDetailsService, passwordEncoder);
    }
    @Bean
    @Qualifier("restaurantJwtRequestFilter")
    JwtRequestFilter restaurantJwtRequestFilter(RestaurantUserDetailsService restaurantUserDetailsService, JwtUtil jwtUtil) {
        return new JwtRequestFilter(restaurantUserDetailsService, jwtUtil);
    }

    @Bean
    @Qualifier("adminJwtRequestFilter")
    JwtRequestFilter adminJwtRequestFilter(AdminUserDetailsService adminUserDetailsService, JwtUtil jwtUtil) {
        return new JwtRequestFilter(adminUserDetailsService, jwtUtil);
    }
}