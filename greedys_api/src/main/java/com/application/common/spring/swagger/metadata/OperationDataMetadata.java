package com.application.common.spring.swagger.metadata;

import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * Metadati completi raccolti da un'operazione controller.
 * 
 * Contiene sia informazioni HTTP che dati per generazione schemi.
 * Il MetadataRegistry si occuperà di organizzare questi dati internamente.
 */
@Data
@Builder
public class OperationDataMetadata {
    
    // === DATI OPERAZIONE HTTP ===
    private final String operationId;
    private final String httpMethod;
    private final String path;
    private final String controllerClass;
    private final String methodName;
    private final String description;
    private final Set<String> statusCodes; // es: ["200", "201", "400"]
    
    // === DATI GENERAZIONE SCHEMA ===
    private final String dataClassName;
    private final WrapperCategory wrapperCategory;
    
    // === RIFERIMENTI SCHEMA (generati dal registry) ===
    private String successSchemaRef;
    private String errorSchemaRef;
    
    /**
     * Aggiorna i riferimenti agli schemi (chiamato dal registry)
     */
    public void updateSchemaRefs(String successRef, String errorRef) {
        this.successSchemaRef = successRef;
        this.errorSchemaRef = errorRef;
    }
}
