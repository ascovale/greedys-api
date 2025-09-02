package com.application.common.spring.swagger.vendor;

import java.util.Map;

/**
 * Definisce una vendor extension da applicare all'OpenAPI spec
 */
public interface VendorExtension {
    
    /**
     * Nome dell'extension (deve iniziare con 'x-')
     */
    String getName();
    
    /**
     * Valore dell'extension
     */
    Object getValue();
    
    /**
     * Livello dove applicare l'extension
     */
    VendorExtensionLevel getLevel();
    
    /**
     * Target specifico per l'extension (operationId, schema name, etc.)
     * Null = applica a tutti gli elementi del livello
     */
    String getTarget();
    
    /**
     * Condizione per applicare l'extension
     * Es: solo per operazioni POST, solo per schemi wrapper, etc.
     */
    default boolean shouldApply(String target, Map<String, Object> context) {
        return true;
    }
    
    /**
     * Priorità di applicazione (maggiore = applicato prima)
     */
    default int getPriority() {
        return 100;
    }
}
