package com.application.common.spring.swagger.vendor.extensions;

import java.util.Map;

import com.application.common.spring.swagger.vendor.VendorExtension;
import com.application.common.spring.swagger.vendor.VendorExtensionLevel;

/**
 * Vendor extension che aggiunge informazioni sulla versione dell'API
 */
public class ApiVersionExtension implements VendorExtension {
    
    private final String version;
    private final String buildTime;
    
    public ApiVersionExtension(String version, String buildTime) {
        this.version = version != null ? version : "1.0.0";
        this.buildTime = buildTime != null ? buildTime : "unknown";
    }
    
    @Override
    public String getName() {
        return "x-api-info";
    }
    
    @Override
    public Object getValue() {
        return Map.of(
            "version", version,
            "buildTime", buildTime,
            "generator", "greedys-swagger-generator",
            "documentation", "Auto-generated OpenAPI specification"
        );
    }
    
    @Override
    public VendorExtensionLevel getLevel() {
        return VendorExtensionLevel.GLOBAL;
    }
    
    @Override
    public String getTarget() {
        return null; // Global extension
    }
    
    @Override
    public int getPriority() {
        return 300; // Massima priorità per info globali
    }
}
