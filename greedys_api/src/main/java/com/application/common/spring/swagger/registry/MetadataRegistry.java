package com.application.common.spring.swagger.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.application.common.spring.swagger.metadata.OperationDataMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry unificato per tutti i metadati raccolti.
 * 
 * Mantiene lo storage principale con dati completi e genera mappe separate
 * per operazioni e schemi per accesso efficiente.
 */
@Component
@Slf4j
public class MetadataRegistry {
    
    /**
     * Storage principale: operationId -> OperationDataMetadata completo
     */
    private final Map<String, OperationDataMetadata> allData = new ConcurrentHashMap<>();
    
    
    
    /**
     * Registra metadati completi di un'operazione
     */
    public void register(OperationDataMetadata metadata) {
        // Genera i riferimenti agli schemi
        String successSchemaRef = metadata.getWrapperCategory().generateSchemaName(
            extractSimpleClassName(metadata.getDataClassName())
        );
        String errorSchemaRef = "ResponseWrapperErrorDetails";
        
        metadata.updateSchemaRefs(
            "#/components/schemas/" + successSchemaRef,
            "#/components/schemas/" + errorSchemaRef
        );
        
        // Memorizza nello storage principale
        allData.put(metadata.getOperationId(), metadata);
        
        log.debug("Registered operation metadata: {}", metadata.getOperationId());
    }
    
    /**
     * Ottiene gli status codes per un'operazione specifica
     */
    public Set<String> getOperationStatusCodes(String operationId) {
        OperationDataMetadata metadata = allData.get(operationId);
        return metadata != null ? metadata.getStatusCodes() : Collections.emptySet();
    }
    
    /**
     * Ottiene tutti gli status codes di tutte le operazioni
     */
    public Map<String, Set<String>> getAllStatusCodes() {
        Map<String, Set<String>> statusCodesMap = new HashMap<>();
        allData.forEach((operationId, metadata) -> {
            if (metadata.getStatusCodes() != null) {
                statusCodesMap.put(operationId, metadata.getStatusCodes());
            }
        });
        return statusCodesMap;
    }
    
    /**
     * Verifica se un'operazione è registrata
     */
    public boolean hasOperation(String operationId) {
        return allData.containsKey(operationId);
    }
    
    /**
     * Ottiene dati completi per operationId (accesso diretto allo storage principale)
     */
    public OperationDataMetadata getCompleteData(String operationId) {
        return allData.get(operationId);
    }
    
    /**
     * Ottiene tutti i dati completi delle operazioni
     */
    public Collection<OperationDataMetadata> getAllCompleteData() {
        return allData.values();
    }
    
    /**
     * Pulisce tutti i metadati (utile per test)
     */
    public void clear() {
        allData.clear();
        log.debug("Cleared all metadata from registry");
    }
    
    /**
     * Statistiche del registry
     */
    public String getStats() {
        return String.format("Total operations: %d", allData.size());
    }
    
    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) return null;
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }
}
