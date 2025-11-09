package com.application.common.spring;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
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
import com.application.agency.AgencyUserAuthenticationProvider;
import com.application.agency.AgencyUserHubValidationFilter;
import com.application.agency.AgencyUserRequestFilter;
import com.application.agency.persistence.dao.AgencyUserDAO;
import com.application.agency.service.security.AgencyUserDetailsService;
import com.application.common.security.SecurityPatterns;
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

        private final RUserRequestFilter restaurantJwtRequestFilter;
        private final CustomerRequestFilter customerJwtRequestFilter;
        private final AdminRequestFilter adminJwtRequestFilter;
        private final AgencyUserRequestFilter agencyJwtRequestFilter;
        private final TokenTypeValidationFilter tokenTypeValidationFilter;
        private final RUserHubValidationFilter hubValidationFilter;
        private final AgencyUserHubValidationFilter agencyHubValidationFilter;

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
        SecurityFilterChain RUserFilterChain(HttpSecurity http, RUserAuthenticationProvider rUserProvider)
                        throws Exception {
                http
                                .securityMatcher("/restaurant/**")
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(SecurityPatterns.getRestaurantPublicPatterns())
                                                .permitAll()
                                                .requestMatchers("/restaurant/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Configura il provider usando il DSL
                                .authenticationProvider(rUserProvider)
                                // SEQUENZA FILTRI (ORDINE IMPORTANTE):
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° HubValidationFilter - Limita Hub agli endpoint permessi  
                                .addFilterAfter(hubValidationFilter, TokenTypeValidationFilter.class)
                                // 3° RUserRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterAfter(restaurantJwtRequestFilter, RUserHubValidationFilter.class);

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
        SecurityFilterChain customerFilterChain(HttpSecurity http, CustomerAuthenticationProvider customerProvider)
                        throws Exception {
                http
                                .securityMatcher("/customer/**")
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(SecurityPatterns.getCustomerPublicPatterns())
                                                .permitAll()
                                                .requestMatchers("/customer/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Configura il provider usando il DSL
                                .authenticationProvider(customerProvider)
                                // SEQUENZA FILTRI:
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token  
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° CustomerRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterAfter(customerJwtRequestFilter, TokenTypeValidationFilter.class);

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
        SecurityFilterChain adminFilterChain(HttpSecurity http, AdminAuthenticationProvider adminProvider)
                        throws Exception {
                http
                                .securityMatcher("/admin/**")
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(SecurityPatterns.getAdminPublicPatterns())
                                                .permitAll()
                                                .requestMatchers("/admin/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Configura il provider usando il DSL
                                .authenticationProvider(adminProvider)
                                // SEQUENZA FILTRI:
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° AdminRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterAfter(adminJwtRequestFilter, TokenTypeValidationFilter.class);

                return http.build();
        }

        /*
         * ============================================================================
         * AGENCY FILTER CHAIN - SEQUENZA DEI 3 FILTRI PER /agency/**
         * ============================================================================
         * 1️⃣ TokenTypeValidationFilter - Verifica Access vs Refresh Token
         * 2️⃣ AgencyUserHubValidationFilter - Limita Agency Hub agli endpoint permessi
         * 3️⃣ AgencyUserRequestFilter - AUTENTICAZIONE (SecurityContext)
         * ============================================================================
         */
        @Bean
        SecurityFilterChain agencyFilterChain(HttpSecurity http, AgencyUserAuthenticationProvider agencyProvider) throws Exception {
                http
                                .securityMatcher("/agency/**")
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(SecurityPatterns.getAgencyPublicPatterns())
                                                .permitAll()
                                                .requestMatchers("/agency/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // Configura il provider usando il DSL
                                .authenticationProvider(agencyProvider)
                                // SEQUENZA FILTRI (ORDINE IMPORTANTE):
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° AgencyUserHubValidationFilter - Limita Agency Hub agli endpoint permessi  
                                .addFilterAfter(agencyHubValidationFilter, TokenTypeValidationFilter.class)
                                // 3° AgencyUserRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterAfter(agencyJwtRequestFilter, AgencyUserHubValidationFilter.class);

                return http.build();
        }

        /*
         * ============================================================================
         * DEFAULT FILTER CHAIN - Per endpoint non coperti dalle altre filter chain
         * ============================================================================
         * Gestisce endpoint come /v3/api-docs/*, /actuator/*, etc.
         * ✅ RISOLTO: Cambiato da .anyRequest().authenticated() a .anyRequest().permitAll()
         * ============================================================================
         */
        @Bean
        SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(SecurityPatterns.DEFAULT_PUBLIC_PATTERNS)
                                                .permitAll()
                                                .anyRequest().permitAll()) // ✅ Permetti tutto - controlli veri nelle altre chain
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                return http.build();
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
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /*
         * ============================================================================
         * AUTHENTICATION PROVIDERS BEAN
         * ============================================================================
         * Questi provider sono usati nelle filter chain per l'autenticazione tramite JWT.
         * Sono separati dai manager usati per il login.
         * ============================================================================
         */
        @Bean
        public RUserAuthenticationProvider rUserAuthenticationProvider(
                        RUserDAO rUserDAO,
                        RUserDetailsService rUserDetailsService,
                        PasswordEncoder passwordEncoder) {
                return new RUserAuthenticationProvider(rUserDAO, rUserDetailsService, passwordEncoder);
        }

        @Bean
        public CustomerAuthenticationProvider customerAuthenticationProvider(
                        CustomerDAO customerDAO,
                        CustomerUserDetailsService customerDetailsService,
                        PasswordEncoder passwordEncoder) {
                return new CustomerAuthenticationProvider(customerDAO, customerDetailsService, passwordEncoder);
        }

        @Bean
        public AdminAuthenticationProvider adminAuthenticationProvider(
                        AdminDAO adminDAO,
                        AdminUserDetailsService adminDetailsService,
                        PasswordEncoder passwordEncoder) {
                return new AdminAuthenticationProvider(adminDAO, adminDetailsService, passwordEncoder);
        }

        @Bean
        public AgencyUserAuthenticationProvider agencyUserAuthenticationProvider(
                        AgencyUserDAO agencyUserDAO,
                        AgencyUserDetailsService agencyUserDetailsService,
                        PasswordEncoder passwordEncoder) {
                return new AgencyUserAuthenticationProvider(agencyUserDAO, agencyUserDetailsService, passwordEncoder);
        }

        /**
         * AuthenticationManager di default per Spring Security.
         * Questo è necessario perché Spring Security richiede un Primary quando
         * ci sono multiple implementazioni di AuthenticationManager.
         * Non verrà mai usato per il login (che usa i manager dedicati),
         * ma serve per evitare conflitti durante la creazione di HttpSecurity.
         */
        @Bean
        @Primary
        AuthenticationManager defaultAuthenticationManager() {
                return authentication -> {
                        throw new AuthenticationServiceException(
                                "Default AuthenticationManager should not be used. Use dedicated managers.");
                };
        }

        @Bean
        HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }

        // Disabilita la registrazione automatica dei filtri a livello globale.
        // Verranno applicati solo nelle rispettive SecurityFilterChain.
        @Bean
        FilterRegistrationBean<TokenTypeValidationFilter> disableTokenTypeValidationFilter(TokenTypeValidationFilter f) {
                FilterRegistrationBean<TokenTypeValidationFilter> reg = new FilterRegistrationBean<>(f);
                reg.setEnabled(false);
                return reg;
        }

        @Bean
        FilterRegistrationBean<RUserRequestFilter> disableRUserRequestFilter(RUserRequestFilter f) {
                FilterRegistrationBean<RUserRequestFilter> reg = new FilterRegistrationBean<>(f);
                reg.setEnabled(false);
                return reg;
        }

        @Bean
        FilterRegistrationBean<RUserHubValidationFilter> disableRUserHubValidationFilter(RUserHubValidationFilter f) {
                FilterRegistrationBean<RUserHubValidationFilter> reg = new FilterRegistrationBean<>(f);
                reg.setEnabled(false);
                return reg;
        }

        @Bean
        FilterRegistrationBean<CustomerRequestFilter> disableCustomerRequestFilter(CustomerRequestFilter f) {
                FilterRegistrationBean<CustomerRequestFilter> reg = new FilterRegistrationBean<>(f);
                reg.setEnabled(false);
                return reg;
        }

        @Bean
        FilterRegistrationBean<AdminRequestFilter> disableAdminRequestFilter(AdminRequestFilter f) {
                FilterRegistrationBean<AdminRequestFilter> reg = new FilterRegistrationBean<>(f);
                reg.setEnabled(false);
                return reg;
        }

        @Bean
        FilterRegistrationBean<AgencyUserRequestFilter> disableAgencyUserRequestFilter(AgencyUserRequestFilter f) {
                FilterRegistrationBean<AgencyUserRequestFilter> reg = new FilterRegistrationBean<>(f);
                reg.setEnabled(false);
                return reg;
        }

        @Bean
        FilterRegistrationBean<AgencyUserHubValidationFilter> disableAgencyUserHubValidationFilter(AgencyUserHubValidationFilter f) {
                FilterRegistrationBean<AgencyUserHubValidationFilter> reg = new FilterRegistrationBean<>(f);
                reg.setEnabled(false);
                return reg;
        }
}