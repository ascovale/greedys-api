package com.application.common.spring.swagger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;

import com.application.common.spring.swagger.helper.StaticAnalysisEngine;
import com.application.common.web.ResponseWrapper;

import io.swagger.v3.oas.models.Operation;
import lombok.extern.slf4j.Slf4j;

/**
 * OperationCustomizer that automatically detects wrapper types through reflection
 * of method return types
 */
@Slf4j
public class WrapperTypeOperationCustomizer implements OperationCustomizer {
    
    private final WrapperTypeRegistry wrapperTypeRegistry;
    private final StaticAnalysisEngine staticAnalysisEngine;

    /**
     * Constructor with specific WrapperTypeRegistry instance
     */
    public WrapperTypeOperationCustomizer(WrapperTypeRegistry wrapperTypeRegistry) {
        this.wrapperTypeRegistry = wrapperTypeRegistry;
        this.staticAnalysisEngine = new StaticAnalysisEngine();
        log.debug("Created WrapperTypeOperationCustomizer with registry");
    }

        @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        
        // log.warn("🔍 FASE1: Analyzing {}.{}", 
        //     method.getDeclaringClass().getSimpleName(), method.getName());
        
        // 🎯 TRACE SPECIFICO PER AuthResponseDTO
        String methodSig = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        if (methodSig.contains("Authentication")) {
            log.warn("🎯 FASE1-AuthResponseDTO: Analyzing {}", methodSig);
        }
        
        // NEW: Static analysis of the method
        OperationAnalysis analysis = staticAnalysisEngine.analyzeMethod(method);
        
        // Detect wrapper types from method signature (existing logic)
        WrapperTypeInfo autoDetected = detectWrapperTypeFromMethod(method);
        if (autoDetected != null) {
            // 🎯 TRACE SPECIFICO PER AuthResponseDTO
            if (autoDetected.dataClassName.contains("AuthResponseDTO")) {
                log.warn("🎯 FASE1-AuthResponseDTO: DETECTED! dataClass={}, wrapperType={}", 
                    autoDetected.dataClassName, autoDetected.wrapperType);
            }
            
            // Register in Registry for FASE 2-3-4-5
            wrapperTypeRegistry.registerDirectWrapperType(
                autoDetected.dataClassName, 
                autoDetected.wrapperType
            );
            
            // ✅ Add x-generic-wrapper and extra hints for client generators
            operation.addExtension("x-generic-wrapper", "ResponseWrapper");
            // Inner type simple name (e.g., ReservationDTO)
            String innerSimple = autoDetected.getDataTypeSimpleName();
            operation.addExtension("x-inner-type", innerSimple);
            // Compute Dart-ish import hints (e.g., reservation_dto, date when needed)
            List<String> imports = new ArrayList<>();
            imports.add(toSnakeCase(innerSimple));
            try {
                Class<?> dtoClass = Class.forName(autoDetected.dataClassName);
                if (dtoRequiresDateImport(dtoClass)) {
                    imports.add("date");
                }
            } catch (ClassNotFoundException e) {
                log.debug("x-imports: class not found for {}: {}", autoDetected.dataClassName, e.getMessage());
            }
            operation.addExtension("x-imports", imports);
            log.debug("✅ Added x-wrapper extensions to operation: {} -> inner={}, imports={}",
                operation.getOperationId() != null ? operation.getOperationId() : methodSig, innerSimple, imports);
            
            // log.warn("✅ FASE1: Registered {} -> {} for {}.{}", 
            //     autoDetected.wrapperType, 
            //     autoDetected.dataClassName,
            //     method.getDeclaringClass().getSimpleName(), 
            //     method.getName());
        } else {
            // 🎯 TRACE SPECIFICO PER AuthResponseDTO
            if (methodSig.contains("Authentication")) {
                log.warn("🎯 FASE1-AuthResponseDTO: NO WRAPPER DETECTED for {}", methodSig);
            }
            // log.warn("❌ FASE1: No wrapper detected for {}.{}", 
            //     method.getDeclaringClass().getSimpleName(), method.getName());
        }
        
