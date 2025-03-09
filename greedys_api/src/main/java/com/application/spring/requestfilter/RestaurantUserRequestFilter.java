package com.application.spring.requestfilter;

import org.springframework.stereotype.Component;

import com.application.security.jwt.JwtRequestFilter;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.customer.CustomerUserDetailsService;
import com.application.security.user.restaurant.RestaurantUserDetailsService;

@Component
public class RestaurantUserRequestFilter extends JwtRequestFilter {
    CustomerUserDetailsService userDetailsService;
    JwtUtil passwordEncoder;

    public RestaurantUserRequestFilter(RestaurantUserDetailsService userDetailsService, JwtUtil passwordEncoder) {
        super(userDetailsService, passwordEncoder);
    }
}
