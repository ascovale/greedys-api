package com.application.common.spring.swagger.customizer;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
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
            // Gestione intelligente dei duplicati PRIMA della registrazione
            String originalOperationId = operation.getOperationId();
            String uniqueOperationId = ensureUniqueOperationId(originalOperationId);
            
            // Aggiorna l'operationId se necessario (previene il _1 automatico di SpringDoc)
            if (!originalOperationId.equals(uniqueOperationId)) {
                operation.setOperationId(uniqueOperationId);
                log.debug("Renamed operation {} -> {} to avoid duplicates", originalOperationId, uniqueOperationId);
            }
            
            // Raccoglie tutti i metadati in un'unica operazione
            OperationDataMetadata metadata = collectMetadata(operation, handlerMethod);
            registry.register(metadata);
            
            log.debug("Collected metadata for operation: {}", operation.getOperationId());
            
        } catch (Exception e) {
            log.error("Error collecting metadata for {}: {}", operation.getOperationId(), e.getMessage(), e);
        }
        return operation;
    }
    
    /**
     * Garantisce che l'operationId sia unico aggiungendo suffissi numerici se necessario.
     * Architettura: previene i conflitti PRIMA che SpringDoc assegni automaticamente i suffissi.
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
                .httpMethod(extractHttpMethod(operation))
                .path(extractPath(operation))
                .controllerClass(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .description(operation.getDescription())
                .statusCodes(analysisResult.getStatusCodes()) // <-- USO DEL NUOVO ANALYZER
                .dataClassName(dataClassName)
                .wrapperCategory(category)
                .build();
    }
    
    // === HELPER METHODS ===
    
    private String extractHttpMethod(Operation operation) {
        // SpringDoc non fornisce direttamente il metodo HTTP nell'Operation
        // Usiamo un fallback o implementeremo un'analisi più sofisticata se necessario
        return "GET"; // Placeholder - potrebbe essere migliorato
    }
    
    private String extractPath(Operation operation) {
        // Il path non è direttamente disponibile nell'Operation
        // Potrebbe essere estratto dal HandlerMethod se necessario
        return "/unknown"; // Placeholder
    }
    
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
                        
                        return fullClassName;
                    }
                }
            }
            return "Object"; // Fallback
        }
        if (type.isPrimitive() || type.getName().startsWith("java.")) return null;
        return type.getName();
    }
    
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) return null;
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }
}
