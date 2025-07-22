package com.application.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.application.admin.AdminAuthenticationProvider;
import com.application.admin.AdminRequestFilter;
import com.application.admin.service.security.AdminUserDetailsService;
import com.application.customer.CustomerAuthenticationProvider;
import com.application.customer.CustomerRequestFilter;
import com.application.customer.service.security.CustomerUserDetailsService;
import com.application.restaurant.RUserAuthenticationProvider;
import com.application.restaurant.RUserRequestFilter;
import com.application.restaurant.service.security.RUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@ComponentScan({ "com.application.security" })
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final RUserDetailsService RUserDetailsService;
        private final CustomerUserDetailsService customerUserDetailsService;
        private final AdminUserDetailsService adminUserDetailsService;
        private final RUserRequestFilter restaurantJwtRequestFilter;
        private final CustomerRequestFilter customerJwtRequestFilter;
        private final AdminRequestFilter adminJwtRequestFilter;

        // TODO: make sure the authentication filter is not required for login,
        // registration, and other public operations
        @Bean
        SecurityFilterChain RUserFilterChain(HttpSecurity http,
                        @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager)
                        throws Exception {
                http
                                .securityMatcher("/restaurant/**")
                                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz

                                                .requestMatchers(
                                                                "/doc**", "/swagger-ui/**",
                                                                "/register/**", "/v3/api-docs*/**", "/api/**",
                                                                "/auth/**",
                                                                "/reservation/**", "/error*", "/actuator/health",
                                                                "/public/**", "/restaurant/user/auth/login",
                                                                "/restaurant/user/auth/google",
                                                                "/logo_api.png", // accesso libero al logo
                                                                "/swagger-groups" // accesso libero alla lista gruppi
                                                                                  // swagger
                                                )
                                                .permitAll()
                                                .requestMatchers("/restaurant/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(restaurantJwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                                .authenticationManager(authenticationManager);

                return http.build();
        }

        @Bean
        SecurityFilterChain customerFilterChain(HttpSecurity http,
                        @Qualifier("customerAuthenticationManager") AuthenticationManager authenticationManager)
                        throws Exception {
                http
                                .securityMatcher("/customer/**")
                                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(
                                                                /*
                                                                 * "/customer/public/**", "/customer/swagger-ui/**",
                                                                 * "/customer/v3/api-docs**",
                                                                 */
                                                                "/doc**", "/swagger-ui/**",
                                                                "/register/**", "/v3/api-docs*/**", "/api/**",
                                                                "/auth/**",
                                                                "/reservation/**", "/error*", "/actuator/health",
                                                                "/public/**", "/customer/auth/**",
                                                                "/customer/user/auth/google",
                                                                "/logo_api.png", // free access to the logo
                                                                "/swagger-groups", // free access to the swagger groups
                                                                                   // list
                                                                "/customer/restaurant/**" // Makes all
                                                                                          // customer/restaurant
                                                                                          // endpoints public
                                                )
                                                .permitAll()
                                                .requestMatchers("/customer/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(customerJwtRequestFilter, UsernamePasswordAuthenticationFilter.class)

                                .authenticationManager(authenticationManager);

                return http.build();
        }

        @Bean
        SecurityFilterChain adminFilterChain(HttpSecurity http,
                        @Qualifier("adminAuthenticationManager") AuthenticationManager authenticationManager)
                        throws Exception {
                http
                                .securityMatcher("/admin/**")
                                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(
                                                                "/doc**", "/swagger-ui/**",
                                                                "/register/**", "/v3/api-docs*/**", "/api/**",
                                                                "/auth/**",
                                                                "/reservation/**", "/error*", "/actuator/health",
                                                                "/public/**", "/admin/auth/**",
                                                                "/logo_api.png", // accesso libero al logo
                                                                "/swagger-groups" // accesso libero alla lista gruppi
                                                                                  // swagger
                                                )
                                                .permitAll()
                                                .requestMatchers("/admin/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(adminJwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                                .authenticationManager(authenticationManager);

                return http.build();
        }

        @Bean
        @Primary
        AuthenticationManager fallbackAuthenticationManager() {
                return authentication -> {
                        throw new UnsupportedOperationException(
                                        "No global AuthenticationManager configured. Use specific ones with @Qualifier.");
                };
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
        @Qualifier("restaurantAuthenticationManager")
        AuthenticationManager restaurantAuthenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
                auth.authenticationProvider(RUserAuthenticationProvider());
                return auth.build();
        }

        @Bean
        RUserAuthenticationProvider RUserAuthenticationProvider() {
                RUserAuthenticationProvider provider = new RUserAuthenticationProvider();
                provider.setUserDetailsService(RUserDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        @Qualifier("customerAuthenticationManager")
        AuthenticationManager customerAuthenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
                auth.authenticationProvider(customerAuthenticationProvider());
                return auth.build();
        }

        @Bean
        CustomerAuthenticationProvider customerAuthenticationProvider() {
                CustomerAuthenticationProvider provider = new CustomerAuthenticationProvider();
                provider.setUserDetailsService(customerUserDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        @Qualifier("adminAuthenticationManager")
        AuthenticationManager adminAuthenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
                auth.authenticationProvider(adminAuthenticationProvider());
                return auth.build();
        }

        @Bean
        @Primary
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        AdminAuthenticationProvider adminAuthenticationProvider() {
                AdminAuthenticationProvider provider = new AdminAuthenticationProvider(adminUserDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        //TODO RIMOVERE TOGLIERE REMEMBER ME FORSE METTERE refreshTOKEN


        @Bean
        HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }
}
