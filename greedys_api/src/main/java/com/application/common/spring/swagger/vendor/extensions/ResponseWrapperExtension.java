package com.application.common.spring.swagger.vendor.extensions;

import java.util.Map;

import com.application.common.spring.swagger.vendor.VendorExtension;
import com.application.common.spring.swagger.vendor.VendorExtensionLevel;

/**
 * Vendor extension che marca gli schemi wrapper con metadati specifici
 */
public class ResponseWrapperExtension implements VendorExtension {
    
    private final boolean isWrapperSchema;
    private final String wrapperType;
    
    public ResponseWrapperExtension() {
        this.isWrapperSchema = true;
        this.wrapperType = "response-wrapper";
    }
    
    @Override
    public String getName() {
        return "x-response-wrapper";
    }
    
    @Override
    public Object getValue() {
        return Map.of(
            "isWrapper", isWrapperSchema,
            "type", wrapperType,
            "generatedBy", "greedys-api-generator"
        );
    }
    
    @Override
    public VendorExtensionLevel getLevel() {
        return VendorExtensionLevel.SCHEMA;
    }
    
    @Override
    public String getTarget() {
        return "*"; // Applica a tutti gli schemi, ma con condizione
    }
    
    @Override
    public boolean shouldApply(String target, Map<String, Object> context) {
        // Applica solo agli schemi che iniziano con "ResponseWrapper"
        return target != null && target.startsWith("ResponseWrapper");
    }
    
    @Override
    public int getPriority() {
        return 200; // Alta priorità per wrapper schemas
    }
}
