package com.application.spring;

import javax.sql.DataSource;

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
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.application.security.google2fa.AdminAuthenticationProvider;
import com.application.security.google2fa.CustomerAuthenticationProvider;
import com.application.security.google2fa.RestaurantUserAuthenticationProvider;
import com.application.security.user.admin.AdminUserDetailsService;
import com.application.security.user.admin.AdminUserRememberMeServices;
import com.application.security.user.customer.CustomerUserDetailsService;
import com.application.security.user.customer.CustomerUserRememberMeServices;
import com.application.security.user.restaurant.RestaurantUserDetailsService;
import com.application.security.user.restaurant.RestaurantUserRememberMeServices;
import com.application.spring.requestfilter.AdminRequestFilter;
import com.application.spring.requestfilter.CustomerRequestFilter;
import com.application.spring.requestfilter.RestaurantUserRequestFilter;

@Configuration
@EnableWebSecurity
@ComponentScan({ "com.application.security" })
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private final RestaurantUserDetailsService restaurantUserDetailsService;
        private final CustomerUserDetailsService customerUserDetailsService;
        private final AdminUserDetailsService adminUserDetailsService;
        private final RestaurantUserRequestFilter restaurantJwtRequestFilter;
        private final CustomerRequestFilter customerJwtRequestFilter;
        private final AdminRequestFilter adminJwtRequestFilter;

        public SecurityConfig(
                        RestaurantUserDetailsService restaurantUserDetailsService,
                        CustomerUserDetailsService customerUserDetailsService,
                        AdminUserDetailsService adminUserDetailsService,
                        RestaurantUserRequestFilter restaurantJwtRequestFilter,
                        CustomerRequestFilter customerJwtRequestFilter,
                        AdminRequestFilter adminJwtRequestFilter,
                        DataSource dataSource) {
                this.restaurantUserDetailsService = restaurantUserDetailsService;
                this.customerUserDetailsService = customerUserDetailsService;
                this.adminUserDetailsService = adminUserDetailsService;
                this.restaurantJwtRequestFilter = restaurantJwtRequestFilter;
                this.customerJwtRequestFilter = customerJwtRequestFilter;
                this.adminJwtRequestFilter = adminJwtRequestFilter;
        }

        @Bean
        SecurityFilterChain restaurantUserFilterChain(HttpSecurity http,
                        @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager)
                        throws Exception {
                http
                                .securityMatcher("/restaurant/**")
                                .requiresChannel(channel -> channel.anyRequest().requiresSecure())
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz

                                                .requestMatchers(/*
                                                                  * "/restaurant/public/**",
                                                                  * "/restaurant_user/swagger-ui/**",
                                                                  * "/restaurant_user/v3/api-docs**",
                                                                  */
                                                                /*
                                                                 * "/restaurant_user/swagger-ui/**",
                                                                 * "/restaurant_user/v3/api-docs**",
                                                                 */
                                                                "/doc**", "/swagger-ui/**",
                                                                "/register/**", "/v3/api-docs*/**", "/api/**",
                                                                "/auth/**",
                                                                "/reservation/**", "/error*", "/actuator/health",
                                                                "/public/**","/restaurant/user/auth/login")
                                                .permitAll().requestMatchers("/restaurant/user/**").authenticated())
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
                                                .requestMatchers(/*
                                                                  * "/customer/public/**", "/customer/swagger-ui/**",
                                                                  * "/customer/v3/api-docs**",
                                                                  */
                                                                "/doc**", "/swagger-ui/**",
                                                                "/register/**", "/v3/api-docs*/**", "/api/**",
                                                                "/auth/**",
                                                                "/reservation/**", "/error*", "/actuator/health",
                                                                "/public/**","/customer/auth/**")
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
                                                                "/public/**","/admin/auth/**")
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
        public AuthenticationManager fallbackAuthenticationManager() {
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
                auth.authenticationProvider(restaurantUserAuthenticationProvider());
                return auth.build();
        }

        @Bean
        public RestaurantUserAuthenticationProvider restaurantUserAuthenticationProvider() {
                RestaurantUserAuthenticationProvider provider = new RestaurantUserAuthenticationProvider();
                provider.setUserDetailsService(restaurantUserDetailsService);
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
        public CustomerAuthenticationProvider customerAuthenticationProvider() {
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
        public AdminAuthenticationProvider adminAuthenticationProvider() {
                AdminAuthenticationProvider provider = new AdminAuthenticationProvider();
                provider.setUserDetailsService(adminUserDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
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

        @Bean(name = "adminUserRememberMe")
        RememberMeServices rememberMeServices() {
                return new AdminUserRememberMeServices("theKey", adminUserDetailsService,
                                new InMemoryTokenRepositoryImpl());
        }

        @Bean
        HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }
}
