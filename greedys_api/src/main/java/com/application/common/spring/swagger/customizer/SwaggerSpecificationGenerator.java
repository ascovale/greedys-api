package com.application.common.spring.swagger.customizer;

import java.util.Collection;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.application.common.spring.swagger.generator.DefaultSchemaGenerator;
import com.application.common.spring.swagger.generator.DtoSchemaGenerator;
import com.application.common.spring.swagger.generator.OperationResponseUpdater;
import com.application.common.spring.swagger.generator.WrapperSchemaGenerator;
import com.application.common.spring.swagger.metadata.OperationDataMetadata;
import com.application.common.spring.swagger.metadata.WrapperCategory;
import com.application.common.spring.swagger.registry.MetadataRegistry;
import com.application.common.spring.swagger.util.VendorExtensionsHelper;
// ...existing imports...
import com.application.common.spring.swagger.utility.SchemaTypeAnalyzer;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generatore coordinatore per schemi e aggiornamento operazioni OpenAPI.
 * 
 * Usa la nuova architettura modulare con generatori specializzati:
 * - DefaultSchemaGenerator: schemi base (metadata, errori)
 * - DtoSchemaGenerator: schemi DTO usando ModelConverters
 * - WrapperSchemaGenerator: schemi wrapper (ResponseWrapperXXX) 
 * - OperationResponseUpdater: aggiornamento responses delle operazioni
 */
@Component
@Order(1000)  // Dopo MetadataCollector (500)
@RequiredArgsConstructor
@Slf4j
public class SwaggerSpecificationGenerator implements OpenApiCustomizer {
    
    private final MetadataRegistry registry;
    
    @Override
    public void customise(OpenAPI openApi) {
        try {
            // 1. Determina il gruppo corrente dall'OpenAPI
            String currentGroup = determineGroupName(openApi);
            
            // 2. Ottieni solo i metadati delle operazioni del gruppo corrente
            Collection<OperationDataMetadata> groupOperations = registry.getAllCompleteData(currentGroup);
            
            if (groupOperations.isEmpty()) {
                log.warn("No operation metadata found in registry for group: {} - schema generation skipped", currentGroup);
                return;
            }
            
            log.info("Generating schemas for {} operations in group: {}", groupOperations.size(), currentGroup);
            
            // 3. Analizza i tipi di schemi da generare (solo per il gruppo corrente)
            SchemaTypeAnalyzer.SchemaAnalysisResult schemaAnalysis = 
                SchemaTypeAnalyzer.analyzeSchemaTypes(groupOperations);
            
            log.info("Schema analysis for group {}: {}", currentGroup, schemaAnalysis.getSummary());
            
            // 4. Genera schemi comuni (errori, metadata)
            DefaultSchemaGenerator.generateCommonSchemas(openApi);
            
            // 5. Genera schemi DTO dai ModelConverters
            for (String dtoClassName : schemaAnalysis.getDtoSchemasToExtract()) {
                if (dtoClassName != null && !dtoClassName.trim().isEmpty()) {
                    DtoSchemaGenerator.generateDataSchema(dtoClassName, openApi);
                } else {
                    log.warn("Skipping null or empty DTO class name in schema generation");
                }
            }
            
            // 6. Genera schemi wrapper (ResponseWrapperXXX) - con informazioni sui tipi e categorie
            Map<String, String> wrapperToClassMapping = schemaAnalysis.getWrapperToDataClassMapping();
            Map<String, WrapperCategory> wrapperToCategory = schemaAnalysis.getWrapperToCategoryMapping();
            for (String wrapperSchemaName : schemaAnalysis.getWrapperSchemasToGenerate()) {
                String dataClassName = wrapperToClassMapping.get(wrapperSchemaName);
                // Prende la categoria dalla mappa calcolata; fallback a deduzione dal nome se assente
                WrapperCategory category = wrapperToCategory.get(wrapperSchemaName);
                WrapperSchemaGenerator.generateWrapperSchema(wrapperSchemaName, dataClassName, category, openApi);
            }

            // 7. Genera catalogo JSON dei ResponseWrapper prodotti (wrapper -> T, categoria) per il gruppo corrente
            com.application.common.spring.swagger.catalog.ResponseWrapperCatalogRegistry.generateAndSave(
                wrapperToClassMapping, wrapperToCategory, schemaAnalysis.getWrapperSchemasToGenerate(), currentGroup
            );
            
            // 8. Aggiorna le operazioni con i nuovi schemi (solo quelle del gruppo corrente)
            updateOperations(groupOperations, openApi);
            
            log.info("Schema generation completed successfully for group: {} - {} total schemas", 
                currentGroup, openApi.getComponents().getSchemas().size());
            
        } catch (Exception e) {
            log.error("Error during OpenAPI customization: {}", e.getMessage(), e);
        }
    }
    
  
    
    /**
     * Aggiorna le operazioni con i riferimenti agli schemi generati
     */
    private void updateOperations(Collection<OperationDataMetadata> allOperations, OpenAPI openApi) {
        allOperations.forEach(operationMetadata -> {
            String operationId = operationMetadata.getOperationId();
            
            // Trova l'operazione corrispondente nell'OpenAPI spec
            openApi.getPaths().forEach((pathKey, pathItem) -> {
                pathItem.readOperations().forEach(operation -> {
                    if (operationId.equals(operation.getOperationId())) {
                        OperationResponseUpdater.updateOperationResponses(operation, operationMetadata);
                        
                        // Aggiungi vendor extensions centralizzate per l'operazione
                        VendorExtensionsHelper.addOperationVendorExtensions(operation, operationMetadata);
                        
                        log.debug("Updated operation responses: {}", operationId);
                    }
                });
            });
        });
    }
    
    /**
     * Determina il nome del gruppo corrente dall'OpenAPI spec
     */
    private String determineGroupName(OpenAPI openApi) {
        // Cerchiamo di dedurlo dai path presenti nell'OpenAPI
        if (openApi.getPaths() != null && !openApi.getPaths().isEmpty()) {
            for (String path : openApi.getPaths().keySet()) {
                if (path.startsWith("/admin/")) return "admin";
                if (path.startsWith("/customer/")) return "customer";
                if (path.startsWith("/restaurant/")) return "restaurant";
            }
        }
        
        // Fallback
        log.warn("Could not determine group name, using 'unknown'");
        return "unknown";
    }
  
}
