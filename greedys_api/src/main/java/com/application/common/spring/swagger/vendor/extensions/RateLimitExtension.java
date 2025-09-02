package com.application.common.spring.swagger.vendor.extensions;

import java.util.Map;

import com.application.common.spring.swagger.vendor.VendorExtension;
import com.application.common.spring.swagger.vendor.VendorExtensionLevel;

/**
 * Vendor extension che aggiunge informazioni sui rate limits alle operazioni
 */
public class RateLimitExtension implements VendorExtension {
    
    private final int requestsPerMinute;
    private final int burstLimit;
    private final String scope;
    
    public RateLimitExtension(int requestsPerMinute, int burstLimit, String scope) {
        this.requestsPerMinute = requestsPerMinute;
        this.burstLimit = burstLimit;
        this.scope = scope != null ? scope : "user";
    }
    
    @Override
    public String getName() {
        return "x-rate-limit";
    }
    
    @Override
    public Object getValue() {
        return Map.of(
            "requestsPerMinute", requestsPerMinute,
            "burstLimit", burstLimit,
            "scope", scope,
            "resetPolicy", "sliding-window"
        );
    }
    
    @Override
    public VendorExtensionLevel getLevel() {
        return VendorExtensionLevel.OPERATION;
    }
    
    @Override
    public String getTarget() {
        return "*"; // Applica a tutte le operazioni, ma con condizione
    }
    
    @Override
    public boolean shouldApply(String target, Map<String, Object> context) {
        // Esempio: applica rate limit solo alle operazioni POST/PUT/DELETE
        Object operation = context.get("operation");
        if (operation != null) {
            String httpMethod = (String) context.get("httpMethod");
            return httpMethod != null && (httpMethod.equals("POST") || httpMethod.equals("PUT") || httpMethod.equals("DELETE"));
        }
        return false;
    }
    
    @Override
    public int getPriority() {
        return 150; // Priorità media
    }
}
