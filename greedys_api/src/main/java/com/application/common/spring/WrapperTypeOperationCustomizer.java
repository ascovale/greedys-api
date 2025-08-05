package com.application.common.spring;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.application.common.controller.annotation.WrapperType;

import io.swagger.v3.oas.models.Operation;

/**
 * OperationCustomizer that processes @WrapperType annotations and adds
 * metadata to operations for the WrapperTypeCustomizer to process
 */
@Component
public class WrapperTypeOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        WrapperType wrapperType = method.getAnnotation(WrapperType.class);
        
        if (wrapperType != null) {
            Map<String, Object> wrapperInfo = new HashMap<>();
            wrapperInfo.put("dataClass", wrapperType.dataClass().getName());
            wrapperInfo.put("type", wrapperType.type().name());
            wrapperInfo.put("description", wrapperType.description());
            
            // Add wrapper type information as extension
            operation.addExtension("x-wrapper-type", wrapperInfo);
        }
        
        return operation;
    }
}
