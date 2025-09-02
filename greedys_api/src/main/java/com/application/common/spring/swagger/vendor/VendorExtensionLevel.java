package com.application.common.spring.swagger.vendor;

/**
 * Livelli dove possono essere applicate le vendor extensions (x-*)
 */
public enum VendorExtensionLevel {
    
    /**
     * Extensions applicate a livello globale dell'OpenAPI spec
     * Es: x-api-version, x-server-info
     */
    GLOBAL,
    
    /**
     * Extensions applicate a operazioni specifiche
     * Es: x-rate-limit, x-cache-ttl, x-auth-required
     */
    OPERATION,
    
    /**
     * Extensions applicate a risposte specifiche
     * Es: x-response-wrapper, x-pagination-info
     */
    RESPONSE,
    
    /**
     * Extensions applicate a schemi (DTO, Wrapper, etc.)
     * Es: x-dto-version, x-wrapper-type, x-validation-rules
     */
    SCHEMA,
    
    /**
     * Extensions applicate a componenti generici (security, parameters, etc.)
     * Es: x-security-scope, x-parameter-source
     */
    COMPONENT
}
