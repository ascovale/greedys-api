package com.application.common.spring.swagger.vendor.extensions;

import com.application.common.spring.swagger.vendor.VendorExtension;
import com.application.common.spring.swagger.vendor.VendorExtensionLevel;

import lombok.Builder;
import lombok.Data;

/**
 * Implementazione base per vendor extensions semplici
 */
@Data
@Builder
public class SimpleVendorExtension implements VendorExtension {
    
    private final String name;
    private final Object value;
    private final VendorExtensionLevel level;
    private final String target;
    private final int priority;
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Object getValue() {
        return value;
    }
    
    @Override
    public VendorExtensionLevel getLevel() {
        return level;
    }
    
    @Override
    public String getTarget() {
        return target;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    // Factory methods per facilità d'uso
    
    public static VendorExtension global(String name, Object value) {
        return SimpleVendorExtension.builder()
                .name(name)
                .value(value)
                .level(VendorExtensionLevel.GLOBAL)
                .priority(100)
                .build();
    }
    
    public static VendorExtension operation(String name, Object value, String operationId) {
        return SimpleVendorExtension.builder()
                .name(name)
                .value(value)
                .level(VendorExtensionLevel.OPERATION)
                .target(operationId)
                .priority(100)
                .build();
    }
    
    public static VendorExtension response(String name, Object value, String operationId, String statusCode) {
        return SimpleVendorExtension.builder()
                .name(name)
                .value(value)
                .level(VendorExtensionLevel.RESPONSE)
                .target(operationId + ":" + statusCode)
                .priority(100)
                .build();
    }
    
    public static VendorExtension schema(String name, Object value, String schemaName) {
        return SimpleVendorExtension.builder()
                .name(name)
                .value(value)
                .level(VendorExtensionLevel.SCHEMA)
                .target(schemaName)
                .priority(100)
                .build();
    }
}
