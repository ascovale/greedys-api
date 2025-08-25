package com.application.common.spring.swagger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.application.common.web.ResponseWrapper;

import io.swagger.v3.oas.models.Operation;

/**
 * OperationCustomizer that automatically detects wrapper types through reflection
 * of method return types
 */
@Component
public class WrapperTypeOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        
        // Use reflection to automatically detect wrapper type
        WrapperTypeInfo autoDetected = detectWrapperTypeFromMethod(method);
        if (autoDetected != null) {
            Map<String, Object> wrapperInfo = new HashMap<>();
            wrapperInfo.put("dataClass", autoDetected.dataClass);
            wrapperInfo.put("type", autoDetected.wrapperType);
            wrapperInfo.put("responseCode", autoDetected.responseCode);
            wrapperInfo.put("description", "Auto-detected from method signature");
            
            // Add wrapper type information as extension
            operation.addExtension("x-wrapper-type", wrapperInfo);
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
                                // Determine response code based on method annotations and name
                                String responseCode = determineResponseCode(method);
                                return new WrapperTypeInfo(typeInfo.dataClass, typeInfo.wrapperType, responseCode);
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Determines the appropriate HTTP response code based on method characteristics
     */
    private String determineResponseCode(Method method) {
        // Check for @CreateApiResponses annotation (indicates 201 Created)
        // Use safe annotation check to handle cases where annotation might be removed
        if (hasCreateApiResponsesAnnotation(method)) {
            return "201";
        }
        
        // Check if method calls executeCreate (indicates CREATE operation)
        if (methodCallsExecuteCreate(method)) {
            return "201";
        }
        
        // Default to 200 OK for all other operations
        return "200";
    }
    
    /**
     * Safely checks for @CreateApiResponses annotation without causing ClassNotFoundException
     */
    private boolean hasCreateApiResponsesAnnotation(Method method) {
        try {
            Class<?> createApiResponsesClass = Class.forName("com.application.common.controller.annotation.CreateApiResponses");
            return method.isAnnotationPresent(createApiResponsesClass.asSubclass(java.lang.annotation.Annotation.class));
        } catch (ClassNotFoundException e) {
            // Annotation class doesn't exist, skip this check
            return false;
        } catch (Exception e) {
            // Any other reflection error, skip this check
            return false;
        }
    }
    
    /**
     * Analyzes method bytecode or source to determine if it calls executeCreate
     */
    private boolean methodCallsExecuteCreate(Method method) {
        try {
            // Get the method's declaring class
            Class<?> declaringClass = method.getDeclaringClass();
            
            // Use reflection to inspect the method body through bytecode analysis
            // We'll look for executeCreate method calls in the bytecode
            java.io.InputStream classStream = declaringClass.getClassLoader()
                .getResourceAsStream(declaringClass.getName().replace('.', '/') + ".class");
            
            if (classStream != null) {
                // Simple bytecode analysis - look for executeCreate method calls
                byte[] bytecode = classStream.readAllBytes();
                String bytecodeString = new String(bytecode, java.nio.charset.StandardCharsets.ISO_8859_1);
                
                // Look for executeCreate string in bytecode (method name appears in constant pool)
                boolean hasExecuteCreate = bytecodeString.contains("executeCreate");
                
                classStream.close();
                return hasExecuteCreate;
            }
            
        } catch (Exception e) {
            // If bytecode analysis fails, fallback to annotation-based detection
            System.err.println("Bytecode analysis failed for " + method.getName() + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Analyzes the data type inside ResponseWrapper to determine wrapper type
     */
    private WrapperTypeInfo analyzeWrapperDataType(Type dataType) {
        if (dataType instanceof Class<?>) {
            Class<?> dataClass = (Class<?>) dataType;
            
            // Single object (String, DTO, etc.)
            return new WrapperTypeInfo(dataClass.getName(), "DTO");
            
        } else if (dataType instanceof ParameterizedType) {
            ParameterizedType parameterizedDataType = (ParameterizedType) dataType;
            Class<?> rawType = (Class<?>) parameterizedDataType.getRawType();
            
            if (rawType.equals(List.class)) {
                // List<T> -> LIST wrapper type
                Type[] listArgs = parameterizedDataType.getActualTypeArguments();
                if (listArgs.length > 0 && listArgs[0] instanceof Class<?>) {
                    Class<?> listElementClass = (Class<?>) listArgs[0];
                    return new WrapperTypeInfo(listElementClass.getName(), "LIST");
                }
                
            } else if (rawType.equals(Page.class)) {
                // Page<T> -> PAGE wrapper type
                Type[] pageArgs = parameterizedDataType.getActualTypeArguments();
                if (pageArgs.length > 0 && pageArgs[0] instanceof Class<?>) {
                    Class<?> pageElementClass = (Class<?>) pageArgs[0];
                    return new WrapperTypeInfo(pageElementClass.getName(), "PAGE");
                }
            }
        }
        
        return null;
    }
    
    /**
     * Data class to hold wrapper type information
     */
    private static class WrapperTypeInfo {
        final String dataClass;
        final String wrapperType;
        final String responseCode;
        
        WrapperTypeInfo(String dataClass, String wrapperType) {
            this.dataClass = dataClass;
            this.wrapperType = wrapperType;
            this.responseCode = "200"; // default
        }
        
        WrapperTypeInfo(String dataClass, String wrapperType, String responseCode) {
            this.dataClass = dataClass;
            this.wrapperType = wrapperType;
            this.responseCode = responseCode;
        }
    }
}
