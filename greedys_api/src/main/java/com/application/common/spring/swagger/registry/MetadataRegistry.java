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
     * Storage principale: gruppo -> (operationId -> OperationDataMetadata completo)
     */
    private final Map<String, Map<String, OperationDataMetadata>> groupedData = new ConcurrentHashMap<>();
    
    
    
    /**
     * Registra metadati completi di un'operazione per un gruppo specifico
     */
    public void register(OperationDataMetadata metadata, String group) {
        // Genera i riferimenti agli schemi
        String successSchemaRef = metadata.getWrapperCategory().generateSchemaName(
            extractSimpleClassName(metadata.getDataClassName())
        );
        String errorSchemaRef = "ResponseWrapperErrorDetails";
        
        metadata.updateSchemaRefs(
            "#/components/schemas/" + successSchemaRef,
            "#/components/schemas/" + errorSchemaRef
        );
        
        // Assicura che esista la mappa per il gruppo
        groupedData.computeIfAbsent(group, k -> new ConcurrentHashMap<>());
        
        // Memorizza nello storage del gruppo
        groupedData.get(group).put(metadata.getOperationId(), metadata);
        
        log.debug("Registered operation metadata: {} for group: {}", metadata.getOperationId(), group);
    }
    
    /**
     * Ottiene gli status codes per un'operazione specifica in un gruppo
     */
    public Set<String> getOperationStatusCodes(String operationId, String group) {
        Map<String, OperationDataMetadata> groupData = groupedData.get(group);
        if (groupData == null) return Collections.emptySet();
        
        OperationDataMetadata metadata = groupData.get(operationId);
        return metadata != null ? metadata.getStatusCodes() : Collections.emptySet();
    }
    
    /**
     * Ottiene tutti gli status codes di tutte le operazioni di un gruppo
     */
    public Map<String, Set<String>> getAllStatusCodes(String group) {
        Map<String, Set<String>> statusCodesMap = new HashMap<>();
        Map<String, OperationDataMetadata> groupData = groupedData.get(group);
        if (groupData != null) {
            groupData.forEach((operationId, metadata) -> {
                if (metadata.getStatusCodes() != null) {
                    statusCodesMap.put(operationId, metadata.getStatusCodes());
                }
            });
        }
        return statusCodesMap;
    }
    
    /**
     * Verifica se un'operazione è registrata in un gruppo
     */
    public boolean hasOperation(String operationId, String group) {
        Map<String, OperationDataMetadata> groupData = groupedData.get(group);
        return groupData != null && groupData.containsKey(operationId);
    }
    
    /**
     * Ottiene dati completi per operationId in un gruppo specifico
     */
    public OperationDataMetadata getCompleteData(String operationId, String group) {
        Map<String, OperationDataMetadata> groupData = groupedData.get(group);
        return groupData != null ? groupData.get(operationId) : null;
    }
    
    /**
     * Ottiene tutti i dati completi delle operazioni di un gruppo
     */
    public Collection<OperationDataMetadata> getAllCompleteData(String group) {
        Map<String, OperationDataMetadata> groupData = groupedData.get(group);
        return groupData != null ? groupData.values() : Collections.emptyList();
    }
    
    /**
     * Pulisce tutti i metadati di un gruppo specifico
     */
    public void clearGroup(String group) {
        groupedData.remove(group);
        log.debug("Cleared metadata for group: {}", group);
    }
    
    /**
     * Pulisce tutti i metadati di tutti i gruppi
     */
    public void clear() {
        groupedData.clear();
        log.debug("Cleared all metadata from registry");
    }
    
    /**
     * Statistiche del registry
     */
    public String getStats() {
        int totalOperations = groupedData.values().stream()
            .mapToInt(Map::size)
            .sum();
        return String.format("Total groups: %d, Total operations: %d", 
            groupedData.size(), totalOperations);
    }
    
    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) return null;
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }
}
