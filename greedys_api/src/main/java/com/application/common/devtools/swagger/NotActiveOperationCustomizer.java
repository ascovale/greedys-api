package com.application.common.devtools.swagger;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.application.common.devtools.annotation.NotActive;

import io.swagger.v3.oas.models.Operation;

/**
 * OpenAPI/Swagger customizer that hides endpoints marked with @NotActive.
 * 
 * This component integrates with SpringDoc to automatically remove disabled
 * endpoints from the API documentation.
 * 
 * Behavior:
 * - Methods annotated with @NotActive are hidden from Swagger
 * - Methods in classes annotated with @NotActive are hidden from Swagger
 * 
 * @author Greedys Development Team
 * @since 1.0
 */
@Component
public class NotActiveOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        // Check method-level annotation
        Method method = handlerMethod.getMethod();
        if (method.isAnnotationPresent(NotActive.class)) {
            return null; // Returning null hides the operation from Swagger
        }
        
        // Check class-level annotation
        Class<?> beanType = handlerMethod.getBeanType();
        if (beanType.isAnnotationPresent(NotActive.class)) {
            return null; // Returning null hides the operation from Swagger
        }
        
        // Add deprecation notice if reason is provided (for partially active controllers)
        Optional.ofNullable(method.getAnnotation(NotActive.class))
                .filter(a -> !a.reason().isEmpty())
                .ifPresent(a -> {
                    String existingDescription = operation.getDescription() != null ? 
                            operation.getDescription() + "\n\n" : "";
                    operation.setDescription(existingDescription + 
                            "**Note:** " + a.reason());
                });
        
        return operation;
    }
}
