package com.application.common.spring.swagger.vendor.builder;

import java.util.HashMap;
import java.util.Map;

import com.application.common.spring.swagger.vendor.VendorExtension;
import com.application.common.spring.swagger.vendor.VendorExtensionLevel;
import com.application.common.spring.swagger.vendor.extensions.SimpleVendorExtension;

/**
 * Builder fluente per creare vendor extensions facilmente
 */
public class VendorExtensionBuilder {
    
    private String name;
    private Object value;
    private VendorExtensionLevel level;
    private String target;
    private int priority = 100;
    
    public static VendorExtensionBuilder create(String name) {
        VendorExtensionBuilder builder = new VendorExtensionBuilder();
        builder.name = name;
        return builder;
    }
    
    public VendorExtensionBuilder value(Object value) {
        this.value = value;
        return this;
    }
    
    public VendorExtensionBuilder mapValue() {
        this.value = new HashMap<String, Object>();
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public VendorExtensionBuilder addToMap(String key, Object mapValue) {
        if (this.value instanceof Map) {
            ((Map<String, Object>) this.value).put(key, mapValue);
        }
        return this;
    }
    
    public VendorExtensionBuilder global() {
        this.level = VendorExtensionLevel.GLOBAL;
        this.target = null;
        return this;
    }
    
    public VendorExtensionBuilder operation(String operationId) {
        this.level = VendorExtensionLevel.OPERATION;
        this.target = operationId;
        return this;
    }
    
    public VendorExtensionBuilder allOperations() {
        this.level = VendorExtensionLevel.OPERATION;
        this.target = "*";
        return this;
    }
    
    public VendorExtensionBuilder response(String operationId, String statusCode) {
        this.level = VendorExtensionLevel.RESPONSE;
        this.target = operationId + ":" + statusCode;
        return this;
    }
    
    public VendorExtensionBuilder schema(String schemaName) {
        this.level = VendorExtensionLevel.SCHEMA;
        this.target = schemaName;
        return this;
    }
    
    public VendorExtensionBuilder allSchemas() {
        this.level = VendorExtensionLevel.SCHEMA;
        this.target = "*";
        return this;
    }
    
    public VendorExtensionBuilder wrapperSchemas() {
        return allSchemas(); // La condizione sarà nel VendorExtension
    }
    
    public VendorExtensionBuilder dtoSchemas() {
        return allSchemas(); // La condizione sarà nel VendorExtension
    }
    
    public VendorExtensionBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }
    
    public VendorExtensionBuilder highPriority() {
        this.priority = 300;
        return this;
    }
    
    public VendorExtensionBuilder lowPriority() {
        this.priority = 50;
        return this;
    }
    
    public VendorExtension build() {
        if (name == null || !name.startsWith("x-")) {
            throw new IllegalArgumentException("Vendor extension name must start with 'x-'");
        }
        if (level == null) {
            throw new IllegalArgumentException("Vendor extension level must be specified");
        }
        
        return SimpleVendorExtension.builder()
                .name(name)
                .value(value)
                .level(level)
                .target(target)
                .priority(priority)
                .build();
    }
    
    // Factory methods per casi comuni
    
    public static VendorExtension globalInfo(String key, Object value) {
        return create("x-" + key)
                .value(value)
                .global()
                .build();
    }
    
    public static VendorExtension operationFeature(String feature, Object config, String operationId) {
        return create("x-" + feature)
                .value(config)
                .operation(operationId)
                .build();
    }
    
    public static VendorExtension wrapperInfo(String info, Object value) {
        return create("x-wrapper-" + info)
                .value(value)
                .wrapperSchemas()
                .highPriority()
                .build();
    }
}
