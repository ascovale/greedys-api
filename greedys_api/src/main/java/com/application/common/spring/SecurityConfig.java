package com.application.common.spring;

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
import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.service.security.AdminUserDetailsService;
import com.application.common.security.TokenTypeValidationFilter;
import com.application.customer.CustomerAuthenticationProvider;
import com.application.customer.CustomerRequestFilter;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.service.security.CustomerUserDetailsService;
import com.application.restaurant.RUserAuthenticationProvider;
import com.application.restaurant.RUserHubValidationFilter;
import com.application.restaurant.RUserRequestFilter;
import com.application.restaurant.persistence.dao.RUserDAO;
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
        private final TokenTypeValidationFilter tokenTypeValidationFilter;
        private final RUserHubValidationFilter hubValidationFilter;
        private final RUserDAO rUserDAO;
        private final CustomerDAO customerDAO;
        private final AdminDAO adminDAO;

        // TODO: make sure the authentication filter is not required for login,
        // registration, and other public operations
        
        /*
         * ============================================================================
         * RESTAURANT FILTER CHAIN - SEQUENZA DEI 3 FILTRI PER /restaurant/**
         * ============================================================================
         * 
         * ESEMPIO: GET /restaurant/switch-restaurant con Hub Access Token
         * 
         * 1️⃣ TokenTypeValidationFilter:
         *    ✅ "È un access token?" → SÌ → continua
         *    ❌ Se fosse refresh token su endpoint normale → BLOCCATO
         * 
         * 2️⃣ HubValidationFilter:
         *    ✅ "È un token Hub?" → SÌ
         *    ✅ "Può accedere a /switch-restaurant?" → SÌ → continua
         *    ❌ Se Hub tentasse /orders → BLOCCATO (403 Forbidden)
         *    ✅ Se fosse RUser normale → passa sempre
         * 
         * 3️⃣ RUserRequestFilter:
         *    ✅ "Estraggo type=hub dal token"
         *    ✅ "Carico claims e creo UserDetails per Hub"
         *    ✅ "Valido il token"
         *    ✅ "FACCIO L'AUTENTICAZIONE" → SecurityContext popolato
         * 
         * 📤 Response: Endpoint accessibile con utente Hub autenticato
         * 
         * NOTA: Solo RUserRequestFilter fa l'autenticazione vera (SecurityContext)
         *       I primi due sono filtri di validazione/autorizzazione
         * ============================================================================
         */
        @Bean
        SecurityFilterChain RUserFilterChain(HttpSecurity http,
                        @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager)
                        throws Exception {
                http
                                .securityMatcher("/restaurant/**")
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
                                // SEQUENZA FILTRI (ORDINE IMPORTANTE):
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° HubValidationFilter - Limita Hub agli endpoint permessi  
                                .addFilterAfter(hubValidationFilter, tokenTypeValidationFilter.getClass())
                                // 3° RUserRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterBefore(restaurantJwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                                .authenticationManager(authenticationManager);

                return http.build();
        }

        /*
         * ============================================================================
         * CUSTOMER FILTER CHAIN - SEQUENZA DEI 2 FILTRI PER /customer/**
         * ============================================================================
         * 1️⃣ TokenTypeValidationFilter - Verifica Access vs Refresh Token
         * 2️⃣ CustomerRequestFilter - AUTENTICAZIONE (SecurityContext)
         * ============================================================================
         */
        @Bean
        SecurityFilterChain customerFilterChain(HttpSecurity http,
                        @Qualifier("customerAuthenticationManager") AuthenticationManager authenticationManager)
                        throws Exception {
                http
                                .securityMatcher("/customer/**")
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
                                // SEQUENZA FILTRI:
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token  
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° CustomerRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterBefore(customerJwtRequestFilter, UsernamePasswordAuthenticationFilter.class)

                                .authenticationManager(authenticationManager);

                return http.build();
        }

        /*
         * ============================================================================
         * ADMIN FILTER CHAIN - SEQUENZA DEI 2 FILTRI PER /admin/**
         * ============================================================================
         * 1️⃣ TokenTypeValidationFilter - Verifica Access vs Refresh Token
         * 2️⃣ AdminRequestFilter - AUTENTICAZIONE (SecurityContext)
         * ============================================================================
         */
        @Bean
        SecurityFilterChain adminFilterChain(HttpSecurity http,
                        @Qualifier("adminAuthenticationManager") AuthenticationManager authenticationManager)
                        throws Exception {
                http
                                .securityMatcher("/admin/**")
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
                                // SEQUENZA FILTRI:
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° AdminRequestFilter - AUTENTICAZIONE finale (SecurityContext)
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
                config.setAllowedOriginPatterns(java.util.Arrays.asList("*")); // Metodo moderno
                config.setAllowedHeaders(java.util.Arrays.asList("*")); // Metodo moderno
                config.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // Specifico
                config.setExposedHeaders(java.util.Arrays.asList("Authorization", "Content-Type")); // Metodo moderno
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
                RUserAuthenticationProvider provider = new RUserAuthenticationProvider(rUserDAO, RUserDetailsService);
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
                CustomerAuthenticationProvider provider = new CustomerAuthenticationProvider(customerDAO, customerUserDetailsService);
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
		AdminAuthenticationProvider provider = new AdminAuthenticationProvider(adminDAO, adminUserDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}        //TODO RIMOVERE TOGLIERE REMEMBER ME FORSE METTERE refreshTOKEN


        @Bean
        HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }
}
