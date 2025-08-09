package com.application.common.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classe che centralizza tutti i pattern di sicurezza per l'applicazione.
 * Utilizzata sia per la configurazione di Spring Security che per i filtri.
 */
public final class SecurityPatterns {

    // ============================================================================
    // PATTERN GLOBALI - Accessibili da tutte le filter chain
    // ============================================================================
    
    /**
     * Pattern globali accessibili a tutti senza autenticazione
     */
    public static final String[] GLOBAL_PUBLIC_PATTERNS = {
        "/doc**", 
        "/swagger-ui/**", 
        "/register/**", 
        "/v3/api-docs*/**", 
        "/api/**", 
        "/auth/**", 
        "/reservation/**", 
        "/error*", 
        "/actuator/health", 
        "/public/**", 
        "/logo_api.png", 
        "/swagger-groups",
        // Risorse statiche
        "/favicon.ico",
        "/css/**",
        "/js/**",
        "/images/**",
        "/static/**"
    };

    // ============================================================================
    // PATTERN SPECIFICI PER DOMINIO - Solo per le rispettive filter chain
    // ============================================================================
    
    /**
     * Pattern specifici per l'autenticazione restaurant (solo /restaurant/**)
     */
    public static final String[] RESTAURANT_AUTH_PATTERNS = {
        "/restaurant/user/auth/login", 
        "/restaurant/user/auth/google",
        "/restaurant/auth/**"
    };
    
    /**
     * Pattern specifici per l'autenticazione customer (solo /customer/**)
     */
    public static final String[] CUSTOMER_AUTH_PATTERNS = {
        "/customer/auth/**", 
        "/customer/user/auth/google", 
        "/customer/restaurant/**"
    };
    
    /**
     * Pattern specifici per l'autenticazione admin (solo /admin/**)
     */
    public static final String[] ADMIN_AUTH_PATTERNS = {
        "/admin/auth/**"
    };
    
    /**
     * Pattern per la defaultFilterChain (endpoint non coperti dalle altre chain)
     */
    public static final String[] DEFAULT_PUBLIC_PATTERNS = {
        "/v3/api-docs/**", 
        "/swagger-ui/**", 
        "/swagger-groups/**",
        "/actuator/**", 
        "/error*", 
        "/logo_api.png"
    };

    // ============================================================================
    // METODI UTILITY
    // ============================================================================
    
    /**
     * Combina i pattern globali con quelli specifici del dominio
     */
    public static String[] getCombinedPatterns(String[] specificPatterns) {
        List<String> combined = new ArrayList<>();
        
        // Prima aggiungi solo i pattern globali che non sono duplicati
        combined.addAll(Arrays.asList(GLOBAL_PUBLIC_PATTERNS));
        
        // Poi aggiungi i pattern specifici
        combined.addAll(Arrays.asList(specificPatterns));
        
        return combined.toArray(new String[0]);
    }
    
    /**
     * Restituisce i pattern pubblici per la filter chain restaurant
     */
    public static String[] getRestaurantPublicPatterns() {
        return getCombinedPatterns(RESTAURANT_AUTH_PATTERNS);
    }
    
    /**
     * Restituisce i pattern pubblici per la filter chain customer
     */
    public static String[] getCustomerPublicPatterns() {
        return getCombinedPatterns(CUSTOMER_AUTH_PATTERNS);
    }
    
    /**
     * Restituisce i pattern pubblici per la filter chain admin
     */
    public static String[] getAdminPublicPatterns() {
        return getCombinedPatterns(ADMIN_AUTH_PATTERNS);
    }
    
    /**
     * Restituisce TUTTI i pattern pubblici dell'applicazione (per i filtri)
     */
    public static String[] getAllPublicPatterns() {
        List<String> allPatterns = new ArrayList<>();
        
        allPatterns.addAll(Arrays.asList(GLOBAL_PUBLIC_PATTERNS));
        allPatterns.addAll(Arrays.asList(RESTAURANT_AUTH_PATTERNS));
        allPatterns.addAll(Arrays.asList(CUSTOMER_AUTH_PATTERNS));
        allPatterns.addAll(Arrays.asList(ADMIN_AUTH_PATTERNS));
        allPatterns.addAll(Arrays.asList(DEFAULT_PUBLIC_PATTERNS));
        
        return allPatterns.toArray(new String[0]);
    }
    
    /**
     * Verifica se un path corrisponde a uno dei pattern pubblici
     */
    public static boolean isPublicPath(String path) {
        return isPublicPath(path, getAllPublicPatterns());
    }
    
    /**
     * Verifica se un path corrisponde a uno dei pattern specificati
     */
    public static boolean isPublicPath(String path, String[] patterns) {
        if (path == null) {
            return false;
        }
        
        for (String pattern : patterns) {
            if (matchesPattern(path, pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Verifica se un path matcha un pattern specifico
     * Supporta i wildcard ** e *
     */
    private static boolean matchesPattern(String path, String pattern) {
        // Rimuovi ** e * per il matching semplice
        if (pattern.endsWith("/**")) {
            String basePattern = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(basePattern);
        } else if (pattern.endsWith("**")) {
            String basePattern = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(basePattern);
        } else if (pattern.endsWith("*")) {
            String basePattern = pattern.substring(0, pattern.length() - 1);
            return path.startsWith(basePattern);
        } else {
            return path.equals(pattern) || path.startsWith(pattern + "/");
        }
    }
    
    // ============================================================================
    // METODI DI DEBUG
    // ============================================================================
    
    /**
     * Stampa tutti i pattern configurati (per debug)
     */
    public static void printAllPatterns() {
        System.out.println("=== SECURITY PATTERNS ===");
        System.out.println("Global Public: " + Arrays.toString(GLOBAL_PUBLIC_PATTERNS));
        System.out.println("Restaurant Auth: " + Arrays.toString(RESTAURANT_AUTH_PATTERNS));
        System.out.println("Customer Auth: " + Arrays.toString(CUSTOMER_AUTH_PATTERNS));
        System.out.println("Admin Auth: " + Arrays.toString(ADMIN_AUTH_PATTERNS));
        System.out.println("Default Public: " + Arrays.toString(DEFAULT_PUBLIC_PATTERNS));
        System.out.println("All Public: " + Arrays.toString(getAllPublicPatterns()));
    }

    // Constructor privato per evitare istanziazione
    private SecurityPatterns() {
        throw new UnsupportedOperationException("Utility class");
    }
}
