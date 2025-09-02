package com.application.common.spring.swagger.analysis;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import lombok.extern.slf4j.Slf4j;

/**
 * Analizzatore unificato per i metodi dei controller.
 * 
 * Estrae i metadati di analisi da un HandlerMethod:
 * - Status codes (200/201 + errori standard)
 * - Logica basata su executeCreate() detection
 * - Standard API responses da @StandardApiResponses
 */
@Component
@Slf4j
public class MethodAnalyzer {
    
    /**
     * Analizza un HandlerMethod e restituisce i metadata completi
     */
    public MethodAnalysisResult analyzeMethod(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            log.warn("HandlerMethod is null, returning default analysis");
            return createDefaultAnalysis();
        }
        
        Method method = handlerMethod.getMethod();
        
        try {
            // 1. Determina status codes
            Set<String> statusCodes = determineStatusCodes(method);
            
            // 2. Altri metadati (per future espansioni)
            String methodSignature = buildMethodSignature(method);
            
            log.debug("Analyzed method {}: statusCodes={}", method.getName(), statusCodes);
            
            return MethodAnalysisResult.builder()
                .statusCodes(statusCodes)
                .methodSignature(methodSignature)
                .analysisNotes("Successful analysis")
                .build();
                
        } catch (Exception e) {
            log.error("Analysis failed for method: {}.{}", 
                method.getDeclaringClass().getSimpleName(), method.getName(), e);
            
            return MethodAnalysisResult.builder()
                .statusCodes(getDefaultStatusCodes())
                .methodSignature(buildMethodSignature(method))
                .analysisNotes("Analysis failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Determina i status codes per un metodo basandosi su HTTP method + executeCreate detection
     */
    private Set<String> determineStatusCodes(Method method) {
        Set<String> statusCodes = new HashSet<>();
        
        // 1. Status code principale: 200 vs 201 (basato su executeCreate())
        String primaryStatusCode = detectPrimaryStatusCode(method);
        statusCodes.add(primaryStatusCode);
        
        // 2. Error codes specifici per tipo di operazione (basato su HTTP method)
        Set<String> errorCodes = determineErrorCodes(method);
        statusCodes.addAll(errorCodes);
        
        return statusCodes;
    }
    
    /**
     * Detecta il status code principale (200 vs 201)
     * Per POST operations: controlla se è create* o ask*
     */
    private String detectPrimaryStatusCode(Method method) {
        String httpMethod = detectHttpMethod(method);
        
        // Solo POST operations possono avere 201
        if ("POST".equals(httpMethod)) {
            String methodName = method.getName().toLowerCase();
            
            // Pattern per operazioni che creano risorse
            if (methodName.startsWith("create") || methodName.startsWith("ask")) {
                log.debug("Method {} [POST] with create/ask pattern -> 201", method.getName());
                return "201";
            }
        }
        
        // Default: tutte le altre operazioni
        log.debug("Method {} [{}] -> 200", method.getName(), httpMethod);
        return "200";
    }
    
    /**
     * Verifica se il metodo contiene una chiamata a executeCreate()
     * Semplificato: controlla solo se è POST + create/ask pattern
     */
    private boolean containsExecuteCreateCall(Method method) {
        return "201".equals(detectPrimaryStatusCode(method));
    }
    
    /**
     * Determina error codes specifici basandosi sul tipo di operazione HTTP
     * Se non riesce a rilevare l'HTTP method, usa error codes sicuri di default
     */
    private Set<String> determineErrorCodes(Method method) {
        String httpMethod = detectHttpMethod(method);
        
        Set<String> errorCodes = new HashSet<>();
        
        // Errori comuni a tutte le operazioni (SEMPRE presenti)
        errorCodes.add("400"); // Bad Request
        errorCodes.add("401"); // Unauthorized  
        errorCodes.add("403"); // Forbidden
        errorCodes.add("500"); // Internal Server Error
        
        // Errori specifici per tipo di operazione - DEFAULT SAFE
        try {
            switch (httpMethod.toUpperCase()) {
                case "GET":
                    errorCodes.add("404"); // Not Found - risorsa non trovata
                    break;
                    
                case "POST":
                    if (containsExecuteCreateCall(method)) {
                        errorCodes.add("409"); // Conflict - risorsa già esistente
                    }
                    // POST che non sono CREATE (login, search, etc.) -> nessun errore aggiuntivo
                    break;
                    
                case "PUT":
                    errorCodes.add("404"); // Not Found - risorsa da aggiornare non esiste
                    errorCodes.add("409"); // Conflict - conflitto di versione
                    break;
                    
                case "DELETE":
                    errorCodes.add("404"); // Not Found - risorsa già eliminata
                    errorCodes.add("409"); // Conflict - impossibile eliminare per dipendenze
                    break;
                    
                default:
                    // HTTP method non riconosciuto - usa default sicuri
                    log.warn("Unknown HTTP method '{}' for method {}, using default error codes", httpMethod, method.getName());
                    // Non aggiungiamo errori specifici, solo quelli comuni sopra
                    break;
            }
        } catch (Exception e) {
            log.error("Error determining error codes for method {}: {}", method.getName(), e.getMessage());
            // In caso di errore, usa solo gli errori comuni (già aggiunti sopra)
        }
        
        log.debug("Method {} [{}] -> error codes: {}", method.getName(), httpMethod, errorCodes);
        return errorCodes;
    }
    
    /**
     * Rileva il metodo HTTP dalle annotations Spring
     */
    private String detectHttpMethod(Method method) {
        // Controlla le annotations Spring mapping
        if (method.isAnnotationPresent(GetMapping.class)) {
            log.debug("Method {} detected as GET via @GetMapping", method.getName());
            return "GET";
        }
        
        if (method.isAnnotationPresent(PostMapping.class)) {
            log.debug("Method {} detected as POST via @PostMapping", method.getName());
            return "POST";
        }
        
        if (method.isAnnotationPresent(PutMapping.class)) {
            log.debug("Method {} detected as PUT via @PutMapping", method.getName());
            return "PUT";
        }
        
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            log.debug("Method {} detected as DELETE via @DeleteMapping", method.getName());
            return "DELETE";
        }
        
        if (method.isAnnotationPresent(PatchMapping.class)) {
            log.debug("Method {} detected as PATCH via @PatchMapping", method.getName());
            return "PATCH";
        }
        
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping.method() != null && requestMapping.method().length > 0) {
                String httpMethod = requestMapping.method()[0].name();
                log.debug("Method {} detected as {} via @RequestMapping", method.getName(), httpMethod);
                return httpMethod;
            }
            log.debug("Method {} detected as GET via @RequestMapping (default)", method.getName());
            return "GET"; // default per @RequestMapping senza method
        }
        
        // Default fallback
        log.warn("Method {} has no recognized HTTP annotation, defaulting to GET", method.getName());
        return "GET";
    }
    
    /**
     * Status codes di default per casi di errore
     */
    private Set<String> getDefaultStatusCodes() {
        Set<String> defaultCodes = new HashSet<>();
        defaultCodes.add("200"); // Status principale di default
        // Errori standard
        defaultCodes.add("400"); // Bad Request
        defaultCodes.add("401"); // Unauthorized
        defaultCodes.add("403"); // Forbidden
        defaultCodes.add("500"); // Internal Server Error
        return defaultCodes;
    }
    
    /**
     * Crea un'analisi di default per casi di errore
     */
    private MethodAnalysisResult createDefaultAnalysis() {
        return MethodAnalysisResult.builder()
            .statusCodes(getDefaultStatusCodes())
            .methodSignature("unknown")
            .analysisNotes("Default analysis - no method provided")
            .build();
    }
    
    /**
     * Costruisce la signature del metodo per debugging
     */
    private String buildMethodSignature(Method method) {
        if (method == null) return "unknown";
        
        return String.format("%s.%s(%d params)", 
            method.getDeclaringClass().getSimpleName(),
            method.getName(),
            method.getParameterCount());
    }
}
