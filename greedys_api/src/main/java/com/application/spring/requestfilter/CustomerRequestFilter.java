package com.application.spring.requestfilter;

import org.springframework.stereotype.Component;

import com.application.security.jwt.JwtRequestFilter;
import com.application.security.jwt.JwtUtil;
import com.application.security.user.customer.CustomerUserDetailsService;

@Component
public class CustomerRequestFilter extends JwtRequestFilter {
    CustomerUserDetailsService userDetailsService;
    JwtUtil passwordEncoder;

    public CustomerRequestFilter(CustomerUserDetailsService userDetailsService, JwtUtil passwordEncoder) {
        super(userDetailsService, passwordEncoder);
    }
}
