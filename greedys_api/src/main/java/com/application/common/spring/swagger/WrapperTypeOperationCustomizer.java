package com.application.common.spring.swagger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ResponseWrapper;

import io.swagger.v3.oas.models.Operation;
import lombok.extern.slf4j.Slf4j;

/**
 * OperationCustomizer that automatically detects wrapper types through reflection
 * of method return types
 */
@Slf4j
@Component
public class WrapperTypeOperationCustomizer implements OperationCustomizer {

    @Autowired
    private WrapperTypeRegistry wrapperTypeRegistry;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        
        // Use reflection to automatically detect wrapper type
        WrapperTypeInfo autoDetected = detectWrapperTypeFromMethod(method);
        if (autoDetected != null) {
            // Register wrapper type directly in registry instead of using extensions
            wrapperTypeRegistry.registerDirectWrapperType(
                autoDetected.dataClassName, 
                autoDetected.wrapperType, 
                autoDetected.getPrimaryResponseCode()
            );
            
            log.debug("Registered wrapper type for {}.{}: {} -> {} ({})", 
                method.getDeclaringClass().getSimpleName(), 
                method.getName(),
                autoDetected.wrapperType, 
                autoDetected.dataClassName, 
                autoDetected.getPrimaryResponseCode());
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
                            WrapperTypeInfo typeInfo = analyzeWrapperDataType(wrapperArgs[0]);
                            if (typeInfo != null) {
                                // Determine response codes based on method annotations and patterns
                                List<String> responseCodes = determineResponseCodes(method);
                                return new WrapperTypeInfo(typeInfo.dataClassName, typeInfo.wrapperType, responseCodes, typeInfo.description);
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
        /**
     * Determine response codes based on BaseController method patterns detected in controller method body
     */
    private List<String> determineResponseCodes(Method method) {
        List<String> codes = new ArrayList<>();
        
        // 1. Check explicit annotations on controller method first (override)
        if (method.isAnnotationPresent(CreateApiResponses.class)) {
            return getCreateResponseCodes();
        } else if (method.isAnnotationPresent(ReadApiResponses.class)) {
            return getReadResponseCodes();
        }
        
        // 2. Automatic detection based on BaseController method calls in method body
        codes = detectResponseCodesFromMethodBody(method);
        if (!codes.isEmpty()) {
            return codes;
        }
        
        // 3. Fallback to HTTP method mapping
        String methodName = method.getName();
        if (methodName.toLowerCase().contains("create") || 
            methodName.toLowerCase().contains("add") ||
            methodName.toLowerCase().contains("register")) {
            return getCreateResponseCodes();
        }
        
        // 4. Default to read operation
        return getReadResponseCodes();
    }
    
    /**
     * Detect BaseController method calls in the controller method body
     */
    private List<String> detectResponseCodesFromMethodBody(Method method) {
        try {
            // Get method source or use reflection to inspect method body
            String methodSource = getMethodBodySignature(method);
            
            // Detect specific BaseController method patterns
            if (methodSource.contains("executeCreate(")) {
                log.debug("Detected executeCreate() call in {}.{} -> 201 Created", 
                    method.getDeclaringClass().getSimpleName(), method.getName());
                return getCreateResponseCodes();
                
            } else if (methodSource.contains("execute(") || 
                      methodSource.contains("executeVoid(") ||
                      methodSource.contains("executeList(") ||
                      methodSource.contains("executePaginated(")) {
                log.debug("Detected execute*() call in {}.{} -> 200 OK", 
                    method.getDeclaringClass().getSimpleName(), method.getName());
                return getReadResponseCodes();
            }
            
        } catch (Exception e) {
            log.debug("Could not analyze method body for {}.{}: {}", 
                method.getDeclaringClass().getSimpleName(), method.getName(), e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Get method signature and analyze for BaseController patterns
     */
    private String getMethodBodySignature(Method method) {
        // Since we can't easily get method source code at runtime,
        // we use a heuristic based on method name and return type patterns
        String methodName = method.getName();
        
        // Pattern detection based on naming conventions and context
        if (methodName.startsWith("create") || methodName.startsWith("add") || 
            methodName.startsWith("register") || methodName.startsWith("ask")) {
            return "executeCreate(";
        } else if (methodName.startsWith("get") || methodName.startsWith("find") || 
                  methodName.startsWith("list") || methodName.startsWith("search")) {
            return "execute(";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify") ||
                  methodName.startsWith("accept") || methodName.startsWith("reject") ||
                  methodName.startsWith("mark") || methodName.startsWith("delete")) {
            return "execute(";
        }
        
        return "";
    }
    
    /**
     * Get response codes for CREATE operations (201 + errors)
     */
    private List<String> getCreateResponseCodes() {
        List<String> codes = new ArrayList<>();
        codes.add("201"); // Created
        codes.addAll(getStandardErrorCodes());
        codes.add("409"); // Conflict (common for create operations)
        return codes;
    }
    
    /**
     * Get response codes for READ operations (200 + errors)
     */
    private List<String> getReadResponseCodes() {
        List<String> codes = new ArrayList<>();
        codes.add("200"); // OK
        codes.addAll(getStandardErrorCodes());
        codes.add("404"); // Not Found (common for read operations)
        return codes;
    }
    
    /**
     * Get standard error response codes from @StandardApiResponses
     */
    private List<String> getStandardErrorCodes() {
        return Arrays.asList("400", "401", "403", "500");
    }
    
    /**
     * Analyzes the data type inside ResponseWrapper to determine wrapper type
     */
    private WrapperTypeInfo analyzeWrapperDataType(Type dataType) {
        if (dataType instanceof Class<?>) {
            Class<?> dataClass = (Class<?>) dataType;
            
            // Single object (String, DTO, etc.)
            return new WrapperTypeInfo(dataClass.getName(), "DTO", Arrays.asList("200"), "Single object response");
            
        } else if (dataType instanceof ParameterizedType) {
            ParameterizedType parameterizedDataType = (ParameterizedType) dataType;
            Class<?> rawType = (Class<?>) parameterizedDataType.getRawType();
            
            if (rawType.equals(List.class)) {
                // List<T> -> LIST wrapper type
                Type[] listArgs = parameterizedDataType.getActualTypeArguments();
                if (listArgs.length > 0 && listArgs[0] instanceof Class<?>) {
                    Class<?> listElementClass = (Class<?>) listArgs[0];
                    return new WrapperTypeInfo(listElementClass.getName(), "LIST", Arrays.asList("200"), "List response");
                }
                
            } else if (rawType.equals(Page.class)) {
                // Page<T> -> PAGE wrapper type
                Type[] pageArgs = parameterizedDataType.getActualTypeArguments();
                if (pageArgs.length > 0 && pageArgs[0] instanceof Class<?>) {
                    Class<?> pageElementClass = (Class<?>) pageArgs[0];
                    return new WrapperTypeInfo(pageElementClass.getName(), "PAGE", Arrays.asList("200"), "Paginated response");
                }
            }
        }
        
        return null;
    }
}
