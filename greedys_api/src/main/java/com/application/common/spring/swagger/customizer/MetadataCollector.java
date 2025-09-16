package com.application.common.spring.swagger.customizer;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import com.application.common.spring.swagger.analysis.MethodAnalysisResult;
import com.application.common.spring.swagger.analysis.MethodAnalyzer;
import com.application.common.spring.swagger.metadata.OperationDataMetadata;
import com.application.common.spring.swagger.metadata.WrapperCategory;
import com.application.common.spring.swagger.registry.MetadataRegistry;

import io.swagger.v3.oas.models.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FASE 1: Raccolta metadati completi per operazioni.
 * 
 * Raccoglie tutti i metadati necessari e li passa al MetadataRegistry
 * che si occupa di organizzarli internamente.
 */
@Component
@Order(500)
@RequiredArgsConstructor
@Slf4j
public class MetadataCollector implements OperationCustomizer {
    
    private final MetadataRegistry registry;
    private final MethodAnalyzer methodAnalyzer;
    
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        try {
            // Auto-prefix tags based on controller type
            enhanceOperationTags(operation, handlerMethod);
            
            // Auto-prefix operationId to avoid duplicates
            String originalOperationId = operation.getOperationId();
            String prefixedOperationId = addPrefixToOperationId(originalOperationId, handlerMethod);
            String uniqueOperationId = ensureUniqueOperationId(prefixedOperationId);
            
            operation.setOperationId(uniqueOperationId);
            
            log.debug("Operation ID enhanced: {} -> {}", originalOperationId, uniqueOperationId);
            
            // Raccoglie i metadati per questa operazione
            OperationDataMetadata metadata = collectMetadata(operation, handlerMethod);
            
            // Registra i metadati raccolti
            registry.register(metadata);
            
            log.debug("Registered metadata for operation: {}", operation.getOperationId());
            
        } catch (Exception e) {
            log.error("Failed to customize operation: {}", e.getMessage(), e);
        }
        return operation;
    }
    
    /**
     * Aggiunge automaticamente prefissi agli operationId per evitare duplicati.
     * Trasforma "createReservation" in "customerCreateReservation", "adminCreateReservation", etc.
     */
    private String addPrefixToOperationId(String operationId, HandlerMethod handlerMethod) {
        if (operationId == null || operationId.trim().isEmpty()) {
            return operationId;
        }
        
        String prefix = extractControllerPrefix(handlerMethod);
        if (prefix == null) {
            return operationId;
        }
        
        // Converte il prefisso in camelCase per l'operationId
        String camelCasePrefix = prefix.toLowerCase();
        
        // Verifica se il prefisso è già presente
        if (operationId.toLowerCase().startsWith(camelCasePrefix)) {
            return operationId;
        }
        
        // Aggiunge il prefisso
        return camelCasePrefix + capitalizeFirst(operationId);
    }
    
    /**
     * Assicura che l'operationId sia unico nel registry.
     */
    private String ensureUniqueOperationId(String operationId) {
        if (!registry.hasOperation(operationId)) {
            return operationId;
        }
        
        int counter = 1;
        String candidateId;
        do {
            candidateId = operationId + "_" + counter;
            counter++;
        } while (registry.hasOperation(candidateId));
        
        return candidateId;
    }
    
    /**
     * Capitalizza la prima lettera di una stringa.
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Migliora automaticamente i tag delle operazioni aggiungendo prefissi basati sul controller.
     * Admin -> "Admin - [original tag]", Customer -> "Customer - [original tag]", etc.
     */
    private void enhanceOperationTags(Operation operation, HandlerMethod handlerMethod) {
        try {
            String controllerPrefix = extractControllerPrefix(handlerMethod);
            if (controllerPrefix != null && operation.getTags() != null && !operation.getTags().isEmpty()) {
                List<String> enhancedTags = operation.getTags().stream()
                    .map(tag -> enhanceTag(tag, controllerPrefix))
                    .toList();
                operation.setTags(enhancedTags);
                log.debug("Enhanced tags for {} with prefix '{}': {}", 
                    operation.getOperationId(), controllerPrefix, enhancedTags);
            }
        } catch (Exception e) {
            log.warn("Failed to enhance tags for {}: {}", operation.getOperationId(), e.getMessage());
        }
    }
    
    /**
     * Estrae il prefisso del controller basato sul package e nome classe.
     */
    private String extractControllerPrefix(HandlerMethod handlerMethod) {
        String fullClassName = handlerMethod.getBeanType().getName();
        
        // Gestisce sottopacchetti specifici per maggiore granularità
        if (fullClassName.contains(".restaurant.controller.google.")) {
            return "Google";
        }
        if (fullClassName.contains(".admin.controller.")) {
            return "Admin";
        }
        if (fullClassName.contains(".customer.controller.")) {
            return "Customer";
        }
        if (fullClassName.contains(".restaurant.controller.")) {
            return "Restaurant";
        }
        
        // Estrae dal nome della classe se contiene indicatori
        String className = handlerMethod.getBeanType().getSimpleName();
        if (className.startsWith("Admin")) {
            return "Admin";
        }
        if (className.startsWith("Customer")) {
            return "Customer";
        }
        if (className.startsWith("Restaurant")) {
            return "Restaurant";
        }
        if (className.contains("Google")) {
            return "Google";
        }
        
        return null; // Nessun prefisso riconosciuto
    }
    
    /**
     * Migliora un singolo tag aggiungendo il prefisso se non già presente.
     */
    private String enhanceTag(String originalTag, String prefix) {
        // Non aggiungere il prefisso se già presente
        if (originalTag.startsWith(prefix + " - ")) {
            return originalTag;
        }
        
        // Non aggiungere il prefisso se il tag già contiene il prefisso
        if (originalTag.toLowerCase().contains(prefix.toLowerCase())) {
            return originalTag;
        }
        
        // Aggiungi il prefisso
        return prefix + " - " + originalTag;
    }
    
    /**
     * Raccoglie tutti i metadati necessari in un'unica struttura
     */
    private OperationDataMetadata collectMetadata(Operation operation, HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        Class<?> returnType = method.getReturnType();
        
        // 1. Analisi del metodo per status codes
        MethodAnalysisResult analysisResult = methodAnalyzer.analyzeMethod(handlerMethod);
        
        // 2. Raccoglie i dati di schema
        WrapperCategory category = determineWrapperCategory(returnType, method);
        String dataClassName = extractDataClassName(returnType, method);
        
        return OperationDataMetadata.builder()
                .operationId(operation.getOperationId())
                .httpMethod(extractHttpMethod(operation, handlerMethod))
                .path(extractPath(operation, handlerMethod))
                .controllerClass(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .description(operation.getDescription())
                .statusCodes(analysisResult.getStatusCodes())
                .dataClassName(dataClassName)
                .wrapperCategory(category)
                .build();
    }
    
    // ================================================================================================
    // HELPER METHODS - Metodi di supporto organizzati per categoria
    // ================================================================================================
    
    // --- HTTP Method & Path Extraction ---
    
    private String extractHttpMethod(Operation operation, HandlerMethod handlerMethod) {
        try {
            // Usa il MethodAnalyzer per determinare il metodo HTTP correttamente
            if (handlerMethod != null && handlerMethod.getMethod() != null) {
                Method method = handlerMethod.getMethod();
                String httpMethod = methodAnalyzer.detectHttpMethodPublic(method);
                if (httpMethod != null && !httpMethod.trim().isEmpty()) {
                    return httpMethod;
                }
            }
            
            // Fallback: prova a dedurre dall'operation toString
            if (operation != null) {
                String operationString = operation.toString();
                if (operationString != null) {
                    // Cerca pattern comuni nella stringa dell'operation
                    if (operationString.contains("POST")) return "POST";
                    if (operationString.contains("PUT")) return "PUT";
                    if (operationString.contains("DELETE")) return "DELETE";
                    if (operationString.contains("PATCH")) return "PATCH";
                }
            }
            
            // Default sicuro
            return "GET";
            
        } catch (Exception e) {
            log.warn("Failed to extract HTTP method: {}", e.getMessage());
            return "GET";
        }
    }
    
    private String extractPath(Operation operation, HandlerMethod handlerMethod) {
        try {
            // Prova a estrarre il path dal HandlerMethod
            if (handlerMethod != null && handlerMethod.getMethod() != null) {
                Method method = handlerMethod.getMethod();
                
                // Controlla le annotations Spring mapping per il path
                String path = extractPathFromAnnotations(method);
                if (path != null && !path.trim().isEmpty()) {
                    return path;
                }
                
                // Fallback: usa il nome del metodo come indicatore
                String methodName = method.getName();
                if (methodName != null) {
                    return "/" + methodName.toLowerCase();
                }
            }
            
            // Fallback: prova a dedurre dall'operation
            if (operation != null && operation.getOperationId() != null) {
                return "/" + operation.getOperationId().toLowerCase();
            }
            
            return "/unknown";
            
        } catch (Exception e) {
            log.warn("Failed to extract path: {}", e.getMessage());
            return "/unknown";
        }
    }
    
    private String extractPathFromAnnotations(Method method) {
        try {
            // Controlla @RequestMapping
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping rm = method.getAnnotation(RequestMapping.class);
                if (rm.value() != null && rm.value().length > 0 && rm.value()[0] != null) {
                    return rm.value()[0];
                }
                if (rm.path() != null && rm.path().length > 0 && rm.path()[0] != null) {
                    return rm.path()[0];
                }
            }
            
            // Controlla @GetMapping
            if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping gm = method.getAnnotation(GetMapping.class);
                if (gm.value() != null && gm.value().length > 0 && gm.value()[0] != null) {
                    return gm.value()[0];
                }
                if (gm.path() != null && gm.path().length > 0 && gm.path()[0] != null) {
                    return gm.path()[0];
                }
            }
            
            // Controlla @PostMapping
            if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping pm = method.getAnnotation(PostMapping.class);
                if (pm.value() != null && pm.value().length > 0 && pm.value()[0] != null) {
                    return pm.value()[0];
                }
                if (pm.path() != null && pm.path().length > 0 && pm.path()[0] != null) {
                    return pm.path()[0];
                }
            }
            
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract path from annotations: {}", e.getMessage());
            return null;
        }
    }
    
    // --- Wrapper Category & Data Type Analysis ---
    
    private WrapperCategory determineWrapperCategory(Class<?> type, Method method) {
        // Per ResponseEntity<ResponseWrapper<T>>, analizziamo il tipo T
        if ("ResponseEntity".equals(type.getSimpleName())) {
            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericReturnType;
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof ParameterizedType) {
                    // ResponseEntity<ResponseWrapper<T>> - analizziamo il tipo T
                    ParameterizedType wrapperType = (ParameterizedType) typeArgs[0];
                    Type[] wrapperArgs = wrapperType.getActualTypeArguments();
                    if (wrapperArgs.length > 0) {
                        String typeName = wrapperArgs[0].getTypeName();
                        
                        // Controlla se è una List
                        if (typeName.startsWith("java.util.List<") || typeName.contains("List<")) {
                            return WrapperCategory.LIST;
                        }
                        // Controlla se è una Page
                        if (typeName.contains("Page<") || typeName.contains("org.springframework.data.domain.Page")) {
                            return WrapperCategory.PAGE;
                        }
                        // Controlla se è una Slice
                        if (typeName.contains("Slice<") || typeName.contains("org.springframework.data.domain.Slice")) {
                            return WrapperCategory.SLICE;
                        }
                        // Controlla se è void
                        if (typeName.equals("java.lang.Void") || typeName.equals("void")) {
                            return WrapperCategory.VOID;
                        }
                        // Altrimenti è SINGLE
                        return WrapperCategory.SINGLE;
                    }
                }
            }
        }
        
        // Fallback per analisi diretta del tipo
        String name = type.getSimpleName();
        if (name.contains("List") || name.contains("Collection")) return WrapperCategory.LIST;
        if (name.contains("Page")) return WrapperCategory.PAGE;
        if (name.contains("Slice")) return WrapperCategory.SLICE;
        if ("void".equals(name) || "Void".equals(name)) return WrapperCategory.VOID;
        return WrapperCategory.SINGLE;
    }
    
    private String extractDataClassName(Class<?> type, Method method) {
        // Per i metodi che ritornano ResponseEntity<ResponseWrapper<T>>, 
        // dobbiamo estrarre il tipo T dai generics
        if ("ResponseEntity".equals(type.getSimpleName())) {
            // Analizziamo il generic type del metodo
            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericReturnType;
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof ParameterizedType) {
                    // ResponseEntity<ResponseWrapper<T>> - prendiamo il tipo T
                    ParameterizedType wrapperType = (ParameterizedType) typeArgs[0];
                    Type[] wrapperArgs = wrapperType.getActualTypeArguments();
                    if (wrapperArgs.length > 0) {
                        String fullClassName = wrapperArgs[0].getTypeName();
                        
                        // Se è una List<SomeDTO>, estraiamo SomeDTO
                        if (fullClassName.startsWith("java.util.List<") && fullClassName.endsWith(">")) {
                            String innerType = fullClassName.substring("java.util.List<".length(), fullClassName.length() - 1);
                            return innerType;
                        }
                        
                        // Se è una Page<SomeDTO>, estraiamo SomeDTO  
                        if (fullClassName.contains("Page<") && fullClassName.endsWith(">")) {
                            int startIndex = fullClassName.indexOf("Page<") + "Page<".length();
                            String innerType = fullClassName.substring(startIndex, fullClassName.length() - 1);
                            return innerType;
                        }
                        
                        // Se è una Slice<SomeDTO>, estraiamo SomeDTO  
                        if (fullClassName.contains("Slice<") && fullClassName.endsWith(">")) {
                            int startIndex = fullClassName.indexOf("Slice<") + "Slice<".length();
                            String innerType = fullClassName.substring(startIndex, fullClassName.length() - 1);
                            return innerType;
                        }
                        
                        return fullClassName;
                    }
                }
            }
            return "Object"; // Fallback
        }
        if (type.isPrimitive() || type.getName().startsWith("java.")) return null;
        return type.getName();
    }
}