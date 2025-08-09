package com.application.common.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
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
                                                .requestMatchers(SecurityPatterns.getRestaurantPublicPatterns())
                                                .permitAll()
                                                .requestMatchers("/restaurant/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // SEQUENZA FILTRI (ORDINE IMPORTANTE):
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° HubValidationFilter - Limita Hub agli endpoint permessi  
                                .addFilterAfter(hubValidationFilter, TokenTypeValidationFilter.class)
                                // 3° RUserRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterAfter(restaurantJwtRequestFilter, RUserHubValidationFilter.class)
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
                                                .requestMatchers(SecurityPatterns.getCustomerPublicPatterns())
                                                .permitAll()
                                                .requestMatchers("/customer/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // SEQUENZA FILTRI:
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token  
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° CustomerRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterAfter(customerJwtRequestFilter, TokenTypeValidationFilter.class)

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
                                                .requestMatchers(SecurityPatterns.getAdminPublicPatterns())
                                                .permitAll()
                                                .requestMatchers("/admin/**").authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // SEQUENZA FILTRI:
                                // 1° TokenTypeValidationFilter - Verifica Access vs Refresh Token
                                .addFilterBefore(tokenTypeValidationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 2° AdminRequestFilter - AUTENTICAZIONE finale (SecurityContext)
                                .addFilterAfter(adminJwtRequestFilter, TokenTypeValidationFilter.class)
                                .authenticationManager(authenticationManager);

                return http.build();
        }

        /*
         * ============================================================================
         * DEFAULT FILTER CHAIN - Per endpoint non coperti dalle altre filter chain
         * ============================================================================
         * Gestisce endpoint come /v3/api-docs/*, /actuator/*, etc.
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
                                                .anyRequest().authenticated())
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
        @Qualifier("restaurantAuthenticationManager")
        AuthenticationManager restaurantAuthenticationManager() {
                return new ProviderManager(RUserAuthenticationProvider());
        }

        @Bean
        RUserAuthenticationProvider RUserAuthenticationProvider() {
                RUserAuthenticationProvider provider = new RUserAuthenticationProvider(rUserDAO, RUserDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        @Qualifier("customerAuthenticationManager")
        AuthenticationManager customerAuthenticationManager() {
                return new ProviderManager(customerAuthenticationProvider());
        }

        @Bean
        CustomerAuthenticationProvider customerAuthenticationProvider() {
                CustomerAuthenticationProvider provider = new CustomerAuthenticationProvider(customerDAO, customerUserDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        @Qualifier("adminAuthenticationManager")
        AuthenticationManager adminAuthenticationManager() {
                return new ProviderManager(adminAuthenticationProvider());
        }

        /*
         * ============================================================================
         * DEFAULT AUTHENTICATION MANAGER
         * ============================================================================
         * 
         * SCOPO: Fornisce un AuthenticationManager di default per Spring Security quando
         * nessun manager specifico è configurato per una richiesta.
         * 
         * MOTIVAZIONE: 
         * - Il sistema ha manager specifici per ogni tipo di utente (/restaurant, /customer, /admin)
         * - Endpoint pubblici come /v3/api-docs, /actuator non richiedono autenticazione
         * - Se per errore una richiesta arriva qui, significa che c'è un problema di configurazione
         * - Meglio fallire esplicitamente con un errore chiaro piuttosto che comportamenti inaspettati
         * 
         * ANNOTAZIONE @Primary: 
         * - Spring Security automaticamente usa il bean @Primary quando:
         *   1. HttpSecurity.build() non trova un AuthenticationManager esplicito
         *   2. Non c'è un @Qualifier specifico nel punto di injection
         *   3. Dependency injection richiede un AuthenticationManager generico
         * 
         * QUANDO VIENE USATO:
         * - defaultFilterChain (NON ha .authenticationManager() esplicito)
         *   → Endpoint pubblici: /v3/api-docs, /actuator che NON dovrebbero autenticare
         * - Injection points senza @Qualifier specifico
         * - Situazioni di errore/configurazione sbagliata
         * 
         * NON viene usato per LOGIN (hanno AuthenticationManager esplicito):
         * - restaurantFilterChain → usa @Qualifier("restaurantAuthenticationManager")
         * - customerFilterChain → usa @Qualifier("customerAuthenticationManager")  
         * - adminFilterChain → usa @Qualifier("adminAuthenticationManager")
         * 
         * IMPORTANTE: Se questo manager viene chiamato durante il login, indica:
         * 1. ERRORE DI DEPENDENCY INJECTION: @Qualifier non funziona nel service
         * 2. ERRORE DI CONFIGURAZIONE: Bean AuthenticationManager specifico non trovato
         * 3. ERRORE DI LOGICA: Service chiama AuthenticationManager sbagliato
         * 4. CONFLITTO DI BEAN: Due bean con stesso nome o tipo
         * 
         * DEBUG SPECIFICO per login con credenziali sbagliate:
         * - CustomerAuthenticationService usa @Qualifier("customerAuthenticationManager")
         * - Se arriva qui = Spring non trova il bean giusto o c'è conflitto
         * - Controlla che customerAuthenticationManager() bean sia correttamente definito
         * - Verifica che non ci siano bean duplicati con stesso @Qualifier
         * - Errore di Validazione
         * 
         * NOTA: Gli endpoint /customer/auth/login usano il service, NON il filter chain
         * Il filter chain AuthenticationManager è per JWT, il service è per login!
         * ============================================================================
         */
        @Bean
        @Primary
        AuthenticationManager defaultAuthenticationManager() {
                // Return a manager that always fails with authentication exception
                return authentication -> {
                        throw new AuthenticationServiceException(
                                "No specific AuthenticationManager configured for this request");
                };
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

	@Bean
	AdminAuthenticationProvider adminAuthenticationProvider() {
		AdminAuthenticationProvider provider = new AdminAuthenticationProvider(adminDAO, adminUserDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	} 

        @Bean
        HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }
}
