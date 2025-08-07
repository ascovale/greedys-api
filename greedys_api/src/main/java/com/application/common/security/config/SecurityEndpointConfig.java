package com.application.common.security.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configurazione centralizzata degli endpoint di sicurezza.
 * Evita duplicazione di logica tra SecurityConfig e filtri.
 */
@Component
@ConfigurationProperties(prefix = "security.endpoints")
@Data
public class SecurityEndpointConfig {
    
    /**
     * Endpoint pubblici che non richiedono autenticazione
     */
    private Set<String> publicPaths = Set.of(
        "/swagger-ui/**", "/favicon.ico", "/logo_api.png", 
        "/css/**", "/js/**", "/v3/api-docs/**", "/doc/**",
        "/error", "/actuator/health",
        "/admin/auth/**", "/customer/auth/**", "/restaurant/auth/**"
    );
    
    /**
     * Endpoint che richiedono refresh token
     */
    private Set<String> refreshPaths = Set.of(
        "**/auth/refresh", "**/refresh-token"
    );
    
    /**
     * Endpoint riservati ai token Hub (pattern matching)
     */
    private Set<String> hubAllowedPaths = Set.of(
        "**/switch-restaurant", "**/change-restaurant",
        "**/available-restaurants", "**/logout", 
        "**/profile/hub", "**/auth/refresh"
    );
    
    /**
     * Verifica se un path è pubblico
     */
    public boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(pattern -> 
            matchesPattern(path, pattern));
    }
    
    /**
     * Verifica se un path richiede refresh token
     */
    public boolean isRefreshPath(String path) {
        return refreshPaths.stream().anyMatch(pattern -> 
            matchesPattern(path, pattern));
    }
    
    /**
     * Verifica se un path è consentito ai token Hub
     */
    public boolean isHubAllowedPath(String path) {
        return hubAllowedPaths.stream().anyMatch(pattern -> 
            matchesPattern(path, pattern));
    }
    
    /**
     * Simple pattern matching (* wildcard support)
     */
    private boolean matchesPattern(String path, String pattern) {
        if (pattern.contains("**")) {
            String prefix = pattern.substring(0, pattern.indexOf("**"));
            String suffix = pattern.substring(pattern.indexOf("**") + 2);
            return path.startsWith(prefix) && path.endsWith(suffix);
        } else if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        } else {
            return path.equals(pattern) || path.contains(pattern.replace("*", ""));
        }
    }
}
