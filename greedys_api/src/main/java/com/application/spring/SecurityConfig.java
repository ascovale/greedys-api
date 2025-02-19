package com.application.spring;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.application.security.jwt.JwtRequestFilter;
import com.application.security.user.customer.CustomerUserRememberMeServices;
import com.application.security.user.restaurant.RestaurantUserRememberMeServices;

@Configuration
@EnableWebSecurity
@ComponentScan({ "com.application.security" })
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService restaurantUserDetailsService;
    private final UserDetailsService customerUserDetailsService;
    private final JwtRequestFilter restaurantJwtRequestFilter;
    private final JwtRequestFilter customerJwtRequestFilter;

    public SecurityConfig(
            @Qualifier("restaurantUserDetailsService") UserDetailsService restaurantUserDetailsService,
            @Qualifier("customerUserDetailsService") UserDetailsService customerUserDetailsService,
            @Qualifier("customerJwtRequestFilter") JwtRequestFilter customerJwtRequestFilter,
            @Qualifier("restaurantJwtRequestFilter") JwtRequestFilter restaurantJwtRequestFilter,
            DataSource dataSource) {
        this.restaurantUserDetailsService = restaurantUserDetailsService;
        this.customerUserDetailsService = customerUserDetailsService;
        this.restaurantJwtRequestFilter = restaurantJwtRequestFilter;
        this.customerJwtRequestFilter = customerJwtRequestFilter;
    }

    @Bean
    SecurityFilterChain restaurantUserFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/restaurant_user/**")
                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authz -> authz

                        .requestMatchers(/* "/restaurant/public/**", "/restaurant_user/swagger-ui/**",
                                "/restaurant_user/v3/api-docs**",*/
                                /* "/restaurant_user/swagger-ui/**", "/restaurant_user/v3/api-docs**",*/
                                "/doc**", "/swagger-ui/**",
                                "/register/**", "/v3/api-docs*/**", "/api/**",
                                "/auth/**", "/restaurant/search*", "/restaurant/*/open-days*",
                                "/restaurant/*/day-slots*", "/restaurant/*/services",
                                "/reservation/**", "/error*", "/actuator/health", "/public/**"
                        )
                        .permitAll().anyRequest().authenticated())
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(restaurantJwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    SecurityFilterChain customerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/customer/**")
                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(/* "/customer/public/**", "/customer/swagger-ui/**", "/customer/v3/api-docs**",*/
                        
                                "/doc**", "/swagger-ui/**",
                                "/register/**", "/v3/api-docs*/**", "/api/**",
                                "/auth/**", "/restaurant/search*", "/restaurant/*/open-days*",
                                "/restaurant/*/day-slots*", "/restaurant/*/services",
                                "/reservation/**", "/error*", "/actuator/health", "/public/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(customerJwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*"); // Allow all origins
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization"); // Add exposed headers
        config.addExposedHeader("Content-Type");
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager1(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(restaurantUserDetailsService)
                .passwordEncoder(passwordEncoder());

        return auth.build();
    }

    @Bean
    AuthenticationManager authenticationManager2(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(customerUserDetailsService)
                .passwordEncoder(passwordEncoder());

        return auth.build();
    }

    @Bean(name = "userEncoder")
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(name = "userSessionRegistry")
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean(name = "customerUserRememberMe")
    RememberMeServices rememberMeServices1() {
        return new CustomerUserRememberMeServices("theKey", customerUserDetailsService,
                new InMemoryTokenRepositoryImpl());
    }

    @Bean(name = "restaurantUserRememberMe")
    RememberMeServices rememberMeServices2() {
        return new RestaurantUserRememberMeServices("theKey", restaurantUserDetailsService,
                new InMemoryTokenRepositoryImpl());
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

}