        // NEW: Register success responses if analysis was successful
        if (analysis.isAnalysisComplete() && !analysis.getSuccessResponses().isEmpty()) {
            // Registra con methodSignature (sempre disponibile)
            wrapperTypeRegistry.registerSuccessResponses(
                analysis.getMethodSignature(), 
                analysis.getSuccessResponses()
            );
            
            // Registra ANCHE con operationId se disponibile
            String operationId = operation.getOperationId();
            if (operationId != null && !operationId.trim().isEmpty()) {
                wrapperTypeRegistry.registerSuccessResponsesByOperationId(
                    operationId, 
                    analysis.getSuccessResponses()
                );
                log.warn("✅ FASE1: Registered {} success responses for operationId '{}' and methodSignature '{}'", 
                    analysis.getSuccessResponses().size(), operationId, analysis.getMethodSignature());
            } else {
                log.warn("⚠️ FASE1: OperationId not available, registered {} success responses only for methodSignature '{}'", 
                    analysis.getSuccessResponses().size(), analysis.getMethodSignature());
            }
        }
        
        return operation;
    }
    
    /**
     * Analyzes method return type to automatically detect wrapper information
     */
    private WrapperTypeInfo detectWrapperTypeFromMethod(Method method) {
        Type returnType = method.getGenericReturnType();
        
        // Check if return type is ResponseEntity<ResponseWrapper<T>>
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            
            // First level: ResponseEntity<X>
            if (parameterizedType.getRawType().equals(ResponseEntity.class)) {
                Type[] responseEntityArgs = parameterizedType.getActualTypeArguments();
                if (responseEntityArgs.length > 0 && responseEntityArgs[0] instanceof ParameterizedType) {
                    ParameterizedType wrapperType = (ParameterizedType) responseEntityArgs[0];
                    
                    // Second level: ResponseWrapper<Y>
                    if (wrapperType.getRawType().equals(ResponseWrapper.class)) {
                        Type[] wrapperArgs = wrapperType.getActualTypeArguments();
                        if (wrapperArgs.length > 0) {
                            return analyzeWrapperDataType(wrapperArgs[0]);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Analyzes the data type inside ResponseWrapper to determine wrapper type
     */
    private WrapperTypeInfo analyzeWrapperDataType(Type dataType) {
        if (dataType instanceof Class<?>) {
            Class<?> dataClass = (Class<?>) dataType;
            
            // Single object (String, DTO, etc.)
            return new WrapperTypeInfo(dataClass.getName(), "DTO", "Single object response");
            
        } else if (dataType instanceof ParameterizedType) {
            ParameterizedType parameterizedDataType = (ParameterizedType) dataType;
            Class<?> rawType = (Class<?>) parameterizedDataType.getRawType();
            
            if (rawType.equals(List.class)) {
                // List<T> -> LIST wrapper type
                Type[] listArgs = parameterizedDataType.getActualTypeArguments();
                if (listArgs.length > 0 && listArgs[0] instanceof Class<?>) {
                    Class<?> listElementClass = (Class<?>) listArgs[0];
                    return new WrapperTypeInfo(listElementClass.getName(), "LIST", "List response");
                }
                
            } else if (rawType.equals(Page.class)) {
                // Page<T> -> PAGE wrapper type
                Type[] pageArgs = parameterizedDataType.getActualTypeArguments();
                if (pageArgs.length > 0 && pageArgs[0] instanceof Class<?>) {
                    Class<?> pageElementClass = (Class<?>) pageArgs[0];
                    return new WrapperTypeInfo(pageElementClass.getName(), "PAGE", "Paginated response");
                }
            }
        }
        
        return null;
    }

    // =====================
    // Helper methods
    // =====================
    private String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String snake = input
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
            .toLowerCase();
        return snake;
    }

    private boolean dtoRequiresDateImport(Class<?> dtoClass) {
        try {
            // Check declared fields and nested types for java.time / java.util.Date usage
            for (Field f : dtoClass.getDeclaredFields()) {
                Class<?> t = f.getType();
                if (isDateLike(t)) return true;
                // Collections<List/Set] of date-like?
                if (Collection.class.isAssignableFrom(t)) {
                    // Best-effort: we cannot reflect generic at runtime reliably; skip
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isDateLike(Class<?> t) {
        return t != null && (
                t.equals(java.util.Date.class) ||
                t.equals(LocalDate.class) ||
                t.equals(LocalDateTime.class) ||
                t.equals(OffsetDateTime.class) ||
                t.equals(ZonedDateTime.class) ||
                t.equals(Instant.class)
        );
    }
}
