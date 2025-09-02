package com.application.common.spring.swagger.vendor;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Applicatore per vendor extensions ai vari livelli dell'OpenAPI spec.
 * 
 * Fornisce metodi specifici per ogni livello di applicazione:
 * - Global (OpenAPI spec)
 * - Operation (singole operazioni)  
 * - Response (risposte specifiche)
 * - Schema (DTO e wrapper schemas)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VendorExtensionApplicator {
    
    private final VendorExtensionRegistry registry;
    
    // ================================================================================================
    // GLOBAL LEVEL - Applicazione a livello OpenAPI spec
    // ================================================================================================
    
    /**
     * Applica vendor extensions globali all'OpenAPI spec
     */
    public void applyGlobalExtensions(OpenAPI openApi) {
        List<VendorExtension> globalExtensions = registry.getExtensions(VendorExtensionLevel.GLOBAL);
        
        for (VendorExtension extension : globalExtensions) {
            if (extension.shouldApply(null, createGlobalContext(openApi))) {
                openApi.addExtension(extension.getName(), extension.getValue());
                log.debug("Applied global extension '{}' = {}", extension.getName(), extension.getValue());
            }
        }
        
        if (!globalExtensions.isEmpty()) {
            log.info("Applied {} global vendor extensions", globalExtensions.size());
        }
    }
    
    // ================================================================================================
    // OPERATION LEVEL - Applicazione a singole operazioni
    // ================================================================================================
    
    /**
     * Applica vendor extensions a tutte le operazioni
     */
    public void applyOperationExtensions(OpenAPI openApi) {
        if (openApi.getPaths() == null) return;
        
        openApi.getPaths().forEach((pathKey, pathItem) -> {
            pathItem.readOperations().forEach(operation -> {
                if (operation.getOperationId() != null) {
                    applyOperationExtensions(operation, operation.getOperationId(), openApi);
                }
            });
        });
    }
    
    /**
     * Applica vendor extensions a un'operazione specifica
     */
    public void applyOperationExtensions(Operation operation, String operationId, OpenAPI openApi) {
        List<VendorExtension> extensions = registry.getApplicableExtensions(
            VendorExtensionLevel.OPERATION, 
            operationId, 
            createOperationContext(operation, operationId, openApi)
        );
        
        for (VendorExtension extension : extensions) {
            operation.addExtension(extension.getName(), extension.getValue());
            log.debug("Applied operation extension '{}' = {} to {}", extension.getName(), extension.getValue(), operationId);
        }
        
        if (!extensions.isEmpty()) {
            log.debug("Applied {} vendor extensions to operation {}", extensions.size(), operationId);
        }
    }
    
    // ================================================================================================
    // RESPONSE LEVEL - Applicazione a risposte specifiche
    // ================================================================================================
    
    /**
     * Applica vendor extensions a una risposta specifica
     */
    public void applyResponseExtensions(ApiResponse response, String operationId, String statusCode, OpenAPI openApi) {
        List<VendorExtension> extensions = registry.getApplicableExtensions(
            VendorExtensionLevel.RESPONSE,
            operationId + ":" + statusCode,
            createResponseContext(response, operationId, statusCode, openApi)
        );
        
        for (VendorExtension extension : extensions) {
            response.addExtension(extension.getName(), extension.getValue());
            log.debug("Applied response extension '{}' = {} to {}:{}", extension.getName(), extension.getValue(), operationId, statusCode);
        }
        
        if (!extensions.isEmpty()) {
            log.debug("Applied {} vendor extensions to response {}:{}", extensions.size(), operationId, statusCode);
        }
    }
    
    // ================================================================================================
    // SCHEMA LEVEL - Applicazione a schemi DTO e wrapper
    // ================================================================================================
    
    /**
     * Applica vendor extensions a uno schema specifico
     */
    @SuppressWarnings("rawtypes")
    public void applySchemaExtensions(Schema schema, String schemaName, OpenAPI openApi) {
        List<VendorExtension> extensions = registry.getApplicableExtensions(
            VendorExtensionLevel.SCHEMA,
            schemaName,
            createSchemaContext(schema, schemaName, openApi)
        );
        
        for (VendorExtension extension : extensions) {
            schema.addExtension(extension.getName(), extension.getValue());
            log.debug("Applied schema extension '{}' = {} to {}", extension.getName(), extension.getValue(), schemaName);
        }
        
        if (!extensions.isEmpty()) {
            log.debug("Applied {} vendor extensions to schema {}", extensions.size(), schemaName);
        }
    }
    
    /**
     * Applica vendor extensions a tutti gli schemi wrapper
     */
    public void applyWrapperSchemaExtensions(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        
        openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
            if (isWrapperSchema(schemaName)) {
                applySchemaExtensions(schema, schemaName, openApi);
            }
        });
    }
    
    // ================================================================================================
    // CONTEXT BUILDERS - Creano contesto per le condizioni delle extensions
    // ================================================================================================
    
    private Map<String, Object> createGlobalContext(OpenAPI openApi) {
        return Map.of(
            "openApi", openApi,
            "schemaCount", openApi.getComponents() != null && openApi.getComponents().getSchemas() != null ? 
                          openApi.getComponents().getSchemas().size() : 0
        );
    }
    
    private Map<String, Object> createOperationContext(Operation operation, String operationId, OpenAPI openApi) {
        return Map.of(
            "operation", operation,
            "operationId", operationId,
            "httpMethod", extractHttpMethodFromOperation(operation),
            "openApi", openApi
        );
    }
    
    private Map<String, Object> createResponseContext(ApiResponse response, String operationId, String statusCode, OpenAPI openApi) {
        return Map.of(
            "response", response,
            "operationId", operationId,
            "statusCode", statusCode,
            "openApi", openApi,
            "isSuccessResponse", statusCode.startsWith("2"),
            "isErrorResponse", !statusCode.startsWith("2")
        );
    }
    
    @SuppressWarnings("rawtypes")
    private Map<String, Object> createSchemaContext(Schema schema, String schemaName, OpenAPI openApi) {
        return Map.of(
            "schema", schema,
            "schemaName", schemaName,
            "openApi", openApi,
            "isWrapperSchema", isWrapperSchema(schemaName),
            "isDtoSchema", isDtoSchema(schemaName)
        );
    }
    
    // ================================================================================================
    // UTILITY METHODS
    // ================================================================================================
    
    private String extractHttpMethodFromOperation(Operation operation) {
        // Logica per estrarre HTTP method dall'operation
        // Potrebbe essere migliorato basandosi sul context dell'operation
        return "UNKNOWN";
    }
    
    private boolean isWrapperSchema(String schemaName) {
        return schemaName != null && schemaName.startsWith("ResponseWrapper");
    }
    
    private boolean isDtoSchema(String schemaName) {
        return schemaName != null && schemaName.endsWith("DTO");
    }
}
